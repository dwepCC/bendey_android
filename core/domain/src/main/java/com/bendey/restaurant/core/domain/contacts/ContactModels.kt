package com.bendey.restaurant.core.domain.contacts

enum class ContactDocType(val code: String, val label: String) {
    SIN_RUC("0", "Sin RUC"),
    DNI("1", "DNI"),
    RUC("6", "RUC"),
    CE("4", "Carnet extranjería"),
    PASAPORTE("7", "Pasaporte"),
    ;

    companion object {
        fun fromCode(code: String?): ContactDocType =
            entries.firstOrNull { it.code == code?.trim() } ?: RUC

        fun supportsConsulta(code: String): Boolean {
            val c = fromCode(code)
            return c == DNI || c == RUC
        }
    }
}

data class CustomerContact(
    val id: Int,
    val docType: String,
    val docNumber: String,
    val businessName: String,
    val tradeName: String?,
    val address: String?,
    val ubigeo: String?,
    val phone: String?,
    val email: String?,
    val active: Boolean,
) {
    val displayName: String get() = businessName.ifBlank { tradeName.orEmpty() }.ifBlank { docNumber }
    val docLabel: String get() = ContactDocType.fromCode(docType).label
}

data class ContactFormInput(
    val docType: ContactDocType = ContactDocType.RUC,
    val docNumber: String = "",
    val businessName: String = "",
    val tradeName: String = "",
    val address: String = "",
    val ubigeo: String = "",
    val phone: String = "",
    val email: String = "",
)

data class ConsultaRucResult(
    val success: Boolean,
    val razonSocial: String? = null,
    val direccion: String? = null,
    val ubigeo: String? = null,
)

data class ConsultaDniResult(
    val success: Boolean,
    val nombreCompleto: String? = null,
)

fun CustomerContact.toFormInput() = ContactFormInput(
    docType = ContactDocType.fromCode(docType),
    docNumber = docNumber,
    businessName = businessName,
    tradeName = tradeName.orEmpty(),
    address = address.orEmpty(),
    ubigeo = ubigeo.orEmpty(),
    phone = phone.orEmpty(),
    email = email.orEmpty(),
)

fun sanitizeDocNumber(docType: ContactDocType, raw: String): String {
    val trimmed = raw.trim()
    return when (docType) {
        ContactDocType.RUC -> trimmed.filter { it.isDigit() }.take(11)
        ContactDocType.DNI -> trimmed.filter { it.isDigit() }.take(8)
        else -> trimmed.take(20)
    }
}

// -------- Historial de compras de un cliente (sección 23 del contrato) --------

data class PurchaseHistorySummary(
    val orders: Int = 0,
    val totalSpent: Double = 0.0,
    val avgTicket: Double = 0.0,
    /** ISO tal cual llegó del servidor; null si nunca compró. */
    val firstVisit: String? = null,
    val lastVisit: String? = null,
)

data class PurchaseTopProduct(val productId: Int, val description: String, val quantity: Double, val total: Double)

data class PurchaseSale(
    val id: Int,
    val number: String,
    val docType: String,
    val issueDate: String,
    val total: Double,
    val status: String,
    val sessionId: Int?,
)

data class PurchaseHistoryPage(
    val summary: PurchaseHistorySummary = PurchaseHistorySummary(),
    val topProducts: List<PurchaseTopProduct> = emptyList(),
    val sales: List<PurchaseSale> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
) {
    val hasMore: Boolean get() = page * limit < total
}

object PurchaseHistoryRules {
    const val PAGE_SIZE = 20

    /** El cliente genérico («clientes varios», documento 99999999 / 00000000) no tiene historial propio. */
    fun isGenericClient(docNumber: String, businessName: String): Boolean {
        val doc = docNumber.replace("-", "").replace(" ", "").trim()
        return doc == "99999999" || doc == "00000000" || businessName.trim().equals("CLIENTES VARIOS", ignoreCase = true)
    }

    /** Une la página nueva a lo ya cargado sin repetir ventas. */
    fun append(current: PurchaseHistoryPage, next: PurchaseHistoryPage): PurchaseHistoryPage {
        val known = current.sales.map { it.id }.toSet()
        return next.copy(sales = current.sales + next.sales.filter { it.id !in known })
    }

    fun docLabel(code: String): String = when (code.lowercase()) {
        "boleta", "03" -> "Boleta"
        "factura", "01" -> "Factura"
        "nota_venta", "nota de venta", "nv" -> "Nota de venta"
        else -> code.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    fun money(v: Double): String = String.format(java.util.Locale.US, "S/ %.2f", v)

    fun quantity(q: Double): String =
        if (q == Math.floor(q)) q.toLong().toString() else String.format(java.util.Locale.US, "%.2f", q)
}
