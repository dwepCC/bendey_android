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
    val paymentMethod: String? = null,
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
    // Vacio = «sin declarar»: lo resuelve la politica del tenant en el backend.
    val igvAffectationType: String = "",
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
    // Moneda del país del tenant («PEN» en Perú, «USD» en Ecuador).
    val currency: String = com.bendey.restaurant.core.domain.locale.TenantLocaleHolder.current.currency,
    val paymentMethod: String,
    val notes: String? = null,
    val items: List<PurchaseItem>,
)

/** Tipos de comprobante que registra el proveedor (no un código SUNAT). */
val PURCHASE_DOC_TYPES = listOf("FACTURA", "BOLETA", "NOTA DE CRÉDITO", "TICKET")

/**
 * Respaldo para cuando todavía no cargaron los métodos reales del tenant (o la lista vino vacía).
 *
 * NO es la lista buena. El backend exige un método y con él decide a qué cuenta —caja o banco— le
 * descuenta la compra, así que los códigos tienen que ser los que ese tenant tiene configurados: si
 * el local renombró un método, agregó uno propio o desactivó Plin, esta lista fija le ofrece
 * opciones que no existen y le esconde las que sí. Los cinco de acá son los que el backend
 * normaliza igual en cualquier tenant, y sirven solo para que el formulario no quede sin opciones.
 *
 * Los reales se piden en [com.bendey.restaurant.feature.compras.ComprasViewModel]; mismo criterio
 * que Bendey Resto (Tauri), donde esto es PAYMENT_METHODS_FALLBACK.
 */
val PURCHASE_PAYMENT_METHODS_FALLBACK: List<Pair<String, String>>
    get() = if (com.bendey.restaurant.core.domain.locale.TenantLocaleHolder.current.isPeru) {
        listOf(
            "efectivo" to "Efectivo",
            "yape" to "Yape",
            "plin" to "Plin",
            "transferencia" to "Transferencia",
            "tarjeta" to "Tarjeta",
        )
    } else {
        // Ecuador: los 4 métodos del país (backend pkg/country/ec).
        listOf("efectivo" to "Efectivo", "tarjeta" to "Tarjeta", "transferencia" to "Transferencia", "cheque" to "Cheque")
    }

/** "" = todas. */
val PURCHASE_STATUS_FILTERS = listOf("" to "Todas", "received" to "Recibidas", "cancelled" to "Anuladas")

data class PurchaseListParams(
    val query: String = "",
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val status: String? = null,
)
