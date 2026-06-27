package com.bendey.restaurant.feature.auth.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.bendey.restaurant.core.designsystem.motion.BendeyExpressiveReveal
import com.bendey.restaurant.core.designsystem.theme.BendeyMotion
import com.bendey.restaurant.core.designsystem.theme.BendeyCardDefaults
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Wallet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyBrandLogo
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.model.PinStation
import com.bendey.restaurant.core.ui.components.BendeyLazyVerticalGrid
import com.bendey.restaurant.core.ui.layout.bendeySafeDrawingPadding

data class StationCard(
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

@Composable
fun HomeScreen(
    onPinStation: (PinStation) -> Unit,
    onAdminLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val tenant by viewModel.tenant.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BendeyColors.Background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BendeyColors.Rest900)
                .bendeySafeDrawingPadding()
                .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.sm),
        ) {
            BendeyExpressiveReveal(index = 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                ) {
                    BendeyBrandLogo(height = 40.dp, showBackground = true)
                    Column {
                        Text(
                            text = "Bendey Resto",
                            style = MaterialTheme.typography.labelSmall,
                            color = BendeyColors.PrimaryContainer.copy(alpha = 0.9f),
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
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.md),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.md),
        ) {
            BendeyExpressiveReveal(index = 1) {
                Surface(
                    shape = BendeyShapeTokens.xl,
                    color = BendeyColors.Surface,
                    border = BendeyCardDefaults.border,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(BendeySpacing.cardPadding)) {
                        Text(
                            text = "Tu restaurante en Bendey",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BendeyColors.Primary,
                        )
                        Text(
                            text = tenant?.name ?: "Restaurante",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        tenant?.ruc?.takeIf { it.isNotBlank() }?.let { ruc ->
                            Text(
                                text = "RUC $ruc",
                                style = MaterialTheme.typography.bodySmall,
                                color = BendeyColors.OnSurfaceVariant,
                                modifier = Modifier.padding(top = BendeySpacing.xxs),
                            )
                        }
                        Spacer(modifier = Modifier.height(BendeySpacing.xxs))
                        Text(
                            text = "Inicia sesión para operar mesas, pedidos y caja.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                }
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
                    Text(
                        text = "PIN",
                        modifier = Modifier
                            .clip(BendeyShapeTokens.md)
                            .background(BendeyColors.PrimaryContainer)
                            .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xxs),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BendeyColors.OnPrimaryContainer,
                    )
                }
            }
            BendeyLazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                state = rememberLazyGridState(),
                contentPadding = PaddingValues(bottom = BendeySpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
            ) {
                items(stations) { card ->
                    val cardIndex = stations.indexOf(card) + 4
                    BendeyExpressiveReveal(index = cardIndex) {
                        StationAccessCard(card = card, onClick = { onPinStation(card.station) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSessionCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BendeyShapeTokens.xl)
            .background(
                Brush.linearGradient(
                    colors = listOf(BendeyColors.Primary, BendeyColors.Rest800),
                ),
            )
            .clickable(onClick = onClick)
            .padding(BendeySpacing.cardPadding),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(BendeyShapeTokens.md)
                    .background(BendeyColors.OnPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = BendeyColors.OnPrimary)
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
                    color = BendeyColors.OnPrimary.copy(alpha = 0.85f),
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = BendeyColors.OnPrimary.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun StationAccessCard(card: StationCard, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val elevation by animateFloatAsState(
        targetValue = if (pressed) 3f else 1f,
        animationSpec = BendeyMotion.ExpressiveEffectsTween,
        label = "station_card_elevation",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.99f else 1f,
        animationSpec = BendeyMotion.ExpressiveSpatialSpring,
        label = "station_card_scale",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = BendeyShapeTokens.xl,
        color = BendeyColors.Surface,
        border = BendeyCardDefaults.border,
        shadowElevation = elevation.dp,
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(BendeySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(BendeyShapeTokens.md)
                    .background(BendeyColors.PrimaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(card.icon, contentDescription = null, tint = BendeyColors.Primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(card.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    card.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = BendeyColors.NavInactive,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
