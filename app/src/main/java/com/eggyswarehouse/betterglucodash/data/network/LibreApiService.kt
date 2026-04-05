package com.eggyswarehouse.betterglucodash.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface for the unofficial LibreLinkUp (Abbott) API.
 *
 * All requests are intercepted by [AuthInterceptor] (injects Bearer token + account-id)
 * and [RegionInterceptor] (rewrites the host to the correct regional endpoint).
 *
 * Regional endpoints:
 *  - US:     api-us.libreview.io
 *  - Canada: api-ca.libreview.io
 *
 * **This API is unofficial / reverse-engineered. It is not publicly documented by Abbott
 * and may change without notice.**
 */
interface LibreApiService {

    /**
     * Authenticate with LibreLinkUp credentials.
     * Returns an [AuthTicket] on success, or a redirect response if the wrong regional
     * endpoint was targeted.
     */
    @POST("llu/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    /**
     * Fetch all patient connections for the authenticated follower account.
     * Each connection contains the latest [GlucoseMeasurement].
     */
    @GET("llu/connections")
    suspend fun getConnections(): ConnectionsResponse

    /**
     * Fetch the current reading and ~8 hours of historical glucose data for a patient.
     * Historical data is in [GraphData.graphData] — no additional API call is needed
     * for V2 chart features.
     */
    @GET("llu/connections/{patientId}/graph")
    suspend fun getGraph(@Path("patientId") patientId: String): GraphResponse
}
