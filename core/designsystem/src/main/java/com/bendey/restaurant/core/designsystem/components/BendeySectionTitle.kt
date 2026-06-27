package com.bendey.restaurant.core.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.bendey.restaurant.core.designsystem.previews.BendeyPhonePreview
import com.bendey.restaurant.core.designsystem.previews.BendeyPreviewSurface
import com.bendey.restaurant.core.designsystem.previews.BendeyTabletPreview
import com.bendey.restaurant.core.designsystem.theme.BendeyColors

@Composable
fun BendeySectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    fontWeight: FontWeight = FontWeight.SemiBold,
    color: Color = BendeyColors.OnSurface,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        fontWeight = fontWeight,
        color = color,
    )
}

@BendeyPhonePreview
@Composable
private fun BendeySectionTitlePhonePreview() {
    BendeyPreviewSurface {
        BendeySectionTitle(text = "Presentaciones / variantes")
    }
}

@BendeyTabletPreview
@Composable
private fun BendeySectionTitleTabletPreview() {
    BendeyPreviewSurface {
        BendeySectionTitle(
            text = "Pedidos recientes",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
