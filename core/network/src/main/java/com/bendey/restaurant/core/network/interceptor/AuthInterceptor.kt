package com.bendey.restaurant.core.network.interceptor

import com.bendey.restaurant.core.network.session.NetworkSessionProvider
import com.bendey.restaurant.core.network.session.SessionExpiryReporter
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionProvider: NetworkSessionProvider,
    private val sessionExpiryReporter: SessionExpiryReporter,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        // No enviar Content-Type en GET/HEAD (p. ej. imágenes vía Coil en /uploads).
        if (original.body != null) {
            builder.header("Content-Type", "application/json")
        }
        if (original.header("Accept") == null) {
            builder.header(
                "Accept",
                if (original.body != null) "application/json" else "*/*",
            )
        }

        sessionProvider.token()?.let { builder.header("Authorization", "Bearer $it") }
        sessionProvider.tenantSlug()?.let { builder.header("X-Tenant-Slug", it) }

        val request = builder.build()
        val response = chain.proceed(request)

        // 401 en un request AUTENTICADO (llevaba Bearer) = token expirado/inválido → cierre global.
        // Un 401 sin Authorization (p. ej. login con credenciales incorrectas) no cierra la sesión.
        if (response.code == 401 && request.header("Authorization") != null) {
            sessionExpiryReporter.reportSessionExpired()
        }

        return response
    }
}
