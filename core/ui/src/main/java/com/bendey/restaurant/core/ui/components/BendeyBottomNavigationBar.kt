package com.bendey.restaurant.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class BendeyNavItem(
    val route: String,
    val label: String,
    val shortLabel: String,
    val icon: ImageVector,
)

/**
 * Barra inferior móvil: Inicio · POS · Mesas (FAB) · Comandas · Más.
 * Alineada con el mockup del dashboard Bendey Resto.
 */
@Composable
fun BendeyBottomNavigationBar(
    currentRoute: String?,
    leftItems: List<BendeyNavItem>,
    centerItem: BendeyNavItem,
    rightItems: List<BendeyNavItem>,
    onNavigate: (BendeyNavItem) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BendeyColors.Surface,
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
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
            CenterMesasFab(
                selected = currentRoute == centerItem.route,
                label = centerItem.shortLabel,
                onClick = { onNavigate(centerItem) },
                modifier = Modifier.weight(1f),
            )
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
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(top = 6.dp, bottom = 4.dp),
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
        )
    }
}

@Composable
private fun CenterMesasFab(
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
                .offset(y = (-10).dp)
                .size(52.dp)
                .shadow(6.dp, CircleShape)
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
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) BendeyColors.Primary else BendeyColors.NavInactive,
            modifier = Modifier.offset(y = (-6).dp),
        )
    }
}

/** Ícono por defecto para Inicio cuando el destino usa Dashboard. */
val BendeyNavHomeIcon: ImageVector = Icons.Default.Home
