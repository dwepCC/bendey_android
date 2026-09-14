package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.previews.BendeyPhonePreview
import com.bendey.restaurant.core.designsystem.previews.BendeyPreviewSurface
import com.bendey.restaurant.core.designsystem.previews.BendeyTabletPreview
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

@Composable
fun BendeyTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = BendeyColors.Primary,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    fontWeight: FontWeight = FontWeight.SemiBold,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    // Opcional — cubre el patrón "+ Icono + texto" que varias pantallas reimplementaban a mano
    // con un TextButton crudo de Material3 (ej. "Agregar producto", "Agregar slot").
    icon: ImageVector? = null,
) {
    // Altura unificada a BendeySpacing.buttonHeight (44dp) — antes usaba touchTarget (48dp), el
    // único de los 6 botones de "acción de diálogo" con una altura distinta a sus hermanos
    // (Primary/Secondary/Outlined/FilledTonal), lo que desalineaba visualmente cualquier fila
    // Cancelar/Guardar que los combinara.
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = BendeySpacing.buttonHeight),
        contentPadding = contentPadding,
        colors = BendeyButtonDefaults.outlinedColors(contentColor = textColor),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(BendeySpacing.xxs))
        }
        Text(
            text = text,
            style = textStyle,
            fontWeight = fontWeight,
            // Sin `color=` fijo: hereda LocalContentColor, que TextButton ya deriva de `colors`
            // según `enabled` — antes el color venía hardcodeado acá y el estado disabled de
            // `colors=` nunca llegaba a pintarse.
        )
    }
}

@BendeyPhonePreview
@Composable
private fun BendeyTextButtonPhonePreview() {
    BendeyPreviewSurface {
        BendeyTextButton(text = "Ver mapa", onClick = {}, textColor = BendeyColors.Info)
    }
}

@BendeyTabletPreview
@Composable
private fun BendeyTextButtonTabletPreview() {
    BendeyPreviewSurface {
        BendeyTextButton(text = "Cancelar", onClick = {})
    }
}
