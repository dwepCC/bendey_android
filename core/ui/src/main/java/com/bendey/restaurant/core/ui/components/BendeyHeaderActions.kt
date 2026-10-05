package com.bendey.restaurant.core.ui.components

import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalLogic
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
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    onNotificationsClick: (() -> Unit)? = null,
    onOpenProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    /** Hueco antes de las acciones (p. ej. el chip de caja). */
    leadingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        leadingContent?.invoke()
        if (showSyncIndicator) {
            BendeyHeaderSyncIndicator(
                status = state.connection,
                compact = compactOnlineIndicator,
            )
        }
        // Sin handler no hay campana: un botón que no hace nada es peor que no tenerlo.
        if (onNotificationsClick != null) {
            BadgedBox(
                badge = {
                    if (state.notificationCount > 0) {
                        Badge { Text(PendingApprovalLogic.badgeLabel(state.notificationCount)) }
                    }
                },
            ) {
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.size(BendeySpacing.touchTarget),
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = PendingApprovalLogic.badgeContentDescription(state.notificationCount),
                        tint = BendeyColors.OnPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        BendeyHeaderUserMenu(
            state = state,
            onOpenProfile = onOpenProfile,
            onLogout = onLogout,
        )
    }
}

@Composable
private fun BendeyHeaderSyncIndicator(
    status: BendeyConnectionStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val label = connectionStatusLabel(status)
    val dotColor = when (status) {
        BendeyConnectionStatus.ONLINE -> BendeyColors.Success
        BendeyConnectionStatus.OFFLINE -> BendeyColors.Error
        BendeyConnectionStatus.CONNECTING -> BendeyColors.Warning
    }
    // Compacto (barra de tablet): solo el punto mientras todo va bien; si algo falla, el texto también,
    // para que el estado nunca dependa solo del color.
    val showText = !compact || status != BendeyConnectionStatus.ONLINE
    Row(
        modifier = modifier
            .padding(horizontal = BendeySpacing.xxs)
            .semantics(mergeDescendants = true) { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        if (showText) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = BendeyColors.OnPrimary.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Texto visible del estado de conexión (tuteo/neutro, sin jerga). */
internal fun connectionStatusLabel(status: BendeyConnectionStatus): String = when (status) {
    BendeyConnectionStatus.ONLINE -> "En línea"
    BendeyConnectionStatus.OFFLINE -> "Sin conexión"
    BendeyConnectionStatus.CONNECTING -> "Conectando…"
}

@Composable
private fun BendeyHeaderUserMenu(
    state: BendeyAppHeaderState,
    onOpenProfile: () -> Unit,
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
                    // Solo el login completo (email/contraseña) tiene datos/contraseña propios
                    // que editar — un turno abierto por PIN no es una cuenta separada.
                    if (state.isAdmin) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(BendeyShapeTokens.md)
                                .clickable {
                                    showUserMenu = false
                                    onOpenProfile()
                                },
                            shape = BendeyShapeTokens.md,
                            color = BendeyColors.SurfaceVariant.copy(alpha = 0.55f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(end = BendeySpacing.xs)
                                        .size(18.dp),
                                    tint = BendeyColors.OnSurfaceVariant,
                                )
                                Text(
                                    text = "Mi perfil",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = BendeyColors.OnSurface,
                                )
                            }
                        }
                    }
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
