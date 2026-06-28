package com.bendey.restaurant.feature.auth.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.feature.auth.components.AuthExpressiveCard
import com.bendey.restaurant.feature.auth.components.AuthWelcomeLayout

@Composable
fun WelcomeScreen(
    onBound: () -> Unit,
    onCreateRestaurant: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rucFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        rucFocusRequester.requestFocus()
    }

    AuthWelcomeLayout(modifier = modifier) {
        AuthExpressiveCard(
            title = "Ingresa el RUC de tu negocio",
            subtitle = "Validaremos que tu restaurante ya esté registrado en Bendey.",
        ) {
            BendeyTextField(
                value = state.ruc,
                onValueChange = viewModel::onRucChange,
                label = "RUC",
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(rucFocusRequester),
                enabled = !state.linking,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (state.canSubmit && !state.linking) {
                            viewModel.submit(onBound)
                        }
                    },
                ),
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
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = BendeyMotion.ExpressiveEffectsTween),
                    exit = fadeOut(animationSpec = BendeyMotion.ExpressiveEffectsTween),
                ) {
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
        Spacer(modifier = Modifier.height(BendeySpacing.lg))
        Text(
            text = "¿Aún no tienes una cuenta?",
            style = MaterialTheme.typography.bodyMedium,
            color = BendeyColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        BendeyTextButton(
            text = "Crear mi restaurante gratis",
            onClick = onCreateRestaurant,
            textStyle = MaterialTheme.typography.titleSmall,
        )
    }
}
