package com.bendey.restaurant.feature.auth.pin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyBrandLogo
import com.bendey.restaurant.core.designsystem.motion.BendeyExpressiveReveal
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyLoadingOverlay
import com.bendey.restaurant.core.ui.components.BendeyPinKeypad
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn
import com.bendey.restaurant.core.ui.layout.bendeySafeDrawingPadding
import com.bendey.restaurant.feature.auth.components.AuthLayoutTokens

@Composable
fun PinLoginScreen(
    onBack: () -> Unit,
    onAuthenticated: (route: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PinViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .bendeySafeDrawingPadding()
            .imePadding(),
    ) {
        BendeyScreenToolbar(
            title = state.station.label,
            subtitle = "Ingresa tu PIN",
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
                    .widthIn(max = AuthLayoutTokens.pinFormMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = BendeySpacing.lg, vertical = BendeySpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BendeyExpressiveReveal(index = 0) {
                    BendeyBrandLogo(height = AuthLayoutTokens.logoHeightPin, showBackground = true)
                }
                Spacer(modifier = Modifier.height(BendeySpacing.sm))
                BendeyExpressiveReveal(index = 1) {
                    Text(
                        text = "Ingresa tu PIN de operación",
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = BendeySpacing.md),
                    )
                }
                BendeyExpressiveReveal(index = 2) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BendeyPinKeypad(
                            onDigit = viewModel::appendDigit,
                            onBackspace = viewModel::backspace,
                            currentLength = state.pin.length,
                        )
                        Spacer(modifier = Modifier.height(BendeySpacing.md))
                        BendeyPrimaryButton(
                            text = if (state.loading) "Ingresando…" else "Ingresar",
                            onClick = { viewModel.submit(onAuthenticated) },
                            loading = state.loading,
                            enabled = !state.loading,
                            modifier = Modifier.fillMaxWidth(),
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
