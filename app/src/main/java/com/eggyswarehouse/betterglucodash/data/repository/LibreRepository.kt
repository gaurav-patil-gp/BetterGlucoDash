package com.eggyswarehouse.betterglucodash.data.repository

import android.util.Log
import com.eggyswarehouse.betterglucodash.data.local.AuthManager
import com.eggyswarehouse.betterglucodash.data.local.LIBRE_TIMESTAMP_FORMATTER
import com.eggyswarehouse.betterglucodash.data.local.db.GlucoseDao
import com.eggyswarehouse.betterglucodash.data.local.db.GlucoseReadingEntity
import com.eggyswarehouse.betterglucodash.data.network.BaseUrlHolder
import com.eggyswarehouse.betterglucodash.data.network.LibreApiService
import com.eggyswarehouse.betterglucodash.data.network.LoginRequest
import com.eggyswarehouse.betterglucodash.data.network.Region
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException

/**
 * Single source of truth for all LibreLinkUp data operations.
 *
 * Responsibilities:
 *  - **Authentication:** Calls [login], persists the JWT + account-id via [AuthManager],
 *    and sets the correct regional endpoint on [BaseUrlHolder] before hitting the network.
 *  - **Glucose polling:** [glucoseFlow] emits a [GlucoseFlowState] every 5 minutes,
 *    matching the Libre 3 sensor's reporting interval. The flow handles transient network
 *    errors silently and surfaces [GlucoseFlowState.SessionExpired] on HTTP 401 so the
 *    UI can navigate back to the Login screen automatically.
 *
 * This class contains no UI logic and no Android framework dependencies beyond [Log].
 */
