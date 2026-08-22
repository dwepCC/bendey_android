package com.bendey.restaurant.core.domain.cash

enum class CashMovementType(val apiValue: String, val label: String) {
    INCOME("income", "Ingreso"),
    EXPENSE("expense", "Egreso"),
    ;

    companion object {
        fun fromApi(value: String): CashMovementType =
            entries.firstOrNull { it.apiValue == value } ?: INCOME
    }
}

enum class CashSessionStatus(val apiValue: String) {
    OPEN("open"),
    CLOSED("closed"),
    ;

    companion object {
        fun fromApi(value: String): CashSessionStatus =
            entries.firstOrNull { it.apiValue == value } ?: OPEN
    }
}

data class CashSession(
    val id: Int,
    val branchId: Int,
    val branchName: String?,
    val openedByName: String?,
    val openingBalance: Double,
    val expectedBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val status: CashSessionStatus,
    val openedAt: String?,
    val closedAt: String? = null,
    val closingBalance: Double? = null,
    val difference: Double? = null,
    val notes: String?,
    val arqueoJson: String? = null,
)

data class CashSessionBrief(
    val id: Int,
    val branchName: String?,
    val openedByName: String?,
    val openingBalance: Double,
    val closingBalance: Double?,
    val expectedBalance: Double,
    val status: CashSessionStatus,
    val openedAt: String?,
    val closedAt: String?,
)

data class CashReportRow(
    val date: String,
    val type: String,
    val docNumber: String,
    val reference: String,
    val amount: Double,
    val paymentMethod: String,
)

data class CashSessionReport(
    val session: CashSessionBrief,
    val incomeDetail: List<CashReportRow>,
    val expenseDetail: List<CashReportRow>,
    val cancelledSalesDetail: List<CashCancelledSaleRow> = emptyList(),
    /** Ventas anuladas de esta caja a las que todavia nadie les registro la devolucion. */
    val pendingRefunds: List<CashPendingRefundRow> = emptyList(),
    val salesByMethod: List<CashMethodTotal> = emptyList(),
    val nonCashSalesByMethod: List<CashMethodTotal> = emptyList(),
    /** Neto real por método no efectivo: ventas − compras − egresos. No solo cobros de venta. */
    val nonCashByMethod: List<CashMethodTotal> = emptyList(),
    val totalIncome: Double,
    val totalExpense: Double,
    val totalSales: Double,
    val finalBalance: Double,
    val totalNetSales: Double,
    val totalVoidedSales: Double,
    /** Efectivo anulado sin devolucion registrada: el faltante que aparecera al contar. */
    val totalPendingRefunds: Double = 0.0,
)

data class CashMovement(
    val id: Int,
    val type: CashMovementType,
    val category: String,
    val reference: String,
    val amount: Double,
    val notes: String?,
    val titular: String?,
    val createdAt: String?,
)

data class AddCashMovementInput(
    val type: CashMovementType,
    val category: String,
    val amount: Double,
    val reference: String = "",
    val notes: String = "",
    val paymentMethod: String = "cash",
    /** A nombre de quién es el ingreso/egreso manual. */
    val titular: String = "",
)

data class CashPaymentMethod(
    val id: Int,
    val name: String,
    val code: String,
    val destinationType: String,
    val bankAccountId: Int?,
    val active: Boolean,
)

data class CashBankAccount(
    val id: Int,
    val name: String,
    val bankName: String,
    val accountNumber: String,
    val currency: String,
    val balance: Double,
    val type: String,
    val paymentMethod: String,
    val active: Boolean,
)

data class CashBankMovement(
    val id: Int,
    val type: String,
    val amount: Double,
    val description: String,
    val reference: String,
    val date: String,
)

data class CashMethodTotal(val method: String, val total: Double)

data class CashPendingRefundRow(
    val saleId: Int,
    val date: String,
    val docNumber: String,
    val amount: Double,
)

data class CashCancelledSaleRow(
    val date: String,
    val docNumber: String,
    val amount: Double,
    val paymentMethod: String,
    val reason: String,
)

