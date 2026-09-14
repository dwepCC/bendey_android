package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.previews.BendeyPhonePreview
import com.bendey.restaurant.core.designsystem.previews.BendeyPreviewSurface
import com.bendey.restaurant.core.designsystem.previews.BendeyTabletPreview
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit

/**
 * Botón de icono para una fila de lista densa — Editar/Anular/Quitar junto a cada ítem de una
 * comanda, un producto, una fila de mesa.
 *
 * La auditoría de 2026 encontró estos botones en POS y Mesas achicados a mano a 32dp de hit
 * target (con `Modifier.size(32.dp)` sobre [BendeyIconButton]) — un 33% por debajo del mínimo
 * de accesibilidad táctil que el propio sistema declara (`BendeySpacing.touchTarget` = 48dp), en
 * las pantallas de mayor presión operativa (cocina, caja en movimiento). Este componente es la
 * talla intermedia oficial para ese caso: 40dp de hit target / 20dp de icono por defecto — lo
 * bastante compacto para una fila densa, sin bajar del umbral razonable de accesibilidad. Nunca
 * usar `Modifier.size()` para achicar [BendeyIconButton] por debajo de esto; usar este componente.
 */
@Composable
fun BendeyCompactIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.sizeIn(
            minWidth = HitTarget,
            minHeight = HitTarget,
        ),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = BendeyColors.OnSurface,
            disabledContentColor = BendeyColors.OnSurface.copy(alpha = BendeyButtonDefaults.DisabledContentAlpha),
        ),
    ) {
        content()
    }
}

@Composable
fun BendeyCompactIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current,
) {
    BendeyCompactIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(GlyphSize),
        )
    }
}

/** Hit target — 40dp, la talla "compacta" oficial (la de navegación sigue en 48dp). */
private val HitTarget = 40.dp

/** Tamaño de glifo por defecto dentro de [BendeyCompactIconButton] — 20dp. */
private val GlyphSize = 20.dp

@BendeyPhonePreview
@Composable
private fun BendeyCompactIconButtonPhonePreview() {
    BendeyPreviewSurface {
        BendeyCompactIconButton(
            onClick = {},
            icon = Icons.Default.Edit,
            contentDescription = "Editar",
        )
    }
}

@BendeyTabletPreview
@Composable
private fun BendeyCompactIconButtonDestructivePreview() {
    BendeyPreviewSurface {
        BendeyCompactIconButton(
            onClick = {},
            icon = Icons.Default.Delete,
            contentDescription = "Eliminar",
            tint = BendeyColors.Error,
        )
    }
}
