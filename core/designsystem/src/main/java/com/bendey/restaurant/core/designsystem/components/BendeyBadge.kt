package com.bendey.restaurant.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.previews.BendeyPhonePreview
import com.bendey.restaurant.core.designsystem.previews.BendeyPreviewSurface
import com.bendey.restaurant.core.designsystem.previews.BendeyTabletPreview
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

enum class BendeyBadgeVariant {
    Inline,
    Filled,
}

@Composable
fun BendeyBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = BendeyColors.OnSurfaceVariant,
    containerColor: Color? = null,
    icon: ImageVector? = null,
    variant: BendeyBadgeVariant = BendeyBadgeVariant.Inline,
) {
    when (variant) {
        BendeyBadgeVariant.Inline -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
        }
        BendeyBadgeVariant.Filled -> {
            val bg = containerColor ?: color.copy(alpha = 0.12f)
            Text(
                text = text,
                modifier = modifier
                    .clip(BendeyShapeTokens.xs)
                    .background(bg)
                    .padding(horizontal = BendeySpacing.xxs, vertical = BendeySpacing.xxs),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@BendeyPhonePreview
@Composable
private fun BendeyBadgePhonePreview() {
    BendeyPreviewSurface {
        BendeyBadge(text = "+12% vs ayer", color = BendeyColors.Success)
    }
}

@BendeyTabletPreview
@Composable
private fun BendeyBadgeTabletPreview() {
    BendeyPreviewSurface {
        BendeyBadge(
            text = "Stock",
            color = BendeyColors.OnSurface,
            containerColor = BendeyColors.InfoContainer,
            variant = BendeyBadgeVariant.Filled,
        )
    }
}
