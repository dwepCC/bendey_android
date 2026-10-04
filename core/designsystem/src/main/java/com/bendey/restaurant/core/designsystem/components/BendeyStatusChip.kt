package com.bendey.restaurant.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.designsystem.theme.BendeyStateColors

/**
 * Chip de estado: texto = `onTint` sobre `tint` (DESIGN-SYSTEM §3.2/§3.5), punto = `fill`.
 * Antes pintaba el color de acento como texto sobre su propio tinte al 12 % (1,8–4,4:1, todos fallaban AA).
 */
@Composable
fun BendeyStatusChip(
    label: String,
    tone: BendeyStateColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(tone.tint, BendeyShapeTokens.pill)
            .padding(horizontal = BendeySpacing.s12, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(tone.fill, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = tone.onTint,
        )
    }
}

/**
 * Compatibilidad con los llamadores existentes que pasan un color de acento: se resuelve al
 * tono conocido (éxito, aviso, peligro, info, estados...) y, si no existe, se calcula un texto
 * oscurecido que cumpla 4,5:1 sobre el tinte al 12 %.
 */
@Composable
fun BendeyStatusChip(
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val tone = remember(accentColor) {
        BendeyColors.toneForAccent(accentColor) ?: fallbackTone(accentColor)
    }
    BendeyStatusChip(label = label, tone = tone, modifier = modifier)
}

private fun fallbackTone(accent: Color): BendeyStateColors {
    val tint = accent.copy(alpha = 0.12f).compositeOver(Color.White)
    var text = accent
    var step = 0
    while (contrast(text, tint) < 4.5f && step < 20) {
        text = Color(
            red = text.red * 0.9f,
            green = text.green * 0.9f,
            blue = text.blue * 0.9f,
            alpha = 1f,
        )
        step++
    }
    return BendeyStateColors(fill = accent, tint = tint, onTint = text)
}

private fun contrast(a: Color, b: Color): Float {
    val l1 = maxOf(a.luminance(), b.luminance())
    val l2 = minOf(a.luminance(), b.luminance())
    return (l1 + 0.05f) / (l2 + 0.05f)
}
