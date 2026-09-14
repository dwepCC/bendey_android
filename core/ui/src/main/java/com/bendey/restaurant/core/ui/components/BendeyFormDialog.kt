package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
    // Anular/Eliminar/Revocar: el botón de confirmar pasa de BendeyPrimaryButton a
    // BendeyDestructiveButton (color Error) — misma semántica que el resto del sistema para una
    // operación irreversible, en vez de cada flujo decidiendo su propio color a mano.
    destructive: Boolean = false,
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
    val requestedDialogHeight = if (tabletPos) {
        PosPolishTokens.dialogMaxHeight(profile, physicalPortrait)
    } else {
        720.dp
    }
    // En pantallas chicas 720dp puede superar el alto real disponible; como solo el contenido
    // interno hace scroll (no el diálogo completo), el resto se desbordaba fuera de pantalla y
    // los botones Cancelar/Confirmar quedaban inalcanzables. Se limita al alto real de pantalla.
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val maxDialogHeight = requestedDialogHeight.coerceAtMost(screenHeightDp * 0.92f)
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
                    if (destructive) {
                        BendeyDestructiveButton(
                            text = if (loading) "Procesando…" else confirmText,
                            onClick = onConfirm,
                            enabled = confirmEnabled && !loading,
                            fillWidth = true,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
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
}

/**
 * Wrapper delgado sobre [BendeySelect] (`searchable = true`) — misma firma pública de siempre,
 * ningún call site cambia. Antes tenía su propia implementación completa de Popup+búsqueda,
 * copiada casi idéntica en [BendeySimpleSelect]; ahora ambas comparten una sola implementación.
 */
@Composable
fun BendeySearchableSelect(
    options: List<BendeySelectOption>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    label: String,
    placeholder: String = "Buscar…",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    BendeySelect(
        options = options,
        selectedOption = options.firstOrNull { it.id == selectedId },
        onSelect = { onSelect(it.id) },
        optionLabel = { it.label },
        label = label,
        modifier = modifier,
        searchable = true,
        placeholder = placeholder,
        isOptionSelected = { it.id == selectedId },
        enabled = enabled,
    )
}

data class BendeyOption(
    val value: String,
    val label: String,
)

/**
 * Select compacto (valor texto) — categorías, roles, IGV, etc.
 * Wrapper delgado sobre [BendeySelect] (`searchable = false`) — ver nota en [BendeySearchableSelect].
 */
@Composable
fun BendeySimpleSelect(
    options: List<BendeyOption>,
    selectedValue: String?,
    onSelect: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Seleccionar",
    enabled: Boolean = true,
) {
    BendeySelect(
        options = options,
        selectedOption = options.firstOrNull { it.value == selectedValue },
        onSelect = { onSelect(it.value) },
        optionLabel = { it.label },
        label = label,
        modifier = modifier,
        searchable = false,
        placeholder = placeholder,
        isOptionSelected = { it.value == selectedValue },
        enabled = enabled,
    )
}
