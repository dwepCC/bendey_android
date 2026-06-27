package com.bendey.restaurant.feature.auth.pin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyBrandLogo
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.designsystem.motion.BendeyExpressiveReveal
import com.bendey.restaurant.core.ui.components.BendeyLoadingOverlay
import com.bendey.restaurant.core.ui.components.BendeyPinKeypad
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.layout.bendeySafeDrawingPadding

@Composable
fun PinLoginScreen(
    onBack: () -> Unit,
    onAuthenticated: (route: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PinViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().bendeySafeDrawingPadding()) {
        BendeyScreenToolbar(
            title = state.station.label,
            subtitle = "Ingresa tu PIN",
            onBack = onBack,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .padding(horizontal = BendeySpacing.lg, vertical = BendeySpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BendeyExpressiveReveal(index = 0) {
                    BendeyBrandLogo(height = 48.dp, showBackground = true)
                }
                Spacer(modifier = Modifier.height(BendeySpacing.sm))
                BendeyExpressiveReveal(index = 1) {
                    Text(
                        text = "Ingresa tu PIN de operación",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = BendeySpacing.md),
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
                            text = "INGRESAR",
                            onClick = { viewModel.submit(onAuthenticated) },
                            loading = state.loading,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        state.error?.let {
                            Spacer(modifier = Modifier.height(BendeySpacing.xs))
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    BendeyLoadingOverlay(visible = state.loading)
}
