package com.bendey.restaurant.core.domain.dashboard

import com.bendey.restaurant.core.domain.model.AppResult

data class DashboardPeriod(
    val from: String = "",
    val to: String = "",
    val durationDays: Int = 1,
)

data class DashboardSummaryBlock(
    val totalRevenue: Double = 0.0,
    val totalSessions: Int = 0,
    val paidSessions: Int = 0,
    val openSessions: Int = 0,
    val cancelledSessions: Int = 0,
    val avgTicket: Double = 0.0,
    val totalGuests: Int = 0,
    val revenueChangePct: Double = 0.0,
    val sessionsChangePct: Double = 0.0,
)

data class DashboardTopProduct(
    val name: String,
    val quantity: Double,
    val revenue: Double,
    val category: String? = null,
)

data class DashboardDailyPoint(
    val date: String,
    val sessions: Int,
    val revenue: Double,
)

data class DashboardPaymentSlice(
    val method: String,
    val amount: Double,
    val count: Int = 0,
)

data class DashboardOrderTypeSlice(
    val type: String,
    val count: Int,
    val revenue: Double,
)

data class DashboardTableSummary(
    val total: Int = 0,
    val libre: Int = 0,
    val ocupada: Int = 0,
    val enConsumo: Int = 0,
    val reservada: Int = 0,
)

data class DashboardRecentSession(
    val id: Int,
    val orderCode: String,
    val orderType: String,
    val orderStatus: String,
    val tableName: String,
    val customerName: String,
    val totalAmount: Double,
    val guests: Int,
    val openedAt: String,
)

data class RestaurantDashboard(
    val period: DashboardPeriod = DashboardPeriod(),
    val summary: DashboardSummaryBlock = DashboardSummaryBlock(),
    val topProducts: List<DashboardTopProduct> = emptyList(),
    val daily30: List<DashboardDailyPoint> = emptyList(),
    val paymentMethods: List<DashboardPaymentSlice> = emptyList(),
    val orderTypes: List<DashboardOrderTypeSlice> = emptyList(),
    val tableSummary: DashboardTableSummary = DashboardTableSummary(),
    val recentSessions: List<DashboardRecentSession> = emptyList(),
    // Legacy compat
    val salesToday: Double = 0.0,
    val ticketAverage: Double = 0.0,
    val activeTables: Int = 0,
    val totalTables: Int = 0,
    val pendingComandas: Int = 0,
)

data class CatalogAnalyticsRow(
    val key: String,
    val label: String,
    val kind: String,
    val quantity: Double,
    val revenue: Double,
)

data class CatalogAnalyticsKpi(
    val totalRevenue: Double = 0.0,
    val productsSold: Double = 0.0,
    val combosSold: Double = 0.0,
    val extrasRevenue: Double = 0.0,
    val avgTicket: Double = 0.0,
    val salesCount: Int = 0,
)

data class CatalogAnalyticsDaily(
    val date: String,
    val revenue: Double,
    val productsSold: Double,
    val combosSold: Double,
    val extrasRevenue: Double,
)

data class CatalogAnalytics(
    val from: String = "",
    val to: String = "",
    val branchId: Int = 0,
    val kpi: CatalogAnalyticsKpi = CatalogAnalyticsKpi(),
    val topProducts: List<CatalogAnalyticsRow> = emptyList(),
    val topPresentations: List<CatalogAnalyticsRow> = emptyList(),
    val topExtras: List<CatalogAnalyticsRow> = emptyList(),
    val topCombos: List<CatalogAnalyticsRow> = emptyList(),
    val comboRevenue: Double = 0.0,
    val comboParticipationPct: Double = 0.0,
    val avgTicketWithCombo: Double = 0.0,
    val avgTicketWithoutCombo: Double = 0.0,
    val daily30: List<CatalogAnalyticsDaily> = emptyList(),
)

interface DashboardRepository {
    suspend fun loadDashboard(from: String? = null, to: String? = null): AppResult<RestaurantDashboard>
    suspend fun loadCatalogAnalytics(from: String? = null, to: String? = null): AppResult<CatalogAnalytics>

    suspend fun loadSummary(from: String? = null, to: String? = null): AppResult<DashboardSummary> =
        when (val result = loadDashboard(from, to)) {
            is AppResult.Success -> AppResult.Success(
                DashboardSummary(
                    salesToday = result.data.salesToday,
                    ticketAverage = result.data.ticketAverage,
                    activeTables = result.data.activeTables,
                    totalTables = result.data.totalTables,
                    pendingComandas = result.data.pendingComandas,
                ),
            )
            is AppResult.Error -> result
            AppResult.Loading -> AppResult.Loading
        }
}

/** @deprecated usar [RestaurantDashboard] */
data class DashboardSummary(
    val salesToday: Double = 0.0,
    val ticketAverage: Double = 0.0,
    val activeTables: Int = 0,
    val totalTables: Int = 0,
    val pendingComandas: Int = 0,
)
