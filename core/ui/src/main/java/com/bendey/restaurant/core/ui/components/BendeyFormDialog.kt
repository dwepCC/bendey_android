package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.rememberPhysicalPortrait
import com.bendey.restaurant.core.ui.pos.PosPolishTokens

data class BendeySelectOption(
    val id: Int,
    val label: String,
)

@Composable
fun BendeyFormDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    confirmText: String = "Guardar",
    dismissText: String = "Cancelar",
    confirmEnabled: Boolean = true,
    loading: Boolean = false,
    enableContentScroll: Boolean = false,
    fullWidth: Boolean = false,
    posTabletOptimized: Boolean = false,
    subtitle: String? = null,
    footerSummary: String? = null,
    validationError: String? = null,
    loadingMessage: String = "Cargando…",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = onDismissRequest,
    content: @Composable () -> Unit,
) {
    val profile = rememberBendeyAdaptiveProfile()
    val physicalPortrait = rememberPhysicalPortrait()
    val tabletPos = posTabletOptimized && PosPolishTokens.isTabletProfile(profile)
    val tabletLandscape = PosPolishTokens.usesPosTabletDialogLayout(profile, physicalPortrait)
    val surfacePadding = if (tabletPos) {
        PosPolishTokens.dialogPadding(profile, physicalPortrait)
    } else {
        BendeySpacing.lg
    }
    val fieldGap = if (tabletPos) {
        PosPolishTokens.dialogFieldGap(profile, physicalPortrait)
    } else {
        BendeySpacing.sm
    }
    val widthFraction = when {
        fullWidth && !tabletPos -> null
        tabletPos -> PosPolishTokens.dialogWidthFraction(profile, physicalPortrait)
        else -> 0.94f
    }
    val maxDialogHeight = if (tabletPos) {
        PosPolishTokens.dialogMaxHeight(profile, physicalPortrait)
    } else {
        720.dp
    }
    val footerReserve = 96.dp + if (tabletPos) BendeySpacing.md else BendeySpacing.lg
    val titleReserve = 36.dp + if (tabletPos) BendeySpacing.sm else BendeySpacing.md
    val defaultContentMaxHeight = (maxDialogHeight - footerReserve - titleReserve - surfacePadding * 2)
        .coerceAtLeast(180.dp)
    val scrollContentMaxHeight = when {
        enableContentScroll && tabletLandscape -> minOf(480.dp, defaultContentMaxHeight)
        enableContentScroll -> minOf(540.dp, defaultContentMaxHeight)
        else -> defaultContentMaxHeight
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .then(
                    when {
                        fullWidth && !tabletPos -> {
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        }
                        widthFraction != null -> Modifier.fillMaxWidth(widthFraction)
                        else -> Modifier.fillMaxWidth(0.94f)
                    },
                )
                .wrapContentHeight()
                .heightIn(max = maxDialogHeight),
            shape = BendeyShapeTokens.xl,
            color = BendeyColors.Surface,
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogHeight)
                    .padding(surfacePadding),
            ) {
                Text(
                    text = title,
                    style = if (tabletLandscape) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurface,
                )
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BendeyColors.OnSurfaceVariant,
                        modifier = Modifier.padding(top = BendeySpacing.xxs),
                    )
                }
                if (loading) {
                    Text(
                        text = loadingMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                        modifier = Modifier.padding(top = BendeySpacing.xxs),
                    )
                }
                val scrollState = rememberScrollState()
                val contentModifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (tabletPos) BendeySpacing.sm else BendeySpacing.md)
                    .heightIn(max = scrollContentMaxHeight)
                    .verticalScroll(scrollState)
                Column(
                    modifier = contentModifier,
                    verticalArrangement = Arrangement.spacedBy(fieldGap),
                ) {
                    content()
                }
                footerSummary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BendeyColors.OnSurface,
                        modifier = Modifier.padding(top = if (tabletPos) BendeySpacing.sm else BendeySpacing.md),
                    )
                }
                validationError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.Error,
                        modifier = Modifier.padding(top = BendeySpacing.xxs),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (tabletPos) BendeySpacing.md else BendeySpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                ) {
                    BendeySecondaryButton(
                        text = dismissText,
                        onClick = onDismiss,
                        enabled = !loading,
                        modifier = Modifier.weight(1f),
                    )
                    BendeyPrimaryButton(
                        text = if (loading) "Procesando…" else confirmText,
                        onClick = onConfirm,
                        enabled = confirmEnabled && !loading,
                        fillWidth = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun BendeySearchableSelect(
    options: List<BendeySelectOption>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    label: String,
    placeholder: String = "Buscar…",
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.label.orEmpty()
    val filtered = remember(options, query) {
        if (query.isBlank()) options
        else options.filter { it.label.contains(query, ignoreCase = true) }
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = BendeyColors.OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(BendeyShapeTokens.md)
                        .border(1.dp, BendeyColors.Outline, BendeyShapeTokens.md)
                        .background(BendeyColors.Surface)
                        .clickable { expanded = !expanded }
                        .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = selectedLabel.ifBlank { "Seleccionar" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedLabel.isBlank()) BendeyColors.OnSurfaceVariant else BendeyColors.OnSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = BendeyColors.OnSurfaceVariant,
                    )
                }
                if (expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                        .clip(BendeyShapeTokens.md)
                        .border(1.dp, BendeyColors.Outline, BendeyShapeTokens.md)
                        .background(BendeyColors.Surface),
                    ) {
                    BendeyTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = placeholder,
                        modifier = Modifier.padding(8.dp),
                    )
                    HorizontalDivider(color = BendeyColors.Outline.copy(alpha = 0.5f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (filtered.isEmpty()) {
                            Text(
                                text = "Sin resultados",
                                modifier = Modifier.padding(12.dp),
                                color = BendeyColors.OnSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            filtered.forEach { option ->
                                Text(
                                    text = option.label,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelect(option.id)
                                            expanded = false
                                            query = ""
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (option.id == selectedId) BendeyColors.Primary else BendeyColors.OnSurface,
                                    fontWeight = if (option.id == selectedId) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

data class BendeyOption(
    val value: String,
    val label: String,
)

/** Select compacto (valor texto) — categorías, roles, IGV, etc. */
@Composable
fun BendeySimpleSelect(
    options: List<BendeyOption>,
    selectedValue: String?,
    onSelect: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Seleccionar",
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label.orEmpty()

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = BendeyColors.OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(BendeyShapeTokens.md)
                        .border(1.dp, BendeyColors.Outline, BendeyShapeTokens.md)
                        .background(BendeyColors.Surface)
                        .clickable { expanded = !expanded }
                        .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = selectedLabel.ifBlank { placeholder },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedLabel.isBlank()) BendeyColors.OnSurfaceVariant else BendeyColors.OnSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = BendeyColors.OnSurfaceVariant,
                    )
                }
                if (expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                        .clip(BendeyShapeTokens.md)
                        .border(1.dp, BendeyColors.Outline, BendeyShapeTokens.md)
                        .background(BendeyColors.Surface),
                    ) {
                    options.forEach { option ->
                        Text(
                            text = option.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(option.value)
                                    expanded = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (option.value == selectedValue) BendeyColors.Primary else BendeyColors.OnSurface,
                            fontWeight = if (option.value == selectedValue) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                    }
                }
            }
        }
    }
}