data class CashMovementReportRow(
    val date: String,
    val type: String,
    val docNumber: String,
    val contactName: String,
    val userName: String,
    val branchName: String,
    val paymentMethod: String,
    val amount: Double,
    val movementId: Int,
    val cashSessionId: Int,
    val category: String?,
    val cashReference: String?,
    val notesDetail: String?,
    val cancelledAt: String? = null,
    val cancelReason: String? = null,
) {
    /** Un movimiento anulado se sigue viendo, pero su importe ya no cuenta para ningún saldo. */
    val estaAnulado: Boolean get() = !cancelledAt.isNullOrBlank()
}

data class CashMovementsReportSummary(
    val totalRows: Int = 0,
    val sumIncome: Double = 0.0,
    val sumExpense: Double = 0.0,
    val netMovement: Double = 0.0,
)

data class CashMovementsReportPage(
    val rows: List<CashMovementReportRow>,
    val total: Int,
    val summary: CashMovementsReportSummary,
)

data class CashMovementsReportQuery(
    val branchId: Int?,
    val userId: Int? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val sessionId: Int? = null,
    val type: String? = null,
    val paymentMethod: String? = null,
    val page: Int = 1,
    val perPage: Int = 25,
)

/** Totales de un metodo electronico en el periodo: las dos patas y el neto que se muestra. */
data class BankMethodTotals(
    val method: String,
    val income: Double,
    val expense: Double,
    val net: Double,
    val incomeCount: Int,
    val expenseCount: Int,
)

/**
 * Resumen no efectivo del periodo, con sus EGRESOS.
 *
 * El panel electronico de la pantalla de movimientos solo listaba cobros y no mostraba ningun total,
 * mientras el de efectivo, justo arriba, muestra ingresos, egresos y neto. Un egreso pagado con Yape no
 * aparecia en ninguna parte de esa mitad.
 */
/** Un movimiento de una cuenta no efectiva, ya resuelto a metodo. */
data class BankMovementRow(
    val id: Int,
    val date: String,
    /** true = entro a la cuenta. Se guarda como booleano y no como el "credit"/"debit" del backend
     *  para que la pantalla no tenga que conocer ese vocabulario en cada lugar donde lo pinta. */
    val isIncome: Boolean,
    val amount: Double,
    val method: String,
    val accountName: String,
    val description: String,
    val reference: String,
    val userName: String,
)

data class BankMovementsSummary(
    val totalRows: Int = 0,
    val sumIncome: Double = 0.0,
    val sumExpense: Double = 0.0,
    val netMovement: Double = 0.0,
    val byMethod: List<BankMethodTotals> = emptyList(),
    val rows: List<BankMovementRow> = emptyList(),
)

data class CashPaymentDetailRow(
    val date: String,
    val saleNumber: String,
    val orderCode: String,
    val orderType: String,
    val userName: String?,
    val method: String,
    val amount: Double,
    val reference: String?,
)

data class CashPaymentsReport(
    val byMethod: List<CashMethodTotalWithCount>,
    val totalIncome: Double,
    val totalCount: Int,
    val detail: List<CashPaymentDetailRow>,
)

data class CashMethodTotalWithCount(
    val method: String,
    val total: Double,
    val count: Int,
)

data class CashSessionProductSold(
    val productId: Int?,
    val code: String,
    val description: String,
    val quantity: Double,
    val total: Double,
)

/**
 * Plato que salió de cocina dentro de un combo. SIN importe: ya está contado en la línea del combo.
 * Repartirlo entre los platos obligaría a inventar un criterio, y sumar las dos listas daría el doble
 * de lo vendido.
 */
data class CashSessionComboComponent(
    val productId: Int,
    val code: String,
    val description: String,
    val quantity: Double,
)

/**
 * El reporte de productos de una sesión: lo vendido y, aparte, los platos que salieron dentro de
 * combos. Van juntos para que nadie pida uno sin el otro — un combo listado sin sus platos es
 * exactamente el reporte incompleto que esto vino a arreglar.
 */
data class CashSessionProductsReport(
    val products: List<CashSessionProductSold> = emptyList(),
    val comboComponents: List<CashSessionComboComponent> = emptyList(),
)

data class CashFilterUser(
    val userId: Int,
    val name: String,
)
