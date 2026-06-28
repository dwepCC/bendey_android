package com.bendey.restaurant.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.layout.BendeyBottomBarHeight
import com.bendey.restaurant.core.ui.layout.BendeyBottomBarInset
import com.bendey.restaurant.core.ui.layout.BendeyNavigationBarScrim

data class BendeyNavItem(
    val route: String,
    val label: String,
    val shortLabel: String,
    val icon: ImageVector,
)

/**
 * Barra inferior móvil: Inicio · Mesas · POS (FAB +) · Comandas · Más.
 * El tomate solo pinta el inset del sistema; la barra blanca queda pegada al contenido.
 */
@Composable
fun BendeyBottomNavigationBar(
    currentRoute: String?,
    leftItems: List<BendeyNavItem>,
    centerItem: BendeyNavItem?,
    rightItems: List<BendeyNavItem>,
    onNavigate: (BendeyNavItem) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCenterFab: Boolean = centerItem != null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BendeyBottomBarInset),
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(BendeyBottomBarHeight),
                color = BendeyColors.Surface,
                shadowElevation = 1.dp,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BendeyBottomBarHeight)
                        .padding(horizontal = BendeySpacing.xxs),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leftItems.forEach { item ->
                        BottomNavTab(
                            selected = currentRoute == item.route,
                            icon = item.icon,
                            label = item.shortLabel,
                            onClick = { onNavigate(item) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (showCenterFab && centerItem != null) {
                        Box(modifier = Modifier.weight(1f))
                    }
                    rightItems.forEach { item ->
                        BottomNavTab(
                            selected = currentRoute == item.route,
                            icon = item.icon,
                            label = item.shortLabel,
                            onClick = { onNavigate(item) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    BottomNavTab(
                        selected = false,
                        icon = Icons.Default.Menu,
                        label = "Más",
                        onClick = onMoreClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (showCenterFab && centerItem != null) {
                CenterPosFab(
                    selected = currentRoute == centerItem.route,
                    label = centerItem.shortLabel,
                    onClick = { onNavigate(centerItem) },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
        BendeyNavigationBarScrim()
    }
}

@Composable
private fun BottomNavTab(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconTint by animateColorAsState(
        targetValue = if (selected) BendeyColors.Primary else BendeyColors.NavInactive,
        label = "navIcon",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) BendeyColors.Primary else BendeyColors.NavInactive,
        label = "navText",
    )
    Column(
        modifier = modifier
            .clip(BendeyShapeTokens.xs)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CenterPosFab(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(if (selected) BendeyColors.Rest800 else BendeyColors.Primary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = label,
                tint = BendeyColors.OnPrimary,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) BendeyColors.Primary else BendeyColors.NavInactive,
            modifier = Modifier.offset(y = BendeySpacing.xxs),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Ícono por defecto para Inicio cuando el destino usa Dashboard. */
val BendeyNavHomeIcon: ImageVector = Icons.Default.Home
