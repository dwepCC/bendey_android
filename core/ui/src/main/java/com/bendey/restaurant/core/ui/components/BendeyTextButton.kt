package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = BendeySpacing.touchTarget),
        contentPadding = contentPadding,
        colors = ButtonDefaults.textButtonColors(contentColor = textColor),
    ) {
        Text(
            text = text,
            style = textStyle,
            fontWeight = fontWeight,
            color = textColor,
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
