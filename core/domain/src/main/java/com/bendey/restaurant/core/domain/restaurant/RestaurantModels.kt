package com.bendey.restaurant.core.domain.restaurant

enum class TableStatus(val backendValue: String, val label: String) {
    LIBRE("libre", "Libre"),
    OCUPADA("ocupada", "Ocupada"),
    RESERVADA("reservada", "Reservada"),
    EN_CONSUMO("en_consumo", "Por cerrar"),
    ;

    companion object {
        fun fromBackend(value: String): TableStatus =
            entries.firstOrNull { it.backendValue == value } ?: LIBRE
    }
}

enum class ComandaStatus(val backendValue: String, val label: String) {
    PENDIENTE("pendiente", "Pendiente"),
    PREPARACION("preparacion", "En preparación"),
    LISTA("lista", "Listo"),
    ENTREGADA("entregada", "Entregado"),
    ;

    companion object {
        fun fromBackend(value: String): ComandaStatus = when (value) {
            "preparacion", "lista", "entregada" -> entries.first { it.backendValue == value }
            else -> PENDIENTE
        }

        fun next(current: String): ComandaStatus? = when (fromBackend(current)) {
            PENDIENTE -> PREPARACION
            PREPARACION -> LISTA
            LISTA -> ENTREGADA
            ENTREGADA -> null
        }
    }
}

data class Floor(
    val id: Int,
    val name: String,
    val sortOrder: Int,
)

data class RestaurantTable(
    val id: Int,
    val floorId: Int,
    val floorName: String?,
    val name: String,
    val capacity: Int,
    val status: TableStatus,
    val sessionId: Int?,
    val totalAmount: Double?,
    val waiterName: String?,
    val guests: Int?,
) {
    val isClickable: Boolean
        get() = status == TableStatus.LIBRE ||
            (status == TableStatus.OCUPADA && sessionId != null) ||
            (status == TableStatus.EN_CONSUMO && sessionId != null)
}

data class StaffOption(
    val id: Int,
    val displayName: String,
    val employeeType: String,
)

data class ProductCategory(
    val id: Int,
    val name: String,
)

data class PosProduct(
    val id: Int,
    val code: String,
    val name: String,
    val salePrice: Double,
    val categoryId: Int?,
    val imageUrl: String?,
    val preparationArea: String? = null,
    val igvAffectationType: String?,
    val priceIncludesIgv: Boolean?,
    val hasModifiers: Boolean = false,
    val hasVariants: Boolean = false,
    val manageStock: Boolean = false,
    val availableForSale: Boolean = true,
)

data class PosCartLine(
    val cartKey: String = "",
    val product: PosProduct,
    val quantity: Int,
    val notes: String = "",
    val itemKind: String = "product",
    val unitPrice: Double? = null,
    val modifiersJson: String? = null,
    val comboId: Int? = null,
    val comboConfigJson: String? = null,
    val subtitle: String? = null,
    val comboSummaryLines: List<String> = emptyList(),
) {
    val key: String get() = cartKey.ifBlank { "p-${product.id}" }
    val effectiveUnitPrice: Double get() = unitPrice ?: product.salePrice
    val lineTotal: Double get() = effectiveUnitPrice * quantity
}

data class OrderItemInput(
    val itemKind: String = "product",
    val productId: Int? = null,
    val productCode: String = "",
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val notes: String? = null,
    val modifiersJson: String? = null,
    val comboId: Int? = null,
    val comboConfigJson: String? = null,
    val igvAffectationType: String? = null,
    val priceIncludesIgv: Boolean? = null,
)

data class ComandaLine(
    val id: Int,
    val productName: String,
    val quantity: Double,
    val notes: String?,
    val modifiersJson: String?,
    val status: ComandaStatus,
    val preparationArea: String? = null,
    val comboSnapshotJson: String? = null,
)

data class AddOrderResult(
    val orderId: Int,
    val orderNumber: Int,
    val comandas: List<ComandaLine>,
)

