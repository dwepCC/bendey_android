package com.bendey.restaurant.core.domain.contacts

import com.bendey.restaurant.core.domain.locale.LabelKey
import com.bendey.restaurant.core.domain.locale.Money
import com.bendey.restaurant.core.domain.locale.TenantLocaleHolder

enum class ContactDocType(val code: String) {
    SIN_RUC("0"),
    DNI("1"),
    RUC("6"),
    CE("4"),
    PASAPORTE("7"),

    // Ecuador (tabla 6 del SRI). Solo se ofrecen cuando el locale del tenant los trae en `identity_types`.
    EC_RUC("04"),
    EC_CEDULA("05"),
    EC_PASAPORTE("06"),
    EC_CONSUMIDOR_FINAL("07"),
    EC_EXTERIOR("08"),
    ;

    /** Tipo peruano (catálogo SUNAT 06) o del país del locale. */
    val isPeruvian: Boolean
        get() = this in PERU_TYPES

    /** Texto visible del tipo de documento, según el país de la sesión (I5). En Perú no cambia. */
    val label: String
        get() = when (this) {
            SIN_RUC -> if (TenantLocaleHolder.current.isPeru) {
                "Sin ${LabelKey.TAX_ID.text}"
            } else {
                // Ecuador: el «Clientes Varios» con documento 0 es consumidor final ante el SRI.
                TenantLocaleHolder.current.finalConsumerIdentity?.label ?: LabelKey.FINAL_CONSUMER.text
            }
            DNI -> LabelKey.NATIONAL_ID.text
            RUC -> LabelKey.TAX_ID.text
            CE -> LabelKey.FOREIGN_ID.peruAs("Carnet extranjería")
            PASAPORTE -> LabelKey.PASSPORT.text
            EC_RUC, EC_CEDULA, EC_PASAPORTE, EC_CONSUMIDOR_FINAL, EC_EXTERIOR ->
                TenantLocaleHolder.current.identityByCode(code)?.label ?: EC_FALLBACK_LABELS.getValue(this)
        }

    companion object {
        private val PERU_TYPES = setOf(SIN_RUC, DNI, RUC, CE, PASAPORTE)

        private val EC_FALLBACK_LABELS: Map<ContactDocType, String> = mapOf(
            EC_RUC to "RUC",
            EC_CEDULA to "Cédula",
            EC_PASAPORTE to "Pasaporte",
            EC_CONSUMIDOR_FINAL to "Consumidor final",
            EC_EXTERIOR to "Identificación del exterior",
        )

        fun fromCode(code: String?): ContactDocType =
            entries.firstOrNull { it.code == code?.trim() } ?: RUC

        fun supportsConsulta(code: String): Boolean {
            val c = fromCode(code)
            return c == DNI || c == RUC
        }

        /**
         * Tipos que ofrecen los formularios de cliente/proveedor. Perú: los 5 de siempre, en el mismo
         * orden. Ecuador (o cualquier país con `identity_types`): los del locale que la app conoce, en su
         * orden; [includeFinalConsumer] = false oculta el tipo «other» (alta rápida en el cobro, como el ERP).
         */
        fun options(includeFinalConsumer: Boolean = true): List<ContactDocType> {
            val locale = TenantLocaleHolder.current
            if (locale.isPeru || locale.identityTypes.isEmpty()) return PERU_TYPES.toList()
            val list = locale.identityTypes
                .filter { includeFinalConsumer || it.key != "other" }
                .mapNotNull { t -> entries.firstOrNull { it.code == t.code && !it.isPeruvian } }
            return list.ifEmpty { PERU_TYPES.toList() }
        }

        /** Tipo con el que abre el formulario de un cliente nuevo: RUC en Perú, cédula en Ecuador. */
        fun defaultForCustomer(): ContactDocType = defaultFor("national_id")

        /** Tipo con el que abre el formulario de un proveedor nuevo: RUC (el del país). */
        fun defaultForSupplier(): ContactDocType = defaultFor("tax_id")

        private fun defaultFor(key: String): ContactDocType {
            val locale = TenantLocaleHolder.current
            if (locale.isPeru) return RUC
            val code = locale.identityByKey(key)?.code ?: return RUC
            return entries.firstOrNull { it.code == code && !it.isPeruvian } ?: RUC
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
    val docType: ContactDocType = ContactDocType.defaultForCustomer(),
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
        // Ecuador: RUC 13 dígitos, cédula 10, consumidor final 13 (9999999999999).
        ContactDocType.EC_RUC, ContactDocType.EC_CONSUMIDOR_FINAL -> trimmed.filter { it.isDigit() }.take(13)
        ContactDocType.EC_CEDULA -> trimmed.filter { it.isDigit() }.take(10)
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
        "boleta", "03" -> LabelKey.DOC_RECEIPT.text
        "factura", "01" -> LabelKey.DOC_INVOICE.text
        "nota_venta", "nota de venta", "nv" -> LabelKey.DOC_SALES_NOTE.text
        else -> code.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    fun money(v: Double): String = Money.format(v)

    fun quantity(q: Double): String =
        if (q == Math.floor(q)) q.toLong().toString() else String.format(java.util.Locale.US, "%.2f", q)
}
