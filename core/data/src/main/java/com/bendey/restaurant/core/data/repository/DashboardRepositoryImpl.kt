package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.dashboard.DashboardDailyPoint
import com.bendey.restaurant.core.domain.dashboard.DashboardPaymentSlice
import com.bendey.restaurant.core.domain.dashboard.DashboardPeriod
import com.bendey.restaurant.core.domain.dashboard.DashboardRepository
import com.bendey.restaurant.core.domain.dashboard.DashboardSummaryBlock
import com.bendey.restaurant.core.domain.dashboard.DashboardTableSummary
import com.bendey.restaurant.core.domain.dashboard.DashboardTopProduct
import com.bendey.restaurant.core.domain.dashboard.RestaurantDashboard
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.network.api.RestaurantApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.DashboardResponseDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : DashboardRepository {

    override suspend fun loadDashboard(from: String?, to: String?): AppResult<RestaurantDashboard> = try {
        val api = tenantRetrofitProvider.create<RestaurantApi>()
        val dto = api.getDashboard(from = from, to = to)
        AppResult.Success(dto.toDomain())
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "No se pudo cargar el dashboard", e)
    }

    private fun DashboardResponseDto.toDomain(): RestaurantDashboard {
        val summaryBlock = summary?.let {
            DashboardSummaryBlock(
                totalRevenue = it.totalRevenue,
                totalSessions = it.totalSessions,
                paidSessions = it.paidSessions,
                openSessions = it.openSessions,
                cancelledSessions = it.cancelledSessions,
                avgTicket = it.avgTicket,
                totalGuests = it.totalGuests,
                revenueChangePct = it.revenueChangePct,
                sessionsChangePct = it.sessionsChangePct,
            )
        } ?: DashboardSummaryBlock()
        val legacy = data
        val salesToday = summaryBlock.totalRevenue.takeIf { it > 0 }
            ?: sales_today ?: legacy?.sales_today ?: 0.0
        return RestaurantDashboard(
            period = period?.let {
                DashboardPeriod(from = it.from, to = it.to, durationDays = it.durationDays)
            } ?: DashboardPeriod(),
            summary = summaryBlock,
            topProducts = topProducts.map {
                DashboardTopProduct(
                    name = it.name,
                    quantity = it.quantity,
                    revenue = it.revenue,
                    category = it.category,
                )
            },
            daily30 = daily30.map { DashboardDailyPoint(it.date, it.sessions, it.revenue) },
            paymentMethods = byPaymentMethod.map { DashboardPaymentSlice(it.method, it.amount) },
            tableSummary = tableSummary?.let {
                DashboardTableSummary(
                    total = it.total,
                    libre = it.libre,
                    ocupada = it.ocupada,
                    enConsumo = it.enConsumo,
                    reservada = it.reservada,
                )
            } ?: DashboardTableSummary(),
            salesToday = salesToday,
            ticketAverage = summaryBlock.avgTicket.takeIf { it > 0 }
                ?: ticket_average ?: legacy?.ticket_average ?: 0.0,
            activeTables = tableSummary?.let { it.ocupada + it.enConsumo }
                ?: active_tables ?: legacy?.active_tables ?: 0,
            totalTables = tableSummary?.total ?: total_tables ?: legacy?.total_tables ?: 0,
            pendingComandas = pending_comandas ?: legacy?.pending_comandas ?: 0,
        )
    }
}
