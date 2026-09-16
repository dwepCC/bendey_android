package com.bendey.restaurant.core.network.interceptor

import android.content.Context
import android.content.pm.PackageManager
import com.bendey.restaurant.core.network.session.NetworkSessionProvider
import com.bendey.restaurant.core.network.session.SessionExpiryReporter
import com.bendey.restaurant.core.network.session.SessionInvalidationGate
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionProvider: NetworkSessionProvider,
    private val sessionExpiryReporter: SessionExpiryReporter,
    private val invalidationGate: SessionInvalidationGate,
    @ApplicationContext private val context: Context,
) : Interceptor {

    // BuildConfig.VERSION_NAME vive en :app (donde está el versionName real), no en este módulo
    // -- core:network no puede referenciarlo sin invertir la dependencia. PackageManager es la
    // forma estándar de leer la versión instalada desde cualquier módulo. Se calcula una sola vez:
    // no cambia durante la vida del proceso.
    private val appVersionName: String? by lazy {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
    }

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

        // Identifica a Bendey Resto frente al panel del ERP, que comparte backend, endpoints y token.
        // El backend la usa para saber qué reglas aplicar: el PIN de operaciones es del restaurante,
        // mientras que el ERP autoriza por roles y permisos.
        builder.header("X-Bendey-App", "resto")

        // Bendey Resto (Tauri/Android) no se autoactualiza solo -- depende de que el usuario
        // actualice desde Play Store. Sin esto no había forma de saber, tenant por tenant, si
        // sigue en una versión vieja (ver pkg/appinstall en backend_go). El ERP web no manda
        // este header, así que nunca genera filas para él.
        appVersionName?.let {
            builder.header("X-App-Platform", "android")
            builder.header("X-App-Version", it)
        }

        sessionProvider.token()?.let { builder.header("Authorization", "Bearer $it") }
        sessionProvider.tenantSlug()?.let { builder.header("X-Tenant-Slug", it) }

        val request = builder.build()
        val hadAuth = request.header("Authorization") != null

        // Corte en seco: si ya detectamos la sesión inválida, no golpeamos el servidor con la
        // ráfaga de peticiones autenticadas (evita el 429 "demasiadas peticiones"). El cierre de
        // sesión + redirect al login ya está en curso. Respondemos 401 local sin tocar la red.
        if (hadAuth && invalidationGate.isInvalidated()) {
            return unauthorizedShortCircuit(request)
        }

        val response = chain.proceed(request)

        // 401 en un request AUTENTICADO (llevaba Bearer) = token expirado/inválido → cierre global.
        // Un 401 sin Authorization (p. ej. login con credenciales incorrectas) no cierra la sesión.
        if (response.code == 401 && hadAuth) {
            invalidationGate.markInvalidated()
            sessionExpiryReporter.reportSessionExpired()
        }

        return response
    }

    private fun unauthorizedShortCircuit(request: okhttp3.Request): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Session invalidated (short-circuited locally)")
            .body("".toResponseBody(null))
            .build()
}
