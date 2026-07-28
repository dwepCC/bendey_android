package com.bendey.restaurant.core.domain.purchases

data class Purchase(
    val id: Int,
    val docType: String,
    val series: String,
    val number: String,
    val issueDate: String,
    val contactId: Int?,
    val supplierName: String?,
    val subtotal: Double,
    val taxAmount: Double,
    val total: Double,
    val currency: String,
    val status: String,
    val notes: String?,
) {
    val isCancelled: Boolean get() = status == "cancelled"
    val documentLabel: String get() = if (series.isNotBlank()) "$series-$number" else number
}

data class PurchaseItem(
    val productId: Int?,
    val code: String,
    val description: String,
    val unit: String,
    val quantity: Double,
    val unitCost: Double,
    val igvAffectationType: String = "10",
    val priceIncludesIgv: Boolean = false,
) {
    val lineTotal: Double get() = quantity * unitCost
}

data class PurchaseDetail(
    val purchase: Purchase,
    val items: List<PurchaseItem>,
)

data class CreatePurchaseInput(
    val branchId: Int? = null,
    val contactId: Int,
    val docType: String = "FACTURA",
    val series: String = "",
    val number: String,
    val issueDate: String,
    val currency: String = "PEN",
    val paymentMethod: String? = null,
    val notes: String? = null,
    val items: List<PurchaseItem>,
)

/** Tipos de comprobante que registra el proveedor (no un código SUNAT). */
val PURCHASE_DOC_TYPES = listOf("FACTURA", "BOLETA", "NOTA DE CRÉDITO", "TICKET")

/** Mismos métodos que Caja/checkout — si se asigna uno, el backend descuenta esa cuenta. */
val PURCHASE_PAYMENT_METHODS = listOf("efectivo", "yape", "plin", "transferencia", "tarjeta")
