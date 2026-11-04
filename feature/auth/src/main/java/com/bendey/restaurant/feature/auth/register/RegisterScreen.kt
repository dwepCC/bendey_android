package com.bendey.restaurant.feature.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.bendey.restaurant.core.ui.components.BendeySecondaryButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardCopy
import com.bendey.restaurant.core.ui.components.BendeyAlert
import com.bendey.restaurant.core.ui.components.BendeyAlertSeverity
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.feature.auth.components.AuthExpressiveCard
import com.bendey.restaurant.feature.auth.components.AuthWelcomeLayout

/**
 * Pantalla 2 del registro (R4): una sola pantalla. Al crear, el restaurante entra solo: la app
 * cambia de raíz cuando la sesión queda iniciada (no hay "éxito → inicio → login").
 */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rucFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    val isEcuador = state.country == "EC"
    val razonSocialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.country) { rucFocusRequester.requestFocus() }
    LaunchedEffect(state.rucValidated) {
        if (state.rucValidated) {
            if (isEcuador) razonSocialFocusRequester.requestFocus() else emailFocusRequester.requestFocus()
        }
    }

    AuthWelcomeLayout(
        modifier = modifier,
        title = "Crea tu restaurante",
        subtitle = if (isEcuador) WizardCopy.REGISTER_SUBTITLE_ECUADOR else "Empieza a vender en minutos.",
        description = if (isEcuador) {
            "Solo necesitamos tu RUC, tus datos, un correo y una contraseña."
        } else {
            "Solo necesitamos tu RUC, un correo y una contraseña."
        },
        scrollable = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
        ) {
            BendeyIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                enabled = !state.loading,
            )
            Text(
                text = "Volver",
                style = MaterialTheme.typography.bodyMedium,
                color = BendeyColors.OnSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(BendeySpacing.xs))
        AuthExpressiveCard(title = WizardCopy.REGISTER_TITLE) {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.formFieldGap)) {
                Text(
                    text = WizardCopy.REGISTER_COUNTRY,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = BendeyColors.OnSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    listOf("PE" to WizardCopy.REGISTER_COUNTRY_PERU, "EC" to WizardCopy.REGISTER_COUNTRY_ECUADOR).forEach { (code, label) ->
                        val enabled = !state.rucValidated && !state.loading
                        if (state.country == code) {
                            BendeyPrimaryButton(text = label, onClick = {}, enabled = enabled, modifier = Modifier.weight(1f))
                        } else {
                            BendeySecondaryButton(text = label, onClick = { viewModel.chooseCountry(code) }, enabled = enabled, modifier = Modifier.weight(1f))
                        }
                    }
                }
                BendeyTextField(
                    value = state.ruc,
                    onValueChange = viewModel::onRucChange,
                    label = "RUC",
                    supportingText = if (state.rucValidated) {
                        null
                    } else if (isEcuador) {
                        WizardCopy.REGISTER_RUC_HINT_ECUADOR
                    } else {
                        WizardCopy.REGISTER_RUC_HINT_PERU
                    },
                    modifier = Modifier.focusRequester(rucFocusRequester),
                    enabled = !state.rucValidated && !state.validating && !state.loading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (state.canValidate) viewModel.validateRuc() }),
                )
                if (!state.rucValidated) {
                    BendeyPrimaryButton(
                        text = when {
                            state.validating -> "Validando…"
                            isEcuador -> WizardCopy.REGISTER_CONTINUE
                            else -> "Validar"
                        },
                        onClick = viewModel::validateRuc,
                        loading = state.validating,
                        enabled = state.canValidate,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "RUC validado",
                            tint = BendeyColors.Success,
                        )
                        Text(
                            // Ecuador: el RUC solo se revisó por estructura; la razón social se escribe abajo.
                            text = if (isEcuador) state.ruc else state.razonSocial,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = BendeyColors.OnSurface,
                            modifier = Modifier.weight(1f),
                        )
                        BendeyTextButton(text = "Cambiar RUC", onClick = viewModel::clearRuc, enabled = !state.loading)
                    }
                    if (state.rucWarning.isNotBlank()) {
                        BendeyAlert(message = state.rucWarning, severity = BendeyAlertSeverity.Warning)
                    }
                    if (isEcuador) {
                        BendeyTextField(
                            value = state.razonSocial,
                            onValueChange = viewModel::onRazonSocialChange,
                            label = WizardCopy.REGISTER_BUSINESS_NAME,
                            modifier = Modifier.focusRequester(razonSocialFocusRequester),
                            enabled = !state.loading,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        )
                        BendeyTextField(
                            value = state.address,
                            onValueChange = viewModel::onAddressChange,
                            label = WizardCopy.REGISTER_ADDRESS,
                            enabled = !state.loading,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { emailFocusRequester.requestFocus() }),
                        )
                    }
                    BendeyTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        label = "Correo electrónico",
                        modifier = Modifier.focusRequester(emailFocusRequester),
                        enabled = !state.loading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                    )
                    BendeyTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = "Contraseña (mínimo 6 caracteres)",
                        modifier = Modifier.focusRequester(passwordFocusRequester),
                        enabled = !state.loading,
                        visualTransformation = if (state.showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (state.canSubmit) viewModel.submit() }),
                    )
                    BendeyTextButton(
                        text = if (state.showPassword) WizardCopy.REGISTER_HIDE_PASSWORD else WizardCopy.REGISTER_SHOW_PASSWORD,
                        onClick = viewModel::togglePasswordVisibility,
                        modifier = Modifier.align(Alignment.End),
                    )
                    BendeyPrimaryButton(
                        text = if (state.loading) WizardCopy.REGISTER_CREATING else WizardCopy.REGISTER_CTA,
                        onClick = viewModel::submit,
                        loading = state.loading,
                        enabled = state.canSubmit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                state.error?.let { error ->
                    BendeyAlert(message = error, severity = BendeyAlertSeverity.Danger)
                }
            }
        }
    }
}
