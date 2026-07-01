package com.bendey.restaurant.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyExpressiveScope
import com.bendey.restaurant.core.designsystem.theme.BendeyMotion
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/**
 * Acciones compartidas del header Bendey (sync, notificaciones, usuario).
 * Reutilizado por [BendeyAppHeader] (móvil) y [BendeyOperationalTopBar] (tablet).
 */
@Composable
fun BendeyHeaderActions(
    state: BendeyAppHeaderState,
    modifier: Modifier = Modifier,
    showSyncIndicator: Boolean = false,
    compactOnlineIndicator: Boolean = false,
    onNotificationsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (showSyncIndicator) {
            BendeyHeaderSyncIndicator(
                isOnline = state.isOnline,
                compact = compactOnlineIndicator,
            )
        }
        BadgedBox(
            badge = {
                if (state.notificationCount > 0) {
                    Badge { Text(state.notificationCount.coerceAtMost(9).toString()) }
                }
            },
        ) {
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier.size(BendeySpacing.touchTarget),
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notificaciones",
                    tint = BendeyColors.OnPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        BendeyHeaderUserMenu(
            state = state,
            onLogout = onLogout,
        )
    }
}

@Composable
private fun BendeyHeaderSyncIndicator(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (compact) {
        Box(
            modifier = modifier
                .padding(horizontal = BendeySpacing.xxs)
                .size(BendeySpacing.touchTarget),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) BendeyColors.Success else BendeyColors.Error),
            )
        }
        return
    }
    Row(
        modifier = modifier.padding(horizontal = BendeySpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isOnline) BendeyColors.Success else BendeyColors.Error),
        )
        Text(
            text = if (isOnline) "En línea" else "Sin conexión",
            style = MaterialTheme.typography.labelSmall,
            color = BendeyColors.OnPrimary.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BendeyHeaderUserMenu(
    state: BendeyAppHeaderState,
    onLogout: () -> Unit,
) {
    var showUserMenu by remember { mutableStateOf(false) }
    BendeyExpressiveScope {
        val avatarScale by animateFloatAsState(
            targetValue = if (showUserMenu) 1.04f else 1f,
            animationSpec = BendeyMotion.ExpressiveSpatialSpring,
            label = "profile_avatar_scale",
        )
        Box {
            Box(
                modifier = Modifier
                    .size(BendeySpacing.touchTarget)
                    .scale(avatarScale)
                    .clip(CircleShape)
                    .background(BendeyColors.OnPrimary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = { showUserMenu = true },
                    modifier = Modifier.size(BendeySpacing.touchTarget),
                ) {
                    Text(
                        text = state.userInitials.ifBlank { "?" },
                        color = BendeyColors.OnPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            DropdownMenu(
                expanded = showUserMenu,
                onDismissRequest = { showUserMenu = false },
                modifier = Modifier.widthIn(min = 272.dp),
                shape = BendeyShapeTokens.lg,
                containerColor = BendeyColors.Surface,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(BendeyColors.PrimaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = state.userInitials.ifBlank { "?" },
                                color = BendeyColors.OnPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.userName.ifBlank { "Usuario" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (state.branchName.isNotBlank()) {
                                Text(
                                    text = state.branchName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BendeyColors.OnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (state.restaurantName.isNotBlank()) {
                                Text(
                                    text = state.restaurantName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BendeyColors.OnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = BendeyColors.Outline.copy(alpha = 0.35f))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(BendeyShapeTokens.md)
                            .clickable {
                                showUserMenu = false
                                onLogout()
                            },
                        shape = BendeyShapeTokens.md,
                        color = BendeyColors.ErrorContainer.copy(alpha = 0.55f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = BendeySpacing.xs)
                                    .size(18.dp),
                                tint = BendeyColors.Error,
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
            }
        }
    }
}
