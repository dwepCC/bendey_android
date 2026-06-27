package com.bendey.restaurant.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bendey.restaurant.core.designsystem.previews.BendeyPhonePreview
import com.bendey.restaurant.core.designsystem.previews.BendeyPreviewSurface
import com.bendey.restaurant.core.designsystem.previews.BendeyTabletPreview
import com.bendey.restaurant.core.designsystem.theme.BendeyCardDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import androidx.compose.ui.unit.dp

@Composable
fun BendeyCard(
    modifier: Modifier = Modifier,
    containerColor: Color = BendeyColors.Surface,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = BendeyCardDefaults.shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = BendeyCardDefaults.elevation(),
        border = BendeyCardDefaults.border,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content,
        )
    }
}

@BendeyPhonePreview
@Composable
private fun BendeyCardPhonePreview() {
    BendeyPreviewSurface {
        BendeyCard(contentPadding = PaddingValues(BendeySpacing.md)) {
            BendeySectionTitle(text = "Sección de ejemplo")
        }
    }
}

@BendeyTabletPreview
@Composable
private fun BendeyCardTabletPreview() {
    BendeyCardPhonePreview()
}
