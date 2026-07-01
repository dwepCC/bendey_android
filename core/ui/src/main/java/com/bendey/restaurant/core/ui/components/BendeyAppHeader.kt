package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

data class BendeyAppHeaderState(
    val restaurantName: String = "",
    val branchName: String = "",
    val userName: String = "",
    val userInitials: String = "",
    val isOnline: Boolean = true,
    val notificationCount: Int = 0,
)

/** Header global móvil (Compact*) — sin cambios visuales respecto a producción. */
@Composable
fun BendeyAppHeader(
    state: BendeyAppHeaderState,
    modifier: Modifier = Modifier,
    isDrawerOpen: Boolean = false,
    onMenuClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
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
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = BendeySpacing.xxs),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = state.restaurantName.ifBlank { "Bendey Resto" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BendeyColors.OnPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BendeyHeaderActions(
            state = state,
            showSyncIndicator = false,
            onNotificationsClick = onNotificationsClick,
            onLogout = onLogout,
        )
    }
}
