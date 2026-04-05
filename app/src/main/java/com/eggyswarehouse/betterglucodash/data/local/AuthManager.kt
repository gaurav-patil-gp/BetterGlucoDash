package com.eggyswarehouse.betterglucodash.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
 * **Note:** Credentials (email/password) are never persisted — only the derived JWT.
 */
class AuthManager(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val JWT_TOKEN  = stringPreferencesKey("jwt_token")
        private val PATIENT_ID = stringPreferencesKey("patient_id")
        private val REGION     = stringPreferencesKey("region")     // "US" or "CA"
        private val ACCOUNT_ID = stringPreferencesKey("account_id") // SHA-256(userId)
    }

    /** Emits the stored region whenever it changes. Observed by [DashboardViewModel]. */
    val regionFlow: Flow<String?> = dataStore.data.map { it[REGION] }

    suspend fun saveToken(token: String) {
        dataStore.edit { it[JWT_TOKEN] = token }
    }

    suspend fun savePatientId(id: String) {
        dataStore.edit { it[PATIENT_ID] = id }
    }

    suspend fun saveRegion(region: String) {
        dataStore.edit { it[REGION] = region }
    }

    suspend fun saveAccountId(accountId: String) {
        dataStore.edit { it[ACCOUNT_ID] = accountId }
    }

    suspend fun getToken(): String?     = dataStore.data.first()[JWT_TOKEN]
    suspend fun getPatientId(): String? = dataStore.data.first()[PATIENT_ID]
    suspend fun getRegion(): String?    = dataStore.data.first()[REGION]
    suspend fun getAccountId(): String? = dataStore.data.first()[ACCOUNT_ID]

    /** Clears all auth state. Called on logout. DataStore region key is also cleared so
     *  the next login re-selects the region correctly. */
    suspend fun clearAuth() {
        dataStore.edit { prefs ->
            prefs.remove(JWT_TOKEN)
            prefs.remove(PATIENT_ID)
            prefs.remove(ACCOUNT_ID)
            prefs.remove(REGION)
        }
    }
}
