package com.bendey.restaurant.core.network.api

import okhttp3.ResponseBody
import retrofit2.http.GET

/**
 * `GET /api/session/capabilities` del tenant: `{schema_version, features, locale}`. Se pide el cuerpo
 * crudo porque la app solo lee el bloque `locale` (paquete de país) y debe tolerar que falte o venga
 * incompleto; el parseo vive en `core/data/locale/TenantLocaleJson`.
 */
interface SessionCapabilitiesApi {
    @GET("/api/session/capabilities")
    suspend fun capabilities(): ResponseBody
}
