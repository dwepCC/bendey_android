package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
    onSwitchUser: () -> Unit = {},
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
            text = "Bendey Resto",
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
            ) {
                if (state.userName.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text(state.userName, fontWeight = FontWeight.SemiBold) },
                        onClick = { showUserMenu = false },
                        enabled = false,
                    )
                }
                DropdownMenuItem(
                    text = { Text("Cambiar usuario") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    onClick = {
                        showUserMenu = false
                        onSwitchUser()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Cerrar sesión") },
                    leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null) },
                    onClick = {
                        showUserMenu = false
                        onLogout()
                    },
                )
            }
        }
    }
}
