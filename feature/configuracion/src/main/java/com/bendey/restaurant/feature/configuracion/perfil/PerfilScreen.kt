package com.bendey.restaurant.feature.configuracion.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeySnackMessage
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyVerticalScrollColumn
import com.bendey.restaurant.core.ui.layout.BendeyListScreenLayout
import com.bendey.restaurant.core.ui.layout.rememberBendeyBottomBarScrollPadding

/** Perfil del usuario logueado — solo se llega acá con login completo (nunca por PIN). */
@Composable
fun PerfilScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    viewModel: PerfilViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BendeySnackMessage(
        message = state.snackMessage,
        onShow = onShowMessage,
        onConsume = viewModel::consumeSnackMessage,
    )

    BendeyListScreenLayout(
        modifier = modifier,
        header = {
            BendeyScreenToolbar(title = "Mi perfil", onBack = onBack)
        },
    ) { contentModifier ->
        if (state.loading) {
            Box(contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // El padding va DENTRO del scroll (contentPadding), no como .padding() del contenedor:
            // así el botón de "Cambiar contraseña" puede desplazarse por encima de la barra de
            // navegación del sistema en vez de quedar tapado justo en el borde — mismo ajuste que
            // ya se hizo en el Reporte de Caja.
            BendeyVerticalScrollColumn(
                modifier = contentModifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = BendeySpacing.md,
                    end = BendeySpacing.md,
                    top = BendeySpacing.md,
                    bottom = rememberBendeyBottomBarScrollPadding(includeBottomBar = false) + BendeySpacing.md,
                ),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.md),
            ) {
                BendeyManagementCard {
                    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
                        Text(
                            "Mis datos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        BendeyTextField(
                            value = state.form.name,
                            onValueChange = { v -> viewModel.updateForm { it.copy(name = v) } },
                            label = "Nombre",
                        )
                        BendeyTextField(
                            value = state.form.email,
                            onValueChange = { v -> viewModel.updateForm { it.copy(email = v) } },
                            label = "Email",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        )
                        BendeyTextField(
                            value = state.form.phone,
                            onValueChange = { v -> viewModel.updateForm { it.copy(phone = v) } },
                            label = "Teléfono",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        )
                        if (state.roleName.isNotBlank() || !state.branchName.isNullOrBlank()) {
                            Text(
                                listOfNotNull(
                                    state.roleName.takeIf { it.isNotBlank() }?.let { "Rol: $it" },
                                    state.branchName?.takeIf { it.isNotBlank() }?.let { "Sucursal: $it" },
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = BendeyColors.OnSurfaceVariant,
                            )
                        }
                        state.error?.let {
                            Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
                        }
                        BendeyPrimaryButton(
                            text = if (state.saving) "Guardando…" else "Guardar",
                            onClick = viewModel::saveProfile,
                            enabled = !state.saving,
                        )
                    }
                }

                BendeyManagementCard {
                    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
                        Text(
                            "Cambiar contraseña",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        HorizontalDivider(color = BendeyColors.Outline.copy(alpha = 0.35f))
                        BendeyTextField(
                            value = state.currentPassword,
                            onValueChange = viewModel::setCurrentPassword,
                            label = "Contraseña actual",
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        BendeyTextField(
                            value = state.newPassword,
                            onValueChange = viewModel::setNewPassword,
                            label = "Contraseña nueva",
                            placeholder = "Mínimo 8 caracteres",
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        BendeyTextField(
                            value = state.confirmPassword,
                            onValueChange = viewModel::setConfirmPassword,
                            label = "Repetir contraseña nueva",
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        state.passwordError?.let {
                            Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
                        }
                        BendeyPrimaryButton(
                            text = if (state.changingPassword) "Actualizando…" else "Cambiar contraseña",
                            onClick = viewModel::changePassword,
                            enabled = !state.changingPassword,
                        )
                    }
                }
            }
        }
    }
}
