package com.bendey.restaurant.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.accentColor
import com.bendey.restaurant.core.domain.restaurant.RestaurantTable
import com.bendey.restaurant.core.domain.restaurant.TableStatus
import java.text.NumberFormat
import java.util.Locale

/** Tarjeta operativa — fondo blanco, barra lateral de estado (React `TableOperationalCard`). */
@Composable
fun BendeyTableCard(
    table: RestaurantTable,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = table.isClickable,
) {
    val accent = table.status.accentColor()
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = BendeyColors.Surface,
        border = BorderStroke(1.dp, BendeyColors.Outline),
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(168.dp)
                    .background(accent),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusDot(color = accent)
                    Text(
                        text = table.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                }
                Text(
                    text = table.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                TableMetaFooter(
                    table = table,
                    accent = accent,
                    currency = currency,
                )
            }
        }
    }
}

@Composable
private fun TableMetaFooter(
    table: RestaurantTable,
    accent: Color,
    currency: NumberFormat,
) {
    val occupied = table.status == TableStatus.OCUPADA || table.status == TableStatus.EN_CONSUMO
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(accent)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MetaLine("Cap.", "${table.capacity} pers.")
        when {
            occupied -> {
                table.guests?.takeIf { it > 0 }?.let { MetaLine("Comensales", "$it") }
                table.waiterName?.takeIf { it.isNotBlank() }?.let { MetaLine("Mozo", it) }
                table.totalAmount?.takeIf { it > 0 }?.let { amount ->
                    MetaLine("Total", currency.format(amount), emphasize = true)
                }
            }
            table.status == TableStatus.RESERVADA -> {
                table.guests?.takeIf { it > 0 }?.let { MetaLine("Comensales", "$it") }
            }
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .width(10.dp)
            .height(10.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}

@Composable
fun BendeyTableStatsRow(
    libre: Int,
    ocupada: Int,
    reservada: Int,
    enConsumo: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StatDot("Libres", libre, BendeyColors.TableLibre)
        StatDot("Ocupadas", ocupada, BendeyColors.TableOcupada)
        StatDot("Reservadas", reservada, BendeyColors.TableReservada)
        StatDot("Consum.", enConsumo, BendeyColors.TableEnConsumo)
    }
}

@Composable
private fun StatDot(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.labelSmall,
            color = BendeyColors.OnSurfaceVariant,
        )
    }
}