data class OpenSessionResult(
    val sessionId: Int,
    val orderCode: String?,
)

data class SessionComandaSummary(
    val id: Int,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val status: ComandaStatus,
    val notes: String?,
    val modifiersJson: String? = null,
    val preparationArea: String? = null,
    val comboSnapshotJson: String? = null,
)

fun SessionComandaSummary.toComandaLine() = ComandaLine(
    id = id,
    productName = productName,
    quantity = quantity,
    notes = notes,
    modifiersJson = modifiersJson,
    status = status,
    preparationArea = preparationArea,
    comboSnapshotJson = comboSnapshotJson,
)

data class SessionOrderSummary(
    val id: Int,
    val orderNumber: Int,
    val comandas: List<SessionComandaSummary>,
)

data class TableSessionDetail(
    val id: Int,
    val tableName: String?,
    val floorName: String?,
    val waiterName: String?,
    val guests: Int,
    val orderCode: String?,
    val totalAmount: Double,
    val orderType: String? = null,
    val contactId: Int? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val deliveryAddress: String? = null,
    val deliveryReference: String? = null,
    val deliveryDriverId: Int? = null,
    val driverName: String? = null,
    val notes: String? = null,
    val estimatedMinutes: Int? = null,
    val orders: List<SessionOrderSummary>,
)

data class PosSessionInput(
    val orderType: String,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val contactId: Int? = null,
    val deliveryDriverId: Int? = null,
    val deliveryAddress: String? = null,
    val deliveryReference: String? = null,
    val estimatedMinutes: Int? = null,
    val notes: String? = null,
    val saveAsDraft: Boolean = false,
)

data class OpenOrderSummary(
    val id: Int,
    val orderCode: String?,
    val orderType: String,
    val orderStatus: String,
    val customerName: String?,
    val customerPhone: String?,
    val deliveryAddress: String?,
    val totalAmount: Double,
    val itemCount: Int,
)

data class DeliveryDriverBrief(
    val id: Int,
    val name: String,
)

data class PrecuentaLine(
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val lineTotal: Double,
)

data class PrecuentaData(
    val tableName: String?,
    val orderCode: String?,
    val total: Double,
    val lines: List<PrecuentaLine>,
)

data class KitchenItem(
    val id: Int,
    val productName: String,
    val quantity: Double,
    val notes: String?,
    val modifiersJson: String?,
    val status: ComandaStatus,
    val orderNumber: Int?,
    val orderCode: String?,
    val tableName: String?,
    val floorName: String?,
    val customerName: String?,
    val waiterName: String?,
    val orderType: String?,
    val preparationArea: String? = null,
    val comboSnapshotJson: String? = null,
    val createdAt: String? = null,
    val sessionOpenedAt: String? = null,
    val displayKey: String = "",
    val displayName: String = "",
    val displayQuantity: Double = 0.0,
    val modifierLines: List<String> = emptyList(),
    val isComboComponent: Boolean = false,
    val comboName: String? = null,
) {
    val kdsName: String get() = displayName.ifBlank { productName }
    val kdsQuantity: Double get() = if (displayQuantity > 0) displayQuantity else quantity
    val kdsKey: String get() = displayKey.ifBlank { id.toString() }
}

data class TableStats(
    val total: Int,
    val libre: Int,
    val ocupada: Int,
    val reservada: Int,
    val enConsumo: Int,
)

fun List<RestaurantTable>.toTableStats(): TableStats {
    var libre = 0
    var ocupada = 0
    var reservada = 0
    var enConsumo = 0
    forEach { table ->
        when (table.status) {
            TableStatus.LIBRE -> libre++
            TableStatus.OCUPADA -> ocupada++
            TableStatus.RESERVADA -> reservada++
            TableStatus.EN_CONSUMO -> enConsumo++
        }
    }
    return TableStats(size, libre, ocupada, reservada, enConsumo)
}
