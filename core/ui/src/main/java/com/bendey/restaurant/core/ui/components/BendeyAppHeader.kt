package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

data class BendeyAppHeaderState(
    val restaurantName: String = "",
    val branchName: String = "",
    val userName: String = "",
    val userInitials: String = "",
    val cashLabel: String? = null,
    val isCashOpen: Boolean = false,
    val isOnline: Boolean = true,
    val notificationCount: Int = 0,
)

@Composable
fun BendeyAppHeader(
    state: BendeyAppHeaderState,
    modifier: Modifier = Modifier,
    isDrawerOpen: Boolean = false,
    onMenuClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    var showUserMenu by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BendeyColors.Rest900)
            .heightIn(min = 52.dp)
            .padding(horizontal = BendeySpacing.xxs, vertical = BendeySpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenuClick, modifier = Modifier.size(BendeySpacing.touchTarget)) {
            Icon(
                if (isDrawerOpen) Icons.Default.Close else Icons.Default.Menu,
                contentDescription = if (isDrawerOpen) "Cerrar menú" else "Menú",
                tint = BendeyColors.OnPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = state.restaurantName.ifBlank { "Bendey Resto" },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BendeyColors.OnPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
        Box {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BendeyColors.OnPrimary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = { showUserMenu = true }, modifier = Modifier.size(36.dp)) {
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
                    state.cashLabel?.let { cashLabel ->
                        Surface(
                            shape = BendeyShapeTokens.sm,
                            color = if (state.isCashOpen) {
                                BendeyColors.SuccessContainer.copy(alpha = 0.65f)
                            } else {
                                BendeyColors.SurfaceVariant
                            },
                        ) {
                            Text(
                                text = cashLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (state.isCashOpen) BendeyColors.Success else BendeyColors.OnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xxs),
                            )
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
