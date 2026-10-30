package com.bendey.restaurant.core.data.receipt

import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.core.domain.billing.SalePrintLine
import com.bendey.restaurant.core.domain.billing.affectationTotalsRows
import com.bendey.restaurant.core.domain.locale.TenantLocale
import com.bendey.restaurant.core.domain.locale.TenantLocaleHolder
import java.text.NumberFormat
import java.util.Locale

/** Una fila de totales: etiqueta, monto ya formateado y si va destacada (VALOR TOTAL). */
internal data class EcTotalRow(val label: String, val amount: Double, val strong: Boolean = false)

/** Valores de una línea del detalle como los imprime el RIDE: P. U., descuento y total sin IVA. */
internal data class EcLineAmounts(val unitPrice: Double, val discount: Double, val base: Double)

/**
 * Lógica (sin dibujo) del RIDE de Ecuador, la misma que ya usan el ERP y Resto Tauri
 * (`receiptPdfEc.ts`): qué comprobantes lo usan, líneas del detalle, totales por tarifa e información adicional.
 */
internal object EcRideModel {
    /** Tipos del SRI que se dibujan con el diseño del RIDE (factura, nota de crédito, nota de débito). */
    val RIDE_TITLES = mapOf("01" to "FACTURA", "04" to "NOTA DE CRÉDITO", "05" to "NOTA DE DÉBITO")

    val TICKET_TITLES = mapOf(
        "01" to "FACTURA ELECTRÓNICA N°:",
        "04" to "NOTA DE CRÉDITO ELECTRÓNICA N°:",
        "05" to "NOTA DE DÉBITO ELECTRÓNICA N°:",
    )

    /** ¿Este comprobante se dibuja con el RIDE? Solo países sin reglas de SUNAT y con tipo del SRI. */
    fun usesEcRide(data: SalePrintData, locale: TenantLocale = TenantLocaleHolder.current): Boolean =
        !locale.isPeru && data.sunatCode in RIDE_TITLES

    private val ALL_NINES = Regex("^9+$")

    /** Consumidor final: sin cliente, tipo de identificación 07 o identificación de solo nueves. */
    fun isFinalConsumer(data: SalePrintData): Boolean {
        val doc = data.clientDocNumber?.trim().orEmpty()
        if (data.clientName == null && data.clientDocNumber == null) return true
        return data.clientDocType == "07" || ALL_NINES.matches(doc)
    }

    fun lineAmounts(item: SalePrintLine): EcLineAmounts {
        // Versión vieja del backend sin `subtotal`: el total de la línea es lo único que hay.
        val base = if (item.subtotal != 0.0) item.subtotal else item.total
        val disc = item.discount
        val pu = if (item.quantity != 0.0) (base + disc) / item.quantity else 0.0
        return EcLineAmounts(pu, disc, base)
    }

    fun qty(n: Double): String = if (n == Math.floor(n) && !n.isInfinite()) n.toLong().toString() else "%.2f".format(Locale.US, n)

    fun fixed2(n: Double): String = "%.2f".format(Locale.US, n)

    fun totalDiscount(data: SalePrintData): Double = data.items.sumOf { it.discount }

    /** Número que va en «NÚMERO AUTORIZACIÓN»: el del SRI y, mientras no autorice, la clave de acceso. */
    fun authText(data: SalePrintData): String =
        data.authorizationNumber?.trim().orEmpty().ifEmpty { data.accessKey?.trim().orEmpty() }

    fun isAuthorized(data: SalePrintData): Boolean =
        !data.authorizationNumber.isNullOrBlank() && !data.ec?.authorizedAt.isNullOrBlank()

    fun environmentLabel(data: SalePrintData): String = if (data.fiscalEnvironment == "produccion") "PRODUCCIÓN" else "PRUEBAS"

    /** Totales del A4: bases por tarifa, subtotal sin impuestos, descuento, impuestos, propina y valor total. */
    fun ridTotals(data: SalePrintData, locale: TenantLocale = TenantLocaleHolder.current): List<EcTotalRow> {
        val rows = affectationTotalsRows(data.affectationTotals, data.taxAmount, locale)
        val out = mutableListOf<EcTotalRow>()
        rows.filter { it.key.startsWith("base_") }.forEach { out += EcTotalRow(it.label.uppercase(), it.amount) }
        out += EcTotalRow("SUBTOTAL SIN IMPUESTOS", data.subtotal)
        val disc = totalDiscount(data)
        if (disc > 0) out += EcTotalRow("TOTAL DESCUENTO", disc)
        rows.filter { it.key.startsWith("tax_") }.forEach { out += EcTotalRow(it.label.uppercase(), it.amount) }
        if (data.serviceChargeAmount > 0) out += EcTotalRow("PROPINA", data.serviceChargeAmount)
        out += EcTotalRow("VALOR TOTAL", data.total, strong = true)
        return out
    }

    /** Totales del ticket (formato de la referencia): subtotal, bases por tarifa, descuento, impuestos, propina. */
    fun ticketTotals(data: SalePrintData, locale: TenantLocale = TenantLocaleHolder.current): List<EcTotalRow> {
        val rows = affectationTotalsRows(data.affectationTotals, data.taxAmount, locale)
        val out = mutableListOf<EcTotalRow>()
        out += EcTotalRow("Subtotal", data.subtotal)
        rows.filter { it.key.startsWith("base_") }.forEach { out += EcTotalRow(it.label, it.amount) }
        val disc = totalDiscount(data)
        if (disc > 0) out += EcTotalRow("Descuento", disc)
        rows.filter { it.key.startsWith("tax_") }.forEach { out += EcTotalRow(it.label, it.amount) }
        if (data.serviceChargeAmount > 0) out += EcTotalRow("Propina", data.serviceChargeAmount)
        return out
    }

    /** «Información Adicional» del A4: correo y teléfono del comprador (si no es consumidor final) y observaciones. */
    fun additionalInfo(data: SalePrintData): List<Pair<String, String>> {
        val cons = isFinalConsumer(data)
        val out = mutableListOf<Pair<String, String>>()
        data.ec?.clientEmail?.takeIf { !cons && it.isNotBlank() }?.let { out += "Correo" to it }
        data.clientPhone?.takeIf { !cons && it.isNotBlank() }?.let { out += "Teléfono" to it }
        data.notes?.takeIf { it.isNotBlank() }?.let { out += "Observaciones" to it }
        return out
    }

    /** ¿Lleva el bloque «Comprobante que se modifica»? Solo NC/ND con comprobante afectado. */
    fun hasAffectedDoc(data: SalePrintData): Boolean = data.sunatCode != "01" && !data.affectedDocNumber.isNullOrBlank()

    fun money(format: NumberFormat, n: Double): String = format.format(n)
}
