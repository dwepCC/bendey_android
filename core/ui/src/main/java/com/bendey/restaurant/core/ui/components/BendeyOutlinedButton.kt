package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.bendey.restaurant.core.designsystem.previews.BendeyPhonePreview
import com.bendey.restaurant.core.designsystem.previews.BendeyPreviewSurface
import com.bendey.restaurant.core.designsystem.previews.BendeyTabletPreview
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

@Composable
fun BendeyOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = BendeySpacing.buttonHeight),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BendeyColors.Primary,
            disabledContentColor = BendeyColors.OnSurfaceVariant,
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = enabled),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@BendeyPhonePreview
@Composable
private fun BendeyOutlinedButtonPhonePreview() {
    BendeyPreviewSurface {
        BendeyOutlinedButton(text = "Acción secundaria", onClick = {}, fillWidth = true)
    }
}

@BendeyTabletPreview
@Composable
private fun BendeyOutlinedButtonTabletPreview() {
    BendeyPreviewSurface {
        BendeyOutlinedButton(text = "Exportar", onClick = {})
    }
}
