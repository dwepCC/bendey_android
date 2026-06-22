package com.bendey.restaurant.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.layout.BendeyBottomBarInset

data class BendeyNavItem(
    val route: String,
    val label: String,
    val shortLabel: String,
    val icon: ImageVector,
)

/**
 * Barra inferior móvil: Inicio · Mesas · POS (FAB +) · Comandas · Más.
 * El botón central flotante abre el POS.
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BendeyBottomBarInset)
            .navigationBarsPadding(),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = BendeyColors.Outline.copy(alpha = 0.65f),
                    shape = BendeyShapeTokens.sheet,
                ),
            color = BendeyColors.Surface,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
            .padding(vertical = BendeySpacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
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
                .shadow(4.dp, CircleShape)
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
        )
    }
}

/** Ícono por defecto para Inicio cuando el destino usa Dashboard. */
val BendeyNavHomeIcon: ImageVector = Icons.Default.Home
