package com.bendey.restaurant.feature.configuracion

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySwitchRow
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.layout.rememberBendeyLazyListContentPadding
import androidx.compose.foundation.lazy.rememberLazyListState

private val PAUSE_OPTIONS = listOf(15 to "15 min", 30 to "30 min", 60 to "1 hora", 120 to "2 horas", 0 to "Hasta reanudar")

/** R-11: interruptor del marketplace en Resto Android + R-9: pausa de pedidos y pedidos sin atender. */
@Composable
fun MarketplaceTab(
    modifier: Modifier = Modifier,
    viewModel: MarketplaceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.snackMessage) {
        state.snackMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSnack()
        }
    }

    if (state.loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    BendeyLazyColumn(
        state = rememberLazyListState(),
        modifier = modifier.fillMaxSize(),
        contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.md, top = BendeySpacing.md),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
    ) {
        state.error?.let { msg -> item { Text(msg, color = BendeyColors.Error) } }

        if (!state.menuDigitalEnabled) {
            item {
                BendeyManagementCard {
                    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                        Text("Falta activar el Menu Digital", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "El Marketplace muestra tu carta usando el Menu Digital. Activalo en la pestana Menu Digital antes de publicarte.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            BendeyManagementCard {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
                    BendeySectionTitle(text = "Marketplace Bendey Cliente")
                    Text(
                        "Con esto activado tu restaurante aparece en la app Bendey Cliente y cualquier persona puede hacerte un pedido de delivery o para recoger. Apagarlo solo te saca del listado: no borra nada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                    BendeySwitchRow(
                        label = "Publicar mi restaurante en el Marketplace",
                        checked = state.enabled,
                        onCheckedChange = viewModel::setEnabled,
                        enabled = state.canManage && !state.busy,
                    )
                }
            }
        }

        val ops = state.ops
        if (state.enabled && ops != null) {
            item {
                BendeyManagementCard {
                    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
                        BendeySectionTitle(text = if (ops.paused) "No estas recibiendo pedidos" else "Recibiendo pedidos")
                        if (ops.paused) {
                            Text(
                                buildString {
                                    append(if (ops.pausedUntil != null) "Se reanuda sola a la hora indicada." else "Se queda asi hasta que lo reanudes.")
                                    if (ops.pauseReason.isNotBlank()) append(" Motivo: ${ops.pauseReason}.")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = BendeyColors.OnSurfaceVariant,
                            )
                            BendeyPrimaryButton(text = "Reanudar", onClick = { viewModel.pause(false) }, enabled = !state.busy)
                        } else {
                            Text(
                                "Pausa los pedidos si te quedaste sin insumos o la cocina esta saturada. Tus clientes veran que no estas recibiendo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BendeyColors.OnSurfaceVariant,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                                PAUSE_OPTIONS.chunked(3).forEach { row ->
                                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                                        row.forEach { (minutes, label) ->
                                            com.bendey.restaurant.core.designsystem.components.BendeyFilterChip(
                                                selected = state.pauseMinutes == minutes,
                                                onClick = { viewModel.setPauseMinutes(minutes) },
                                                text = label,
                                            )
                                        }
                                    }
                                }
                            }
                            BendeyTextField(
                                value = state.pauseReason,
                                onValueChange = viewModel::setPauseReason,
                                label = "Motivo (opcional)",
                                enabled = !state.busy,
                            )
                            BendeyOutlinedButton(
                                text = "Dejar de recibir pedidos",
                                onClick = { viewModel.pause(true) },
                                enabled = !state.busy,
                                fillWidth = true,
                            )
                        }
                    }
                }
            }

            item {
                BendeyManagementCard {
                    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
                        BendeySectionTitle(text = "Pedidos sin atender")
                        Text(
                            "Un pedido esta sin atender mientras la cocina no empezo a prepararlo. Puedes avisar al personal y, si quieres, cancelarlo solo. Pon 0 para apagar cada opcion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                        BendeyTextField(
                            value = state.alertMinutes,
                            onValueChange = viewModel::setAlertMinutes,
                            label = "Avisar a los (min)",
                            enabled = state.canManage,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        BendeyTextField(
                            value = state.autoCancelMinutes,
                            onValueChange = viewModel::setAutoCancelMinutes,
                            label = "Cancelar a los (min)",
                            enabled = state.canManage,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        BendeyPrimaryButton(text = "Guardar", onClick = viewModel::saveThresholds, enabled = state.canManage && !state.busy)
                    }
                }
            }
        }
    }
}
