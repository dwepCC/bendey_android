package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.previews.BendeyPhonePreview
import com.bendey.restaurant.core.designsystem.previews.BendeyPreviewSurface
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/**
 * Botón para acciones destructivas o irreversibles: eliminar, anular, borrar, revocar, cancelar
 * una operación que ya no se puede deshacer.
 *
 * La auditoría de 2026 encontró CINCO tratamientos visuales distintos para "Eliminar/Anular" en
 * la app — desde color primario sin rojo (Mesas, Compras) hasta ningún color de advertencia en
 * absoluto (Caja: "Eliminar" método de pago se veía idéntico a "Editar"). No es solo estética:
 * un botón destructivo indistinguible de uno inocuo invita al error operativo. Este es el único
 * botón "rojo" oficial — mismo molde que [BendeyPrimaryButton] (altura, radio, tipografía,
 * elevación, disabled), solo cambia el color semántico a [BendeyColors.Error].
 *
 * Esto es SOLO la variante de botón completo. Para una fila de lista densa (ej. "Eliminar" como
 * icono junto a cada producto) usar [BendeyCompactIconButton] con `tint = BendeyColors.Error` —
 * la semántica de color es la misma, la representación se adapta al contexto.
 */
@Composable
fun BendeyDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    fillWidth: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = BendeySpacing.buttonHeight),
        shape = MaterialTheme.shapes.medium,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 1.dp,
        ),
        colors = BendeyButtonDefaults.filledColors(containerColor = BendeyColors.Error),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = BendeyColors.OnPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@BendeyPhonePreview
@Composable
private fun BendeyDestructiveButtonPreview() {
    BendeyPreviewSurface {
        BendeyDestructiveButton(text = "Eliminar", onClick = {})
    }
}
