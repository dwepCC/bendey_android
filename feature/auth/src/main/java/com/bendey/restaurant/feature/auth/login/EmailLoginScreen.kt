package com.bendey.restaurant.feature.auth.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyBrandLogo
import com.bendey.restaurant.core.designsystem.motion.BendeyExpressiveReveal
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyLoadingOverlay
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn
import com.bendey.restaurant.core.ui.layout.bendeySafeDrawingPadding
import com.bendey.restaurant.feature.auth.components.AuthLayoutTokens

@Composable
fun EmailLoginScreen(
    onBack: () -> Unit,
    onAuthenticated: (route: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmailLoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        emailFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .bendeySafeDrawingPadding()
            .imePadding(),
    ) {
        BendeyScreenToolbar(
            title = "Administración",
            onBack = onBack,
        )
        BendeyVerticalScrollColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            showScrollHints = false,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = AuthLayoutTokens.loginFormMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = BendeySpacing.lg, vertical = BendeySpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BendeyExpressiveReveal(index = 0) {
                    BendeyBrandLogo(height = AuthLayoutTokens.logoHeightLogin, showBackground = true)
                }
                Spacer(modifier = Modifier.height(BendeySpacing.xs))
                BendeyExpressiveReveal(index = 1) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Bienvenido de nuevo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Inicia sesión en tu cuenta para continuar",
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = BendeySpacing.xxs, bottom = BendeySpacing.lg),
                        )
                    }
                }
                BendeyExpressiveReveal(index = 2) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BendeyTextField(
                            value = state.email,
                            onValueChange = viewModel::onEmailChange,
                            label = "Email",
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(emailFocusRequester),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { passwordFocusRequester.requestFocus() },
                            ),
                        )
                        Spacer(modifier = Modifier.height(BendeySpacing.sm))
                        BendeyTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = "Contraseña",
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(passwordFocusRequester),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (!state.loading) {
                                        viewModel.submit(onAuthenticated)
                                    }
                                },
                            ),
                        )
                        Spacer(modifier = Modifier.height(BendeySpacing.md))
                        BendeyPrimaryButton(
                            text = if (state.loading) "Iniciando sesión…" else "Iniciar sesión",
                            onClick = { viewModel.submit(onAuthenticated) },
                            loading = state.loading,
                            enabled = !state.loading,
                        )
                        state.error?.let { error ->
                            Spacer(modifier = Modifier.height(BendeySpacing.xs))
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
        }
    }
    BendeyLoadingOverlay(visible = state.loading)
}
