package com.eggyswarehouse.betterglucodash.data.network

import com.eggyswarehouse.betterglucodash.data.local.AuthManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that injects LibreLinkUp-specific headers into every request.
 *
 * **Headers applied to all requests:**
 * - `User-Agent`    — iOS Safari UA string required by Abbott's Cloudflare configuration.
 * - `Content-Type`  — Must include the `charset=UTF-8` suffix.
 * - `version`       — LibreLinkUp app version string (must be ≥ 4.16.0).
 * - `product`       — Must be `llu.ios` (the known-working product identifier).
 *
 * **Headers applied to authenticated requests only:**
 * - `Authorization` — `Bearer <jwt>` from the stored [AuthTicket].
 * - `account-id`    — SHA-256 hash of the user's `id` field from the login response.
 *                     Required by Abbott's API on all post-login calls.
 */
class AuthInterceptor(
    private val authManager: AuthManager
) : Interceptor {

    companion object {
        /**
         * Must match the official LibreLinkUp iOS app's User-Agent.
         * Abbott routes traffic through Cloudflare, which fingerprints the UA string —
         * requests with unrecognised agents receive a 403.
         */
        private const val USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU OS 17_4.1 like Mac OS X) AppleWebKit/536.26 " +
            "(KHTML, like Gecko) Version/17.4.1 Mobile/10A5355d Safari/8536.25"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Content-Type", "application/json;charset=UTF-8")
            .addHeader("Accept", "application/json")
            .addHeader("version", "4.16.0")
            .addHeader("product", "llu.ios")

        val token = runBlocking { authManager.getToken() }
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val accountId = runBlocking { authManager.getAccountId() }
        if (!accountId.isNullOrEmpty()) {
            requestBuilder.addHeader("account-id", accountId)
        }

        return chain.proceed(requestBuilder.build())
    }
}
