package com.eggyswarehouse.betterglucodash.data.repository

import com.eggyswarehouse.betterglucodash.data.network.GlucoseMeasurement

/**
 * Represents the state of the live glucose polling flow.
 *
 * Collectors (e.g. [com.eggyswarehouse.betterglucodash.ui.dashboard.DashboardViewModel])
 * should handle all four states:
 *  - [Loading]        — Initial state before the first successful poll.
 *  - [Success]        — A fresh reading was retrieved from the Abbott API.
 *  - [Error]          — A recoverable network error occurred; previous reading remains valid.
 *  - [SessionExpired] — The auth token was rejected (HTTP 401). The user must log in again.
 */
sealed class GlucoseFlowState {
    /** Waiting for the first reading after login. */
    data object Loading : GlucoseFlowState()

    /** A valid glucose reading is available. */
    data class Success(val measurement: GlucoseMeasurement) : GlucoseFlowState()

    /**
     * A transient error occurred (e.g. no network). The dashboard should retain the last
     * successful reading and display a subtle "last updated X minutes ago" indicator.
     */
    data class Error(val message: String) : GlucoseFlowState()

    /**
     * Abbott API returned HTTP 401 — the JWT has expired or been invalidated.
     * The UI must navigate back to the Login screen immediately.
     */
    data object SessionExpired : GlucoseFlowState()
}
