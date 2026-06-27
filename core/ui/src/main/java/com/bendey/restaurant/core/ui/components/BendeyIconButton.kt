package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.bendey.restaurant.core.designsystem.previews.BendeyPhonePreview
import com.bendey.restaurant.core.designsystem.previews.BendeyPreviewSurface
import com.bendey.restaurant.core.designsystem.previews.BendeyTabletPreview
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit

@Composable
fun BendeyIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.sizeIn(
            minWidth = BendeySpacing.touchTarget,
            minHeight = BendeySpacing.touchTarget,
        ),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = BendeyColors.OnSurface,
            disabledContentColor = BendeyColors.OnSurfaceVariant,
        ),
    ) {
        content()
    }
}

@Composable
fun BendeyIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current,
) {
    BendeyIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}

@BendeyPhonePreview
@Composable
private fun BendeyIconButtonPhonePreview() {
    BendeyPreviewSurface {
        BendeyIconButton(
            onClick = {},
            icon = Icons.Default.Edit,
            contentDescription = "Editar",
        )
    }
}

@BendeyTabletPreview
@Composable
private fun BendeyIconButtonTabletPreview() {
    BendeyPreviewSurface {
        BendeyIconButton(
            onClick = {},
            icon = Icons.Default.Edit,
            contentDescription = "Editar",
            tint = BendeyColors.Error,
        )
    }
}
