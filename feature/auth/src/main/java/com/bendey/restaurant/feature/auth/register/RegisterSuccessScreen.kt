package com.bendey.restaurant.feature.auth.register

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.auth.InitialAdminPinHolder
import com.bendey.restaurant.core.domain.auth.initialPinNotice
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.feature.auth.components.AuthExpressiveCard
import com.bendey.restaurant.feature.auth.components.AuthLayoutTokens
import com.bendey.restaurant.feature.auth.components.AuthWelcomeLayout

@Composable
fun RegisterSuccessScreen(
    restaurantName: String,
    onContinueToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // PIN inicial: solo en memoria y de corta vida. Sin PIN (pantalla recargada) se muestra el texto sin PIN.
    val initialPin = remember { InitialAdminPinHolder.peek() }
    val pinNotice = initialPinNotice(initialPin)
    val activity = LocalContext.current.findActivity()
    DisposableEffect(Unit) {
        onDispose {
            // Una rotación recrea la pantalla: el PIN debe seguir ahí; al salir de verdad se descarta.
            if (activity?.isChangingConfigurations != true) InitialAdminPinHolder.clear()
        }
    }
    AuthWelcomeLayout(
        modifier = modifier,
        title = "¡Listo!",
        subtitle = "Restaurante creado correctamente",
        description = "",
    ) {
        AuthExpressiveCard(tonal = true) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Registro completado",
                    tint = BendeyColors.Success,
                    modifier = Modifier.size(AuthLayoutTokens.successIconSize),
                )
            }
            Spacer(modifier = Modifier.height(BendeySpacing.md))
            Text(
                text = "Tu negocio ya está listo.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = BendeyColors.OnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (restaurantName.isNotBlank()) {
                Spacer(modifier = Modifier.height(BendeySpacing.xxs))
                Text(
                    text = restaurantName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = BendeyColors.Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (initialPin != null && pinNotice != null) {
                Spacer(modifier = Modifier.height(BendeySpacing.sm))
                Text(
                    text = "Tu PIN inicial de administrador es",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BendeyColors.OnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = initialPin,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp,
                    color = BendeyColors.OnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = pinNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(BendeySpacing.sm))
            Text(
                text = "Inicia sesión con tu correo y la contraseña que registraste. Como administrador no necesitas un PIN para entrar.",
                style = MaterialTheme.typography.bodyMedium,
                color = BendeyColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(BendeySpacing.lg))
            BendeyPrimaryButton(
                text = "Ir al inicio de sesión",
                onClick = onContinueToLogin,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
