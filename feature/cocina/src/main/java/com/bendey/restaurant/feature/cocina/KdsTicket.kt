package com.bendey.restaurant.feature.cocina

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.designsystem.theme.stateColors
import com.bendey.restaurant.core.domain.kitchen.KdsAction
import com.bendey.restaurant.core.domain.kitchen.KdsColumn
import com.bendey.restaurant.core.domain.kitchen.KdsCopy
import com.bendey.restaurant.core.domain.kitchen.KdsRound
import com.bendey.restaurant.core.domain.kitchen.KdsUrgency
import com.bendey.restaurant.core.domain.kitchen.kdsActions
import com.bendey.restaurant.core.domain.kitchen.kdsElapsedLabel
import com.bendey.restaurant.core.domain.kitchen.kdsTicketTitle
import com.bendey.restaurant.core.domain.restaurant.ComandaStatus
import com.bendey.restaurant.core.domain.restaurant.KitchenItem
import java.time.Instant

/** Tokens KDS (DESIGN-SYSTEM §9, objetivo). */
private val NoteBackground = Color(0xFFFEF3C7)
private val NoteText = Color(0xFF7C2D12) // sobre #FEF3C7: ~8:1
private val UrgencyGreen = Color(0xFF2E7D32)
private val UrgencyAmberText = Color(0xFFB45309) // el ambar #F9A825 no llega a 3:1 como texto sobre blanco
private val UrgencyAmberBorder = Color(0xFFF9A825)
private val UrgencyRed = Color(0xFFC62828)
private val ACTION_HEIGHT = 64.dp

private fun KdsUrgency.textColor(): Color = when (this) {
    KdsUrgency.VERDE -> UrgencyGreen
    KdsUrgency.AMBAR -> UrgencyAmberText
    KdsUrgency.ROJO -> UrgencyRed
}

/**
 * Tarjeta de UNA ronda en el KDS. Estructura: encabezado (mesa + cronometro) -> linea secundaria ->
 * lineas de item (modificadores y notas sin truncar) -> pie con accion -> menu ⋮ (anular).
 * El atraso NUNCA es solo color: borde de 6 dp + icono + texto "ATRASADO".
 */
@Composable
fun KdsTicket(
    round: KdsRound,
    now: Instant,
    canAdvance: Boolean,
    canVoid: Boolean,
    onAction: (KdsAction) -> Unit,
    onVoidItem: (KitchenItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val urgency = round.urgency(now)
    val minutes = round.minutes(now)
    val readOnly = round.column == KdsColumn.ENTREGADO
    val borderWidth = when {
        readOnly -> 1.dp
        urgency == KdsUrgency.ROJO -> 6.dp
        urgency == KdsUrgency.AMBAR -> 3.dp
        else -> 2.dp
    }
    val borderColor = when {
        readOnly -> BendeyColors.StateEntregado.fill
        urgency == KdsUrgency.ROJO -> UrgencyRed
        urgency == KdsUrgency.AMBAR -> UrgencyAmberBorder
        else -> UrgencyGreen
    }
    val tone = round.column.status.stateColors()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BendeyShapeTokens.md,
        color = BendeyColors.Surface,
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Encabezado: mesa / tipo a la izquierda, cronometro a la derecha.
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = kdsTicketTitle(round),
                        fontSize = 36.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = BendeyColors.OnSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!readOnly) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (urgency != KdsUrgency.VERDE) {
                                Icon(
                                    imageVector = if (urgency == KdsUrgency.ROJO) Icons.Default.Warning else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = urgency.textColor(),
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Text(
                                text = kdsElapsedLabel(minutes),
                                fontSize = 32.sp,
                                lineHeight = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = urgency.textColor(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                            )
                        }
                        if (urgency == KdsUrgency.ROJO) {
                            Text(KdsCopy.LATE, fontSize = 20.sp, fontWeight = FontWeight.Black, color = UrgencyRed)
                        }
                    }
                }
                if (canVoid) {
                    VoidMenu(round = round, onVoidItem = onVoidItem)
                }
            }
            val secondary = listOfNotNull(
                round.floorName?.takeIf { it.isNotBlank() },
                if (round.orderType == "dine_in" || round.orderType == null) {
                    round.waiterName?.takeIf { it.isNotBlank() }?.let { KdsCopy.waiter(it) }
                } else {
                    round.customerName?.takeIf { it.isNotBlank() }
                },
                KdsCopy.round(round.orderNumber ?: 0),
            ).joinToString(" · ")
            if (secondary.isNotBlank()) {
                Text(secondary, fontSize = 16.sp, color = BendeyColors.OnSurfaceVariant)
            }

            // Lineas de item.
            round.items.forEachIndexed { index, item ->
                if (index > 0) Spacer(Modifier.height(4.dp))
                KdsItemLine(item = item, column = round.column, qtyColor = tone.onTint)
            }

            // Pie: acciones (un toque por ronda), >= 64 dp, ancho completo.
            if (canAdvance) {
                val actions = kdsActions(round.column)
                actions.forEachIndexed { i, action ->
                    KdsActionButton(action = action, primary = i == 0, onClick = { onAction(action) })
                }
            }
        }
    }
}

