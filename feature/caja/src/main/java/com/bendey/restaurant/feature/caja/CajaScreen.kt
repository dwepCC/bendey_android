package com.bendey.restaurant.feature.caja

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyKpiCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.cash.CashMovement
import com.bendey.restaurant.core.domain.cash.CashMovementType
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CajaScreen(
    modifier: Modifier = Modifier,
    viewModel: CajaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    LaunchedEffect(state.snackMessage) {
        if (state.snackMessage != null) viewModel.consumeSnackMessage()
    }

    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BendeyScreenToolbar(
                title = "Caja",
                subtitle = state.branchName ?: state.session?.branchName,
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                },
            )
            if (state.session == null && !state.loading) {
                ClosedCashCard(onOpen = viewModel::showOpenDialog)
            } else {
                state.session?.let { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BendeyKpiCard(
                            title = "Saldo estimado",
                            value = currency.format(session.expectedBalance),
                            hint = "Apertura ${currency.format(session.openingBalance)}",
                            accentColor = BendeyColors.AccentTeal,
                            icon = Icons.Default.Add,
                            modifier = Modifier.weight(1f),
                        )
                        BendeyKpiCard(
                            title = "Movimientos",
                            value = "+${currency.format(session.totalIncome)}",
                            hint = "-${currency.format(session.totalExpense)}",
                            accentColor = BendeyColors.AccentPurple,
                            icon = Icons.Default.Remove,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BendeyPrimaryButton(
                            text = "+ Ingreso",
                            onClick = { viewModel.showMovementDialog(CashMovementType.INCOME) },
                            modifier = Modifier.weight(1f),
                        )
                        BendeyPrimaryButton(
                            text = "- Egreso",
                            onClick = { viewModel.showMovementDialog(CashMovementType.EXPENSE) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = "Movimientos del turno",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(state.movements, key = { it.id }) { movement ->
                            MovementCard(movement, currency)
                        }
                    }
                    BendeyPrimaryButton(
                        text = if (state.actionLoading) "Cerrando…" else "Cerrar caja",
                        onClick = viewModel::showCloseDialog,
                        enabled = !state.actionLoading,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            state.error?.let {
                Text(it, color = BendeyColors.Error, modifier = Modifier.padding(16.dp))
            }
        }
    }

    if (state.showOpenDialog) {
        OpenCashDialog(
            form = state.openForm,
            loading = state.actionLoading,
            mandatory = state.session == null,
            onDismiss = viewModel::dismissOpenDialog,
            onConfirm = viewModel::confirmOpenSession,
            onFormChange = viewModel::updateOpenForm,
        )
    }

    if (state.showMovementDialog) {
        MovementDialog(
            form = state.movementForm,
            loading = state.actionLoading,
            onDismiss = viewModel::dismissMovementDialog,
            onConfirm = viewModel::confirmMovement,
            onFormChange = viewModel::updateMovementForm,
        )
    }

    if (state.showCloseDialog) {
        CloseCashDialog(
            form = state.closeForm,
            loading = state.actionLoading,
            onDismiss = viewModel::dismissCloseDialog,
            onConfirm = viewModel::confirmCloseSession,
            onFormChange = viewModel::updateCloseForm,
        )
    }
}

@Composable
private fun ClosedCashCard(onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Caja cerrada",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Debes abrir caja para registrar movimientos",
            color = BendeyColors.OnSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        BendeyPrimaryButton(text = "Abrir caja", onClick = onOpen)
    }
}

@Composable
private fun MovementCard(movement: CashMovement, currency: NumberFormat) {
    val isIncome = movement.type == CashMovementType.INCOME
    val accent = if (isIncome) BendeyColors.Success else BendeyColors.Error
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(movement.category.replace('_', ' '), fontWeight = FontWeight.Medium)
                movement.reference.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                }
            }
            Text(
                text = "${if (isIncome) "+" else "-"}${currency.format(movement.amount)}",
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun OpenCashDialog(
    form: OpenCashForm,
    loading: Boolean,
    mandatory: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onFormChange: ((OpenCashForm) -> OpenCashForm) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!mandatory) onDismiss() },
        title = { Text("Abrir caja") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Monto inicial en efectivo para iniciar el turno.")
                BendeyTextField(
                    value = form.openingBalance,
                    onValueChange = { v -> onFormChange { it.copy(openingBalance = v) } },
                    label = "Monto de apertura (S/)",
                )
                BendeyTextField(
                    value = form.notes,
                    onValueChange = { v -> onFormChange { it.copy(notes = v) } },
                    label = "Notas (opcional)",
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            BendeyPrimaryButton(
                text = if (loading) "Abriendo…" else "Abrir caja",
                onClick = onConfirm,
                enabled = !loading,
            )
        },
        dismissButton = {
            if (!mandatory) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}

@Composable
private fun MovementDialog(
    form: MovementForm,
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onFormChange: ((MovementForm) -> MovementForm) -> Unit,
) {
    val incomeCategories = listOf("ingreso_manual", "otro_ingreso", "devolucion")
    val expenseCategories = listOf("egreso_manual", "retiro", "gasto", "otro_egreso")
    val categories = if (form.type == CashMovementType.INCOME) incomeCategories else expenseCategories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (form.type == CashMovementType.INCOME) "Registrar ingreso" else "Registrar egreso")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = form.category == cat,
                            onClick = { onFormChange { it.copy(category = cat) } },
                            label = { Text(cat.replace('_', ' ')) },
                        )
                    }
                }
                BendeyTextField(
                    value = form.amount,
                    onValueChange = { v -> onFormChange { it.copy(amount = v) } },
                    label = "Monto (S/)",
                )
                BendeyTextField(
                    value = form.reference,
                    onValueChange = { v -> onFormChange { it.copy(reference = v) } },
                    label = "Referencia",
                )
                BendeyTextField(
                    value = form.notes,
                    onValueChange = { v -> onFormChange { it.copy(notes = v) } },
                    label = "Notas",
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            BendeyPrimaryButton(
                text = if (loading) "Guardando…" else "Guardar",
                onClick = onConfirm,
                enabled = !loading,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun CloseCashDialog(
    form: CloseCashForm,
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onFormChange: ((CloseCashForm) -> CloseCashForm) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cerrar caja") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Indica el efectivo contado en caja al cierre del turno.")
                BendeyTextField(
                    value = form.closingBalance,
                    onValueChange = { v -> onFormChange { it.copy(closingBalance = v) } },
                    label = "Efectivo contado (S/)",
                )
                BendeyTextField(
                    value = form.notes,
                    onValueChange = { v -> onFormChange { it.copy(notes = v) } },
                    label = "Notas de cierre",
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            BendeyPrimaryButton(
                text = if (loading) "Cerrando…" else "Cerrar caja",
                onClick = onConfirm,
                enabled = !loading,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
