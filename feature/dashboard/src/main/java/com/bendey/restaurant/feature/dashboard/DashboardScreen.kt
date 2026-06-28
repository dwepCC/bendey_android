package com.bendey.restaurant.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Cancel
import com.bendey.restaurant.core.designsystem.components.BendeyBadge
import com.bendey.restaurant.core.designsystem.components.BendeyCard
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.designsystem.motion.BendeyExpressiveCrossfadeValue
import com.bendey.restaurant.core.designsystem.motion.BendeyExpressiveReveal
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.layout.rememberBendeyLazyListContentPadding
import com.bendey.restaurant.core.ui.layout.rememberIsExpandedWidth
import com.bendey.restaurant.core.domain.dashboard.CatalogAnalytics
import com.bendey.restaurant.core.domain.dashboard.CatalogAnalyticsRow
import com.bendey.restaurant.core.domain.dashboard.DashboardDailyPoint
import com.bendey.restaurant.core.domain.dashboard.DashboardRecentSession
import com.bendey.restaurant.core.domain.dashboard.DashboardTableSummary
import com.bendey.restaurant.core.domain.dashboard.DashboardTopProduct
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenMesas: () -> Unit = {},
    onOpenVentas: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isExpanded = rememberIsExpandedWidth()
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    val dash = state.dashboard
    val listState = rememberLazyListState()
    val contentPadding = rememberBendeyLazyListContentPadding(
        includeBottomBar = !isExpanded,
        extraBottom = -BendeySpacing.sm,
    )

    PullToRefreshBox(
        isRefreshing = state.loading || state.catalogLoading,
        onRefresh = viewModel::refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        BendeyLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BendeyColors.Background),
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.sectionGap),
        ) {
            if (state.canChangeDateRange) {
                item {
                    var showCustomRange by remember { mutableStateOf(false) }
                    DateRangeFilterRow(
                        selected = state.range,
                        onSelect = viewModel::selectRange,
                        onCustomClick = { showCustomRange = true },
                    )
                    if (showCustomRange) {
                        CustomDateRangeDialog(
                            from = state.fromApi,
                            to = state.toApi,
                            onDismiss = { showCustomRange = false },
                            onApply = { from, to ->
                                viewModel.applyCustomRange(from, to)
                                showCustomRange = false
                            },
                        )
                    }
                }
            }
            item {
                BendeyHorizontalScrollRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DashboardTab.entries.forEach { tab ->
                        BendeyFilterChip(
                            selected = state.tab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = tab.label,
                        )
                    }
                }
            }
            if (state.tab == DashboardTab.OPERACION) {
            item {
                if (isExpanded) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        BendeyExpressiveReveal(index = 0, resetKey = state.range, modifier = Modifier.weight(1f)) {
                            DashboardMetricCard(
                                label = revenueLabel(state.range),
                                value = currency.format(dash.summary.totalRevenue),
                                changePct = dash.summary.revenueChangePct,
                                range = state.range,
                                icon = Icons.AutoMirrored.Filled.ShowChart,
                                iconBg = BendeyColors.PrimaryContainer,
                                iconTint = BendeyColors.Primary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        BendeyExpressiveReveal(index = 1, resetKey = state.range, modifier = Modifier.weight(1f)) {
                            DashboardMetricCard(
                                label = ordersLabel(state.range),
                                value = dash.summary.totalSessions.toString(),
                                changePct = dash.summary.sessionsChangePct,
                                range = state.range,
                                icon = Icons.Default.ShoppingBag,
                                iconBg = BendeyColors.SuccessContainer,
                                iconTint = BendeyColors.Success,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        BendeyExpressiveReveal(index = 2, resetKey = state.range, modifier = Modifier.weight(1f)) {
                            DashboardMetricCard(
                                label = "Ticket promedio",
                                value = currency.format(dash.summary.avgTicket),
                                changePct = null,
                                range = state.range,
                                icon = Icons.Default.Wallet,
                                iconBg = BendeyColors.AccentPurpleContainer,
                                iconTint = BendeyColors.AccentPurple,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        BendeyExpressiveReveal(index = 3, resetKey = state.range, modifier = Modifier.weight(1f)) {
                            DashboardMetricCard(
                                label = "Comensales",
                                value = dash.summary.totalGuests.toString(),
                                changePct = null,
                                range = state.range,
                                icon = Icons.Default.Group,
                                iconBg = BendeyColors.InfoContainer,
                                iconTint = BendeyColors.Info,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            BendeyExpressiveReveal(index = 0, resetKey = state.range, modifier = Modifier.weight(1f)) {
                                DashboardMetricCard(
                                    label = revenueLabel(state.range),
                                    value = currency.format(dash.summary.totalRevenue),
                                    changePct = dash.summary.revenueChangePct,
                                    range = state.range,
                                    icon = Icons.AutoMirrored.Filled.ShowChart,
                                    iconBg = BendeyColors.PrimaryContainer,
                                    iconTint = BendeyColors.Primary,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            BendeyExpressiveReveal(index = 1, resetKey = state.range, modifier = Modifier.weight(1f)) {
                                DashboardMetricCard(
                                    label = ordersLabel(state.range),
                                    value = dash.summary.totalSessions.toString(),
                                    changePct = dash.summary.sessionsChangePct,
                                    range = state.range,
                                    icon = Icons.Default.ShoppingBag,
                                    iconBg = BendeyColors.SuccessContainer,
                                    iconTint = BendeyColors.Success,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            BendeyExpressiveReveal(index = 2, resetKey = state.range, modifier = Modifier.weight(1f)) {
                                DashboardMetricCard(
                                    label = "Ticket promedio",
                                    value = currency.format(dash.summary.avgTicket),
                                    changePct = null,
                                    range = state.range,
                                    icon = Icons.Default.Wallet,
                                    iconBg = BendeyColors.AccentPurpleContainer,
                                    iconTint = BendeyColors.AccentPurple,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            BendeyExpressiveReveal(index = 3, resetKey = state.range, modifier = Modifier.weight(1f)) {
                                DashboardMetricCard(
                                    label = "Comensales",
                                    value = dash.summary.totalGuests.toString(),
                                    changePct = null,
                                    range = state.range,
                                    icon = Icons.Default.Group,
                                    iconBg = BendeyColors.InfoContainer,
                                    iconTint = BendeyColors.Info,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
            item {
                if (isExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sectionGap),
                    ) {
                        SessionStatsRow(summary = dash.summary, modifier = Modifier.weight(1f))
                        TableStatusSection(
                            summary = dash.tableSummary,
                            onVerMapa = onOpenMesas,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    SessionStatsRow(summary = dash.summary)
                }
            }
            if (dash.recentSessions.isNotEmpty()) {
                item {
                    RecentSessionsSection(sessions = dash.recentSessions, currency = currency)
                }
            }
            if (!isExpanded) {
                item {
                    TableStatusSection(
                        summary = dash.tableSummary,
                        onVerMapa = onOpenMesas,
                    )
                }
            }
            item {
                if (isExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sectionGap),
                    ) {
                        RevenueTrendCard(
                            points = dash.daily30,
                            currency = currency,
                            onVerReporte = onOpenVentas,
                            modifier = Modifier.weight(1f),
                        )
                        TopProductsSection(
                            products = dash.topProducts,
                            currency = currency,
                            onVerTodos = onOpenVentas,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    RevenueTrendCard(
                        points = dash.daily30,
                        currency = currency,
                        onVerReporte = onOpenVentas,
                    )
                }
            }
            if (!isExpanded) {
                item {
                    TopProductsSection(
                        products = dash.topProducts,
                        currency = currency,
                        onVerTodos = onOpenVentas,
                    )
                }
            }
            item {
                if (isExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sectionGap),
                    ) {
                        OrderTypesSection(
                            slices = dash.orderTypes,
                            currency = currency,
                            modifier = Modifier.weight(1f),
                        )
                        PaymentMethodsSection(
                            methods = dash.paymentMethods,
                            currency = currency,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    OrderTypesSection(
                        slices = dash.orderTypes,
                        currency = currency,
                    )
                }
            }
            if (!isExpanded) {
                item {
                    PaymentMethodsSection(
                        methods = dash.paymentMethods,
                        currency = currency,
                    )
                }
            }
            }
            if (state.tab == DashboardTab.CATALOGO) {
                if (state.catalogLoading && state.catalog == null) {
                    item { CircularProgressIndicator(modifier = Modifier.padding(24.dp)) }
                } else {
                    state.catalog?.let { catalog ->
                        item {
                            BendeyExpressiveReveal(index = 0, resetKey = state.tab) {
                                CatalogKpiSection(catalog, currency, isExpanded)
                            }
                        }
                        if (isExpanded) {
                            if (catalog.topProducts.isNotEmpty() && catalog.topCombos.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sectionGap),
                                    ) {
                                        CatalogRankSection(
                                            "Top productos",
                                            catalog.topProducts,
                                            currency,
                                            modifier = Modifier.weight(1f),
                                        )
                                        CatalogRankSection(
                                            "Top combos",
                                            catalog.topCombos,
                                            currency,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            } else {
                                if (catalog.topProducts.isNotEmpty()) {
                                    item { CatalogRankSection("Top productos", catalog.topProducts, currency) }
                                }
                                if (catalog.topCombos.isNotEmpty()) {
                                    item { CatalogRankSection("Top combos", catalog.topCombos, currency) }
                                }
                            }
                            if (catalog.topPresentations.isNotEmpty() && catalog.topExtras.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sectionGap),
                                    ) {
                                        CatalogRankSection(
                                            "Top presentaciones",
                                            catalog.topPresentations,
                                            currency,
                                            modifier = Modifier.weight(1f),
                                        )
                                        CatalogRankSection(
                                            "Top extras",
                                            catalog.topExtras,
                                            currency,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            } else {
                                if (catalog.topPresentations.isNotEmpty()) {
                                    item { CatalogRankSection("Top presentaciones", catalog.topPresentations, currency) }
                                }
                                if (catalog.topExtras.isNotEmpty()) {
                                    item { CatalogRankSection("Top extras", catalog.topExtras, currency) }
                                }
                            }
                        } else {
                            if (catalog.topProducts.isNotEmpty()) {
                                item { CatalogRankSection("Top productos", catalog.topProducts, currency) }
                            }
                            if (catalog.topCombos.isNotEmpty()) {
                                item { CatalogRankSection("Top combos", catalog.topCombos, currency) }
                            }
                            if (catalog.topPresentations.isNotEmpty()) {
                                item { CatalogRankSection("Top presentaciones", catalog.topPresentations, currency) }
                            }
                            if (catalog.topExtras.isNotEmpty()) {
                                item { CatalogRankSection("Top extras", catalog.topExtras, currency) }
                            }
                        }
                        item { CatalogComboStats(catalog, currency) }
                    } ?: item {
                        Text("Sin datos de catálogo", color = BendeyColors.OnSurfaceVariant, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            state.error?.let { error ->
                item {
                    Text(error, color = BendeyColors.Error, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(
    label: String,
    value: String,
    changePct: Double?,
    range: DashboardRange,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    BendeyCard(
        modifier = modifier,
        contentPadding = PaddingValues(BendeySpacing.sm),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = BendeyColors.OnSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
            }
            BendeyExpressiveCrossfadeValue(targetState = value) { animatedValue ->
                Text(
                    text = animatedValue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            changePct?.let { pct ->
                ChangeBadge(pct = pct, range = range)
            }
        }
    }
}

@Composable
private fun ChangeBadge(pct: Double, range: DashboardRange) {
    val suffix = when (range) {
        DashboardRange.TODAY -> " vs ayer"
        DashboardRange.YESTERDAY -> " vs ant. ayer"
        DashboardRange.WEEK -> " vs sem. ant."
        DashboardRange.MONTH -> " vs mes ant."
        DashboardRange.CUSTOM -> ""
    }
    val color = when {
        pct > 0 -> BendeyColors.Success
        pct < 0 -> BendeyColors.Error
        else -> BendeyColors.OnSurfaceVariant
    }
    val icon = when {
        pct > 0 -> Icons.AutoMirrored.Filled.TrendingUp
        pct < 0 -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.AutoMirrored.Filled.TrendingFlat
    }
    BendeyBadge(
        text = when {
            pct > 0 -> "+${pct.roundToInt()}%$suffix"
            pct < 0 -> "${pct.roundToInt()}%$suffix"
            else -> "Sin cambio$suffix"
        },
        color = color,
        icon = icon,
    )
}

@Composable
private fun TableStatusSection(
    summary: DashboardTableSummary,
    onVerMapa: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BendeyCard(modifier = modifier, contentPadding = PaddingValues(BendeySpacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BendeySectionTitle(
                    text = "Estado de mesas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                BendeyTextButton(
                    text = "Ver mapa",
                    onClick = onVerMapa,
                    textColor = BendeyColors.Info,
                    contentPadding = PaddingValues(),
                )
            }
            if (summary.total == 0) {
                Text("No hay mesas configuradas", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TableStatusChip("Libres", summary.libre, BendeyColors.DashboardTableLibre, BendeyColors.DashboardTableLibreContainer, Modifier.weight(1f))
                    TableStatusChip("Ocupadas", summary.ocupada, BendeyColors.DashboardTableOcupada, BendeyColors.DashboardTableOcupadaContainer, Modifier.weight(1f))
                    TableStatusChip("Reservadas", summary.reservada, BendeyColors.DashboardTableReservada, BendeyColors.DashboardTableReservadaContainer, Modifier.weight(1f))
                    TableStatusChip("Consum.", summary.enConsumo, BendeyColors.DashboardTableConsumo, BendeyColors.DashboardTableConsumoContainer, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TableStatusChip(
    label: String,
    count: Int,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = BendeyShapeTokens.md,
        color = bgColor,
    ) {
        Column(
            modifier = Modifier.padding(vertical = BendeySpacing.sm, horizontal = BendeySpacing.xxs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
        ) {
            Icon(Icons.Default.EventSeat, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.85f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RevenueTrendCard(
    points: List<DashboardDailyPoint>,
    currency: NumberFormat,
    onVerReporte: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BendeyCard(modifier = modifier, contentPadding = PaddingValues(BendeySpacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BendeySectionTitle(
                    text = "Tendencia de ingresos (30 días)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                BendeyTextButton(
                    text = "Ver reporte",
                    onClick = onVerReporte,
                    textColor = BendeyColors.Info,
                    contentPadding = PaddingValues(),
                )
            }
            if (points.isEmpty()) {
                Text("Sin datos de tendencia", color = BendeyColors.OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            } else {
                RevenueTrendChart(points = points, currency = currency)
            }
        }
    }
}

@Composable
private fun RevenueTrendChart(
    points: List<DashboardDailyPoint>,
    currency: NumberFormat,
) {
    val maxRevenue = points.maxOf { it.revenue }.coerceAtLeast(1.0)
    val ySteps = 4
    val yMaxLabel = ((maxRevenue / 200.0).toInt().coerceAtLeast(1) * 200.0).coerceAtLeast(200.0)
    val dateFmt = DateTimeFormatter.ofPattern("d MMM", Locale("es", "PE"))

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            val chartLeft = 44.dp
            val chartBottom = 20.dp
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = chartLeft, bottom = chartBottom),
            ) {
                val w = size.width
                val h = size.height
                val stepX = w / (points.size - 1).coerceAtLeast(1)
                val linePath = Path()
                val fillPath = Path()
                points.forEachIndexed { i, p ->
                    val x = i * stepX
                    val y = h - (p.revenue / yMaxLabel * h * 0.92f).toFloat()
                    if (i == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(w, h)
                fillPath.lineTo(0f, h)
                fillPath.close()
                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BendeyColors.Primary.copy(alpha = 0.28f),
                            BendeyColors.Primary.copy(alpha = 0.02f),
                        ),
                        startY = 0f,
                        endY = h,
                    ),
                )
                drawPath(linePath, BendeyColors.Primary, style = Stroke(width = 2.5f))
                for (i in 0..ySteps) {
                    val y = h * i / ySteps
                    drawLine(
                        color = BendeyColors.Outline.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .height(140.dp)
                    .width(chartLeft - 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                for (i in ySteps downTo 0) {
                    val value = yMaxLabel * i / ySteps
                    Text(
                        text = currency.format(value).replace(",00", ""),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val indices = listOf(0, points.size / 4, points.size / 2, points.size * 3 / 4, points.lastIndex)
                .distinct()
                .filter { it in points.indices }
            indices.forEach { i ->
                val label = runCatching {
                    LocalDate.parse(points[i].date).format(dateFmt)
                }.getOrDefault(points[i].date.take(5))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TopProductsSection(
    products: List<DashboardTopProduct>,
    currency: NumberFormat,
    onVerTodos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BendeyCard(modifier = modifier, contentPadding = PaddingValues(BendeySpacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BendeySectionTitle(
                    text = "Platos más vendidos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                BendeyTextButton(
                    text = "Ver todos",
                    onClick = onVerTodos,
                    textColor = BendeyColors.Info,
                    contentPadding = PaddingValues(),
                )
            }
            if (products.isEmpty()) {
                Text(
                    "Sin ventas en el período",
                    color = BendeyColors.OnSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                products.take(8).forEachIndexed { index, product ->
                    TopProductRow(
                        rank = index + 1,
                        product = product,
                        currency = currency,
                    )
                    if (index < products.take(8).lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

private val topProductPalette = listOf(
    BendeyColors.Primary,
    BendeyColors.Success,
    BendeyColors.Warning,
    BendeyColors.AccentPurple,
    BendeyColors.Info,
    BendeyColors.DashboardChartOrange,
    BendeyColors.DashboardChartLime,
    BendeyColors.DashboardChartPink,
)

@Composable
private fun TopProductRow(
    rank: Int,
    product: DashboardTopProduct,
    currency: NumberFormat,
) {
    val accent = topProductPalette[(rank - 1) % topProductPalette.size]
    val qtyLabel = if (product.quantity % 1.0 == 0.0) {
        "${product.quantity.toInt()} vendidos"
    } else {
        "${"%.1f".format(product.quantity)} vendidos"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(accent.copy(alpha = 0.15f), BendeyShapeTokens.xs),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = qtyLabel,
                style = MaterialTheme.typography.labelSmall,
                color = BendeyColors.OnSurfaceVariant,
            )
        }
        Text(
            text = currency.format(product.revenue),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = BendeyColors.Primary,
        )
    }
}

private fun revenueLabel(range: DashboardRange): String = when (range) {
    DashboardRange.TODAY -> "Ingresos hoy"
    DashboardRange.YESTERDAY -> "Ingresos ayer"
    DashboardRange.WEEK -> "Ingresos (7 días)"
    DashboardRange.MONTH -> "Ingresos (30 días)"
    DashboardRange.CUSTOM -> "Ingresos del período"
}

private fun ordersLabel(range: DashboardRange): String = when (range) {
    DashboardRange.TODAY -> "Pedidos hoy"
    DashboardRange.YESTERDAY -> "Pedidos ayer"
    DashboardRange.WEEK -> "Pedidos (7 días)"
    DashboardRange.MONTH -> "Pedidos (30 días)"
    DashboardRange.CUSTOM -> "Pedidos del período"
}

@Composable
private fun DateRangeFilterRow(
    selected: DashboardRange,
    onSelect: (DashboardRange) -> Unit,
    onCustomClick: () -> Unit,
) {
    BendeyHorizontalScrollRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        DashboardRange.entries.filter { it != DashboardRange.CUSTOM }.forEach { range ->
            BendeyFilterChip(
                selected = selected == range,
                onClick = { onSelect(range) },
                text = range.label,
            )
        }
        BendeyFilterChip(
            selected = selected == DashboardRange.CUSTOM,
            onClick = onCustomClick,
            text = "Rango",
        )
    }
}

@Composable
private fun CustomDateRangeDialog(
    from: String,
    to: String,
    onDismiss: () -> Unit,
    onApply: (String, String) -> Unit,
) {
    var fromValue by remember(from) { mutableStateOf(from) }
    var toValue by remember(to) { mutableStateOf(to) }
    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = "Rango de fechas",
        confirmText = "Aplicar",
        onConfirm = { onApply(fromValue.trim(), toValue.trim()) },
        onDismiss = onDismiss,
    ) {
        BendeyTextField(
            value = fromValue,
            onValueChange = { fromValue = it },
            label = "Desde (AAAA-MM-DD)",
        )
        BendeyTextField(
            value = toValue,
            onValueChange = { toValue = it },
            label = "Hasta (AAAA-MM-DD)",
        )
    }
}

@Composable
private fun SessionStatsRow(
    summary: com.bendey.restaurant.core.domain.dashboard.DashboardSummaryBlock,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MiniStatChip(
            label = "Cobrados",
            value = summary.paidSessions.toString(),
            icon = Icons.Default.CheckCircle,
            tint = BendeyColors.Success,
            bg = BendeyColors.SuccessContainer,
            modifier = Modifier.weight(1f),
        )
        MiniStatChip(
            label = "Abiertos",
            value = summary.openSessions.toString(),
            icon = Icons.Default.Schedule,
            tint = BendeyColors.DashboardChartAmber,
            bg = BendeyColors.WarningContainer,
            modifier = Modifier.weight(1f),
        )
        MiniStatChip(
            label = "Cancelados",
            value = summary.cancelledSessions.toString(),
            icon = Icons.Default.Cancel,
            tint = BendeyColors.Error,
            bg = BendeyColors.ErrorContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MiniStatChip(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = BendeyShapeTokens.md,
        color = BendeyColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BendeyColors.Outline.copy(alpha = 0.5f)),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(BendeySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(bg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(label, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PaymentMethodsSection(
    methods: List<com.bendey.restaurant.core.domain.dashboard.DashboardPaymentSlice>,
    currency: NumberFormat,
    modifier: Modifier = Modifier,
) {
    DashboardChartCard(title = "Métodos de pago", modifier = modifier) {
        if (methods.isEmpty()) {
            Text("Sin pagos en el período", color = BendeyColors.OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        } else {
            val max = methods.maxOf { it.amount }.coerceAtLeast(1.0)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                methods.forEach { slice ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(slice.method, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                            Text(
                                currency.format(slice.amount),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(BendeyColors.SurfaceVariant, BendeyShapeTokens.bar),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((slice.amount / max).toFloat())
                                    .height(8.dp)
                                    .background(BendeyColors.AccentTeal, BendeyShapeTokens.bar),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun orderTypeLabel(type: String): String = when (type.lowercase()) {
    "mesa", "dine_in" -> "Mesa"
    "takeaway", "llevar", "para_llevar" -> "Para llevar"
    "delivery" -> "Delivery"
    "pos", "quick_sale", "mostrador" -> "Mostrador"
    else -> type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "PE")) else it.toString() }
}

@Composable
private fun OrderTypesSection(
    slices: List<com.bendey.restaurant.core.domain.dashboard.DashboardOrderTypeSlice>,
    currency: NumberFormat,
    modifier: Modifier = Modifier,
) {
    DashboardChartCard(title = "Pedidos por tipo", modifier = modifier) {
        if (slices.isEmpty()) {
            Text("Sin pedidos en el período", color = BendeyColors.OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        } else {
            val max = slices.maxOf { it.count }.coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                slices.forEach { slice ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(modifier = Modifier.width(88.dp)) {
                            Text(
                                orderTypeLabel(slice.type),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                currency.format(slice.revenue),
                                style = MaterialTheme.typography.labelSmall,
                                color = BendeyColors.OnSurfaceVariant,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .background(BendeyColors.SurfaceVariant, BendeyShapeTokens.bar),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((slice.count.toDouble() / max).toFloat())
                                    .height(8.dp)
                                    .background(BendeyColors.Primary, BendeyShapeTokens.bar),
                            )
                        }
                        Text(
                            slice.count.toString(),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardChartCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BendeyCard(modifier = modifier, contentPadding = PaddingValues(BendeySpacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
            BendeySectionTitle(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun RecentSessionsSection(
    sessions: List<DashboardRecentSession>,
    currency: NumberFormat,
) {
    DashboardChartCard(title = "Pedidos recientes") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sessions.take(12).forEach { session ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            session.orderCode.ifBlank { "#${session.id}" },
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOfNotNull(
                                session.tableName.takeIf { it.isNotBlank() },
                                session.customerName.takeIf { it.isNotBlank() },
                                session.orderType,
                                recentSessionStatusLabel(session.orderStatus),
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        currency.format(session.totalAmount),
                        fontWeight = FontWeight.Bold,
                        color = BendeyColors.Primary,
                    )
                }
            }
            if (sessions.size > 12) {
                Text(
                    "${sessions.size - 12} pedidos más en el período",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CatalogKpiSection(catalog: CatalogAnalytics, currency: NumberFormat, isExpanded: Boolean) {
    val kpi = catalog.kpi
    val cards = listOf(
        "Ingresos" to currency.format(kpi.totalRevenue),
        "Ventas" to kpi.salesCount.toString(),
        "Productos" to String.format("%.0f", kpi.productsSold),
        "Combos" to String.format("%.0f", kpi.combosSold),
        "Ticket prom." to currency.format(kpi.avgTicket),
        "Extras" to currency.format(kpi.extrasRevenue),
    )
    if (isExpanded) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            cards.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { (label, value) ->
                        CatalogKpiCard(label, value, Modifier.weight(1f))
                    }
                }
            }
        }
    } else {
        CatalogKpiRow(catalog, currency)
    }
}

@Composable
private fun CatalogKpiRow(catalog: CatalogAnalytics, currency: NumberFormat) {
    val kpi = catalog.kpi
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CatalogKpiCard("Ingresos", currency.format(kpi.totalRevenue), Modifier.weight(1f))
            CatalogKpiCard("Ventas", kpi.salesCount.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CatalogKpiCard("Productos", String.format("%.0f", kpi.productsSold), Modifier.weight(1f))
            CatalogKpiCard("Combos", String.format("%.0f", kpi.combosSold), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CatalogKpiCard("Ticket prom.", currency.format(kpi.avgTicket), Modifier.weight(1f))
            CatalogKpiCard("Extras", currency.format(kpi.extrasRevenue), Modifier.weight(1f))
        }
    }
}

@Composable
private fun CatalogKpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    BendeyManagementCard(modifier = modifier, contentPadding = PaddingValues(BendeySpacing.sm)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = BendeyColors.OnSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, color = BendeyColors.OnPrimaryContainer)
    }
}

@Composable
private fun CatalogRankSection(
    title: String,
    rows: List<CatalogAnalyticsRow>,
    currency: NumberFormat,
    modifier: Modifier = Modifier,
) {
    DashboardChartCard(title = title, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.take(10).forEachIndexed { index, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${index + 1}. ${row.label}", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(currency.format(row.revenue), fontWeight = FontWeight.SemiBold, color = BendeyColors.Primary)
                        Text("×${String.format("%.0f", row.quantity)}", style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogComboStats(catalog: CatalogAnalytics, currency: NumberFormat) {
    DashboardChartCard(title = "Combos") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ingresos combos")
                Text(currency.format(catalog.comboRevenue), fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Participación")
                Text(String.format("%.1f%%", catalog.comboParticipationPct), fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ticket con combo")
                Text(currency.format(catalog.avgTicketWithCombo))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ticket sin combo")
                Text(currency.format(catalog.avgTicketWithoutCombo))
            }
        }
    }
}
