package com.bendey.restaurant.feature.auth.register

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.bendey.restaurant.feature.auth.components.RucValidationSection

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: (restaurantName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AuthWelcomeLayout(
        modifier = modifier,
        title = "Crea tu restaurante",
        subtitle = "Empieza a vender en minutos.",
        description = "Controla mesas, pedidos y caja desde una sola plataforma.",
        scrollable = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = BendeyColors.OnSurfaceVariant,
                )
            }
            Text(
                text = "Volver",
                style = MaterialTheme.typography.bodyMedium,
                color = BendeyColors.OnSurfaceVariant,
            )
        }
        BendeyManagementCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Tu restaurante listo para trabajar",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = BendeyColors.OnSurface,
            )
            Spacer(modifier = Modifier.height(BendeySpacing.sm))
            RucValidationSection(
                ruc = state.ruc,
                onRucChange = viewModel::onRucChange,
                razonSocial = state.razonSocial,
                commercialName = state.restaurantName,
                onCommercialNameChange = viewModel::onRestaurantNameChange,
                rucValidated = state.rucValidated,
                validating = state.validating,
                showCommercialName = true,
                onValidate = viewModel::validateRuc,
                onClearRuc = viewModel::clearRuc,
                commercialNameLabel = "Nombre comercial *",
            )
            if (state.rucValidated) {
                Spacer(modifier = Modifier.height(BendeySpacing.sm))
                BendeyTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Correo electrónico *",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                BendeyTextField(
                    value = state.phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = "Celular",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                BendeyTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Contraseña *",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                BendeyTextField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = "Confirmar contraseña *",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Spacer(modifier = Modifier.height(BendeySpacing.md))
                BendeyPrimaryButton(
                    text = if (state.loading) "Creando restaurante…" else "Crear mi restaurante",
                    onClick = { viewModel.submit(onRegistered) },
                    loading = state.loading,
                    enabled = state.canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
    }
}
