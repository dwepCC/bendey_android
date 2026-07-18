package com.bendey.restaurant.core.ui.subscription

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton

/**
 * Bloquea el contenido cuando el plan del tenant no incluye [hasModule].
 * El acceso a la pantalla sigue siendo posible (nunca se oculta del menú) — solo el
 * contenido se reemplaza por un CTA hacia la pantalla de suscripción.
 */
@Composable
fun RequireModule(
    hasModule: Boolean,
    onNavigateToSubscription: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Función disponible en un plan superior",
    description: String = "Actualiza tu plan para habilitar la facturación electrónica y esta sección.",
    content: @Composable () -> Unit,
) {
    if (hasModule) {
        content()
        return
    }
    BendeyEmptyState(
        title = title,
        description = description,
        modifier = modifier.fillMaxSize(),
        action = {
            BendeyPrimaryButton(
                text = "Ver planes",
                onClick = onNavigateToSubscription,
                fillWidth = false,
            )
        },
    )
}

/** Icono candado reutilizable para chips/botones bloqueados por plan. */
@Composable
fun ProLockIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = null,
        tint = BendeyColors.OnSurfaceVariant,
        modifier = modifier,
    )
}

val ProLockIconSize = 14.dp
