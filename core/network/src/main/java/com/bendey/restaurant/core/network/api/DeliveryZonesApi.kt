package com.bendey.restaurant.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

@Serializable
data class DeliveryZoneDto(
    val id: Int = 0,
    @SerialName("ubigeo_code") val ubigeoCode: String = "",
    val name: String = "",
    val fee: Double? = null,
    @SerialName("min_order") val minOrder: Double? = null,
    @SerialName("eta_minutes") val etaMinutes: Int? = null,
    val active: Boolean? = null,
)

@Serializable
data class DeliveryZonesEnvelopeDto(
    val data: List<DeliveryZoneDto> = emptyList(),
    @SerialName("restrict_to_zones") val restrictToZones: Boolean? = null,
)

@Serializable
data class DeliveryZoneEnvelopeDto(val data: DeliveryZoneDto? = null)

@Serializable
data class DeliveryZoneRequestDto(
    @SerialName("ubigeo_code") val ubigeoCode: String,
    val name: String,
    val fee: Double,
    @SerialName("min_order") val minOrder: Double,
    @SerialName("eta_minutes") val etaMinutes: Int,
    val active: Boolean,
)

/** Zonas de reparto por distrito (§26). Lectura: d.v, o.ch o s.m. Alta, cambio y baja: s.m. */
interface DeliveryZonesApi {
    @GET("/api/restaurant/delivery/zones")
    suspend fun zones(): DeliveryZonesEnvelopeDto

    @PUT("/api/restaurant/delivery/zones")
    suspend fun saveZone(@Body body: DeliveryZoneRequestDto): DeliveryZoneEnvelopeDto

    @DELETE("/api/restaurant/delivery/zones/{id}")
    suspend fun deleteZone(@Path("id") id: Int): SuccessOnlyDto
}

@Serializable
data class PendingEarningDto(
    @SerialName("driver_id") val driverId: Int = 0,
    val name: String? = null,
    val deliveries: Int? = null,
    val total: Double? = null,
)

@Serializable
data class PendingEarningsEnvelopeDto(val data: List<PendingEarningDto> = emptyList())

@Serializable
data class EarningsPayoutRequestDto(@SerialName("driver_id") val driverId: Int)

@Serializable
data class EarningsPayoutDto(
    val amount: Double? = null,
    @SerialName("deliveries_count") val deliveriesCount: Int? = null,
    val booked: Boolean? = null,
)

@Serializable
data class EarningsPayoutDataDto(val payout: EarningsPayoutDto? = null, val name: String? = null)

@Serializable
data class EarningsPayoutEnvelopeDto(val data: EarningsPayoutDataDto? = null)

/** Ganancias de repartidores (§27). Pendientes: o.ch o s.m. Pagar: o.ch (sale de la caja de quien paga). */
interface DeliveryEarningsApi {
    @GET("/api/restaurant/delivery/earnings/pending")
    suspend fun pending(): PendingEarningsEnvelopeDto

    @POST("/api/restaurant/delivery/earnings/payout")
    suspend fun payout(@Body body: EarningsPayoutRequestDto): EarningsPayoutEnvelopeDto
}
