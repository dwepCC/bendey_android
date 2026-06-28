package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/**
 * Selector +/- con área táctil mínima de 48dp (Material Design).
 * Reutilizable en POS configure, carrito y pantallas similares.
 *
 * @param compact Reduce espacio alrededor del stepper (configure POS); no reduce botones.
 */
@Composable
fun BendeyQuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    decreaseEnabled: Boolean = quantity > 0,
    increaseEnabled: Boolean = true,
    buttonSize: Dp = BendeySpacing.touchTarget,
    compact: Boolean = false,
    dense: Boolean = false,
) {
    val innerGap = when {
        dense -> BendeySpacing.xxs
        compact -> BendeySpacing.sm
        else -> BendeySpacing.md
    }
    val quantityMinWidth = when {
        dense -> 28.dp
        compact -> 36.dp
        else -> 44.dp
    }
    val iconSize = if (compact) 20.dp else 22.dp
    val quantityStyle = if (compact) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.headlineSmall
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(innerGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BendeyStepperButton(
            onClick = onDecrease,
            enabled = decreaseEnabled,
            backgroundColor = BendeyColors.Error,
            size = buttonSize,
            contentDescription = "Disminuir cantidad",
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = null,
                tint = BendeyColors.OnPrimary,
                modifier = Modifier.size(iconSize),
            )
        }
        Box(
            modifier = Modifier
                .widthIn(min = quantityMinWidth)
                .heightIn(min = buttonSize),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = quantity.toString(),
                style = quantityStyle,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = BendeyColors.OnSurface,
            )
        }
        BendeyStepperButton(
            onClick = onIncrease,
            enabled = increaseEnabled,
            backgroundColor = Color(0xFF1A1A1A),
            size = buttonSize,
            contentDescription = "Aumentar cantidad",
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = BendeyColors.OnPrimary,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun BendeyStepperButton(
    onClick: () -> Unit,
    enabled: Boolean,
    backgroundColor: Color,
    size: Dp,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = if (pressed && enabled) 0.92f else 1f
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.38f)
            .clip(BendeyShapeTokens.md)
            .background(backgroundColor)
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                onClickLabel = contentDescription,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
