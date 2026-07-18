package com.bendey.restaurant.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Paleta Bendey Restaurant — tema claro exclusivo.
 * Primario tomate + acentos vivos (Toast/Square POS).
 */
@Immutable
object BendeyColors {
    // Primario — Tomate Bendey (alineado React rest-600)
    val Primary = Color(0xFFC9393B)
    val OnPrimary = Color.White
    val PrimaryContainer = Color(0xFFFFEBEE)
    val OnPrimaryContainer = Color(0xFF721F21)
    /** Marco exterior app (React `rest-900`) */
    val Rest900 = Color(0xFF721F21)
    val Rest800 = Color(0xFF8B2426)

    // Semánticos
    val Success = Color(0xFF2E7D32)
    val SuccessContainer = Color(0xFFE8F5E9)
    val OnSuccess = Color.White

    val Info = Color(0xFF0288D1)
    val InfoContainer = Color(0xFFE1F5FE)
    val OnInfo = Color.White

    val Warning = Color(0xFFF9A825)
    val WarningContainer = Color(0xFFFFF8E1)
    val OnWarning = Color(0xFF5D4037)

    val Error = Color(0xFFD32F2F)
    val ErrorContainer = Color(0xFFFFEBEE)
    val OnErrorContainer = Color(0xFF5F2120)

    // Acentos decorativos
    val AccentPurple = Color(0xFF7B1FA2)
    val AccentPurpleContainer = Color(0xFFF3E5F5)
    val AccentTeal = Color(0xFF00897B)
    val AccentTealContainer = Color(0xFFE0F2F1)

    // Neutros Material 3
    val Background = Color(0xFFF8F9FA)
    val Surface = Color.White
    val SurfaceVariant = Color(0xFFF1F3F4)
    val Outline = Color(0xFFE0E0E0)
    val OnSurface = Color(0xFF1C1B1F)
    val OnSurfaceVariant = Color(0xFF5F6368)
    val NavInactive = Color(0xFF9E9E9E)

    // Mesas (backend exacto — colores visuales aprobados)
    val TableLibre = Color(0xFF2E7D32)
    val TableOcupada = Color(0xFFF97316)
    val TableReservada = Color(0xFFF9A825)
    val TableEnConsumo = Color(0xFF0288D1)
    val TableBrowsing = Color(0xFF6366F1)

    // Cocina KDS
    val KitchenPendiente = Color(0xFFF9A825)
    val KitchenPreparando = Color(0xFF0288D1)
    val KitchenListo = Color(0xFF2E7D32)
    val KitchenEntregado = Color(0xFF9E9E9E)

    // KPI Dashboard accents
    val KpiSales = AccentTeal
    val KpiTicket = AccentPurple
    val KpiTables = Success
    val KpiComandas = Warning

    /** Chips resumen mesas en Dashboard (valores fijos aprobados — distintos del mapa operativo). */
    val DashboardTableLibre = Success
    val DashboardTableLibreContainer = SuccessContainer
    val DashboardTableOcupada = Color(0xFFDC2626)
    val DashboardTableOcupadaContainer = Color(0xFFFEE2E2)
    val DashboardTableReservada = Color(0xFFD97706)
    val DashboardTableReservadaContainer = Color(0xFFFEF3C7)
    val DashboardTableConsumo = Color(0xFF2563EB)
    val DashboardTableConsumoContainer = Color(0xFFDBEAFE)

    /** Paleta gráficos Dashboard (top productos / tipos). */
    val DashboardChartOrange = Color(0xFFF97316)
    val DashboardChartLime = Color(0xFF84CC16)
    val DashboardChartPink = Color(0xFFEC4899)
    val DashboardChartAmber = Color(0xFFD97706)
}
