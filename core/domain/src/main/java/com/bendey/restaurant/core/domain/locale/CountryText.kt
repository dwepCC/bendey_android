package com.bendey.restaurant.core.domain.locale

/**
 * Textos fijos escritos para Perú, adaptados al país del tenant. Gemelo de `localizeText` /
 * `hasReceiptDoc` de Tauri (`tenantLabels.ts`): mismas reglas.
 */
object CountryText {
    /** El país emite boleta además de factura (Perú). En Ecuador «Boleta» y «Factura» son lo mismo. */
    fun hasReceiptDoc(): Boolean =
        !LabelKey.DOC_RECEIPT.text.equals(LabelKey.DOC_INVOICE.text, ignoreCase = true)

    /**
     * SUNAT → autoridad del país, IGV → su impuesto, «S/ 10» → «$10» y, si el país no tiene boleta,
     * «boletas y facturas» → «facturas». Regla «Perú idéntico»: en Perú devuelve el texto tal cual.
     */
    fun localize(s: String): String {
        if (s.isEmpty() || TenantLocaleHolder.current.isPeru) return s
        var out = s
        if (!hasReceiptDoc()) {
            out = out
                .replace(Regex("""(?i)(notas?) de venta, boletas y facturas""")) { "${it.groupValues[1]} de venta y facturas" }
                .replace(Regex("""(Notas?) de Venta, Boleta o Factura""")) { "${it.groupValues[1]} de Venta o Factura" }
                .replace(Regex("""\b([Bb])oletas, facturas""")) { if (it.groupValues[1] == "B") "Facturas" else "facturas" }
                .replace(Regex("""\b(boletas y facturas|facturas y boletas)\b"""), "facturas")
                .replace(Regex("""\b(Boletas y facturas|Facturas y boletas)\b"""), "Facturas")
                .replace(Regex("""\b(boleta o factura|factura o boleta)\b"""), "factura")
                .replace(Regex("""\b(Boleta o factura|Factura o boleta)\b"""), "Factura")
                .replace(Regex("""\b(boletas o facturas|facturas o boletas)\b"""), "facturas")
        }
        return out
            .replace(Regex("""\bSUNAT\b"""), Regex.escapeReplacement(LabelKey.TAX_AUTHORITY.text))
            .replace(Regex("""\bIGV\b"""), Regex.escapeReplacement(LabelKey.TAX_NAME.text))
            .replace(Regex("""S/\s?(?=\d)"""), Regex.escapeReplacement(Money.prefix()))
    }
}
