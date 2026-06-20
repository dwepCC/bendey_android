package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatalogAnalyticsRowDto(
    val key: String = "",
    val label: String = "",
    val kind: String = "",
    val quantity: Double = 0.0,
    val revenue: Double = 0.0,
)

@Serializable
data class CatalogAnalyticsKpiDto(
    @SerialName("total_revenue") val totalRevenue: Double = 0.0,
    @SerialName("products_sold") val productsSold: Double = 0.0,
    @SerialName("combos_sold") val combosSold: Double = 0.0,
    @SerialName("extras_revenue") val extrasRevenue: Double = 0.0,
    @SerialName("avg_ticket") val avgTicket: Double = 0.0,
    @SerialName("sales_count") val salesCount: Int = 0,
)

@Serializable
data class CatalogAnalyticsDailyDto(
    val date: String = "",
    val revenue: Double = 0.0,
    @SerialName("products_sold") val productsSold: Double = 0.0,
    @SerialName("combos_sold") val combosSold: Double = 0.0,
    @SerialName("extras_revenue") val extrasRevenue: Double = 0.0,
)

@Serializable
data class CatalogAnalyticsDto(
    val from: String = "",
    val to: String = "",
    @SerialName("branch_id") val branchId: Int = 0,
    val kpi: CatalogAnalyticsKpiDto = CatalogAnalyticsKpiDto(),
    @SerialName("top_products") val topProducts: List<CatalogAnalyticsRowDto> = emptyList(),
    @SerialName("top_presentations") val topPresentations: List<CatalogAnalyticsRowDto> = emptyList(),
    @SerialName("bottom_presentations") val bottomPresentations: List<CatalogAnalyticsRowDto> = emptyList(),
    @SerialName("top_extras") val topExtras: List<CatalogAnalyticsRowDto> = emptyList(),
    @SerialName("bottom_extras") val bottomExtras: List<CatalogAnalyticsRowDto> = emptyList(),
    @SerialName("top_combos") val topCombos: List<CatalogAnalyticsRowDto> = emptyList(),
    @SerialName("combo_revenue") val comboRevenue: Double = 0.0,
    @SerialName("combo_participation_pct") val comboParticipationPct: Double = 0.0,
    @SerialName("avg_ticket_with_combo") val avgTicketWithCombo: Double = 0.0,
    @SerialName("avg_ticket_without_combo") val avgTicketWithoutCombo: Double = 0.0,
    @SerialName("daily_30") val daily30: List<CatalogAnalyticsDailyDto> = emptyList(),
)

@Serializable
data class CatalogAnalyticsResponseDto(
    val data: CatalogAnalyticsDto? = null,
)
