package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.bendey.restaurant.core.designsystem.components.BendeyCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert

/** Una acción de [BendeyListRow] — inline (icono junto al contenido) u overflow (menú "⋮"). */
data class BendeyListRowAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
)

/**
 * Fila de lista reutilizable — patrón de referencia tomado de Clientes/Compras, los dos módulos
 * que la auditoría de 2026 marcó como "disciplinados". El objetivo es compartir la ESTRUCTURA
 * visual (tarjeta, selección, acciones, click) entre módulos, no forzar que todo el contenido se
 * vea igual: `content` queda totalmente libre para que cada pantalla mantenga su propia identidad
 * funcional (líneas de texto, chips de estado, cifras, lo que corresponda a ese dominio).
 *
 * Cubre los siete casos que puede necesitar una lista:
 * - **Sin acciones**: no pasar `actions` ni `overflowActions`.
 * - **Acción de menú**: pasar `overflowActions` (colapsa en un botón "⋮" con [DropdownMenu]).
 * - **Acciones inline**: pasar `actions` (se renderizan como [BendeyCompactIconButton] en fila —
 *   el tamaño oficial de icono compacto, nunca un `Modifier.size()` improvisado).
 * - **Selección**: `selectable = true` muestra un checkbox a la izquierda.
 * - **Click**: `onClick` hace clickeable toda la fila (además o en vez de acciones puntuales).
 * - **Estado**: `selected = true` tiñe la tarjeta; para un estado del propio dominio (activo/
 *   inactivo, anulado…) va como parte de `content`, ej. un `BendeyStatusChip`.
 * - **Contenido secundario**: `leadingContent` — una miniatura o avatar antes del texto, cuando
 *   el dominio realmente tiene una imagen que identifica la fila (ej. foto de producto).
 */
@Composable
fun BendeyListRow(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    selectable: Boolean = false,
    onSelectedChange: ((Boolean) -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    actions: List<BendeyListRowAction> = emptyList(),
    overflowActions: List<BendeyListRowAction> = emptyList(),
    contentPadding: PaddingValues = PaddingValues(BendeySpacing.cardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    val containerColor = if (selected) BendeyColors.PrimaryContainer else BendeyColors.Surface
    BendeyCard(
        modifier = modifier,
        containerColor = containerColor,
        contentPadding = contentPadding,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            leadingContent?.invoke()
            if (selectable) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = onSelectedChange,
                    colors = CheckboxDefaults.colors(checkedColor = BendeyColors.Primary),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
                content = content,
            )
            if (actions.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs)) {
                    actions.forEach { action ->
                        BendeyCompactIconButton(
                            onClick = action.onClick,
                            icon = action.icon,
                            contentDescription = action.contentDescription,
                            enabled = action.enabled,
                            tint = if (action.destructive) BendeyColors.Error else BendeyColors.OnSurfaceVariant,
                        )
                    }
                }
            }
            if (overflowActions.isNotEmpty()) {
                var menuExpanded by remember { mutableStateOf(false) }
                BendeyCompactIconButton(
                    onClick = { menuExpanded = true },
                    icon = Icons.Default.MoreVert,
                    contentDescription = "Más acciones",
                )
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    overflowActions.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(action.contentDescription) },
                            leadingIcon = {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = null,
                                    tint = if (action.destructive) BendeyColors.Error else BendeyColors.OnSurfaceVariant,
                                )
                            },
                            enabled = action.enabled,
                            onClick = {
                                menuExpanded = false
                                action.onClick()
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Título estándar de la primera línea de un [BendeyListRow]. */
@Composable
fun BendeyListRowTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = BendeyColors.OnSurface,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = color,
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Línea secundaria estándar (subtítulo, dato de apoyo) de un [BendeyListRow]. */
@Composable
fun BendeyListRowSubtitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = BendeyColors.OnSurfaceVariant,
        modifier = modifier,
    )
}
