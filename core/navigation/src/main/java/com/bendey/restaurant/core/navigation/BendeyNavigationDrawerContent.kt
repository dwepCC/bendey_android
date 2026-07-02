package com.bendey.restaurant.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyAppHeaderState
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn
import com.bendey.restaurant.core.ui.layout.adaptive.AdaptiveSpacing
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile

@Composable
fun BendeyNavigationDrawerContent(
    currentRoute: String?,
    appVersion: String,
    headerState: BendeyAppHeaderState,
    destinations: List<BendeyDrawerDestination> = BendeyDrawerDestination.entries,
    onNavigate: (BendeyDrawerDestination) -> Unit,
    onClose: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = rememberBendeyAdaptiveProfile()
    val sectionGap = AdaptiveSpacing.drawerSectionGap(profile)

    ModalDrawerSheet(
        modifier = modifier.widthIn(min = 300.dp, max = 360.dp),
        drawerContainerColor = BendeyColors.Surface,
        drawerContentColor = BendeyColors.OnSurface,
    ) {
        BendeyVerticalScrollColumn(
            modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.md),
        ) {
            BendeyDrawerHeader(
                headerState = headerState,
                appVersion = appVersion,
                onClose = onClose,
            )
            Spacer(modifier = Modifier.height(sectionGap))

            BendeyDrawerDestination.groupedOrder.forEach { group ->
                val groupItems = destinations.filter { it.group == group }
                if (groupItems.isEmpty()) return@forEach

                Text(
                    text = group.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = BendeySpacing.xs, vertical = BendeySpacing.xxs),
                )
                groupItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        selected = selected,
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        onClick = { onNavigate(item) },
                        modifier = Modifier.padding(horizontal = BendeySpacing.xxs, vertical = 2.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = BendeyColors.PrimaryContainer.copy(alpha = 0.55f),
                            unselectedContainerColor = BendeyColors.Surface,
                            selectedIconColor = BendeyColors.Primary,
                            selectedTextColor = BendeyColors.Primary,
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(BendeySpacing.sm))
            }

            HorizontalDivider(color = BendeyColors.Outline.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(BendeySpacing.sm))

            BendeyDrawerSystemSection(onLogout = onLogout)
        }
    }
}

@Composable
private fun BendeyDrawerHeader(
    headerState: BendeyAppHeaderState,
    appVersion: String,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(BendeyShapeTokens.md)
                    .background(BendeyColors.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "B",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.OnPrimary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headerState.restaurantName.ifBlank { "Bendey Resto" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (headerState.branchName.isNotBlank()) {
                    Text(
                        text = headerState.branchName,
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (appVersion.isNotBlank()) {
                    Text(
                        text = "v$appVersion",
                        style = MaterialTheme.typography.labelMedium,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
            }
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar menú", tint = BendeyColors.OnSurfaceVariant)
        }
    }
}

@Composable
private fun BendeyDrawerSystemSection(
    onLogout: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BendeyShapeTokens.md)
            .clickable(onClick = onLogout),
        shape = BendeyShapeTokens.md,
        color = BendeyColors.ErrorContainer.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = BendeyColors.Error,
                modifier = Modifier
                    .padding(end = BendeySpacing.xs)
                    .size(18.dp),
            )
            Text(
                text = "Cerrar sesión",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = BendeyColors.OnErrorContainer,
            )
        }
    }
}
