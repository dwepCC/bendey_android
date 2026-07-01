package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyCardDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.restaurant.PosCartLine
import com.bendey.restaurant.core.ui.layout.adaptive.AdaptivePos
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BendeyPosCartPane(
    title: String,
    lines: List<PosCartLine>,
    total: Double,
    currency: NumberFormat,
    sending: Boolean,
    sendLabel: String = "Enviar comanda",
    onIncrement: (PosCartLine) -> Unit,
    onDecrement: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClearCart: (() -> Unit)? = null,
    canClearCart: Boolean = false,
    editablePrice: Boolean = false,
    showLineNotes: Boolean = false,
    onLineNotesClick: ((PosCartLine) -> Unit)? = null,
    onLineUnitPriceChange: ((PosCartLine, String) -> Unit)? = null,
    primaryAction: (@Composable () -> Unit)? = null,
    secondaryAction: (@Composable () -> Unit)? = null,
    showHeader: Boolean = true,
    showTotal: Boolean = true,
    lineSpacing: Dp = BendeySpacing.xxs,
    workspaceLines: Boolean = false,
    lineStepperSize: Dp = 40.dp,
    lineInnerPadding: Dp = BendeySpacing.sm,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (showHeader) BendeyColors.Surface else Color.Transparent)
            .padding(horizontal = if (showHeader) BendeySpacing.sm else 0.dp, vertical = if (showHeader) BendeySpacing.sm else 0.dp),
    ) {
        if (showHeader) {
            Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(BendeyColors.PrimaryContainer, BendeyShapeTokens.md),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = BendeyColors.Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (canClearCart && onClearCart != null && lines.isNotEmpty()) {
                IconButton(
                    onClick = onClearCart,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Vaciar carrito",
                        tint = BendeyColors.Error,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        }
        if (showHeader) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = BendeySpacing.sm),
                color = BendeyColors.Outline.copy(alpha = 0.35f),
            )
        }
        if (lines.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Sin productos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                    Text(
                        "Toca un producto para agregarlo",
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = BendeySpacing.xxs),
                    )
                }
            }
        } else {
            val listState = rememberLazyListState()
            BendeyLazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(lineSpacing),
            ) {
                items(lines, key = { it.key }) { line ->
                    BendeyCartLineCard(
                        line = line,
                        currency = currency,
                        editablePrice = editablePrice,
                        showNotesButton = showLineNotes,
                        onNotesClick = onLineNotesClick?.let { cb -> { cb(line) } },
                        onUnitPriceChange = onLineUnitPriceChange?.let { cb -> { price -> cb(line, price) } },
                        onIncrement = { onIncrement(line) },
                        onDecrement = { onDecrement(line.key) },
                        workspaceStyle = workspaceLines,
                        stepperSize = lineStepperSize,
                        lineInnerPadding = lineInnerPadding,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (showTotal) BendeySpacing.sm else 0.dp)
                .then(
                    if (showTotal) {
                        Modifier
                            .clip(BendeyShapeTokens.xl)
                            .background(BendeyColors.PrimaryContainer.copy(alpha = 0.4f))
                            .border(1.dp, BendeyColors.Primary.copy(alpha = 0.12f), BendeyShapeTokens.xl)
                            .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.sm)
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (showTotal) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = BendeyColors.OnSurfaceVariant,
                )
                Text(
                    currency.format(total),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.Primary,
                    modifier = Modifier.padding(top = BendeySpacing.xxs),
                )
            }
            primaryAction?.let { action ->
                Column(
                    modifier = Modifier.padding(top = if (showTotal) BendeySpacing.sm else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                ) {
                    action()
                }
            }
            secondaryAction?.let { action ->
                Box(modifier = Modifier.padding(top = BendeySpacing.sm)) { action() }
            }
        }
    }
}

