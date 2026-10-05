package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/**
 * Cabecera mínima del modo quiosco de Cocina (R2b): nombre del restaurante, estado de conexión (siempre con
 * texto) y «Salir». Sin menú, sin avatar, sin campana de pedidos ni acceso a otras pantallas. La campana de
 * alerta de atraso (🔔/🔕) vive en la propia pantalla de Cocina.
 */
@Composable
fun BendeyKioskHeader(
    state: BendeyAppHeaderState,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    /** Ayuda es para todos los puestos: en el quiosco no hay avatar, va como un icono discreto. */
    onHelp: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BendeyColors.Rest900)
            .heightIn(min = 52.dp)
            .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.restaurantName.ifBlank { "Bendey Resto" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BendeyColors.OnPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        BendeyHeaderSyncIndicator(status = state.connection)
        if (onHelp != null) {
            IconButton(onClick = onHelp, modifier = Modifier.size(BendeySpacing.touchTarget)) {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Ayuda",
                    tint = BendeyColors.OnPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        TextButton(
            onClick = onLogout,
            modifier = Modifier.heightIn(min = BendeySpacing.touchTarget),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = BendeyColors.OnPrimary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Salir",
                modifier = Modifier.padding(start = BendeySpacing.xxs),
                color = BendeyColors.OnPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
