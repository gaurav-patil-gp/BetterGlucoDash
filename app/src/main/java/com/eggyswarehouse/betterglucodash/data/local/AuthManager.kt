package com.eggyswarehouse.betterglucodash.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Manages persistent local storage of LibreLinkUp authentication state.
 *
 * All data is stored in an encrypted [DataStore<Preferences>] and survives app restarts.
 * This is the single source of truth for:
 *  - The JWT bearer token ([saveToken] / [getToken])
 *  - The account-id SHA-256 hash required by Abbott's API ([saveAccountId] / [getAccountId])
 *  - The selected region ("US" or "CA") which controls the API endpoint and display units
 *  - The active patient ID used for polling the /graph endpoint
 *
 * **Thread safety:** [cachedToken], [cachedAccountId], and [cachedRegion] are @Volatile
 * in-memory copies populated by [warmCache] on app start. [AuthInterceptor] reads these
 * synchronously on OkHttp's I/O thread — no `runBlocking` or coroutine dispatch needed.
 *
 * **Note:** Credentials (email/password) are never persisted — only the derived JWT.
 */
class AuthManager(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val JWT_TOKEN = stringPreferencesKey("jwt_token")
        private val PATIENT_ID = stringPreferencesKey("patient_id")
        private val REGION = stringPreferencesKey("region") // "US" or "CA"
        private val ACCOUNT_ID = stringPreferencesKey("account_id") // SHA-256(userId)
        private val THEME_IS_DARK = booleanPreferencesKey("theme_is_dark")
    }

    /** In-memory cache of the JWT. Populated by [warmCache]; updated on every [saveToken]. */
    @Volatile var cachedToken: String? = null
        private set

    /** In-memory cache of the account-id. Populated by [warmCache]; updated on every [saveAccountId]. */
    @Volatile var cachedAccountId: String? = null
        private set

    /** In-memory cache of the region code. Populated by [warmCache]; updated on every [saveRegion]. */
    @Volatile var cachedRegion: String? = null
        private set

    /**
     * Emits the stored region whenever it changes.
     * Observed by [DashboardViewModel][com.eggyswarehouse.betterglucodash.ui.dashboard.DashboardViewModel]
     * and [AverageViewModel][com.eggyswarehouse.betterglucodash.ui.dashboard.average.AverageViewModel].
     */
    val regionFlow: Flow<String?> = dataStore.data.map { it[REGION] }

    /** Cold-start default: dark. Persisted across restarts. */
    val themeIsDarkFlow: Flow<Boolean> = dataStore.data.map { it[THEME_IS_DARK] ?: true }

    suspend fun setThemeIsDark(isDark: Boolean) {
        dataStore.edit { it[THEME_IS_DARK] = isDark }
    }

    /**
     * Reads all cached fields from DataStore in a single pass.
     * Called once at app start by [AppContainer][com.eggyswarehouse.betterglucodash.di.AppContainer]
     * so that [AuthInterceptor] can read [cachedToken] and [cachedAccountId] synchronously.
     */
    suspend fun warmCache() {
        dataStore.data.first().let { prefs ->
            cachedToken = prefs[JWT_TOKEN]
            cachedAccountId = prefs[ACCOUNT_ID]
            cachedRegion = prefs[REGION]
        }
    }

    suspend fun saveToken(token: String) {
        cachedToken = token
        dataStore.edit { it[JWT_TOKEN] = token }
    }

    suspend fun savePatientId(id: String) {
        dataStore.edit { it[PATIENT_ID] = id }
    }

    suspend fun saveRegion(region: String) {
        cachedRegion = region
        dataStore.edit { it[REGION] = region }
    }

    suspend fun saveAccountId(accountId: String) {
        cachedAccountId = accountId
        dataStore.edit { it[ACCOUNT_ID] = accountId }
    }

    suspend fun getToken(): String? = dataStore.data.first()[JWT_TOKEN]

    suspend fun getPatientId(): String? = dataStore.data.first()[PATIENT_ID]

    suspend fun getRegion(): String? = dataStore.data.first()[REGION]

    suspend fun getAccountId(): String? = dataStore.data.first()[ACCOUNT_ID]

    /**
     * Clears all auth state and volatile caches. Called on logout.
     * DataStore region key is also cleared so the next login re-selects the region correctly.
     */
    suspend fun clearAuth() {
        cachedToken = null
        cachedAccountId = null
        cachedRegion = null
        dataStore.edit { prefs ->
            prefs.remove(JWT_TOKEN)
            prefs.remove(PATIENT_ID)
            prefs.remove(ACCOUNT_ID)
            prefs.remove(REGION)
        }
    }
}
