package com.bendey.restaurant.core.domain.inventory

import com.bendey.restaurant.core.domain.model.AppResult

data class StockMovementItem(
    val id: Int,
    val productId: Int,
    val productCode: String?,
    val productName: String?,
    val branchId: Int,
    val branchName: String?,
    val type: String,
    val quantity: Double,
    val balance: Double?,
    val reference: String?,
    val notes: String?,
    val userName: String?,
    val createdAt: String,
)

data class StockMovementQuery(
    val productQ: String = "",
    val branchId: Int? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val movementKind: String? = null,
    val refNotesQ: String = "",
    val page: Int = 1,
    val perPage: Int = 25,
)

data class InventoryAdjustmentInput(
    val productId: Int,
    val branchId: Int,
    /** Requerido si el producto lleva stock por presentación. */
    val presentationId: Int? = null,
    val type: String,
    val quantity: Double,
    val notes: String,
)

interface InventoryRepository {
    suspend fun getStock(productId: Int, branchId: Int? = null): AppResult<Double>

    suspend fun listMovements(query: StockMovementQuery): AppResult<Pair<List<StockMovementItem>, Int>>

    suspend fun listAllMovementsForExport(query: StockMovementQuery): AppResult<List<StockMovementItem>>

    suspend fun recordAdjustment(input: InventoryAdjustmentInput): AppResult<Unit>
}

// -------- Valorización y conteo físico (sección 25 del contrato) --------

data class ValuationRow(
    val productId: Int,
    val presentationId: Int,
    val code: String,
    val name: String,
    val quantity: Double,
    val averageCost: Double,
    val value: Double,
    val negative: Boolean,
)

data class InventoryValuation(
    val branchId: Int = 0,
    val rows: List<ValuationRow> = emptyList(),
    val totalValue: Double = 0.0,
    val totalProducts: Int = 0,
    val negativeCount: Int = 0,
)

data class CountLineInput(val productId: Int, val presentationId: Int, val counted: Double)

data class CountResultLine(
    val productId: Int,
    val presentationId: Int,
    val name: String,
    val system: Double,
    val counted: Double,
    val difference: Double,
    val unitCost: Double,
    val valueDiff: Double,
    val applied: Boolean,
    val error: String?,
)

data class PhysicalCountResult(
    val applied: Boolean = false,
    val lines: List<CountResultLine> = emptyList(),
    val adjustedCount: Int = 0,
    val failedCount: Int = 0,
    val totalValueDiff: Double = 0.0,
)

/** Reglas puras del conteo físico y de la valorización. */
object PhysicalCountRules {
    const val MAX_LINES = 500

    /** Cantidad tecleada -> número (≥ 0); null si está vacía o no sirve. */
    fun parseCounted(text: String): Double? {
        val v = text.replace(',', '.').trim().toDoubleOrNull() ?: return null
        return if (v < 0.0 || v.isNaN() || v.isInfinite()) null else v
    }

    /** Líneas a enviar: solo los productos con cantidad contada válida. */
    fun buildLines(counted: Map<Pair<Int, Int>, String>): List<CountLineInput> =
        counted.mapNotNull { (key, text) ->
            parseCounted(text)?.let { CountLineInput(key.first, key.second, it) }
        }

    /** Productos con texto que no es un número válido (para avisar antes de enviar). */
    fun invalidKeys(counted: Map<Pair<Int, Int>, String>): List<Pair<Int, Int>> =
        counted.filter { (_, t) -> t.isNotBlank() && parseCounted(t) == null }.keys.toList()

    /** Mensaje si no se puede calcular; null si está bien. */
    fun validate(counted: Map<Pair<Int, Int>, String>): String? {
        if (invalidKeys(counted).isNotEmpty()) return "Revisa las cantidades contadas: no pueden ser negativas ni inválidas."
        val n = buildLines(counted).size
        if (n == 0) return "Cuenta al menos un producto."
        if (n > MAX_LINES) return "Un conteo admite hasta $MAX_LINES productos a la vez."
        return null
    }

    fun withDifference(r: PhysicalCountResult): List<CountResultLine> = r.lines.filter { Math.abs(it.difference) > 0.0005 || it.error != null }

    /** «+3», «−2.5», «Sin diferencia». */
    fun differenceLabel(d: Double): String = when {
        Math.abs(d) <= 0.0005 -> "Sin diferencia"
        d > 0 -> "+" + qty(d) + " (sobra)"
        else -> "−" + qty(-d) + " (falta)"
    }

    fun qty(v: Double): String = String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.').ifEmpty { "0" }

    fun money(v: Double): String = String.format(java.util.Locale.US, "S/ %.2f", v)

    fun signedMoney(v: Double): String = (if (v < 0) "−" else if (v > 0) "+" else "") + money(Math.abs(v))

    fun valuationRowKey(r: ValuationRow): String = "${r.productId}-${r.presentationId}"

    /** Filtro de texto local sobre código y nombre. */
    fun filter(rows: List<ValuationRow>, q: String): List<ValuationRow> {
        val t = q.trim().lowercase()
        if (t.isEmpty()) return rows
        return rows.filter { it.name.lowercase().contains(t) || it.code.lowercase().contains(t) }
    }
}

interface InventoryCountRepository {
    suspend fun valuation(branchId: Int?, q: String): AppResult<InventoryValuation>
    suspend fun physicalCount(branchId: Int, lines: List<CountLineInput>, note: String, apply: Boolean): AppResult<PhysicalCountResult>
}
