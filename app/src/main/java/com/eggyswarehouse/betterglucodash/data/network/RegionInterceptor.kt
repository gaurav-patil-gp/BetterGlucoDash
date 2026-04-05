package com.eggyswarehouse.betterglucodash.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Holds the mutable LibreLinkUp regional API hostname.
 *
 * [LibreRepository][com.eggyswarehouse.betterglucodash.data.repository.LibreRepository]
 * updates [host] before every login call based on the user's region selection, allowing
 * [RegionInterceptor] to route all subsequent requests to the correct Abbott endpoint
 * without rebuilding the Retrofit instance.
 *
 * @property host  The bare hostname (no scheme). Defaults to the US endpoint.
 */
class BaseUrlHolder(@Volatile var host: String = LLU_HOSTS.US)

/**
 * Canonical LibreLinkUp regional API hostnames.
 * Only US and Canada are in scope for MVP. Additional regions can be added in V2.
 */
object LLU_HOSTS {
    const val US = "api-us.libreview.io"
    const val CA = "api-ca.libreview.io"

    /** Maps a region code from the Login screen to the correct API hostname. */
    fun fromRegion(region: String): String = when (region.uppercase()) {
        "CA", "CAN", "CANADA" -> CA
        else -> US
    }
}

/**
 * OkHttp interceptor that rewrites the `host` of every request to the value
 * currently stored in [BaseUrlHolder].
 *
 * **Ordering:** Must be the **first** interceptor in the OkHttp chain so that
 * [AuthInterceptor] and the logging interceptor see the already-corrected URL.
 */
class RegionInterceptor(private val holder: BaseUrlHolder) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val newUrl = original.url.newBuilder()
            .host(holder.host)
            .build()
        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}
