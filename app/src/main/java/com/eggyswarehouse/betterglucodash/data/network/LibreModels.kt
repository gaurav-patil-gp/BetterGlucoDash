package com.eggyswarehouse.betterglucodash.data.network

import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
// Auth models
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val status: Int, val data: AuthData? = null)

@Serializable
data class AuthData(
    val authTicket: AuthTicket? = null,
    val user: UserData? = null,
    /** True when the initial login hit the wrong regional endpoint. */
    val redirect: Boolean = false,
    /** The correct region code returned by the server on a redirect (e.g. "ca", "us"). */
    val region: String? = null
)

@Serializable
data class UserData(val id: String = "")

/**
 * JWT bundle returned by /llu/auth/login.
 *
 * @property token  The Bearer token to attach to every subsequent request.
 * @property expires Unix epoch **seconds** at which the token expires (~180 days).
 * @property duration Token validity window in milliseconds.
 */
@Serializable
data class AuthTicket(val token: String, val expires: Long = 0, val duration: Long = 0)

// ─────────────────────────────────────────────────────────────────────────────
// Connection / patient models
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class ConnectionsResponse(val status: Int, val data: List<PatientConnection>? = null)

/**
 * Represents a LibreLinkUp follower connection (patient the authenticated user is following).
 */
@Serializable
data class PatientConnection(
    val patientId: String,
    val firstName: String = "",
    val lastName: String = "",
    /** The most recent CGM reading for this patient — always present on /connections. */
    val glucoseMeasurement: GlucoseMeasurement? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// Glucose / graph models
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A single CGM glucose reading as returned by the LibreLinkUp API.
 *
 * **Important:** Abbott **pre-converts** [Value] to the user's regional unit before
 * returning it — no client-side unit conversion is required or desired.
 *  - Canada (`api-ca.libreview.io`): [Value] is in **mmol/L** (e.g. `10.1`)
 *  - US (`api-us.libreview.io`): [Value] is in **mg/dL**  (e.g. `182`)
 *
 * Use [ValueInMgPerDl] only for raw sensor math or future unit conversion (V2+).
 *
 * @property Value             Glucose in the user's region unit (Double).
 * @property ValueInMgPerDl    Raw mg/dL integer from the sensor.
 * @property TrendArrow        Direction (1=↓↓ fast, 2=↓, 3=→ flat, 4=↑, 5=↑↑ fast).
 * @property FactoryTimestamp  UTC timestamp from the sensor hardware.
 * @property Timestamp         Local-time timestamp.
 * @property MeasurementColor  Colour band: 1=green, 2=yellow, 3=orange/high, 4=red/low.
 * @property isHigh            True if reading exceeds the patient's high threshold.
 * @property isLow             True if reading falls below the patient's low threshold.
 */
@Serializable
data class GlucoseMeasurement(
    val Value: Double = 0.0,
    val ValueInMgPerDl: Int = 0,
    val TrendArrow: Int = 0,
    val FactoryTimestamp: String = "",
    val Timestamp: String = "",
    val MeasurementColor: Int = 1,
    val isHigh: Boolean = false,
    val isLow: Boolean = false
)

@Serializable
data class GraphResponse(val status: Int, val data: GraphData? = null)

/**
 * Data payload from GET /llu/connections/{patientId}/graph.
 *
 * @property connection   The patient connection with the latest [GlucoseMeasurement].
 * @property graphData    Historical readings for the past ~8 hours at 5-minute intervals.
 *                        Available immediately — no extra API call needed for V2 charts.
 */
@Serializable
data class GraphData(val connection: PatientConnection? = null, val graphData: List<GlucoseMeasurement> = emptyList())
