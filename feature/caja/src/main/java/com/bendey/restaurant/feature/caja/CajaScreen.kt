package com.bendey.restaurant.feature.caja

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyKpiCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.cash.CashBankAccount
import com.bendey.restaurant.core.domain.cash.CashBankMovement
import com.bendey.restaurant.core.domain.cash.CashMovement
import com.bendey.restaurant.core.domain.cash.CashMovementType
import com.bendey.restaurant.core.domain.cash.CashPaymentMethod
import com.bendey.restaurant.core.domain.cash.CashSessionBrief
import com.bendey.restaurant.core.domain.cash.CashSessionReport
import com.bendey.restaurant.core.domain.cash.CashSessionStatus
import com.bendey.restaurant.core.ui.components.BindSnackMessage
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeyTextField
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CajaScreen(
    modifier: Modifier = Modifier,
    onShowMessage: (String) -> Unit = {},
    viewModel: CajaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    val context = LocalContext.current

    BindSnackMessage(
        message = state.snackMessage,
        onShow = onShowMessage,
        onConsume = viewModel::consumeSnackMessage,
    )

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CajaTab.entries.filter { tab ->
                    tab != CajaTab.CONFIG || state.canViewCashSettings
                }.forEach { tab ->
                    FilterChip(
                        selected = state.tab == tab,
                        onClick = { viewModel.setTab(tab) },
                        label = { Text(tab.label) },
                    )
                }
            }
            when {
                state.session == null && state.tab == CajaTab.SESSION && !state.loading -> {
                    ClosedCashCard(onOpen = viewModel::showOpenDialog)
                }
                state.tab == CajaTab.SESSION && state.session != null -> {
                    SessionTab(state, currency, viewModel)
                }
                state.tab == CajaTab.MOVEMENTS -> {
                    MovementsTab(state, currency, viewModel, context)
                }
                state.tab == CajaTab.REPORT -> {
                    ReportTab(state, currency, viewModel, context)
                }
                state.tab == CajaTab.HISTORY -> {
                    HistoryTab(state, currency, viewModel)
                }
                state.tab == CajaTab.CONFIG -> {
                    ConfigTab(state, currency, viewModel)
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
            paymentMethods = state.paymentMethods,
            loading = state.actionLoading,
            onDismiss = viewModel::dismissMovementDialog,
            onConfirm = viewModel::confirmMovement,
            onFormChange = viewModel::updateMovementForm,
        )
    }
    if (state.showPaymentMethodDialog) {
        PaymentMethodDialog(
            form = state.paymentMethodForm,
            bankAccounts = state.bankAccounts,
            loading = state.actionLoading,
            onDismiss = viewModel::dismissPaymentMethodDialog,
            onConfirm = viewModel::confirmPaymentMethod,
            onFormChange = viewModel::updatePaymentMethodForm,
        )
    }
    if (state.showBankAccountDialog) {
        BankAccountDialog(
            form = state.bankAccountForm,
            paymentMethods = state.paymentMethods,
            loading = state.actionLoading,
            onDismiss = viewModel::dismissBankAccountDialog,
            onConfirm = viewModel::confirmBankAccount,
            onFormChange = viewModel::updateBankAccountForm,
        )
    }
    if (state.showBankMovementsDialog) {
        BankMovementsDialog(
            accountName = state.bankMovementsAccountName,
            movements = state.bankMovements,
            form = state.bankMovementForm,
            loading = state.bankMovementsLoading || state.actionLoading,
            currency = currency,
            onDismiss = viewModel::dismissBankMovementsDialog,
            onConfirm = viewModel::confirmBankMovement,
            onFormChange = viewModel::updateBankMovementForm,
        )
    }
    ArqueoDialog(
        open = state.showArqueoDialog,
        values = state.arqueoDraft,
        expectedBalance = state.currentBalance,
        loading = state.actionLoading,
        currency = currency,
        onQtyChange = viewModel::setArqueoQty,
        onDismiss = viewModel::dismissArqueoDialog,
        onConfirm = viewModel::confirmSaveArqueo,
    )
    if (state.showCloseDialog) {
        CloseCashDialog(
            form = state.closeForm,
            expectedBalance = state.currentBalance,
            loading = state.actionLoading,
            currency = currency,
            operationalStatus = state.operationalStatus,
            onDismiss = viewModel::dismissCloseDialog,
            onConfirm = viewModel::requestCloseSession,
            onFormChange = viewModel::updateCloseForm,
            onArqueoQtyChange = viewModel::setCloseArqueoQty,
        )
    }
    if (state.showCloseForceConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCloseForceConfirm,
            title = { Text("Operaciones activas") },
            text = {
                Text(
                    "Hay mesas, sesiones o comandas activas. ¿Desea cerrar la caja de todas formas?",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                BendeyPrimaryButton(
                    text = if (state.actionLoading) "Cerrando…" else "Continuar cierre",
                    onClick = viewModel::confirmCloseSessionForced,
                    enabled = !state.actionLoading,
                )
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCloseForceConfirm) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun SessionTab(state: CajaUiState, currency: NumberFormat, viewModel: CajaViewModel) {
    val session = state.session ?: return
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BendeyPrimaryButton("+ Ingreso", { viewModel.showMovementDialog(CashMovementType.INCOME) }, Modifier.weight(1f))
            BendeyPrimaryButton("- Egreso", { viewModel.showMovementDialog(CashMovementType.EXPENSE) }, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = viewModel::showArqueoDialog, modifier = Modifier.weight(1f)) { Text("Arqueo") }
            BendeyPrimaryButton(
                text = if (state.actionLoading) "Cerrando…" else "Cerrar caja",
                onClick = viewModel::showCloseDialog,
                enabled = !state.actionLoading,
                modifier = Modifier.weight(1f),
            )
        }
        session.openedByName?.let {
            Text("Operador: $it", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "Últimos movimientos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(state.movements.take(8), key = { it.id }) { movement ->
                MovementCard(movement, currency)
            }
        }
    }
}

