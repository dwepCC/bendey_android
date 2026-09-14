package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/**
 * Selector genérico — reemplaza la implementación duplicada de `BendeySearchableSelect` y
 * `BendeySimpleSelect` (antes cada uno tenía su propio popup, casi idéntico, copiado dos veces).
 * `BendeySearchableSelect`/`BendeySimpleSelect` siguen existiendo tal cual —mismo nombre, misma
 * firma, ningún call site cambia— pero ahora son wrappers delgados sobre este único componente.
 *
 * Un tercer patrón de "elegir de una lista" convivía en la app vía `ExposedDropdownMenuBox` de
 * Material3 puro (Ventas, Reportes, algunos dropdowns de POS) — ese no queda cubierto acá porque
 * su forma de anclarse es distinta (campo con flecha nativo vs. este popup); se migra pantalla
 * por pantalla en la Fase 06 donde no arriesgue comportamiento de teclado/foco ya afinado.
 */
@Composable
fun <T> BendeySelect(
    options: List<T>,
    selectedOption: T?,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    label: String,
    modifier: Modifier = Modifier,
    searchable: Boolean = false,
    placeholder: String = if (searchable) "Buscar…" else "Seleccionar",
    isOptionSelected: (T) -> Boolean = { it == selectedOption },
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val disabledAlpha = if (enabled) 1f else BendeyButtonDefaults.DisabledContentAlpha
    var query by remember { mutableStateOf("") }
    val selectedLabel = selectedOption?.let(optionLabel).orEmpty()
    val filtered = remember(options, query, searchable) {
        if (!searchable || query.isBlank()) options
        else options.filter { optionLabel(it).contains(query, ignoreCase = true) }
    }

    Column(modifier = modifier) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.padding(bottom = BendeySpacing.xxs),
            )
        }
        var triggerWidthPx by remember { mutableStateOf(0) }
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { triggerWidthPx = it.width }
                    .clip(BendeyShapeTokens.md)
                    .border(1.dp, BendeyColors.Outline.copy(alpha = disabledAlpha), BendeyShapeTokens.md)
                    .background(BendeyColors.Surface.copy(alpha = disabledAlpha))
                    .clickable(enabled = enabled) { expanded = !expanded }
                    .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = selectedLabel.ifBlank { if (searchable) "Seleccionar" else placeholder },
                    style = MaterialTheme.typography.bodyMedium,
                    color = (if (selectedLabel.isBlank()) BendeyColors.OnSurfaceVariant else BendeyColors.OnSurface)
                        .copy(alpha = disabledAlpha),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = BendeyColors.OnSurfaceVariant.copy(alpha = disabledAlpha),
                )
            }
            // Flota sobre el contenido (Popup, no empuja layout): igual que antes, esta fila
            // puede vivir dentro de un header de altura fija cuyo hermano usa weight(1f) para
            // una lista — si el desplegable creciera inline, le robaría espacio a esa lista.
            if (expanded) {
                Popup(
                    popupPositionProvider = remember {
                        object : PopupPositionProvider {
                            override fun calculatePosition(
                                anchorBounds: IntRect,
                                windowSize: IntSize,
                                layoutDirection: LayoutDirection,
                                popupContentSize: IntSize,
                            ): IntOffset = IntOffset(anchorBounds.left, anchorBounds.bottom + 4)
                        }
                    },
                    onDismissRequest = { expanded = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    val triggerWidthDp = with(LocalDensity.current) { triggerWidthPx.toDp() }
                    Surface(
                        modifier = Modifier.width(triggerWidthDp),
                        shape = BendeyShapeTokens.md,
                        color = BendeyColors.Surface,
                        border = BorderStroke(1.dp, BendeyColors.Outline),
                        shadowElevation = 8.dp,
                    ) {
                        Column {
                            if (searchable) {
                                BendeyTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    label = placeholder,
                                    modifier = Modifier.padding(BendeySpacing.xs),
                                )
                                HorizontalDivider(color = BendeyColors.Outline.copy(alpha = 0.5f))
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                if (filtered.isEmpty()) {
                                    Text(
                                        text = "Sin resultados",
                                        modifier = Modifier.padding(BendeySpacing.sm),
                                        color = BendeyColors.OnSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                } else {
                                    filtered.forEach { option ->
                                        val selected = isOptionSelected(option)
                                        Text(
                                            text = optionLabel(option),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onSelect(option)
                                                    expanded = false
                                                    query = ""
                                                }
                                                .padding(horizontal = BendeySpacing.sm, vertical = 10.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (selected) BendeyColors.Primary else BendeyColors.OnSurface,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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
}
