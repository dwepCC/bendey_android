package com.bendey.restaurant.feature.auth.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.bendey.restaurant.core.designsystem.theme.BendeyCardDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/** Card principal de flujo auth — jerarquía Expressive con superficie elevada opcional. */
@Composable
fun AuthExpressiveCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    elevated: Boolean = true,
    tonal: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BendeyShapeTokens.xl,
        color = when {
            tonal -> BendeyColors.PrimaryContainer.copy(alpha = 0.28f)
            else -> BendeyColors.Surface
        },
        shadowElevation = if (elevated) AuthLayoutTokens.cardElevationRaised else AuthLayoutTokens.cardElevationRest,
        tonalElevation = if (elevated) AuthLayoutTokens.cardElevationRest else AuthLayoutTokens.cardElevationRest,
        border = BendeyCardDefaults.border,
    ) {
        Column(modifier = Modifier.padding(BendeySpacing.lg)) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurface,
                )
                subtitle?.let { sub ->
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                        modifier = Modifier.padding(top = BendeySpacing.xxs),
                    )
                }
                Spacer(modifier = Modifier.height(BendeySpacing.md))
            }
            content()
        }
    }
}
