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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.dashboard.DashboardDailyPoint
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
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    val dash = state.dashboard

    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BendeyColors.Background),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                GreetingCard(
                    userName = state.userName,
                    isOnline = state.isOnline,
                    cashLabel = state.cashLabel,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardMetricCard(
                        label = revenueLabel(state.range),
                        value = currency.format(dash.summary.totalRevenue),
                        changePct = dash.summary.revenueChangePct,
                        range = state.range,
                        icon = Icons.Default.ShowChart,
                        iconBg = BendeyColors.PrimaryContainer,
                        iconTint = BendeyColors.Primary,
                        modifier = Modifier.weight(1f),
                    )
                    DashboardMetricCard(
                        label = ordersLabel(state.range),
                        value = dash.summary.totalSessions.toString(),
                        changePct = dash.summary.sessionsChangePct,
                        range = state.range,
                        icon = Icons.Default.ShoppingBag,
                        iconBg = BendeyColors.SuccessContainer,
                        iconTint = BendeyColors.Success,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardMetricCard(
                        label = "Ticket promedio",
                        value = currency.format(dash.summary.avgTicket),
                        changePct = null,
                        range = state.range,
                        icon = Icons.Default.Wallet,
                        iconBg = BendeyColors.AccentPurpleContainer,
                        iconTint = BendeyColors.AccentPurple,
                        modifier = Modifier.weight(1f),
                    )
                    DashboardMetricCard(
                        label = "Comensales",
                        value = dash.summary.totalGuests.toString(),
                        changePct = null,
                        range = state.range,
                        icon = Icons.Default.Group,
                        iconBg = BendeyColors.InfoContainer,
                        iconTint = BendeyColors.Info,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                TableStatusSection(
                    summary = dash.tableSummary,
                    onVerMapa = onOpenMesas,
                )
            }
            item {
                RevenueTrendCard(
                    points = dash.daily30,
                    currency = currency,
                    onVerReporte = onOpenVentas,
                )
            }
            item {
                TopProductsSection(
                    products = dash.topProducts,
                    currency = currency,
                    onVerTodos = onOpenVentas,
                )
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
private fun GreetingCard(
    userName: String,
    isOnline: Boolean,
    cashLabel: String?,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hola, ${userName.ifBlank { "Administrador" }} 👋",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.OnSurface,
                )
                Text(
                    text = "Resumen de tu restaurante",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isOnline) BendeyColors.Success else BendeyColors.Error,
                                CircleShape,
                            ),
                    )
                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOnline) BendeyColors.Success else BendeyColors.Error,
                    )
                }
                Text(
                    text = cashLabel ?: "Caja S/ 0.00",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.OnSurface,
                )
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BendeyColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
        DashboardRange.WEEK -> " vs sem. ant."
        DashboardRange.MONTH -> " vs mes ant."
    }
    val color = when {
        pct > 0 -> BendeyColors.Success
        pct < 0 -> BendeyColors.Error
        else -> BendeyColors.OnSurfaceVariant
    }
    val icon = when {
        pct > 0 -> Icons.AutoMirrored.Filled.TrendingUp
        pct < 0 -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.Default.TrendingFlat
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(
            text = when {
                pct > 0 -> "+${pct.roundToInt()}%$suffix"
                pct < 0 -> "${pct.roundToInt()}%$suffix"
                else -> "Sin cambio$suffix"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun TableStatusSection(
    summary: DashboardTableSummary,
    onVerMapa: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Estado de mesas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onVerMapa, contentPadding = PaddingValues(0.dp)) {
                    Text("Ver mapa", color = BendeyColors.Info, fontWeight = FontWeight.SemiBold)
                }
            }
            if (summary.total == 0) {
                Text("No hay mesas configuradas", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TableStatusChip("Libres", summary.libre, Color(0xFF2E7D32), Color(0xFFE8F5E9), Modifier.weight(1f))
                    TableStatusChip("Ocupadas", summary.ocupada, Color(0xFFDC2626), Color(0xFFFEE2E2), Modifier.weight(1f))
                    TableStatusChip("Reservadas", summary.reservada, Color(0xFFD97706), Color(0xFFFEF3C7), Modifier.weight(1f))
                    TableStatusChip("Consum.", summary.enConsumo, Color(0xFF2563EB), Color(0xFFDBEAFE), Modifier.weight(1f))
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
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
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
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Tendencia de ingresos (30 días)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onVerReporte, contentPadding = PaddingValues(0.dp)) {
                    Text("Ver reporte", color = BendeyColors.Info, fontWeight = FontWeight.SemiBold)
                }
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
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Platos más vendidos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onVerTodos, contentPadding = PaddingValues(0.dp)) {
                    Text("Ver todos", color = BendeyColors.Info, fontWeight = FontWeight.SemiBold)
                }
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
    Color(0xFFF97316),
    Color(0xFF84CC16),
    Color(0xFFEC4899),
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
                .background(accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
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
    DashboardRange.WEEK -> "Ingresos (7 días)"
    DashboardRange.MONTH -> "Ingresos (30 días)"
}

private fun ordersLabel(range: DashboardRange): String = when (range) {
    DashboardRange.TODAY -> "Pedidos hoy"
    DashboardRange.WEEK -> "Pedidos (7 días)"
    DashboardRange.MONTH -> "Pedidos (30 días)"
}
