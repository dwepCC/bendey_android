package com.bendey.restaurant.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** Reporte «Productos vendidos» (GET /api/restaurant/reports/sold-products). */
@Serializable
data class SoldItemDto(
    val description: String? = null,
    val components: List<String>? = null,
    val quantity: Double? = null,
    @SerialName("unit_price") val unitPrice: Double? = null,
    val total: Double? = null,
)

@Serializable
data class SoldPaymentDto(val method: String? = null, val amount: Double? = null)

@Serializable
data class SoldSaleDto(
    @SerialName("sale_id") val saleId: Int? = null,
    @SerialName("issue_date") val issueDate: String? = null,
    @SerialName("registered_at") val registeredAt: String? = null,
    @SerialName("doc_kind") val docKind: String? = null,
    val number: String? = null,
    val valid: Boolean? = null,
    @SerialName("cashier_name") val cashierName: String? = null,
    @SerialName("waiter_name") val waiterName: String? = null,
    val service: String? = null,
    @SerialName("order_type") val orderType: String? = null,
    @SerialName("table_name") val tableName: String? = null,
    val total: Double? = null,
    val payments: List<SoldPaymentDto>? = null,
    val items: List<SoldItemDto>? = null,
)

@Serializable
data class SoldSummaryDto(
    @SerialName("valid_sales") val validSales: Int? = null,
    @SerialName("valid_amount") val validAmount: Double? = null,
    @SerialName("cancelled_sales") val cancelledSales: Int? = null,
    @SerialName("cancelled_amount") val cancelledAmount: Double? = null,
)

@Serializable
data class SoldReportDataDto(
    val total: Int? = null,
    val summary: SoldSummaryDto? = null,
    val sales: List<SoldSaleDto>? = null,
)

@Serializable
data class SoldReportResponseDto(val data: SoldReportDataDto? = null)

@Serializable
data class SoldExportRowDto(
    @SerialName("issue_date") val issueDate: String? = null,
    @SerialName("registered_at") val registeredAt: String? = null,
    @SerialName("doc_kind") val docKind: String? = null,
    val number: String? = null,
    val valid: Boolean? = null,
    val service: String? = null,
    @SerialName("order_type") val orderType: String? = null,
    @SerialName("table_name") val tableName: String? = null,
    @SerialName("cashier_name") val cashierName: String? = null,
    @SerialName("waiter_name") val waiterName: String? = null,
    val payments: String? = null,
    val description: String? = null,
    val quantity: Double? = null,
    @SerialName("unit_price") val unitPrice: Double? = null,
    @SerialName("line_total") val lineTotal: Double? = null,
    @SerialName("sale_total") val saleTotal: Double? = null,
)

@Serializable
data class SoldExportDataDto(val rows: List<SoldExportRowDto>? = null, val truncated: Boolean? = null)

@Serializable
data class SoldExportResponseDto(val data: SoldExportDataDto? = null)

interface SoldReportApi {
    @GET("/api/restaurant/reports/sold-products")
    suspend fun page(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("user_id") userId: Int? = null,
        @Query("payment_method") paymentMethod: String? = null,
        @Query("doc_kind") docKind: String? = null,
        @Query("only_valid") onlyValid: Int? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 25,
    ): SoldReportResponseDto

    @GET("/api/restaurant/reports/sold-products?format=rows")
    suspend fun export(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("user_id") userId: Int? = null,
        @Query("payment_method") paymentMethod: String? = null,
        @Query("doc_kind") docKind: String? = null,
        @Query("only_valid") onlyValid: Int? = null,
    ): SoldExportResponseDto
}
