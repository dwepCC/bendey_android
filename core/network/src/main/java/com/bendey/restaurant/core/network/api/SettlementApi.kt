package com.bendey.restaurant.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class SettlementOrderDto(
    @SerialName("payment_id") val paymentId: Int,
    @SerialName("session_id") val sessionId: Int = 0,
    @SerialName("order_code") val orderCode: String = "",
    @SerialName("customer_name") val customerName: String = "",
    val amount: Double = 0.0,
)

@Serializable
data class SettlementGroupDto(
    @SerialName("collector_kind") val collectorKind: String,
    @SerialName("collector_id") val collectorId: Int,
    @SerialName("collector_name") val collectorName: String = "",
    val total: Double = 0.0,
    val orders: List<SettlementOrderDto> = emptyList(),
)

@Serializable
data class RefundPendingDto(
    @SerialName("payment_id") val paymentId: Int,
    @SerialName("order_code") val orderCode: String = "",
    val amount: Double = 0.0,
    @SerialName("collector_name") val collectorName: String = "",
    @SerialName("cancel_reason") val cancelReason: String = "",
)

@Serializable
data class SettlementsPendingDto(
    val groups: List<SettlementGroupDto> = emptyList(),
    val refunds: List<RefundPendingDto> = emptyList(),
)

@Serializable
data class SettlementsPendingEnvelopeDto(val data: SettlementsPendingDto)

@Serializable
data class SettlementRecordDto(
    val id: Int,
    @SerialName("collector_name") val collectorName: String = "",
    @SerialName("received_by_name") val receivedByName: String = "",
    @SerialName("orders_count") val ordersCount: Int = 0,
    @SerialName("received_total") val receivedTotal: Double = 0.0,
    val difference: Double = 0.0,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class SettlementHistoryEnvelopeDto(val data: List<SettlementRecordDto> = emptyList())

@Serializable
data class ReceiveSettlementRequestDto(
    @SerialName("collector_kind") val collectorKind: String,
    @SerialName("collector_id") val collectorId: Int,
    @SerialName("payment_ids") val paymentIds: List<Int>,
    @SerialName("received_amount") val receivedAmount: Double,
    @SerialName("difference_reason") val differenceReason: String? = null,
)

@Serializable
data class ReceiveSettlementDataDto(@SerialName("difference_warning") val differenceWarning: String? = null)

@Serializable
data class ReceiveSettlementResponseDto(val success: Boolean = false, val data: ReceiveSettlementDataDto? = null)

@Serializable
data class RefundResolvedRequestDto(val note: String)

@Serializable
data class SuccessOnlyDto(val success: Boolean = false)

/** Rendicion de efectivo del delivery (D2c). Solo personal con `o.ch`; el repartidor recibe 403. */
interface SettlementApi {
    @GET("/api/restaurant/delivery/settlements/pending")
    suspend fun pending(): SettlementsPendingEnvelopeDto

    @GET("/api/restaurant/delivery/settlements")
    suspend fun history(@Query("limit") limit: Int = 15): SettlementHistoryEnvelopeDto

    @POST("/api/restaurant/delivery/settlements")
    suspend fun receive(@Body body: ReceiveSettlementRequestDto): ReceiveSettlementResponseDto

    @POST("/api/restaurant/delivery/payments/{id}/refund-resolved")
    suspend fun resolveRefund(@Path("id") paymentId: Int, @Body body: RefundResolvedRequestDto): SuccessOnlyDto
}
