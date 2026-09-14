package com.bendey.restaurant.core.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import kotlin.math.roundToInt

/**
 * Variación porcentual de un KPI frente a un período anterior — la funcionalidad de tendencia
 * que antes solo existía en el `ChangeBadge` privado de `DashboardScreen` (que envolvía su propio
 * `DashboardMetricCard`, casi un duplicado visual de este componente). Deliberadamente no depende
 * de ningún enum específico del Dashboard: `suffix` ya trae el texto formateado por el caller
 * ("vs ayer", "vs mes ant.", o vacío) para que este componente siga siendo reutilizable en
 * cualquier pantalla con KPIs, no solo el Dashboard.
 */
data class BendeyKpiTrend(
    val pct: Double,
    val suffix: String = "",
)

@Composable
fun BendeyKpiCard(
    title: String,
    value: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    hint: String? = null,
    containerColor: Color? = null,
    compact: Boolean = false,
    trend: BendeyKpiTrend? = null,
    animateValue: Boolean = false,
) {
    val padding = if (compact) BendeySpacing.sm else BendeySpacing.md
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BendeyShapeTokens.lg,
        color = containerColor ?: BendeyColors.Surface,
        border = BorderStroke(1.dp, BendeyColors.Outline.copy(alpha = 0.65f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(if (compact) BendeySpacing.xxs else BendeySpacing.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 32.dp else 34.dp)
                        .background(accentColor.copy(alpha = 0.12f), BendeyShapeTokens.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(if (compact) 16.dp else 17.dp),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            val valueStyle = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium
            if (animateValue) {
                // El Dashboard anima el valor al refrescar (crossfade) — antes solo lo hacía su
                // DashboardMetricCard privado; ahora cualquier KPI puede pedirlo.
                AnimatedContent(targetState = value, label = "BendeyKpiCardValue") { animatedValue ->
                    Text(
                        text = animatedValue,
                        style = valueStyle,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Text(
                    text = value,
                    style = valueStyle,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                    maxLines = 1,
                )
            }
            trend?.let { BendeyKpiTrendBadge(it) }
        }
    }
}

/**
 * Badge de tendencia (+12% vs ayer / -3% / Sin cambio) — pública porque además de usarla
 * [BendeyKpiCard] internamente, `DashboardScreen` la reutiliza en su propio `DashboardMetricCard`
 * (layout distinto a propósito — icono arriba-a-la-derecha y valor animado en cross-fade, un
 * patrón que ya funcionaba bien y no había motivo para rediseñar) en vez de mantener una segunda
 * copia de esta fórmula pct→color/ícono/texto.
 */
@Composable
fun BendeyKpiTrendBadge(trend: BendeyKpiTrend, modifier: Modifier = Modifier) {
    val trendColor = when {
        trend.pct > 0 -> BendeyColors.Success
        trend.pct < 0 -> BendeyColors.Error
        else -> BendeyColors.OnSurfaceVariant
    }
    val trendIcon = when {
        trend.pct > 0 -> Icons.AutoMirrored.Filled.TrendingUp
        trend.pct < 0 -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.AutoMirrored.Filled.TrendingFlat
    }
    val trendText = when {
        trend.pct > 0 -> "+${trend.pct.roundToInt()}%${trend.suffix}"
        trend.pct < 0 -> "${trend.pct.roundToInt()}%${trend.suffix}"
        else -> "Sin cambio${trend.suffix}"
    }
    BendeyBadge(text = trendText, color = trendColor, icon = trendIcon, modifier = modifier)
}
