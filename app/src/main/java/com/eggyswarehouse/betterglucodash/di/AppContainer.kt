package com.eggyswarehouse.betterglucodash.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.eggyswarehouse.betterglucodash.BuildConfig
import com.eggyswarehouse.betterglucodash.data.local.AuthManager
import com.eggyswarehouse.betterglucodash.data.network.AuthInterceptor
import com.eggyswarehouse.betterglucodash.data.network.BaseUrlHolder
import com.eggyswarehouse.betterglucodash.data.network.LibreApiService
import com.eggyswarehouse.betterglucodash.data.network.LLU_HOSTS
import com.eggyswarehouse.betterglucodash.data.network.RegionInterceptor
import com.eggyswarehouse.betterglucodash.data.repository.LibreRepository
import com.eggyswarehouse.betterglucodash.data.local.db.GlucoseDatabase
import com.eggyswarehouse.betterglucodash.data.local.db.GlucoseDao
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Manual dependency injection container for BetterGlucoDash.
 *
 * Acts as the app-level service locator. All dependencies are created lazily on first access
 * and are singletons for the lifetime of the application process.
 *
 * **Dependency graph:**
 * ```
 * AppContainer
 *  ├── AuthManager (DataStore)
 *  ├── BaseUrlHolder
 *  ├── RegionInterceptor(BaseUrlHolder)   ← rewrites host per-request
 *  ├── AuthInterceptor(AuthManager)       ← injects Bearer + account-id
 *  ├── OkHttpClient(Region, Auth, Logger)
 *  ├── Retrofit(OkHttpClient)
 *  ├── LibreApiService(Retrofit)
 *  └── LibreRepository(ApiService, AuthManager, BaseUrlHolder)
 * ```
 *
 * Note: The base URL in Retrofit is a placeholder — [RegionInterceptor] overrides
 * the host at request time based on [BaseUrlHolder.host].
 */
class AppContainer(private val context: Context) {

    val authManager: AuthManager by lazy {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("better_gluco_dash_prefs") }
        )
        AuthManager(dataStore)
    }

    /**
     * Mutable regional host holder. [LibreRepository.login] updates this before
     * every API call so that [RegionInterceptor] routes to the correct endpoint.
     */
    val baseUrlHolder = BaseUrlHolder()

    private val authInterceptor by lazy { AuthInterceptor(authManager) }

    val glucoseDatabase: GlucoseDatabase by lazy { GlucoseDatabase.getDatabase(context) }
    val glucoseDao: GlucoseDao by lazy { glucoseDatabase.glucoseDao() }

    private val regionInterceptor by lazy { RegionInterceptor(baseUrlHolder) }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(regionInterceptor) // Must be first — rewrites host before auth headers are added
            .addInterceptor(authInterceptor)
            .apply {
                // Full request/response body logging is only enabled in debug builds.
                // In release builds this is NONE to prevent JWT tokens appearing in logcat.
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                    )
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            // Placeholder host — RegionInterceptor overwrites this at request time.
            .baseUrl("https://${LLU_HOSTS.US}/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private val libreApiService: LibreApiService by lazy {
        retrofit.create(LibreApiService::class.java)
    }

    val libreRepository: LibreRepository by lazy {
        LibreRepository(libreApiService, authManager, baseUrlHolder, glucoseDao)
    }
}
