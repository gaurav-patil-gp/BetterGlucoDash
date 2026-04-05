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
 * On app restart, [AppContainer][com.eggyswarehouse.betterglucodash.di.AppContainer]
 * restores [host] from the persisted region via [Region.fromCode].
 *
 * @property host  The bare hostname (no scheme). Defaults to the US endpoint.
 */
class BaseUrlHolder(@Volatile var host: String = Region.US.apiHost)

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
        val newUrl =
            original.url
                .newBuilder()
                .host(holder.host)
                .build()
        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}
