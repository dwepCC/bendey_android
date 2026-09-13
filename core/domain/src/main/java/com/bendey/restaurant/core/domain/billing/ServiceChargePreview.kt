package com.bendey.restaurant.core.domain.billing

/**
 * Previsualización del Recargo al Consumo (RC) en el checkout — mismo criterio que
 * `front_tenant_restaurant_tauri/src/utils/serviceChargePreview.ts`.
 *
 * El backend es quien calcula y congela el RC de verdad (internal/servicecharge.Resolve, sobre el
 * subtotal neto de descuento, sin IGV) al crear la venta — este helper NUNCA se envía al backend.
 * Existe únicamente para que el checkout muestre y sume al total el monto CORRECTO antes de
 * cobrar: sin esto, el cajero cobra el total sin RC y el backend rechaza el pago por insuficiente
 * en cuanto agrega el RC al total real ("monto pagado menor al total").
 *
 * `subtotalRatio` es subtotal-sin-IGV / total-con-IGV de lo que se está cobrando — Android no
 * trae desglose de afectación por línea en el carrito (a diferencia del POS de escritorio), así
 * que siempre se asume 100% gravado con la tasa de IGV vigente (`CheckoutMeta.taxRate`), igual
 * que hace Mesa en Tauri.
 */
fun calcServiceChargePreview(
    total: Double,
    discountAmount: Double,
    rate: Double,
    enabled: Boolean,
    taxRatePercent: Double,
): Double {
    if (!enabled || rate <= 0 || total <= 0) return 0.0
    val subtotalRatio = 1.0 / (1.0 + taxRatePercent / 100.0)
    val rawSubtotal = total * subtotalRatio
    val netSubtotal = (rawSubtotal - discountAmount * subtotalRatio).coerceAtLeast(0.0)
    return roundDisplay(netSubtotal * rate / 100.0)
}

/** Label corto del RC en pantalla: "RC 5%" con el porcentaje configurado, o genérico si es 0. */
fun formatServiceChargeLabel(rate: Double): String {
    if (rate <= 0) return "Recargo al Consumo"
    val trimmed = if (rate == rate.toLong().toDouble()) {
        rate.toLong().toString()
    } else {
        "%.2f".format(rate).trimEnd('0').trimEnd('.')
    }
    return "RC $trimmed%"
}
