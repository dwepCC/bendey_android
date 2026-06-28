package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.catalog.resolvePublicAssetUrl
import java.text.NumberFormat

/** Card catálogo POS — tap en todo el card agrega al carrito. */
@Composable
fun BendeyPosProductCard(
    name: String,
    price: Double,
    currency: NumberFormat,
    imageUrl: String?,
    assetsBaseUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val resolvedUrl = resolvePublicAssetUrl(assetsBaseUrl, imageUrl).takeIf { it.isNotBlank() }
    val alpha = if (enabled) 1f else 0.5f
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .alpha(alpha)
            .border(1.dp, BendeyColors.Outline.copy(alpha = 0.55f), BendeyShapeTokens.lg)
            .clickable(enabled = enabled, onClick = onClick),
        shape = BendeyShapeTokens.lg,
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 72.dp)
                    .background(BendeyColors.SurfaceVariant),
            ) {
                if (resolvedUrl != null) {
                    AsyncImage(
                        model = resolvedUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = BendeyColors.OnSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    androidx.compose.ui.graphics.Color.Transparent,
                                    BendeyColors.OnSurface.copy(alpha = 0.18f),
                                ),
                            ),
                        ),
                )
                Text(
                    text = currency.format(price),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = BendeySpacing.xxs)
                        .clip(BendeyShapeTokens.sm)
                        .background(BendeyColors.Primary)
                        .padding(horizontal = BendeySpacing.sm, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.OnPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = name,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 40.dp)
                    .padding(
                        horizontal = BendeySpacing.xs,
                        vertical = BendeySpacing.xxs,
                    ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.labelSmall.lineHeight,
                color = BendeyColors.OnSurface,
            )
        }
    }
}
