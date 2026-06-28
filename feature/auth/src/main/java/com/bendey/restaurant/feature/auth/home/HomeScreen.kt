package com.bendey.restaurant.feature.auth.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyBrandLogo
import com.bendey.restaurant.core.designsystem.motion.BendeyExpressiveReveal
import com.bendey.restaurant.core.designsystem.theme.BendeyCardDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyMotion
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.model.PinStation
import com.bendey.restaurant.core.ui.components.BendeyLazyVerticalGrid
import com.bendey.restaurant.core.ui.layout.bendeySafeDrawingPadding
import com.bendey.restaurant.core.ui.layout.rememberIsExpandedWidth
import com.bendey.restaurant.feature.auth.components.AuthLayoutTokens

private data class StationCard(
    val station: PinStation,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

private val stations = listOf(
    StationCard(PinStation.WAITER, "Mozo", "Mesas y pedidos", Icons.Default.Person),
    StationCard(PinStation.CASHIER, "Cajero", "POS y cobros", Icons.Default.Wallet),
    StationCard(PinStation.KITCHEN, "Cocina", "Comandas en vivo", Icons.Default.Restaurant),
    StationCard(PinStation.DELIVERY, "Delivery", "Repartos y rutas", Icons.Default.DeliveryDining),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPinStation: (PinStation) -> Unit,
    onAdminLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val tenant by viewModel.tenant.collectAsStateWithLifecycle()
    val isExpanded = rememberIsExpandedWidth()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BendeyColors.Background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BendeyColors.Rest900, BendeyColors.Rest800),
                    ),
                )
                .bendeySafeDrawingPadding()
                .padding(
                    horizontal = BendeySpacing.md,
                    vertical = BendeySpacing.sm,
                ),
        ) {
            BendeyExpressiveReveal(index = 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                ) {
                    BendeyBrandLogo(
                        height = AuthLayoutTokens.logoHeightHomeHeaderCompact,
                        showBackground = true,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bendey Resto",
                            style = MaterialTheme.typography.labelMedium,
                            color = BendeyColors.PrimaryContainer.copy(alpha = 0.95f),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Operación en sala",
                            style = MaterialTheme.typography.titleMedium,
                            color = BendeyColors.OnPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = if (isExpanded) AuthLayoutTokens.homeContentMaxWidth else Dp.Unspecified)
                    .fillMaxWidth()
                    .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.md),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.md),
            ) {
                BendeyExpressiveReveal(index = 1) {
                    RestaurantIdentityRow(
                        name = tenant?.name ?: "Restaurante",
                        ruc = tenant?.ruc,
                    )
                }
                BendeyExpressiveReveal(index = 2) {
                    AdminSessionCard(onClick = onAdminLogin)
                }
                BendeyExpressiveReveal(index = 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Acceso por estación",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(
                            shape = BendeyShapeTokens.pill,
                            color = BendeyColors.PrimaryContainer,
                        ) {
                            Text(
                                text = "PIN",
                                modifier = Modifier.padding(
                                    horizontal = BendeySpacing.sm,
                                    vertical = BendeySpacing.xxs,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BendeyColors.OnPrimaryContainer,
                            )
                        }
                    }
                }
                BendeyLazyVerticalGrid(
                    columns = if (isExpanded) {
                        GridCells.Adaptive(minSize = AuthLayoutTokens.stationGridMinCell)
                    } else {
                        GridCells.Fixed(2)
                    },
                    modifier = Modifier.weight(1f),
                    state = rememberLazyGridState(),
                    contentPadding = PaddingValues(bottom = BendeySpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                ) {
                    itemsIndexed(
                        items = stations,
                        key = { _, card -> card.station.routeKey },
                    ) { index, card ->
                        BendeyExpressiveReveal(index = index + 4) {
                            StationAccessCard(
                                card = card,
                                onClick = { onPinStation(card.station) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RestaurantIdentityRow(
    name: String,
    ruc: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BendeySpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Storefront,
            contentDescription = null,
            tint = BendeyColors.Primary,
            modifier = Modifier.size(AuthLayoutTokens.iconNav),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = BendeyColors.OnSurface,
            )
            ruc?.takeIf { it.isNotBlank() }?.let { value ->
                Text(
                    text = "RUC $value",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminSessionCard(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = BendeyMotion.ExpressiveSpatialSpring,
        label = "admin_card_scale",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .semantics { contentDescription = "Sesión administrativa" },
        shape = BendeyShapeTokens.xl,
        shadowElevation = if (pressed) {
            AuthLayoutTokens.cardElevationPressed
        } else {
            AuthLayoutTokens.cardElevationProminent
        },
        onClick = onClick,
        interactionSource = interactionSource,
        color = BendeyColors.Primary,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(BendeyColors.Primary, BendeyColors.Rest800),
                    ),
                )
                .padding(BendeySpacing.lg),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(AuthLayoutTokens.iconContainerProminent)
                        .clip(BendeyShapeTokens.lg)
                        .background(BendeyColors.OnPrimary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = BendeyColors.OnPrimary,
                        modifier = Modifier.size(AuthLayoutTokens.iconProminent),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sesión administrativa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BendeyColors.OnPrimary,
                    )
                    Text(
                        text = "Email y contraseña · configuración",
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnPrimary.copy(alpha = 0.88f),
                        modifier = Modifier.padding(top = BendeySpacing.xxs),
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = BendeyColors.OnPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.size(AuthLayoutTokens.iconNav),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationAccessCard(
    card: StationCard,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val elevation by animateFloatAsState(
        targetValue = if (pressed) 4f else 1f,
        animationSpec = BendeyMotion.ExpressiveEffectsTween,
        label = "station_card_elevation",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = BendeyMotion.ExpressiveSpatialSpring,
        label = "station_card_scale",
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .semantics { contentDescription = "${card.title}. ${card.subtitle}" },
        shape = BendeyShapeTokens.xl,
        colors = BendeyCardDefaults.colors(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
        border = BendeyCardDefaults.border,
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BendeySpacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(AuthLayoutTokens.iconContainerStandard)
                        .clip(BendeyShapeTokens.lg)
                        .background(BendeyColors.PrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        card.icon,
                        contentDescription = null,
                        tint = BendeyColors.Primary,
                        modifier = Modifier.size(AuthLayoutTokens.iconStandard),
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = BendeyColors.NavInactive,
                    modifier = Modifier.size(BendeySpacing.md),
                )
            }
            Spacer(modifier = Modifier.height(BendeySpacing.sm))
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = card.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.padding(top = BendeySpacing.xxs),
            )
        }
    }
}
