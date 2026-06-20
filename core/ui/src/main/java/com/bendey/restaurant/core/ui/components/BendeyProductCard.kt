package com.bendey.restaurant.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.catalog.resolvePublicAssetUrl
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import java.text.NumberFormat

fun PosProduct.productBadges(): List<String> = buildList {
    if (!availableForSale) add("No disp.")
    if (hasModifiers) add("Mods")
    if (hasVariants) add("Var.")
    if (manageStock) add("Stock")
}

@Composable
fun BendeyProductCard(
    name: String,
    price: Double,
    currency: NumberFormat,
    imageUrl: String?,
    assetsBaseUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    stockHint: String? = null,
    lowStock: Boolean = false,
    badges: List<String> = emptyList(),
    compact: Boolean = true,
    enabled: Boolean = true,
) {
    val resolvedUrl = resolvePublicAssetUrl(assetsBaseUrl, imageUrl).takeIf { it.isNotBlank() }
    val cardAlpha = if (enabled) 1f else 0.55f
    val cardHeight = when {
        !compact -> 196.dp
        else -> 128.dp
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .alpha(cardAlpha)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(if (compact) 10.dp else 12.dp),
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(BendeyColors.AccentTealContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (resolvedUrl != null) {
                    AsyncImage(
                        model = resolvedUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = BendeyColors.AccentTeal.copy(alpha = 0.45f),
                        modifier = Modifier.padding(if (compact) 16.dp else 20.dp),
                    )
                }
                if (badges.isNotEmpty() || stockHint != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        badges.take(2).forEach { badge ->
                            ProductBadge(text = badge, warning = badge == "No disp.")
                        }
                        stockHint?.let {
                            ProductBadge(
                                text = it,
                                warning = lowStock,
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BendeyColors.PrimaryContainer.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = if (compact) 5.dp else 7.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = name,
                    style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currency.format(price),
                    style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.Primary,
                )
            }
        }
    }
}

@Composable
private fun ProductBadge(text: String, warning: Boolean) {
    val bg by animateColorAsState(
        targetValue = when {
            warning -> BendeyColors.WarningContainer
            text == "Stock" -> BendeyColors.InfoContainer
            else -> BendeyColors.SuccessContainer
        },
        label = "badgeBg",
    )
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = if (warning) BendeyColors.OnWarning else BendeyColors.OnSurface,
    )
}
