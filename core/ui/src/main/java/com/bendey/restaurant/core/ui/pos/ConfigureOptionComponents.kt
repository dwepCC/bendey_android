package com.bendey.restaurant.core.ui.pos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyChipDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyQuantityStepper
import com.bendey.restaurant.core.ui.components.BendeyTextField

/** Espaciado compacto para pantallas de configure POS (~15–20% menos alto por fila). */
internal object ConfigureUiTokens {
    val rowPaddingVertical = 8.dp
    val labelToStepperGap = 4.dp
    val dividerTop = 4.dp
    val priceStartGap = 6.dp
    val groupItemGap = 4.dp
}

@Composable
internal fun ConfigureSectionHeader(
    title: String,
    required: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Text(
        text = if (required) "$title *" else title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = BendeyColors.OnSurface,
        modifier = modifier,
    )
}

@Composable
internal fun ConfigureQuantityOptionRow(
    name: String,
    extraPriceLabel: String?,
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseEnabled: Boolean,
    increaseEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ConfigureUiTokens.rowPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(ConfigureUiTokens.labelToStepperGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = BendeyColors.OnSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
            )
            extraPriceLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.Primary,
                    modifier = Modifier.padding(start = ConfigureUiTokens.priceStartGap),
                    maxLines = 1,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            BendeyQuantityStepper(
                quantity = quantity,
                onDecrease = onDecrease,
                onIncrease = onIncrease,
                decreaseEnabled = decreaseEnabled,
                increaseEnabled = increaseEnabled,
                compact = true,
            )
        }
    }
    HorizontalDivider(
        color = BendeyColors.Outline.copy(alpha = 0.4f),
        modifier = Modifier.padding(top = ConfigureUiTokens.dividerTop),
    )
}

@Composable
internal fun ConfigureSelectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BendeySpacing.touchTarget),
        colors = BendeyChipDefaults.posFilterChipColors(),
        shape = BendeyShapeTokens.chip,
        border = null,
    )
}

@Composable
internal fun ConfigureOptionsGroup(
    title: String,
    required: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ConfigureUiTokens.groupItemGap),
    ) {
        ConfigureSectionHeader(title = title, required = required)
        content()
    }
}

@Composable
internal fun ConfigureKitchenNoteField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    BendeyTextField(
        value = value,
        onValueChange = onValueChange,
        label = "Notas para cocina",
        singleLine = false,
        modifier = Modifier.fillMaxWidth(),
    )
}