class LibreRepository(
    private val api: LibreApiService,
    private val authManager: AuthManager,
    private val baseUrlHolder: BaseUrlHolder,
    private val glucoseDao: GlucoseDao
) {
    companion object {
        private const val TAG = "LibreRepository"

        /** Matches the Libre 3 CGM sensor's 5-minute reading interval. */
        private const val POLL_INTERVAL_MS = 300_000L

        /** Keep up to 90 days of data in the local DB. */
        private const val RETENTION_MS = 90L * 24L * 60L * 60L * 1000L
    }

    /** Signal to trigger an immediate glucose poll and reset the timer. */
    private val refreshSignal =
        MutableSharedFlow<Unit>(replay = 1).apply {
            tryEmit(Unit)
        }

    fun refresh() {
        refreshSignal.tryEmit(Unit)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Authentication
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Authenticates with the LibreLinkUp API and persists the resulting JWT.
     *
     * Flow:
     *  1. Sets the regional API endpoint on [BaseUrlHolder] based on [region].
     *  2. POSTs credentials to `/llu/auth/login`.
     *  3. On success: saves token, account-id (SHA-256 of userId), and region.
     *  4. Fetches `/llu/connections` to resolve and save the [patientId] needed for polling.
     *
     * @param email    LibreLinkUp account email.
     * @param pass     LibreLinkUp account password.
     * @param region   User-selected region: "US" or "CA".
     * @return [Result.success] on full authentication + connection resolution,
     *         [Result.failure] with a descriptive message otherwise.
     */
    suspend fun login(email: String, pass: String, region: String): Result<Unit> {
        return try {
            val r = Region.fromCode(region)
            baseUrlHolder.host = r.apiHost
            Log.d(TAG, "login → region=${r.code}, host=${r.apiHost}")

            val res = api.login(LoginRequest(email, pass))
            Log.d(TAG, "login → response status=${res.status}, data=${res.data}")

            if (res.status != 0) {
                val msg = "Login rejected by server (status=${res.status}). Check credentials."
                Log.e(TAG, msg)
                return Result.failure(Exception(msg))
            }

            val data = res.data
            if (data?.redirect == true) {
                val redirectRegion = data.region?.uppercase() ?: "unknown"
                val msg = "Region mismatch: server redirected to '$redirectRegion'. Ensure you are using the US/CA endpoint."
                Log.e(TAG, msg)
                return Result.failure(Exception(msg))
            }

            val token = data?.authTicket?.token
            val userId = data?.user?.id
            Log.d(
                TAG,
                "login → token=${if (token != null) {
                    "present (${token.take(
                        10
                    )}...)"
                } else {
                    "NULL"
                }}, userId=$userId"
            )

            if (!token.isNullOrEmpty()) {
                // User identity isolation: wipe stale glucose data before persisting
                // credentials for a different account. Same user re-login preserves data.
                val newAccountId = sha256(userId.orEmpty())
                val existingAccountId = authManager.cachedAccountId
                if (!existingAccountId.isNullOrEmpty() && existingAccountId != newAccountId) {
                    Log.i(TAG, "login → different account detected, wiping glucose DB")
                    glucoseDao.deleteAll()
                }

                authManager.saveToken(token)
                authManager.saveRegion(region)
                Log.d(TAG, "login → token and region saved")

                if (!userId.isNullOrEmpty()) {
                    authManager.saveAccountId(newAccountId)
                    Log.d(TAG, "login → account-id saved")
                } else {
                    Log.w(TAG, "login → userId missing — account-id NOT saved")
                }

                Log.d(TAG, "login → fetching connections…")
                val connRes = api.getConnections()
                Log.d(
                    TAG,
                    "login → connections status=${connRes.status}, count=${connRes.data?.size}"
                )

                val patientId = connRes.data?.firstOrNull()?.patientId
                return if (patientId != null) {
                    authManager.savePatientId(patientId)
                    Log.d(TAG, "login → SUCCESS. patientId=$patientId")
                    Result.success(Unit)
                } else {
                    val msg = "Login OK but no active CGM connections found in LibreLinkUp."
                    Log.e(TAG, msg)
                    Result.failure(Exception(msg))
                }
            } else {
                val msg = "Authentication failed: server returned no token."
                Log.e(TAG, msg)
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "login → caught exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Clears all persisted auth state.
     *
     * Note: Does NOT wipe the local glucose database. Use [clearDatabase] for that.
     */
    suspend fun logout() {
        authManager.clearAuth()
    }

    /**
     * Wipes the local glucose database and triggers an immediate poll.
     *
     * This is useful for clearing stale data without logging out, ensuring the fresh
     * session re-populates cleanly starting from the API's 8-h backfill.
     */
    suspend fun clearDatabase() {
        glucoseDao.deleteAll()
        refresh()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Glucose polling
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A cold [Flow] that polls the LibreLinkUp `/graph` endpoint every 5 minutes and
     * emits [GlucoseFlowState] values.
     *
     * State transitions:
     *  - Emits [GlucoseFlowState.Loading] immediately on collection.
     *  - Emits [GlucoseFlowState.Success] on each successful reading.
     *  - Emits [GlucoseFlowState.Error] on transient network failures (retries next cycle).
     *  - Emits [GlucoseFlowState.SessionExpired] on HTTP 401 and **terminates** — the UI
     *    must navigate to the Login screen; a new flow will be created after re-auth.
     *
     * This flow responds to [refresh] signals by triggering an immediate poll and
     * resetting the 5-minute interval timer.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val glucoseFlow: Flow<GlucoseFlowState> =
        refreshSignal.flatMapLatest {
            flow {
                emit(GlucoseFlowState.Loading)
                while (true) {
                    try {
                        val patientId = authManager.getPatientId()
                        if (patientId != null) {
                            val graphRes = api.getGraph(patientId)
                            val connection = graphRes.data?.connection
                            val measurement = connection?.glucoseMeasurement
                            val graphData = graphRes.data?.graphData ?: emptyList()

                            if (measurement != null) {
                                val region = authManager.getRegion() ?: "US"

                                // Merge the live reading with the historical graphData batch.
                                // INSERT OR REPLACE on timestampUtc PK makes this self-healing:
                                // duplicates are silently updated in-place, so API recovery after
                                // a 3h outage naturally back-fills the gap without accumulating dupes.
                                val entities = (listOf(measurement) + graphData).map {
                                    it.toEntity(region)
                                }
                                glucoseDao.insertReadings(entities)

                                // Prune readings older than 90 days to cap DB growth.
                                val cutoff = System.currentTimeMillis() - RETENTION_MS
                                glucoseDao.deleteOlderThan(cutoff)

                                emit(GlucoseFlowState.Success(measurement))
                            }
                        }
                    } catch (e: HttpException) {
                        if (e.code() == 401) {
                            Log.e(TAG, "glucoseFlow → HTTP 401: session expired")
                            emit(GlucoseFlowState.SessionExpired)
                            return@flow // Stop polling — re-auth required
                        }
                        val msg = "Network error (${e.code()}). Retrying in 5 minutes."
                        Log.w(TAG, "glucoseFlow → HTTP ${e.code()}: ${e.message}")
                        emit(GlucoseFlowState.Error(msg))
                    } catch (e: Exception) {
                        // Transient network failures (DNS, timeout, no connectivity).
                        // IMPORTANT: if this is the very first emission (loading state still active),
                        // we must emit an Error so the UI escapes the infinite spinner.
                        // After the first success, we silently retry to keep the last reading visible.
                        val msg = "Could not connect. Retrying in 5 minutes."
                        Log.w(TAG, "glucoseFlow → transient error: ${e.message}")
                        emit(GlucoseFlowState.Error(msg))
                    }
                    delay(POLL_INTERVAL_MS)
                }
            }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Historical Data (Room)
    // ─────────────────────────────────────────────────────────────────────────

    fun getReadingsForWindow(hours: Int): Flow<List<GlucoseReadingEntity>> {
        val sinceMs = System.currentTimeMillis() - (hours * 60 * 60 * 1000L)
        return glucoseDao.getReadingsSince(sinceMs)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun com.eggyswarehouse.betterglucodash.data.network.GlucoseMeasurement.toEntity(region: String): GlucoseReadingEntity {
        // FactoryTimestamp example: "10/24/2023 2:45:00 PM"
        val ts =
            try {
                LocalDateTime
                    .parse(this.FactoryTimestamp, LIBRE_TIMESTAMP_FORMATTER)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        return GlucoseReadingEntity(
            timestampUtc = ts,
            valueMgDl = this.ValueInMgPerDl,
            valueDisplay = this.Value,
            trendArrow = this.TrendArrow,
            measurementColor = this.MeasurementColor,
            region = region
        )
    }
}
