package com.bendey.restaurant.feature.auth.welcome

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.feature.auth.components.AuthWelcomeLayout

@Composable
fun WelcomeScreen(
    onBound: () -> Unit,
    onCreateRestaurant: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AuthWelcomeLayout(modifier = modifier) {
        BendeyManagementCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Ingresa el RUC de tu negocio",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = BendeyColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(BendeySpacing.sm))
            BendeyTextField(
                value = state.ruc,
                onValueChange = viewModel::onRucChange,
                label = "RUC",
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.linking,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(modifier = Modifier.height(BendeySpacing.md))
            BendeyPrimaryButton(
                text = if (state.linking) "Continuando…" else "Continuar",
                onClick = { viewModel.submit(onBound) },
                loading = state.linking,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { error ->
                Spacer(modifier = Modifier.height(BendeySpacing.sm))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(modifier = Modifier.height(BendeySpacing.lg))
        Text(
            text = "¿Aún no tienes una cuenta?",
            style = MaterialTheme.typography.bodyMedium,
            color = BendeyColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onCreateRestaurant) {
            Text(
                text = "Crear mi restaurante gratis",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = BendeyColors.Primary,
            )
        }
    }
}