@Composable
private fun BendeyCartLineCard(
    line: PosCartLine,
    currency: NumberFormat,
    editablePrice: Boolean = false,
    showNotesButton: Boolean = false,
    onNotesClick: (() -> Unit)? = null,
    onUnitPriceChange: ((String) -> Unit)? = null,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    workspaceStyle: Boolean = false,
    stepperSize: Dp = 40.dp,
    lineInnerPadding: Dp = BendeySpacing.sm,
) {
    Card(
        shape = BendeyShapeTokens.lg,
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BendeyColors.Outline.copy(alpha = 0.35f), BendeyShapeTokens.lg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = lineInnerPadding,
                    vertical = if (workspaceStyle) lineInnerPadding else BendeySpacing.xs,
                ),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        line.product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    line.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = BendeyColors.OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (line.comboSummaryLines.isNotEmpty()) {
                        line.comboSummaryLines.forEach { summaryLine ->
                            Text(
                                summaryLine,
                                style = MaterialTheme.typography.labelSmall,
                                color = BendeyColors.OnSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    line.notes.takeIf { it.isNotBlank() }?.let { notes ->
                        Text(
                            notes,
                            style = MaterialTheme.typography.labelSmall,
                            color = BendeyColors.Warning,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                CartQuantityStepper(
                    quantity = line.quantity,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                    buttonSize = stepperSize,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
            ) {
                if (showNotesButton && onNotesClick != null) {
                    BendeyIconButton(
                        onClick = onNotesClick,
                        icon = Icons.Default.EditNote,
                        contentDescription = if (line.notes.isBlank()) "Notas" else "Editar notas",
                        tint = if (line.notes.isBlank()) BendeyColors.OnSurfaceVariant else BendeyColors.Primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
                if (editablePrice && onUnitPriceChange != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Text(
                            text = "P.u.",
                            style = MaterialTheme.typography.labelSmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                        CartUnitPriceField(
                            lineKey = line.key,
                            unitPrice = line.effectiveUnitPrice,
                            onUnitPriceChange = onUnitPriceChange,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Text(
                    currency.format(line.lineTotal),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.Primary,
                )
            }
        }
    }
}

private fun formatUnitPriceDisplay(value: Double): String =
    String.format(Locale.US, "%.2f", value)

private fun isValidPartialPriceInput(input: String): Boolean {
    if (input.isEmpty()) return true
    return input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))
}

@Composable
private fun CartUnitPriceField(
    lineKey: String,
    unitPrice: Double,
    onUnitPriceChange: (String) -> Unit,
) {
    var text by remember(lineKey) { mutableStateOf(formatUnitPriceDisplay(unitPrice)) }
    var isFocused by remember(lineKey) { mutableStateOf(false) }
    val borderColor = if (isFocused) {
        BendeyColors.Primary
    } else {
        BendeyColors.Outline.copy(alpha = 0.45f)
    }

    LaunchedEffect(unitPrice, lineKey) {
        if (!isFocused) {
            text = formatUnitPriceDisplay(unitPrice)
        }
    }

    BasicTextField(
        value = text,
        onValueChange = { newValue ->
            val normalized = newValue.replace(',', '.')
            if (isValidPartialPriceInput(normalized)) {
                text = normalized
                if (normalized.isNotBlank() && normalized != ".") {
                    onUnitPriceChange(normalized)
                }
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = BendeyColors.OnSurface,
            fontWeight = FontWeight.SemiBold,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .width(72.dp)
            .height(36.dp)
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (!state.isFocused) {
                    val parsed = text.replace(',', '.').trim().toDoubleOrNull()
                    val committed = parsed ?: unitPrice
                    text = formatUnitPriceDisplay(committed)
                    onUnitPriceChange(formatUnitPriceDisplay(committed))
                }
            },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(BendeyShapeTokens.xs)
                    .border(1.dp, borderColor, BendeyShapeTokens.xs)
                    .background(BendeyColors.Surface)
                    .padding(horizontal = BendeySpacing.sm),
                contentAlignment = Alignment.CenterStart,
            ) {
                innerTextField()
            }
        },
    )
}

@Composable
private fun CartQuantityStepper(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    buttonSize: Dp = 40.dp,
) {
    BendeyQuantityStepper(
        quantity = quantity,
        onDecrease = onDecrement,
        onIncrease = onIncrement,
        buttonSize = buttonSize,
        compact = true,
        dense = true,
    )
}

data class BendeyCartAction(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val style: BendeyCartActionStyle = BendeyCartActionStyle.FilledTonal,
)

enum class BendeyCartActionStyle {
    Primary,
    FilledTonal,
    SecondaryFilled,
    NeutralFilled,
    LowEmphasisFilled,
}

/**
 * Grid adaptativo de acciones del carrito POS (2 columnas, filas automáticas).
 */
@Composable
fun BendeyCartActionGrid(
    actions: List<BendeyCartAction>,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = BendeySpacing.sm,
    verticalSpacing: Dp = BendeySpacing.sm,
) {
    if (actions.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            ) {
                rowActions.forEach { action ->
                    val buttonModifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                    when (action.style) {
                        BendeyCartActionStyle.Primary -> BendeyPrimaryButton(
                            text = action.text,
                            onClick = action.onClick,
                            enabled = action.enabled,
                            modifier = buttonModifier,
                        )
                        BendeyCartActionStyle.FilledTonal -> BendeyFilledTonalButton(
                            text = action.text,
                            onClick = action.onClick,
                            enabled = action.enabled,
                            modifier = buttonModifier,
                        )
                        BendeyCartActionStyle.SecondaryFilled -> BendeyFilledSecondaryCartButton(
                            text = action.text,
                            onClick = action.onClick,
                            enabled = action.enabled,
                            modifier = buttonModifier,
                        )
                        BendeyCartActionStyle.NeutralFilled -> BendeyFilledNeutralCartButton(
                            text = action.text,
                            onClick = action.onClick,
                            enabled = action.enabled,
                            modifier = buttonModifier,
                        )
                        BendeyCartActionStyle.LowEmphasisFilled -> BendeyFilledLowEmphasisCartButton(
                            text = action.text,
                            onClick = action.onClick,
                            enabled = action.enabled,
                            modifier = buttonModifier,
                        )
                    }
                }
                if (rowActions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