@Composable
private fun MovementsTab(state: CajaUiState, currency: NumberFormat, viewModel: CajaViewModel, context: android.content.Context) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.session != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BendeyPrimaryButton("+ Ingreso", { viewModel.showMovementDialog(CashMovementType.INCOME) }, Modifier.weight(1f))
                    BendeyPrimaryButton("- Egreso", { viewModel.showMovementDialog(CashMovementType.EXPENSE) }, Modifier.weight(1f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BendeyTextField(
                    value = state.movementsFilter.dateFrom,
                    onValueChange = { v -> viewModel.updateMovementsFilter { it.copy(dateFrom = v) } },
                    label = "Desde",
                    modifier = Modifier.weight(1f),
                )
                BendeyTextField(
                    value = state.movementsFilter.dateTo,
                    onValueChange = { v -> viewModel.updateMovementsFilter { it.copy(dateTo = v) } },
                    label = "Hasta",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BendeyTextField(
                    value = state.movementsFilter.sessionId?.toString().orEmpty(),
                    onValueChange = { v -> viewModel.updateMovementsFilter { it.copy(sessionId = v.trim().toIntOrNull()) } },
                    label = "Sesión #",
                    modifier = Modifier.weight(1f),
                )
                BendeyTextField(
                    value = state.movementsFilter.paymentMethod,
                    onValueChange = { v -> viewModel.updateMovementsFilter { it.copy(paymentMethod = v) } },
                    label = "Método pago",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BendeyTextField(
                    value = state.movementsFilter.userId?.toString().orEmpty(),
                    onValueChange = { v -> viewModel.updateMovementsFilter { it.copy(userId = v.trim().toIntOrNull()) } },
                    label = "Usuario #",
                    modifier = Modifier.weight(1f),
                )
                BendeyTextField(
                    value = state.movementsFilter.type,
                    onValueChange = { v -> viewModel.updateMovementsFilter { it.copy(type = v) } },
                    label = "Tipo (income/expense)",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BendeyPrimaryButton("Buscar", viewModel::searchMovementsReport, Modifier.weight(1f))
                OutlinedButton(
                    onClick = { viewModel.exportMovementsReport(context) },
                    enabled = !state.movementsExportBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.movementsExportBusy) "Exportando…" else "Exportar Excel")
                }
            }
        }
        item {
            Text(
                "Ingresos: ${currency.format(state.movementsReportSummary.sumIncome)} · " +
                    "Egresos: ${currency.format(state.movementsReportSummary.sumExpense)} · " +
                    "Neto: ${currency.format(state.movementsReportSummary.netMovement)}",
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
        }
        if (state.movementsReportLoading) {
            item { CircularProgressIndicator(modifier = Modifier.padding(12.dp)) }
        }
        item {
            Text("Movimientos efectivo", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        }
        if (state.movementsReportRows.isEmpty() && !state.movementsReportLoading) {
            item { Text("Sin movimientos en el periodo", color = BendeyColors.OnSurfaceVariant) }
        }
        items(state.movementsReportRows, key = { it.movementId }) { row ->
            Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
                Column(Modifier.padding(10.dp)) {
                    Text(row.docNumber.ifBlank { row.category.orEmpty() }, fontWeight = FontWeight.Medium)
                    Text("${row.date} · ${row.userName} · ${row.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                    Text(currency.format(row.amount), fontWeight = FontWeight.Bold, color = BendeyColors.Primary)
                }
            }
        }
        val electronic = state.paymentsReport?.detail?.filter { row ->
            row.method.trim().lowercase() !in setOf("efectivo", "cash", "contado")
        }.orEmpty()
        if (electronic.isNotEmpty()) {
            item {
                Text("Cobros electrónicos", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            }
            items(electronic, key = { "${it.saleNumber}-${it.date}-${it.amount}" }) { row ->
                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
                    Column(Modifier.padding(10.dp)) {
                        Text(row.saleNumber.ifBlank { row.orderCode }, fontWeight = FontWeight.Medium)
                        Text("${row.date} · ${row.method}", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                        Text(currency.format(row.amount), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportTab(
    state: CajaUiState,
    currency: NumberFormat,
    viewModel: CajaViewModel,
    context: android.content.Context,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val sessions = buildList {
            state.session?.let { add(it.id) }
            addAll(state.historySessions.map { it.id })
        }.distinct()
        if (sessions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                sessions.take(10).forEach { id ->
                    FilterChip(
                        selected = state.reportSessionId == id,
                        onClick = { viewModel.loadReport(id) },
                        label = { Text("Sesión #$id") },
                    )
                }
            }
        }
        if (state.reportLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        } else {
            state.report?.let { report ->
                ReportContent(report, state.reportProducts, currency)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedButton(
                        onClick = {
                            val text = formatSessionReportText(report, currency)
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Reporte caja #${report.session.id}")
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    },
                                    "Compartir reporte",
                                ),
                            )
                        },
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Text("Compartir", modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(onClick = { viewModel.exportSessionReportPdf(context) }) {
                        Text("Exportar PDF")
                    }
                }
            } ?: Text("Selecciona una sesión para ver el reporte", color = BendeyColors.OnSurfaceVariant)
        }
    }
}

@Composable
private fun ReportContent(
    report: CashSessionReport,
    products: List<com.bendey.restaurant.core.domain.cash.CashSessionProductSold>,
    currency: NumberFormat,
) {
    val session = report.session
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(BendeyColors.Surface, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Reporte sesión #${session.id}", fontWeight = FontWeight.Bold)
        session.branchName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        session.openedAt?.let { Text("Apertura: $it", style = MaterialTheme.typography.bodySmall) }
        session.closedAt?.let { Text("Cierre: $it", style = MaterialTheme.typography.bodySmall) }
        ReportRow("Apertura", currency.format(session.openingBalance))
        ReportRow("Ingresos", currency.format(report.totalIncome))
        ReportRow("Egresos", currency.format(report.totalExpense))
        ReportRow("Ventas netas", currency.format(report.totalNetSales))
        if (report.totalVoidedSales > 0) {
            ReportRow("Ventas anuladas", currency.format(report.totalVoidedSales))
        }
        ReportRow("Saldo final", currency.format(report.finalBalance), bold = true)
        if (report.salesByMethod.isNotEmpty()) {
            Text("Ventas por método", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            report.salesByMethod.forEach { row ->
                ReportRow(row.method, currency.format(row.total))
            }
        }
        if (report.nonCashSalesByMethod.isNotEmpty()) {
            Text("Ventas no efectivo", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            report.nonCashSalesByMethod.forEach { row ->
                ReportRow(row.method, currency.format(row.total))
            }
        }
        if (report.incomeDetail.isNotEmpty()) {
            Text("Ingresos detalle", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            report.incomeDetail.take(15).forEach { row ->
                Text("${row.docNumber.ifBlank { row.type }} · ${currency.format(row.amount)}", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (report.expenseDetail.isNotEmpty()) {
            Text("Egresos detalle", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            report.expenseDetail.take(15).forEach { row ->
                Text("${row.reference.ifBlank { row.type }} · ${currency.format(row.amount)}", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (report.cancelledSalesDetail.isNotEmpty()) {
            Text("Ventas anuladas", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            report.cancelledSalesDetail.take(15).forEach { row ->
                Text("${row.docNumber} · ${row.paymentMethod} · ${currency.format(row.amount)}", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (products.isNotEmpty()) {
            Text("Productos vendidos", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            products.forEach { product ->
                Text(
                    "${product.quantity}× ${product.description} · ${currency.format(product.total)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = BendeyColors.Primary)
    }
}

@Composable
private fun HistoryTab(state: CajaUiState, currency: NumberFormat, viewModel: CajaViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.historySessions.isEmpty()) {
            item { Text("Sin historial de sesiones", color = BendeyColors.OnSurfaceVariant) }
        }
        items(state.historySessions, key = { it.id }) { session ->
            HistorySessionCard(session, currency, onOpenReport = { viewModel.loadReport(session.id) })
        }
    }
}

@Composable
private fun HistorySessionCard(
    session: CashSessionBrief,
    currency: NumberFormat,
    onOpenReport: () -> Unit,
) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Sesión #${session.id}", fontWeight = FontWeight.Bold)
                BendeyStatusChip(
                    label = if (session.status == CashSessionStatus.OPEN) "Abierta" else "Cerrada",
                    accentColor = if (session.status == CashSessionStatus.OPEN) BendeyColors.Success else BendeyColors.OnSurfaceVariant,
                )
            }
            session.openedAt?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant) }
            session.openedByName?.let { Text("Operador: $it", style = MaterialTheme.typography.bodySmall) }
            Text("Apertura: ${currency.format(session.openingBalance)}", style = MaterialTheme.typography.bodySmall)
            session.closingBalance?.let { Text("Cierre: ${currency.format(it)}", style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(onClick = onOpenReport, modifier = Modifier.padding(top = 8.dp)) { Text("Ver reporte") }
        }
    }
}

@Composable
private fun ClosedCashCard(onOpen: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Caja cerrada", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Debes abrir caja para registrar movimientos", color = BendeyColors.OnSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        BendeyPrimaryButton("Abrir caja", onOpen)
    }
}

@Composable
private fun MovementCard(movement: CashMovement, currency: NumberFormat) {
    val isIncome = movement.type == CashMovementType.INCOME
    val accent = if (isIncome) BendeyColors.Success else BendeyColors.Error
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(cashMovementCategoryLabel(movement.category), fontWeight = FontWeight.Medium)
                movement.reference.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                }
                movement.createdAt?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
                }
            }
            Text("${if (isIncome) "+" else "-"}${currency.format(movement.amount)}", color = accent, fontWeight = FontWeight.Bold)
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
                BendeyTextField(form.openingBalance, { v -> onFormChange { it.copy(openingBalance = v) } }, "Monto de apertura (S/)")
                BendeyTextField(form.notes, { v -> onFormChange { it.copy(notes = v) } }, "Notas (opcional)", singleLine = false)
            }
        },
        confirmButton = { BendeyPrimaryButton(if (loading) "Abriendo…" else "Abrir caja", onConfirm, enabled = !loading) },
        dismissButton = { if (!mandatory) TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun MovementDialog(
    form: MovementForm,
    paymentMethods: List<CashPaymentMethod>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onFormChange: ((MovementForm) -> MovementForm) -> Unit,
) {
    val categories = if (form.type == CashMovementType.INCOME) incomeMovementCategories else expenseMovementCategories
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (form.type == CashMovementType.INCOME) "Registrar ingreso" else "Registrar egreso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = form.category == cat.value,
                            onClick = { onFormChange { it.copy(category = cat.value) } },
                            label = { Text(cat.label) },
                        )
                    }
                }
                BendeyTextField(form.amount, { v -> onFormChange { it.copy(amount = v) } }, "Monto (S/)")
                if (paymentMethods.isNotEmpty()) {
                    Text("Método de pago", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        paymentMethods.filter { it.active }.forEach { pm ->
                            FilterChip(
                                selected = form.paymentMethod == pm.code,
                                onClick = { onFormChange { it.copy(paymentMethod = pm.code) } },
                                label = { Text(pm.name) },
                            )
                        }
                    }
                }
                BendeyTextField(form.reference, { v -> onFormChange { it.copy(reference = v) } }, "Referencia")
                BendeyTextField(form.notes, { v -> onFormChange { it.copy(notes = v) } }, "Notas", singleLine = false)
            }
        },
        confirmButton = { BendeyPrimaryButton(if (loading) "Guardando…" else "Guardar", onConfirm, enabled = !loading) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ConfigTab(state: CajaUiState, currency: NumberFormat, viewModel: CajaViewModel) {
    if (state.configLoading) {
        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Cuentas bancarias", fontWeight = FontWeight.Bold)
                if (state.canManageCashSettings) {
                    BendeyPrimaryButton("+ Cuenta", viewModel::showCreateBankAccount)
                }
            }
        }
        if (state.bankAccounts.isEmpty()) {
            item { Text("Sin cuentas registradas", color = BendeyColors.OnSurfaceVariant) }
        }
        items(state.bankAccounts, key = { it.id }) { acc ->
            BankAccountCard(
                acc,
                currency,
                canManage = state.canManageCashSettings,
                onEdit = { viewModel.showEditBankAccount(acc) },
                onMovements = { viewModel.showBankMovements(acc) },
            )
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Métodos de pago", fontWeight = FontWeight.Bold)
                if (state.canManageCashSettings) {
                    BendeyPrimaryButton("+ Método", viewModel::showCreatePaymentMethod)
                }
            }
        }
        if (state.paymentMethods.isEmpty()) {
            item { Text("Sin métodos de pago", color = BendeyColors.OnSurfaceVariant) }
        }
        items(state.paymentMethods, key = { it.id }) { pm ->
            PaymentMethodCard(
                pm,
                state.bankAccounts,
                canManage = state.canManageCashSettings,
                onEdit = { viewModel.showEditPaymentMethod(pm) },
                onDelete = { viewModel.deletePaymentMethod(pm.id) },
            )
        }
    }
}

@Composable
private fun BankAccountCard(
    acc: CashBankAccount,
    currency: NumberFormat,
    canManage: Boolean,
    onEdit: () -> Unit,
    onMovements: () -> Unit,
) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(acc.name, fontWeight = FontWeight.Bold)
                BendeyStatusChip(
                    label = if (acc.active) "Activa" else "Inactiva",
                    accentColor = if (acc.active) BendeyColors.Success else BendeyColors.OnSurfaceVariant,
                )
            }
            listOf(acc.bankName, acc.accountNumber).filter { it.isNotBlank() }.joinToString(" · ").takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            }
            Text("Saldo: ${currency.format(acc.balance)}", style = MaterialTheme.typography.bodySmall)
            if (canManage) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(onClick = onEdit) { Text("Editar") }
                    OutlinedButton(onClick = onMovements) { Text("Movimientos") }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    pm: CashPaymentMethod,
    bankAccounts: List<CashBankAccount>,
    canManage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val destLabel = if (pm.destinationType == "bank_account") {
        bankAccounts.find { it.id == pm.bankAccountId }?.name ?: "Cuenta"
    } else {
        "Caja"
    }
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(pm.name, fontWeight = FontWeight.Bold)
                BendeyStatusChip(
                    label = if (pm.active) "Activo" else "Inactivo",
                    accentColor = if (pm.active) BendeyColors.Success else BendeyColors.OnSurfaceVariant,
                )
            }
            Text("Código: ${pm.code}", style = MaterialTheme.typography.bodySmall)
            Text("Destino: $destLabel", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            if (canManage) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(onClick = onEdit) { Text("Editar") }
                    OutlinedButton(onClick = onDelete) { Text("Eliminar") }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodDialog(
    form: PaymentMethodForm,
    bankAccounts: List<CashBankAccount>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onFormChange: ((PaymentMethodForm) -> PaymentMethodForm) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (form.id != null) "Editar método" else "Nuevo método de pago") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BendeyTextField(form.name, { v -> onFormChange { it.copy(name = v) } }, "Nombre")
                BendeyTextField(form.code, { v -> onFormChange { it.copy(code = v) } }, "Código")
                Text("Destino", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = form.destinationType == "cash", onClick = { onFormChange { it.copy(destinationType = "cash", bankAccountId = null) } }, label = { Text("Caja") })
                    FilterChip(selected = form.destinationType == "bank_account", onClick = { onFormChange { it.copy(destinationType = "bank_account") } }, label = { Text("Cuenta") })
                }
                if (form.destinationType == "bank_account") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bankAccounts.forEach { acc ->
                            FilterChip(
                                selected = form.bankAccountId == acc.id,
                                onClick = { onFormChange { it.copy(bankAccountId = acc.id) } },
                                label = { Text(acc.name) },
                            )
                        }
                    }
                }
                if (form.id != null) {
                    FilterChip(
                        selected = form.active,
                        onClick = { onFormChange { it.copy(active = !it.active) } },
                        label = { Text(if (form.active) "Activo" else "Inactivo") },
                    )
                }
            }
        },
        confirmButton = { BendeyPrimaryButton(if (loading) "Guardando…" else "Guardar", onConfirm, enabled = !loading) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun BankAccountDialog(
    form: BankAccountForm,
    paymentMethods: List<CashPaymentMethod>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onFormChange: ((BankAccountForm) -> BankAccountForm) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (form.id != null) "Editar cuenta" else "Nueva cuenta bancaria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BendeyTextField(form.name, { v -> onFormChange { it.copy(name = v) } }, "Nombre")
                BendeyTextField(form.bankName, { v -> onFormChange { it.copy(bankName = v) } }, "Banco")
                BendeyTextField(form.accountNumber, { v -> onFormChange { it.copy(accountNumber = v) } }, "Número de cuenta")
                if (form.id == null) {
                    BendeyTextField(form.initialBalance, { v -> onFormChange { it.copy(initialBalance = v) } }, "Saldo inicial")
                }
                if (paymentMethods.isNotEmpty()) {
                    Text("Método vinculado", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        paymentMethods.filter { it.active }.forEach { pm ->
                            FilterChip(
                                selected = form.paymentMethod == pm.code,
                                onClick = { onFormChange { it.copy(paymentMethod = pm.code) } },
                                label = { Text(pm.name) },
                            )
                        }
                    }
                }
                if (form.id != null) {
                    FilterChip(
                        selected = form.active,
                        onClick = { onFormChange { it.copy(active = !it.active) } },
                        label = { Text(if (form.active) "Activa" else "Inactiva") },
                    )
                }
            }
        },
        confirmButton = { BendeyPrimaryButton(if (loading) "Guardando…" else "Guardar", onConfirm, enabled = !loading) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun BankMovementsDialog(
    accountName: String?,
    movements: List<CashBankMovement>,
    form: BankMovementForm,
    loading: Boolean,
    currency: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onFormChange: ((BankMovementForm) -> BankMovementForm) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Movimientos · ${accountName.orEmpty()}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = form.type == "credit", onClick = { onFormChange { it.copy(type = "credit") } }, label = { Text("Ingreso") })
                    FilterChip(selected = form.type == "debit", onClick = { onFormChange { it.copy(type = "debit") } }, label = { Text("Egreso") })
                }
                BendeyTextField(form.description, { v -> onFormChange { it.copy(description = v) } }, "Descripción")
                BendeyTextField(form.amount, { v -> onFormChange { it.copy(amount = v) } }, "Monto")
                BendeyTextField(form.date, { v -> onFormChange { it.copy(date = v) } }, "Fecha (AAAA-MM-DD)")
                BendeyTextField(form.reference, { v -> onFormChange { it.copy(reference = v) } }, "Referencia")
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                if (movements.isEmpty()) {
                    Text("Sin movimientos", color = BendeyColors.OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else {
                    movements.take(8).forEach { mov ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(mov.description, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(mov.date, style = MaterialTheme.typography.labelSmall, color = BendeyColors.OnSurfaceVariant)
                            }
                            Text(
                                "${if (mov.type == "credit") "+" else "-"}${currency.format(mov.amount)}",
                                color = if (mov.type == "credit") BendeyColors.Success else BendeyColors.Error,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { BendeyPrimaryButton(if (loading) "Guardando…" else "Registrar", onConfirm, enabled = !loading) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}
