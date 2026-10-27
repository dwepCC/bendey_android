package com.bendey.restaurant.core.domain.locale

/**
 * Catálogo de textos visibles que cambian por país (incremento I5 del paquete de país; mismas claves
 * que `country.LabelKeys` del backend, `pkg/country/labels.go`).
 *
 * Cada clave lleva su texto peruano ([pe]), que es la forma corta y en mayúsculas/minúsculas normales.
 * Las variantes de presentación («BOLETA» en un ticket, «Boleta electrónica», «RUC/DNI») las arma
 * quien muestra el texto a partir de esta forma corta, para que en Perú queden idénticas.
 *
 * Uso: `LabelKey.DOC_RECEIPT.text` (país vigente de la sesión) o `locale.label(LabelKey.TAX_ID)`.
 */
enum class LabelKey(val key: String, val pe: String) {
    // Identidad
    TAX_ID("tax_id", "RUC"),
    NATIONAL_ID("national_id", "DNI"),
    FOREIGN_ID("foreign_id", "Carnet de extranjería"),
    PASSPORT("passport", "Pasaporte"),
    BUSINESS_NAME("business_name", "Razón social"),
    FINAL_CONSUMER("final_consumer", "Clientes Varios"),

    // Impuestos y autoridad
    TAX_NAME("tax_name", "IGV"),
    TAX_AUTHORITY("tax_authority", "SUNAT"),
    TAX_TAXED("tax.taxed", "Gravado"),
    TAX_EXEMPT("tax.exempt", "Exonerado"),
    TAX_UNAFFECTED("tax.unaffected", "Inafecto"),
    SERVICE_CHARGE("service_charge", "Recargo al consumo"),

    // Comprobantes
    DOC_INVOICE("doc.invoice", "Factura"),
    DOC_RECEIPT("doc.receipt", "Boleta"),
    DOC_SALES_NOTE("doc.sales_note", "Nota de venta"),
    DOC_CREDIT_NOTE("doc.credit_note", "Nota de crédito"),
    DOC_DEBIT_NOTE("doc.debit_note", "Nota de débito"),
    DOC_DESPATCH("doc.despatch", "Guía de remisión"),

    // División territorial
    GEO_REGION("geo.region", "Departamento"),
    GEO_PROVINCE("geo.province", "Provincia"),
    GEO_DISTRICT("geo.district", "Distrito"),
    GEO_CODE("geo.code", "Ubigeo"),
    ;

    /** Texto de esta clave para el país de la sesión ([TenantLocaleHolder]). */
    val text: String
        get() = TenantLocaleHolder.current.label(this)

    /** Igual que [text] pero en MAYÚSCULAS (tickets, encabezados). En Perú: «BOLETA», «RUC»… */
    val upper: String
        get() = text.uppercase(java.util.Locale.ROOT)

    /**
     * Para los sitios que en Perú muestran una variante de mayúsculas/tildes que no sale de la forma
     * corta (p. ej. «Recargo al Consumo», «NOTA DE CREDITO»): en Perú devuelve [peVariant] tal cual,
     * en otro país la forma corta del servidor ([text]).
     */
    fun peruAs(peVariant: String): String =
        if (TenantLocaleHolder.current.isPeru) peVariant else text

    companion object {
        private val byKey: Map<String, LabelKey> = entries.associateBy { it.key }

        fun of(key: String): LabelKey? = byKey[key]

        /** Los textos peruanos exactos, por clave (lo que manda el backend para PE). */
        val LABELS_PE: Map<String, String> = entries.associate { it.key to it.pe }
    }
}
