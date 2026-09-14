package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

@Composable
fun BendeySecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // Antes, un BendeySecondaryButton deshabilitado no cambiaba fondo ni borde — solo el texto
    // se atenuaba, así que un botón "apagado" seguía viéndose clickeable. Ahora fondo/borde/texto
    // bajan juntos al mismo alpha que el resto de los botones (BendeyButtonDefaults).
    val disabledAlpha = if (enabled) 1f else BendeyButtonDefaults.DisabledContentAlpha
    Row(
        modifier = modifier
            .heightIn(min = BendeySpacing.buttonHeight)
            .clip(BendeyShapeTokens.md)
            .border(1.dp, BendeyColors.Outline.copy(alpha = 0.75f * disabledAlpha), BendeyShapeTokens.md)
            .background(BendeyColors.SurfaceVariant.copy(alpha = 0.45f * disabledAlpha))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = BendeyColors.OnSurface.copy(alpha = disabledAlpha),
            fontWeight = FontWeight.Medium,
        )
    }
}
