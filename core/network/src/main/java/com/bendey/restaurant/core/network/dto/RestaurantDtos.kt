package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListResponseDto<T>(
    val data: List<T> = emptyList(),
)

@Serializable
data class FloorUpsertRequestDto(
    val name: String,
    @SerialName("sort_order") val sortOrder: Int? = null,
    val active: Boolean? = null,
)

@Serializable
data class TableUpsertRequestDto(
    @SerialName("floor_id") val floorId: Int,
    val name: String,
    val capacity: Int,
    val active: Boolean? = null,
)

@Serializable
data class FloorDto(
    val id: Int,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val active: Boolean = true,
)

@Serializable
data class RestaurantTableDto(
    val id: Int,
    @SerialName("floor_id") val floorId: Int,
    @SerialName("floor_name") val floorName: String? = null,
    val name: String,
    val capacity: Int = 0,
    val status: String = "libre",
    val active: Boolean = true,
    @SerialName("session_id") val sessionId: Int? = null,
    @SerialName("total_amount") val totalAmount: Double? = null,
    @SerialName("waiter_name") val waiterName: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val guests: Int? = null,
    @SerialName("opened_at") val openedAt: String? = null,
)

@Serializable
data class StaffOptionDto(
    val id: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("employee_type") val employeeType: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("staff_code") val staffCode: String? = null,
)

@Serializable
data class OpenSessionRequestDto(
    @SerialName("table_id") val tableId: Int? = null,
    @SerialName("staff_id") val staffId: Int? = null,
    val guests: Int? = null,
    val notes: String? = null,
    @SerialName("order_type") val orderType: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("customer_phone") val customerPhone: String? = null,
    @SerialName("save_as_draft") val saveAsDraft: Boolean? = null,
)

@Serializable
data class OpenSessionResponseDto(
    val success: Boolean = true,
    val data: SessionIdDto? = null,
)

@Serializable
data class SessionIdDto(
    val id: Int,
    @SerialName("order_code") val orderCode: String? = null,
)

@Serializable
data class OrderItemInputDto(
    @SerialName("item_kind") val itemKind: String? = "product",
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("product_code") val productCode: String? = null,
    @SerialName("product_name") val productName: String,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double,
    val notes: String? = null,
    @SerialName("modifiers_json") val modifiersJson: String? = null,
    @SerialName("igv_affectation_type") val igvAffectationType: String? = null,
    @SerialName("price_includes_igv") val priceIncludesIgv: Boolean? = null,
)

@Serializable
data class AddOrderRequestDto(
    @SerialName("staff_id") val staffId: Int? = null,
    val notes: String? = null,
    val items: List<OrderItemInputDto>,
)

@Serializable
data class ComandaDto(
    val id: Int,
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("session_id") val sessionId: Int = 0,
    @SerialName("product_name") val productName: String,
    @SerialName("product_code") val productCode: String? = null,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    val notes: String? = null,
    @SerialName("modifiers_json") val modifiersJson: String? = null,
    val status: String = "pendiente",
    @SerialName("preparation_area") val preparationArea: String? = null,
)

@Serializable
data class TableOrderDto(
    val id: Int,
    @SerialName("order_number") val orderNumber: Int = 0,
    val comandas: List<ComandaDto> = emptyList(),
)

@Serializable
data class AddOrderResponseDto(
    val success: Boolean = true,
    val data: TableOrderDto? = null,
)

@Serializable
data class KitchenComandaDto(
    val id: Int,
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("session_id") val sessionId: Int = 0,
    @SerialName("product_name") val productName: String,
    @SerialName("product_code") val productCode: String? = null,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    val notes: String? = null,
    @SerialName("modifiers_json") val modifiersJson: String? = null,
    val status: String = "pendiente",
    @SerialName("preparation_area") val preparationArea: String? = null,
    @SerialName("order_number") val orderNumber: Int? = null,
    @SerialName("order_code") val orderCode: String? = null,
    @SerialName("order_type") val orderType: String? = null,
    @SerialName("table_id") val tableId: Int? = null,
    @SerialName("table_name") val tableName: String? = null,
    @SerialName("floor_name") val floorName: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("waiter_name") val waiterName: String? = null,
    @SerialName("session_opened_at") val sessionOpenedAt: String? = null,
)

@Serializable
data class UpdateComandaStatusRequestDto(
    val status: String,
)

@Serializable
data class SuccessResponseDto(
    val success: Boolean = true,
)

@Serializable
data class SessionOrderDto(
    val id: Int,
    @SerialName("order_number") val orderNumber: Int = 0,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val comandas: List<ComandaDto> = emptyList(),
)

@Serializable
data class SessionDetailDto(
    val id: Int,
    @SerialName("table_id") val tableId: Int? = null,
    @SerialName("table_name") val tableName: String? = null,
    @SerialName("floor_name") val floorName: String? = null,
    @SerialName("waiter_name") val waiterName: String? = null,
    val guests: Int = 0,
    @SerialName("opened_at") val openedAt: String? = null,
    val status: String? = null,
    @SerialName("order_code") val orderCode: String? = null,
    @SerialName("order_type") val orderType: String? = null,
    @SerialName("order_status") val orderStatus: String? = null,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    val notes: String? = null,
    val orders: List<SessionOrderDto> = emptyList(),
)

@Serializable
data class SessionDetailResponseDto(
    val data: SessionDetailDto? = null,
)

@Serializable
data class PrecuentaLineDto(
    @SerialName("product_name") val productName: String,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("line_total") val lineTotal: Double? = null,
    val notes: String? = null,
)

@Serializable
data class PrecuentaDto(
    @SerialName("order_code") val orderCode: String? = null,
    @SerialName("order_type") val orderType: String? = null,
    @SerialName("table_name") val tableName: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val subtotal: Double? = null,
    @SerialName("tax_amount") val taxAmount: Double? = null,
    val total: Double = 0.0,
    val lines: List<PrecuentaLineDto> = emptyList(),
)

@Serializable
data class PrecuentaResponseDto(
    val data: PrecuentaDto? = null,
)
