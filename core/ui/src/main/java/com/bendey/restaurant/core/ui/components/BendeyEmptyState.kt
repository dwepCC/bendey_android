package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/**
 * Estado vacío. [icon] es opcional (decorativo: el título ya dice lo mismo).
 *
 * - Modo normal: bloque centrado con icono de 48 dp, título, descripción y acción.
 * - Modo [inline]: versión compacta alineada a la izquierda para listas; ahora SÍ muestra
 *   [description] y [action] (antes los descartaba) y usa tipografía del tema.
 */
@Composable
fun BendeyEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: @Composable (() -> Unit)? = null,
    inline: Boolean = false,
    icon: ImageVector? = null,
) {
    if (inline) {
        Row(
            modifier = modifier.padding(BendeySpacing.s16),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
            verticalAlignment = Alignment.Top,
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = BendeyColors.OnSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = BendeyColors.OnSurface,
                )
                description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
                action?.let {
                    Column(modifier = Modifier.padding(top = BendeySpacing.s4)) { it() }
                }
            }
        }
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(BendeySpacing.s24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = BendeyColors.OnSurface,
            textAlign = TextAlign.Center,
        )
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = BendeyColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        action?.invoke()
    }
}
