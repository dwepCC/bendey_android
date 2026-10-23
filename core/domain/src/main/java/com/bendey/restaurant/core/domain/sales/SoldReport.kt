package com.bendey.restaurant.core.domain.sales

import com.bendey.restaurant.core.domain.delivery.ReportRange
import com.bendey.restaurant.core.domain.delivery.ReportRangeRules
import com.bendey.restaurant.core.domain.model.AppResult
import java.time.LocalDate
import java.util.Locale

/** Reporte «Productos vendidos» (GET /api/restaurant/reports/sold-products). */
data class SoldItem(
    val description: String,
    val components: List<String>,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double,
)

data class SoldPayment(val method: String, val amount: Double)

data class SoldSale(
    val saleId: Int,
    val issueDate: String,
    val registeredAt: String,
    val docKind: String,
    val number: String,
    val valid: Boolean,
    val cashierName: String,
    val waiterName: String?,
    val service: String,
    val orderType: String,
    val tableName: String?,
    val total: Double,
    val payments: List<SoldPayment>,
    val items: List<SoldItem>,
)

data class SoldReportSummary(
    val validSales: Int = 0,
    val validAmount: Double = 0.0,
    val cancelledSales: Int = 0,
    val cancelledAmount: Double = 0.0,
)

data class SoldReportPage(
    val total: Int,
    val summary: SoldReportSummary,
    val sales: List<SoldSale>,
)

/** Una fila por producto vendido, con los datos de su venta repetidos (exportación). */
data class SoldExportRow(
    val issueDate: String,
    val registeredAt: String,
    val docKind: String,
    val number: String,
    val valid: Boolean,
    val service: String,
    val orderType: String,
    val tableName: String?,
    val cashierName: String,
    val waiterName: String?,
    val payments: String,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val lineTotal: Double,
    val saleTotal: Double,
)

data class SoldExport(val rows: List<SoldExportRow>, val truncated: Boolean)

data class SoldReportFilter(
    val from: String,
    val to: String,
    val userId: Int? = null,
    val paymentMethod: String? = null,
    val docKind: String? = null,
    val onlyValid: Boolean = false,
)

interface SoldReportRepository {
    suspend fun page(filter: SoldReportFilter, page: Int, perPage: Int): AppResult<SoldReportPage>

    /** Exportación completa del rango; el servidor verifica que el plan permita exportar. */
    suspend fun export(filter: SoldReportFilter): AppResult<SoldExport>
}

/** Rangos del reporte: hoy (por defecto), el mes en curso o fechas elegidas. */
enum class SoldReportPreset(val label: String) {
    TODAY("Hoy"),
    MONTH("Mes actual"),
    CUSTOM("Elegir fechas"),
}

object SoldReportRules {
    const val PER_PAGE = 25

    val DOC_KINDS: List<Pair<String, String>> = listOf(
        "boleta" to "Boleta",
        "factura" to "Factura",
        "nota_venta" to "Nota de venta",
    )

    /** Hoy = solo hoy; Mes actual = del día 1 a hoy (en Lima). CUSTOM no tiene rango propio. */
    fun preset(preset: SoldReportPreset, today: LocalDate = ReportRangeRules.todayLima()): ReportRange? = when (preset) {
        SoldReportPreset.TODAY -> ReportRange(today, today)
        SoldReportPreset.MONTH -> ReportRange(today.withDayOfMonth(1), today)
        SoldReportPreset.CUSTOM -> null
    }

    fun docKindLabel(kind: String): String = DOC_KINDS.firstOrNull { it.first == kind }?.second ?: "Nota de venta"

    /** «Mesa 4», «Mesa» si no se sabe cuál, o el tipo de venta directa. */
    fun serviceLabel(service: String, orderType: String, tableName: String?): String {
        if (service == "mesa") return tableName?.takeIf { it.isNotBlank() } ?: "Mesa"
        return when (orderType) {
            "takeaway" -> "Para llevar"
            "delivery" -> "Delivery"
            else -> "Venta directa"
        }
    }

    fun money(value: Double): String = String.format(Locale.US, "S/ %.2f", value)

    /** «Yape S/ 20.00 + Efectivo S/ 5.00». */
    fun paymentsLabel(payments: List<SoldPayment>): String =
        if (payments.isEmpty()) "—" else payments.joinToString(" + ") { "${salePaymentMethodLabelEs(it.method)} ${money(it.amount)}" }

    /** Cantidad sin decimales de más: 2 → «2», 1.5 → «1.5». */
    fun qty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')

    /** Línea con los platos del combo: «Combo Duo (2× Pizza, 1× Gaseosa)». */
    fun itemLabel(description: String, components: List<String>): String =
        if (components.isEmpty()) description else "$description (${components.joinToString(", ")})"
}
