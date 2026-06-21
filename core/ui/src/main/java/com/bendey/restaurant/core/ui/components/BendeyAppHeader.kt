package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                if (isDrawerOpen) Icons.Default.Close else Icons.Default.Menu,
                contentDescription = if (isDrawerOpen) "Cerrar menú" else "Menú",
                tint = BendeyColors.OnPrimary,
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
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notificaciones",
                    tint = BendeyColors.OnPrimary,
                )
            }
        }
        Box {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BendeyColors.OnPrimary.copy(alpha = 0.18f)),
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
                modifier = Modifier.widthIn(min = 260.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(BendeyColors.PrimaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = state.userInitials.ifBlank { "?" },
                                color = BendeyColors.Primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
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
                        Text(
                            text = cashLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.isCashOpen) BendeyColors.Success else BendeyColors.OnSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = BendeyColors.Outline.copy(alpha = 0.35f))
                    OutlinedButton(
                        onClick = {
                            showUserMenu = false
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                        )
                        Text("Cerrar sesión")
                    }
                }
            }
        }
    }
}
