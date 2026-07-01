package com.bendey.restaurant.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile

/**
 * Header operativo tablet (Medium / Expanded).
 *
 * Evolución del [BendeyAppHeader] móvil: misma barra oscura Rest900, jerarquía por tipografía
 * y espaciado — sin SegmentedButton flotante.
 *
 * Móvil sigue usando [BendeyAppHeader] + [BendeyBottomNavigationBar] sin cambios.
 */
@Composable
fun BendeyOperationalTopBar(
    state: BendeyAppHeaderState,
    currentRoute: String?,
    operationalDestinations: List<BendeyOperationalNavItem>,
    onOperationalNavigate: (BendeyOperationalNavItem) -> Unit,
    modifier: Modifier = Modifier,
    profile: BendeyAdaptiveProfile = rememberBendeyAdaptiveProfile(),
    isDrawerOpen: Boolean = false,
    onMenuClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val barHeight = OperationalTopBarTokens.barHeight(profile)
    val horizontalPadding = OperationalTopBarTokens.horizontalPadding(profile)
    val groupGap = OperationalTopBarTokens.groupGap(profile)
    val isPortrait = profile == BendeyAdaptiveProfile.MediumPortrait
    val selectedRoute = remember(currentRoute, operationalDestinations) {
        operationalDestinations.firstOrNull { it.route == currentRoute }?.route
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BendeyColors.Rest900,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = barHeight)
                .padding(horizontal = horizontalPadding, vertical = BendeySpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BendeyOperationalBrandBlock(
                state = state,
                profile = profile,
                isDrawerOpen = isDrawerOpen,
                onMenuClick = onMenuClick,
            )

            if (!isPortrait) {
                Spacer(modifier = Modifier.size(groupGap))
            }

            BendeyOperationalNavTabs(
                destinations = operationalDestinations,
                selectedRoute = selectedRoute,
                profile = profile,
                onNavigate = onOperationalNavigate,
                modifier = if (isPortrait) {
                    Modifier
                        .weight(1f)
                        .padding(horizontal = BendeySpacing.xxs)
                } else {
                    Modifier.weight(1f, fill = true)
                },
            )

            if (!isPortrait) {
                Spacer(modifier = Modifier.size(groupGap))
            }

            BendeyHeaderActions(
                state = state,
                showSyncIndicator = true,
                compactOnlineIndicator = true,
                onNotificationsClick = onNotificationsClick,
                onLogout = onLogout,
            )
        }
    }
}

/** Nivel 1 — menú + logo + nombre forman identidad unificada. */
@Composable
private fun BendeyOperationalBrandBlock(
    state: BendeyAppHeaderState,
    profile: BendeyAdaptiveProfile,
    isDrawerOpen: Boolean,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val logoSize = OperationalTopBarTokens.brandLogoSize(profile)
    val maxWidth = OperationalTopBarTokens.brandMaxWidth(profile)

    Row(
        modifier = modifier.widthIn(max = maxWidth),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
    ) {
        IconButton(onClick = onMenuClick, modifier = Modifier.size(BendeySpacing.touchTarget)) {
            Icon(
                if (isDrawerOpen) Icons.Default.Close else Icons.Default.Menu,
                contentDescription = if (isDrawerOpen) "Cerrar menú" else "Menú",
                tint = BendeyColors.OnPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OperationalTopBarTokens.brandLogoTextGap),
        ) {
            Box(
                modifier = Modifier
                    .size(logoSize)
                    .clip(BendeyShapeTokens.md)
                    .background(BendeyColors.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "B",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.OnPrimary,
                )
            }
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    text = state.restaurantName.ifBlank { "Bendey Resto" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.OnPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AnimatedContent(
                    targetState = state.branchName,
                    transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                    label = "branch_name",
                ) { branchName ->
                    if (branchName.isNotBlank()) {
                        Text(
                            text = branchName,
                            style = MaterialTheme.typography.labelSmall,
                            color = BendeyColors.OnPrimary.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Nivel 2 — navegación operativa integrada en la barra (sin contenedor blanco).
 * Mismo lenguaje que ítems seleccionados del bottom nav móvil: Primary sobre Rest900.
 */
@Composable
private fun BendeyOperationalNavTabs(
    destinations: List<BendeyOperationalNavItem>,
    selectedRoute: String?,
    profile: BendeyAdaptiveProfile,
    onNavigate: (BendeyOperationalNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (destinations.isEmpty()) return

    val showLabels = OperationalTopBarTokens.navShowLabels(profile)
    val itemHeight = OperationalTopBarTokens.navItemHeight(profile)
    val itemPadH = OperationalTopBarTokens.navItemHorizontalPadding(profile)
    val gap = OperationalTopBarTokens.navGap(profile)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (profile == BendeyAdaptiveProfile.MediumPortrait) {
            Arrangement.SpaceEvenly
        } else {
            Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
        },
    ) {
        destinations.forEach { destination ->
            val selected = destination.route == selectedRoute
            val containerColor by animateColorAsState(
                targetValue = if (selected) BendeyColors.Primary else BendeyColors.Rest900,
                animationSpec = tween(180),
                label = "nav_tab_bg",
            )
            val contentColor by animateColorAsState(
                targetValue = if (selected) {
                    BendeyColors.OnPrimary
                } else {
                    BendeyColors.OnPrimary.copy(alpha = 0.78f)
                },
                animationSpec = tween(180),
                label = "nav_tab_fg",
            )

            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = itemHeight)
                    .clip(BendeyShapeTokens.sm)
                    .background(containerColor)
                    .semantics { role = Role.Tab }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onNavigate(destination) },
                    )
                    .padding(horizontal = itemPadH, vertical = BendeySpacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
            ) {
                Icon(
                    destination.icon,
                    contentDescription = destination.label,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
                if (showLabels) {
                    Text(
                        text = destination.shortLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
