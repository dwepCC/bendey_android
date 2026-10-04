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
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
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

    AuthWelcomeLayout(modifier = modifier) {
        BendeyPrimaryButton(
            text = "Crear mi restaurante gratis",
            onClick = onCreateRestaurant,
            enabled = !state.linking,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(BendeySpacing.lg))
        Text(
            text = "¿Ya tienes un restaurante en Bendey?",
            style = MaterialTheme.typography.bodyMedium,
            color = BendeyColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(BendeySpacing.xs))
        AuthExpressiveCard(
            title = "Vincula tu restaurante con el RUC",
            subtitle = "Validaremos que tu restaurante ya esté registrado en Bendey.",
        ) {
            BendeyTextField(
                value = state.ruc,
                onValueChange = viewModel::onRucChange,
                label = "RUC",
                modifier = Modifier.fillMaxWidth(),
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
            BendeyOutlinedButton(
                text = if (state.linking) "Continuando…" else "Continuar",
                onClick = { viewModel.submit(onBound) },
                enabled = state.canSubmit && !state.linking,
                fillWidth = true,
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
    }
}
