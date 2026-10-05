package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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

/** Estado de conexión que ve el usuario en el header. Nunca se comunica solo con color: siempre lleva texto. */
enum class BendeyConnectionStatus { ONLINE, OFFLINE, CONNECTING }

data class BendeyAppHeaderState(
    val restaurantName: String = "",
    val branchName: String = "",
    val userName: String = "",
    val userInitials: String = "",
    /** Por defecto CONNECTING: nunca se muestra "En linea" (verde) antes de comprobarlo. */
    val connection: BendeyConnectionStatus = BendeyConnectionStatus.CONNECTING,
    val notificationCount: Int = 0,
    /** Solo el login completo (email/contraseña) puede editar su perfil — un turno por PIN no. */
    val isAdmin: Boolean = false,
)

/** Header global móvil (Compact*) — sin cambios visuales respecto a producción. */
@Composable
fun BendeyAppHeader(
    state: BendeyAppHeaderState,
    modifier: Modifier = Modifier,
    /** null = sin botón de menú (el rol no administra: no hay Mi negocio). */
    onMenuClick: (() -> Unit)? = null,
    menuContentDescription: String = "Mi negocio",
    userMenuItems: List<BendeyUserMenuItem> = emptyList(),
    /** null = no hay centro de notificaciones: la campana no se dibuja (sin botones muertos). */
    onNotificationsClick: (() -> Unit)? = null,
    onOpenProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    leadingActions: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BendeyColors.Rest900)
            .heightIn(min = 52.dp)
            .padding(horizontal = BendeySpacing.xxs, vertical = BendeySpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onMenuClick != null) {
            IconButton(onClick = onMenuClick, modifier = Modifier.size(BendeySpacing.touchTarget)) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = menuContentDescription,
                    tint = BendeyColors.OnPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            Spacer(Modifier.size(BendeySpacing.touchTarget))
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
            // En móvil el indicador solo aparece cuando algo anda mal (Sin conexión / Conectando).
            showSyncIndicator = state.connection != BendeyConnectionStatus.ONLINE,
            onNotificationsClick = onNotificationsClick,
            onOpenProfile = onOpenProfile,
            onLogout = onLogout,
            userMenuItems = userMenuItems,
            leadingContent = leadingActions,
        )
    }
}
