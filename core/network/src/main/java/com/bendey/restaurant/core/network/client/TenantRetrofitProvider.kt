package com.bendey.restaurant.core.network.client

import com.bendey.restaurant.core.network.session.NetworkSessionProvider
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TenantRetrofitProvider @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val sessionProvider: NetworkSessionProvider,
) {
    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    private var cachedRetrofit: Retrofit? = null

    fun retrofit(): Retrofit {
        val baseUrl = sessionProvider.tenantApiBaseUrl()
            ?: error("Tenant API URL no configurada. Vincule el RUC primero.")
        if (cachedBaseUrl == baseUrl && cachedRetrofit != null) {
            return cachedRetrofit!!
        }
        synchronized(this) {
            if (cachedBaseUrl == baseUrl && cachedRetrofit != null) {
                return cachedRetrofit!!
            }
            val normalized = normalizeBaseUrl(baseUrl)
            cachedRetrofit = Retrofit.Builder()
                .baseUrl(normalized)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
            cachedBaseUrl = baseUrl
            return cachedRetrofit!!
        }
    }

    fun invalidate() {
        synchronized(this) {
            cachedBaseUrl = null
            cachedRetrofit = null
        }
    }

    inline fun <reified T> create(): T = retrofit().create(T::class.java)

    private fun normalizeBaseUrl(url: String): String {
        var base = url.trim().trimEnd('/')
        if (base.endsWith("/api")) base = base.removeSuffix("/api")
        return "$base/"
    }
}
