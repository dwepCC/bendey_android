package com.bendey.restaurant.core.ui.pos.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile

/** Acciones del header del carrito — iconos simples, sin barra adicional. */
@Composable
fun BendeyPosCartHeaderActions(
    pendingCount: Int,
    showEditDetails: Boolean,
    onOpenPending: () -> Unit,
    onEditDetails: () -> Unit,
    modifier: Modifier = Modifier,
    canClearCart: Boolean = false,
    onClearCart: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canClearCart && onClearCart != null) {
            BendeyIconButton(
                onClick = onClearCart,
                icon = Icons.Default.DeleteSweep,
                contentDescription = "Vaciar carrito",
                tint = BendeyColors.Error,
                modifier = Modifier.size(BendeySpacing.touchTarget),
            )
        }
        if (showEditDetails) {
            BendeyIconButton(
                onClick = onEditDetails,
                icon = Icons.Default.Edit,
                contentDescription = "Datos del pedido",
                tint = BendeyColors.Primary,
                modifier = Modifier.size(BendeySpacing.touchTarget),
            )
        }
        BadgedBox(
            badge = {
                if (pendingCount > 0) {
                    Badge {
                        Text(if (pendingCount > 99) "99+" else pendingCount.toString())
                    }
                }
            },
        ) {
            BendeyIconButton(
                onClick = onOpenPending,
                icon = Icons.AutoMirrored.Filled.List,
                contentDescription = "Pedidos pendientes",
                tint = BendeyColors.OnSurface,
                modifier = Modifier.size(BendeySpacing.touchTarget),
            )
        }
    }
}

/**
 * Tipo de pedido — réplica visual de [OrderTypeRow] en móvil.
 * Mismos colores, formas y tipografía; solo más ancho en tablet.
 */
@Composable
fun BendeyPosOrderTypeStrip(
    selectedLabel: String,
    options: List<BendeyPosOrderTypeOption>,
    onSelect: (String) -> Unit,
    profile: BendeyAdaptiveProfile,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(BendeyShapeTokens.lg)
            .background(BendeyColors.SurfaceVariant.copy(alpha = 0.65f))
            .padding(BendeySpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
    ) {
        options.forEach { option ->
            val selected = option.label == selectedLabel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(BendeyShapeTokens.sm)
                    .background(if (selected) BendeyColors.Primary else Color.Transparent)
                    .clickable { onSelect(option.label) }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) BendeyColors.OnPrimary else BendeyColors.OnSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

data class BendeyPosOrderTypeOption(val label: String)
