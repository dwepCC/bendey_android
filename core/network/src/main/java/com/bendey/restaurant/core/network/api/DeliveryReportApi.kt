package com.bendey.restaurant.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** Reporte de delivery (D5). Todo opcional; el envoltorio `data` se desenvuelve al mapear. */
@Serializable
data class DeliveryReportDto(
    val summary: ReportSummaryDto? = null,
    val channels: List<ReportChannelDto>? = null,
    val times: ReportTimesDto? = null,
    val cancellations: List<ReportCancellationDto>? = null,
    val payments: List<ReportPaymentDto>? = null,
    val drivers: List<ReportDriverDto>? = null,
    val refunds: ReportRefundsDto? = null,
    val truncated: Boolean? = null,
    val data: DeliveryReportDto? = null,
)

@Serializable
data class ReportSummaryDto(
    val orders: Int? = null,
    val delivered: Int? = null,
    @SerialName("in_progress") val inProgress: Int? = null,
    val cancelled: Int? = null,
    val gross: Double? = null,
    @SerialName("avg_ticket") val avgTicket: Double? = null,
    @SerialName("lost_amount") val lostAmount: Double? = null,
    @SerialName("cancel_rate") val cancelRate: Double? = null,
)

@Serializable
data class ReportChannelDto(
    val source: String? = null,
    val type: String? = null,
    val orders: Int? = null,
    val cancelled: Int? = null,
    val amount: Double? = null,
)

@Serializable
data class ReportTimesDto(
    @SerialName("accept_minutes") val acceptMinutes: Double? = null,
    @SerialName("prep_minutes") val prepMinutes: Double? = null,
    @SerialName("delivery_minutes") val deliveryMinutes: Double? = null,
    @SerialName("total_minutes") val totalMinutes: Double? = null,
    @SerialName("accept_samples") val acceptSamples: Int? = null,
    @SerialName("prep_samples") val prepSamples: Int? = null,
    @SerialName("delivery_samples") val deliverySamples: Int? = null,
    @SerialName("total_samples") val totalSamples: Int? = null,
)

@Serializable
data class ReportCancellationDto(
    val reason: String? = null,
    val label: String? = null,
    val orders: Int? = null,
    val amount: Double? = null,
    val sample: String? = null,
)

@Serializable
data class ReportPaymentDto(
    val mode: String? = null,
    val method: String? = null,
    val label: String? = null,
    val orders: Int? = null,
    val amount: Double? = null,
)

@Serializable
data class ReportDriverDto(
    @SerialName("driver_id") val driverId: Int? = null,
    val name: String? = null,
    val assigned: Int? = null,
    val delivered: Int? = null,
    val failed: Int? = null,
    val rejected: Int? = null,
    @SerialName("avg_delivery_minutes") val avgDeliveryMinutes: Double? = null,
    val earnings: Double? = null,
    @SerialName("cash_collected") val cashCollected: Double? = null,
    @SerialName("cash_pending") val cashPending: Double? = null,
)

@Serializable
data class ReportRefundsDto(
    val pending: Int? = null,
    @SerialName("pending_amount") val pendingAmount: Double? = null,
)

/** `GET /api/restaurant/delivery/report` (c.v o s.m; el repartidor no). */
interface DeliveryReportApi {
    @GET("/api/restaurant/delivery/report")
    suspend fun report(@Query("from") from: String, @Query("to") to: String): DeliveryReportDto
}
