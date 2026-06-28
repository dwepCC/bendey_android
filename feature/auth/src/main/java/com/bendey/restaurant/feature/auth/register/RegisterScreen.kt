package com.bendey.restaurant.feature.auth.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyMotion
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.feature.auth.components.AuthExpressiveCard
import com.bendey.restaurant.feature.auth.components.AuthFormSection
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
    val rucFocusRequester = remember { FocusRequester() }
    val commercialFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val phoneFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        rucFocusRequester.requestFocus()
    }

    LaunchedEffect(state.rucValidated) {
        if (state.rucValidated) {
            commercialFocusRequester.requestFocus()
        }
    }

    AuthWelcomeLayout(
        modifier = modifier,
        title = "Crea tu restaurante",
        subtitle = "Empieza a vender en minutos.",
        description = "Controla mesas, pedidos y caja desde una sola plataforma.",
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
            )
            Text(
                text = "Volver",
                style = MaterialTheme.typography.bodyMedium,
                color = BendeyColors.OnSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(BendeySpacing.xs))
        AuthExpressiveCard(
            title = "Tu restaurante listo para trabajar",
            subtitle = "Completa los datos para activar tu cuenta.",
        ) {
            AuthFormSection(title = "Información SUNAT") {
                RucValidationSection(
                    ruc = state.ruc,
                    onRucChange = viewModel::onRucChange,
                    razonSocial = state.razonSocial,
                    commercialName = state.restaurantName,
                    onCommercialNameChange = viewModel::onRestaurantNameChange,
                    rucValidated = state.rucValidated,
                    validating = state.validating,
                    showCommercialName = false,
                    onValidate = viewModel::validateRuc,
                    onClearRuc = viewModel::clearRuc,
                    commercialNameLabel = "Nombre comercial *",
                    rucFocusRequester = rucFocusRequester,
                    commercialNameFocusRequester = commercialFocusRequester,
                    onCommercialNameDone = { emailFocusRequester.requestFocus() },
                )
            }
            AnimatedVisibility(
                visible = state.rucValidated,
                enter = fadeIn(animationSpec = BendeyMotion.ExpressiveEffectsTween),
                exit = fadeOut(animationSpec = BendeyMotion.ExpressiveEffectsTween),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.lg),
                ) {
                    AuthFormSection(title = "Información comercial") {
                        BendeyTextField(
                            value = state.restaurantName,
                            onValueChange = viewModel::onRestaurantNameChange,
                            label = "Nombre comercial *",
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(commercialFocusRequester),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { emailFocusRequester.requestFocus() },
                            ),
                        )
                    }
                    AuthFormSection(title = "Información de contacto") {
                        BendeyTextField(
                            value = state.email,
                            onValueChange = viewModel::onEmailChange,
                            label = "Correo electrónico *",
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(emailFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { phoneFocusRequester.requestFocus() },
                            ),
                        )
                        BendeyTextField(
                            value = state.phone,
                            onValueChange = viewModel::onPhoneChange,
                            label = "Celular",
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(phoneFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { passwordFocusRequester.requestFocus() },
                            ),
                        )
                    }
                    AuthFormSection(title = "Credenciales") {
                        BendeyTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = "Contraseña *",
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(passwordFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { confirmPasswordFocusRequester.requestFocus() },
                            ),
                        )
                        BendeyTextField(
                            value = state.confirmPassword,
                            onValueChange = viewModel::onConfirmPasswordChange,
                            label = "Confirmar contraseña *",
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(confirmPasswordFocusRequester),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (state.canSubmit && !state.loading) {
                                        viewModel.submit(onRegistered)
                                    }
                                },
                            ),
                        )
                    }
                    BendeyPrimaryButton(
                        text = if (state.loading) "Creando restaurante…" else "Crear mi restaurante",
                        onClick = { viewModel.submit(onRegistered) },
                        loading = state.loading,
                        enabled = state.canSubmit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            state.error?.let { error ->
                Spacer(modifier = Modifier.height(BendeySpacing.sm))
                Text(
                    text = error,
                    color = BendeyColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
