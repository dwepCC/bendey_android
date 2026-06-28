package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import kotlinx.coroutines.delay

private const val AutoDismissMs = 5_000L

/**
 * Banner flotante para errores o avisos breves sobre barras compactas POS/Mesa.
 */
@Composable
fun BendeyOverlayBanner(
    message: String?,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    autoDismiss: Boolean = onDismiss != null,
) {
    if (message.isNullOrBlank()) return

    if (autoDismiss && onDismiss != null) {
        LaunchedEffect(message) {
            delay(AutoDismissMs)
            onDismiss()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BendeyColors.ErrorContainer,
        shape = BendeyShapeTokens.md,
        shadowElevation = 4.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = BendeySpacing.md,
                    end = BendeySpacing.xxs,
                    top = BendeySpacing.xs,
                    bottom = BendeySpacing.xs,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
        ) {
            Text(
                text = message,
                color = BendeyColors.Error,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (onDismiss != null) {
                BendeyIconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(BendeySpacing.touchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = BendeyColors.Error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
