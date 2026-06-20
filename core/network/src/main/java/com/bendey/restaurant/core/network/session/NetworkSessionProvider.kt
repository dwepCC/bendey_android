package com.bendey.restaurant.core.network.session

/** Puente sincrónico para interceptores OkHttp — implementado por SessionManager. */
interface NetworkSessionProvider {
    fun token(): String?
    fun tenantSlug(): String?
    fun tenantApiBaseUrl(): String?
}
