package com.bendey.restaurant.feature.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.bendey.restaurant.core.designsystem.components.BendeyBrandLogo
import com.bendey.restaurant.core.designsystem.motion.BendeyExpressiveReveal
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn
import com.bendey.restaurant.core.ui.layout.bendeySafeDrawingPadding
import com.bendey.restaurant.core.ui.layout.rememberIsExpandedWidth

@Composable
fun AuthWelcomeLayout(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String = "Bendey Resto, sistema de gestión para restaurantes",
    description: String = "Administra tu restaurante desde cualquier lugar. Ventas, mesas, cocina, caja e inventario en una sola aplicación.",
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isExpanded = rememberIsExpandedWidth()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BendeyColors.Background)
            .bendeySafeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val columnModifier = Modifier
            .widthIn(
                max = if (isExpanded) {
                    AuthLayoutTokens.contentMaxWidthExpanded
                } else {
                    AuthLayoutTokens.contentMaxWidthCompact
                },
            )
            .fillMaxWidth()
            .padding(
                horizontal = BendeySpacing.lg,
                vertical = if (isExpanded) BendeySpacing.lg else BendeySpacing.md,
            )

        val welcomeBody: @Composable ColumnScope.() -> Unit = {
            BendeyExpressiveReveal(index = 0) {
                BendeyBrandLogo(height = AuthLayoutTokens.logoHeightWelcome, showBackground = true)
            }
            Spacer(modifier = Modifier.height(BendeySpacing.md))
            BendeyExpressiveReveal(index = 1) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = BendeyColors.OnSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(BendeySpacing.xs))
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BendeyColors.OnSurface,
                        textAlign = TextAlign.Center,
                    )
                    if (description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(BendeySpacing.xs))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BendeyColors.OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(BendeySpacing.lg))
            BendeyExpressiveReveal(index = 2) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content,
                )
            }
        }

        if (scrollable) {
            BendeyVerticalScrollColumn(modifier = columnModifier) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = welcomeBody,
                )
            }
        } else {
            BendeyVerticalScrollColumn(
                modifier = columnModifier,
                showScrollHints = false,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = welcomeBody,
                )
            }
        }
    }
}
