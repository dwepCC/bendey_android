package com.bendey.restaurant.feature.auth.register

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
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
            Spacer(modifier = Modifier.height(BendeySpacing.sm))
            Text(
                text = "Inicia sesión con tu correo y contraseña. Tu PIN de administrador por defecto es 7410.",
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
