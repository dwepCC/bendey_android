package com.bendey.restaurant.feature.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.components.BendeyBrandLogo
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.layout.bendeySafeDrawingPadding

@Composable
fun AuthWelcomeLayout(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String = "Bendey Resto, sistema de gestión para restaurantes",
    description: String = "Administra tu restaurante desde cualquier lugar. Ventas, mesas, cocina, caja e inventario en una sola aplicación.",
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BendeyColors.Background)
            .bendeySafeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = BendeySpacing.lg, vertical = BendeySpacing.xl)
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BendeyBrandLogo(height = 64.dp, showBackground = true)
            Spacer(modifier = Modifier.height(BendeySpacing.lg))
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
                color = BendeyColors.OnSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(BendeySpacing.xs))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = BendeyColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(BendeySpacing.xl))
            content()
        }
    }
}