@Composable
private fun KdsItemLine(item: KitchenItem, column: KdsColumn, qtyColor: Color) {
    val ahead = item.status.ordinal > column.status.ordinal
    Column(modifier = Modifier.fillMaxWidth().alpha(if (ahead) 0.6f else 1f)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "${kdsFormatQty(item.kdsQuantity)}×",
                fontSize = 36.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Black,
                color = qtyColor,
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.kdsName,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.OnSurface,
                    maxLines = 2, // el plato puede ocupar 2 lineas; modificadores y notas NUNCA se truncan
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.isComboComponent && item.comboName != null) {
                    Text("↳ ${item.comboName}", fontSize = 16.sp, color = BendeyColors.AccentPurple)
                }
                if (ahead) {
                    Text("✓ ${KdsCopy.ITEM_DONE}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BendeyColors.OnSurfaceVariant)
                }
            }
        }
        item.modifierLines.forEach { line ->
            Text(
                text = line,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = BendeyColors.OnSurface,
                modifier = Modifier.padding(start = 52.dp),
            )
        }
        item.notes?.takeIf { it.isNotBlank() }?.let { note ->
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .background(NoteBackground, BendeyShapeTokens.xs)
                    .padding(8.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Nota del cliente", tint = NoteText, modifier = Modifier.size(28.dp))
                Text(note, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, color = NoteText)
            }
        }
    }
}

@Composable
private fun KdsActionButton(action: KdsAction, primary: Boolean, onClick: () -> Unit) {
    val tone = action.target.stateColors()
    val fill = if (action.target == ComandaStatus.ENTREGADA) tone.onTint else tone.fill
    if (primary) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().heightIn(min = ACTION_HEIGHT),
            shape = BendeyShapeTokens.md,
            colors = ButtonDefaults.buttonColors(containerColor = fill, contentColor = Color.White),
        ) {
            Text(action.label, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().heightIn(min = ACTION_HEIGHT),
            shape = BendeyShapeTokens.md,
            border = BorderStroke(2.dp, tone.fill),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = tone.onTint),
        ) {
            Text(action.label, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun VoidMenu(round: KdsRound, onVoidItem: (KitchenItem) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val voidable = round.items.filter { it.status != ComandaStatus.ENTREGADA }.distinctBy { it.id }
    if (voidable.isEmpty()) return
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(BendeySpacing.touchPrimary)) {
            Icon(Icons.Default.MoreVert, contentDescription = KdsCopy.TICKET_MENU, tint = BendeyColors.OnSurfaceVariant)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            voidable.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            KdsCopy.voidItem("${kdsFormatQty(item.quantity)}× ${item.productName}"),
                            fontSize = 18.sp,
                            color = BendeyColors.Error,
                        )
                    },
                    onClick = {
                        open = false
                        onVoidItem(item)
                    },
                    modifier = Modifier.heightIn(min = BendeySpacing.touchKds),
                )
            }
        }
    }
}

fun kdsFormatQty(qty: Double): String =
    if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()
