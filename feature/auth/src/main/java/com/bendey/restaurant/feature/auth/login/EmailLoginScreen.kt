package com.bendey.restaurant.feature.auth.login

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyBrandLogo
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyLoadingOverlay
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.layout.bendeySafeDrawingPadding

@Composable
fun EmailLoginScreen(
    onBack: () -> Unit,
    onAuthenticated: (route: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmailLoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().bendeySafeDrawingPadding()) {
        BendeyScreenToolbar(
            title = "Administración",
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
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(horizontal = BendeySpacing.lg, vertical = BendeySpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BendeyBrandLogo(height = 52.dp, showBackground = true)
                Spacer(modifier = Modifier.height(BendeySpacing.xs))
                Text(
                    text = "Bienvenido de nuevo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Inicia sesión en tu cuenta para continuar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = BendeySpacing.xxs, bottom = BendeySpacing.lg),
                )
                BendeyTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Email",
                )
                Spacer(modifier = Modifier.height(BendeySpacing.sm))
                BendeyTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Contraseña",
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(modifier = Modifier.height(BendeySpacing.md))
                BendeyPrimaryButton(
                    text = "INICIAR SESIÓN",
                    onClick = { viewModel.submit(onAuthenticated) },
                    loading = state.loading,
                )
                state.error?.let {
                    Spacer(modifier = Modifier.height(BendeySpacing.xs))
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    BendeyLoadingOverlay(visible = state.loading)
}
