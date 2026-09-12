package com.bendey.restaurant.feature.caja

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import com.bendey.restaurant.core.ui.components.BendeyBottomSheet
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyWidthTier
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveInfo
import com.bendey.restaurant.core.ui.layout.rememberBendeyBottomBarScrollPadding
import com.bendey.restaurant.core.ui.layout.rememberBendeyLazyListContentPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyKpiCard
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.cash.CashBankAccount
import com.bendey.restaurant.core.domain.cash.CashBankMovement
import com.bendey.restaurant.core.domain.cash.CashMovement
import com.bendey.restaurant.core.domain.cash.CashMovementType
import com.bendey.restaurant.core.domain.cash.CashPaymentMethod
import com.bendey.restaurant.core.domain.cash.CashSessionBrief
import com.bendey.restaurant.core.domain.cash.CashSessionReport
import com.bendey.restaurant.core.domain.cash.CashSessionStatus
import com.bendey.restaurant.core.domain.sales.salePaymentMethodLabelEs
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow
import com.bendey.restaurant.core.ui.components.BendeyIconButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeySnackMessage
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.layout.BendeyListScreenLayout
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CajaScreen(
    modifier: Modifier = Modifier,
    onShowMessage: (String) -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    viewModel: CajaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    val context = LocalContext.current
    // En tablet (ancho Expanded, ~840dp+) Historial y Reporte se ven juntos en dos paneles, sin
    // saltar de pestaña para ver un cierre. En teléfono (Compact/Medium) sigue igual que siempre.
    val showHistoryReportTwoPane = rememberBendeyAdaptiveInfo().widthTier == BendeyWidthTier.Expanded

    BendeySnackMessage(
        message = state.snackMessage,
        onShow = onShowMessage,
        onConsume = viewModel::consumeSnackMessage,
    )

    BendeyListScreenLayout(
        modifier = modifier,
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        header = {
            BendeyScreenToolbar(
                title = "Caja",
                subtitle = state.branchName ?: state.session?.branchName,
                actions = {
                    BendeyIconButton(
                        onClick = viewModel::refresh,
                        icon = Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                    )
                },
            )
            BendeyHorizontalScrollRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = BendeySpacing.sm,
                    vertical = BendeySpacing.xxs,
                ),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                CajaTab.entries.filter { tab ->
                    tab != CajaTab.CONFIG || state.canViewCashSettings
                }.forEach { tab ->
                    BendeyFilterChip(
                        selected = state.tab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = tab.label,
                    )
                }
            }
        },
    ) { contentModifier ->
        when {
            state.session == null && state.tab == CajaTab.SESSION && !state.loading -> {
                ClosedCashCard(onOpen = viewModel::showOpenDialog, modifier = contentModifier)
            }
            state.tab == CajaTab.SESSION && state.session != null -> {
                SessionTab(state, currency, viewModel, contentModifier)
            }
            state.tab == CajaTab.MOVEMENTS -> {
                MovementsTab(state, currency, viewModel, context, onNavigateToSubscription, contentModifier)
            }
            showHistoryReportTwoPane && (state.tab == CajaTab.REPORT || state.tab == CajaTab.HISTORY) -> {
                HistoryReportTwoPane(state, currency, viewModel, context, onNavigateToSubscription, contentModifier)
            }
            state.tab == CajaTab.REPORT -> {
                ReportTab(state, currency, viewModel, context, onNavigateToSubscription, contentModifier)
            }
            state.tab == CajaTab.HISTORY -> {
                HistoryTab(state, currency, viewModel, contentModifier)
            }
            state.tab == CajaTab.CONFIG -> {
                ConfigTab(state, currency, viewModel, contentModifier)
            }
        }
        state.error?.let {
            Text(it, color = BendeyColors.Error, modifier = Modifier.padding(BendeySpacing.md))
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
        canPrint = state.canPrintArqueo,
        docBusy = state.arqueoDocBusy,
        onQtyChange = viewModel::setArqueoQty,
        onExportPdf = { viewModel.exportArqueoPdf(context) },
        onPrint = viewModel::printArqueo,
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
            salesSummary = state.closeSummary,
            salesSummaryLoading = state.closeSummaryLoading,
            onDismiss = viewModel::dismissCloseDialog,
            onConfirm = viewModel::requestCloseSession,
            onFormChange = viewModel::updateCloseForm,
            onArqueoQtyChange = viewModel::setCloseArqueoQty,
        )
    }
    if (state.showCloseForceConfirm) {
        BendeyAlertDialog(
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
                BendeyTextButton(text = "Cancelar", onClick = viewModel::dismissCloseForceConfirm)
            },
        )
    }
}

@Composable
private fun SessionTab(
    state: CajaUiState,
    currency: NumberFormat,
    viewModel: CajaViewModel,
    modifier: Modifier = Modifier,
) {
    val session = state.session ?: return
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(BendeySpacing.md),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = BendeySpacing.md),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            BendeyPrimaryButton("+ Ingreso", { viewModel.showMovementDialog(CashMovementType.INCOME) }, Modifier.weight(1f))
            BendeyPrimaryButton("- Egreso", { viewModel.showMovementDialog(CashMovementType.EXPENSE) }, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(BendeySpacing.md),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        ) {
            OutlinedButton(onClick = viewModel::showArqueoDialog, modifier = Modifier.weight(1f)) { Text("Arqueo") }
            if (state.canOpenCashDrawer) {
                OutlinedButton(
                    onClick = viewModel::openCashDrawer,
                    enabled = !state.openingDrawer,
                    modifier = Modifier.weight(1f),
                ) { Text(if (state.openingDrawer) "Abriendo…" else "Abrir gaveta") }
            }
            BendeyPrimaryButton(
                text = if (state.actionLoading) "Cerrando…" else "Cerrar caja",
                onClick = viewModel::showCloseDialog,
                enabled = !state.actionLoading,
                modifier = Modifier.weight(1f),
            )
        }
        session.openedByName?.let {
            Text("Operador: $it", modifier = Modifier.padding(horizontal = BendeySpacing.md), style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "Últimos movimientos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xs),
        )
        BendeyLazyColumn(state = rememberLazyListState(),
            contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.md, top = BendeySpacing.xs),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            modifier = Modifier.weight(1f),
        ) {
            items(state.movements.take(8), key = { it.id }) { movement ->
                MovementCard(movement, currency)
            }
        }
    }
}

@Composable
private fun MovementsTab(
    state: CajaUiState,
    currency: NumberFormat,
    viewModel: CajaViewModel,
    context: android.content.Context,
    onNavigateToSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BendeyLazyColumn(state = rememberLazyListState(),
        contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.md, top = BendeySpacing.md),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        modifier = modifier.fillMaxSize(),
    ) {
        if (state.session != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    BendeyPrimaryButton("+ Ingreso", { viewModel.showMovementDialog(CashMovementType.INCOME) }, Modifier.weight(1f))
                    BendeyPrimaryButton("- Egreso", { viewModel.showMovementDialog(CashMovementType.EXPENSE) }, Modifier.weight(1f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                BendeyPrimaryButton("Buscar", viewModel::searchMovementsReport, Modifier.weight(1f))
                if (state.allowsReportExport) {
                    OutlinedButton(
                        onClick = { viewModel.exportMovementsReport(context) },
                        enabled = !state.movementsExportBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.movementsExportBusy) "Exportando…" else "Exportar Excel")
                    }
                } else {
                    OutlinedButton(
                        onClick = onNavigateToSubscription,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Text("Excel (Pro)", modifier = Modifier.padding(start = BendeySpacing.xs))
                    }
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
            item { CircularProgressIndicator(modifier = Modifier.padding(BendeySpacing.sm)) }
        }
        item {
            Text("Movimientos efectivo", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = BendeySpacing.xs))
        }
        if (state.movementsReportRows.isEmpty() && !state.movementsReportLoading) {
            item { Text("Sin movimientos en el periodo", color = BendeyColors.OnSurfaceVariant) }
        }
        items(state.movementsReportRows, key = { it.movementId }) { row ->
            BendeyManagementCard {
                Column {
                    // Un movimiento anulado desde otra terminal sigue en la lista, pero su importe ya
                    // no cuenta para ningun total: sin la marca se lee como plata que entro o salio.
                    Text(
                        row.docNumber.ifBlank { row.category.orEmpty() } + if (row.estaAnulado) " · ANULADO" else "",
                        fontWeight = FontWeight.Medium,
                        color = if (row.estaAnulado) BendeyColors.OnSurfaceVariant else Color.Unspecified,
                    )
                    Text("${row.date} · ${row.userName} · ${row.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                    Text(
                        currency.format(row.amount),
                        fontWeight = FontWeight.Bold,
                        color = if (row.estaAnulado) BendeyColors.OnSurfaceVariant else BendeyColors.Primary,
                        textDecoration = if (row.estaAnulado) TextDecoration.LineThrough else null,
                    )
                }
            }
        }
        // MEDIOS ELECTRONICOS, CON SUS EGRESOS.
        //
        // Arriba, la mitad de efectivo muestra ingresos, egresos y neto. Esta solo listaba cobros: un
        // egreso pagado con Yape no aparecia en ningun total de esta pantalla. Un cliente registro el
        // mismo egreso dos veces porque el numero no le bajaba.
        val bank = state.bankMovementsSummary
        if (bank.sumIncome > 0.0 || bank.sumExpense > 0.0) {
            item {
                Text("Medios electrónicos", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = BendeySpacing.sm))
            }
            item {
                Text(
                    "Ingresos: ${currency.format(bank.sumIncome)} · " +
                        "Egresos: ${currency.format(bank.sumExpense)} · " +
                        "Neto: ${currency.format(bank.netMovement)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            items(bank.byMethod, key = { "metodo-${it.method}" }) { m ->
                BendeyManagementCard {
                    Column {
                        Text(m.method, fontWeight = FontWeight.Medium)
                        // El desglose va siempre que haya un egreso: sin el, un neto de 29 donde
                        // entraron 30 parece un error de redondeo en vez de un egreso de 1.
                        Text(
                            if (m.expense > 0.0) {
                                "+${currency.format(m.income)} − ${currency.format(m.expense)}"
                            } else {
                                "${m.incomeCount} cobro${if (m.incomeCount != 1) "s" else ""}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                        Text(currency.format(m.net), fontWeight = FontWeight.Bold)
                    }
                }
            }
            // LAS FILAS, NO SOLO EL TOTAL. Con el neto pero sin el detalle, el encargado ve bajar el
            // numero y no encuentra la linea que lo explica: el egreso de Yape no estaba en esta mitad
            // ni en la de efectivo. El cliente lo reporto asi.
            items(bank.rows, key = { "bank-${it.id}" }) { row ->
                BendeyManagementCard {
                    Column {
                        Text(
                            row.description.ifBlank { row.accountName },
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "${row.date} · ${if (row.isIncome) "Ingreso" else "Egreso"} · ${row.method}" +
                                if (row.userName.isNotBlank()) " · ${row.userName}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                        // El signo va en la CIFRA, no solo en el texto de arriba: un egreso que se lee
                        // "S/ 1.00" es indistinguible de un ingreso para quien mira solo el monto.
                        Text(
                            (if (row.isIncome) "" else "-") + currency.format(row.amount),
                            fontWeight = FontWeight.Bold,
                            color = if (row.isIncome) BendeyColors.Primary else BendeyColors.Error,
                        )
                    }
                }
            }
        }

        val electronic = state.paymentsReport?.detail?.filter { row ->
            row.method.trim().lowercase() !in setOf("efectivo", "cash", "contado")
        }.orEmpty()
        if (electronic.isNotEmpty()) {
            item {
                Text("Cobros electrónicos", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = BendeySpacing.sm))
            }
            items(electronic, key = { "${it.saleNumber}-${it.date}-${it.amount}" }) { row ->
                BendeyManagementCard {
                    Column {
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
    onNavigateToSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSessions = state.session != null || state.historySessions.isNotEmpty()

    Column(modifier = modifier.fillMaxSize().padding(BendeySpacing.md)) {
        if (hasSessions) {
            ReportSessionPickerField(
                state = state,
                onOpen = viewModel::showReportSessionPicker,
                modifier = Modifier.padding(bottom = BendeySpacing.sm),
            )
        }
        // CON `weight`, NO SIN EL. El reporte tiene su propio `verticalScroll`, pero dentro de esta
        // Column sin peso recibia altura SIN LIMITE: un contenedor con scroll y altura libre crece hasta
        // el tamano de su contenido y nunca desplaza nada — se ve el principio y el resto queda fuera de
        // la pantalla, sin forma de bajar. Con el peso toma la altura que sobra, que es finita, y ahi el
        // scroll empieza a funcionar.
        //
        // Se nota recien cuando el contenido pasa de una pantalla, y el bloque que lo delata es
        // "Productos vendidos": va ultimo y no tiene tope de filas.
        ReportDetailContent(state, currency, viewModel, context, onNavigateToSubscription, Modifier.weight(1f))
    }

    if (state.reportPickerOpen) {
        ReportSessionPickerSheet(
            state = state,
            onDismiss = viewModel::dismissReportSessionPicker,
            onQueryChange = viewModel::setReportPickerQuery,
            onSelect = viewModel::selectReportSession,
        )
    }
}

/** Contenido del reporte de una sesión — compartido por la pestaña Reporte y el panel de tablet. */
@Composable
private fun ReportDetailContent(
    state: CajaUiState,
    currency: NumberFormat,
    viewModel: CajaViewModel,
    context: android.content.Context,
    onNavigateToSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (state.reportLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(BendeySpacing.lg))
        } else {
            state.report?.let { report ->
                // TODO EL CONTENIDO EN UN SOLO SCROLL, botones incluidos.
                //
                // `ReportContent` tenía su propio `verticalScroll` y esta Column (sin scroll propio)
                // ponía la fila de Compartir/Exportar PDF COMO HERMANA, después de él. Con `weight`
                // acotando la altura, esa Column interna llenaba todo el espacio disponible con el
                // reporte y la fila de botones quedaba empujada fuera — nunca se veía, ni con scroll,
                // porque el scroll de adentro no la incluía y esta Column de afuera no tenía scroll
                // propio. Un cliente probó en celular real: ni los botones ni el final de la lista de
                // productos aparecían, tapados por el menú inferior.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = rememberBendeyBottomBarScrollPadding()),
                ) {
                    ReportContent(report, state.reportProducts, state.reportComboComponents, currency)
                    BendeyHorizontalScrollRow(
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
                        modifier = Modifier.padding(top = BendeySpacing.sm),
                    ) {
                        OutlinedButton(
                            onClick = {
                                val text = formatSessionReportText(report, currency, state.reportComboComponents)
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
                            Text("Compartir", modifier = Modifier.padding(start = BendeySpacing.xs))
                        }
                        // Tauri imprime este mismo reporte en la ticketera — acá faltaba el botón.
                        // Solo aparece con impresora directa configurada (ver printSessionReport).
                        if (state.canPrintSessionReport) {
                            OutlinedButton(
                                onClick = viewModel::printSessionReport,
                                enabled = !state.sessionReportPrintBusy,
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null)
                                Text(
                                    if (state.sessionReportPrintBusy) "Imprimiendo…" else "Imprimir",
                                    modifier = Modifier.padding(start = BendeySpacing.xs),
                                )
                            }
                        }
                        if (state.allowsReportExport) {
                            OutlinedButton(
                                onClick = { viewModel.exportSessionReportPdf(context) },
                                enabled = !state.sessionReportExportBusy,
                            ) {
                                Text(if (state.sessionReportExportBusy) "Exportando PDF…" else "Exportar PDF")
                            }
                        } else {
                            OutlinedButton(onClick = onNavigateToSubscription) {
                                Icon(Icons.Default.Lock, contentDescription = null)
                                Text("PDF (Pro)", modifier = Modifier.padding(start = BendeySpacing.xs))
                            }
                        }
                    }
                }
            } ?: Text("Selecciona una sesión para ver el reporte", color = BendeyColors.OnSurfaceVariant)
        }
    }
}

/** Fila que resume la sesión elegida y abre el buscador — reemplaza la fila de 10 chips como tope. */
@Composable
private fun ReportSessionPickerField(
    state: CajaUiState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedId = state.reportSessionId
    val selected = state.report?.session?.takeIf { it.id == selectedId }
        ?: state.historySessions.firstOrNull { it.id == selectedId }
    BendeyManagementCard(onClick = onOpen, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    selected?.let { "Sesión #${it.id}" } ?: "Elegir sesión",
                    fontWeight = FontWeight.Bold,
                )
                val subtitle = sessionMomentAndOperator(selected)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                }
            }
            Text("Cambiar", color = BendeyColors.Primary, fontWeight = FontWeight.Medium)
        }
    }
}

/** Hoja de búsqueda sin tope — reemplaza al selector de 10 chips del Reporte. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSessionPickerSheet(
    state: CajaUiState,
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelect: (Int) -> Unit,
) {
    val candidates = remember(state.session, state.historySessions) {
        buildList {
            state.session?.let { s ->
                add(
                    CashSessionBrief(
                        id = s.id,
                        branchName = s.branchName,
                        openedByName = s.openedByName,
                        openingBalance = s.openingBalance,
                        closingBalance = s.closingBalance,
                        expectedBalance = s.expectedBalance,
                        status = s.status,
                        openedAt = s.openedAt,
                        closedAt = s.closedAt,
                    ),
                )
            }
            addAll(state.historySessions)
        }.distinctBy { it.id }.sortedByDescending { it.openedAt.orEmpty() }
    }
    val filtered = remember(candidates, state.reportPickerQuery) {
        CajaHistoryFilters.apply(candidates, state.reportPickerQuery, HistoryDateFilter.ALL)
    }
    BendeyBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = BendeySpacing.md)) {
            Text("Elegir sesión", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            BendeyTextField(
                value = state.reportPickerQuery,
                onValueChange = onQueryChange,
                label = "Buscar",
                placeholder = "N.° de sesión u operador",
                modifier = Modifier.padding(top = BendeySpacing.sm, bottom = BendeySpacing.xs),
            )
            BendeyLazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "Sin resultados",
                            color = BendeyColors.OnSurfaceVariant,
                            modifier = Modifier.padding(vertical = BendeySpacing.sm),
                        )
                    }
                }
                items(filtered, key = { it.id }) { session ->
                    ReportPickerRow(
                        session = session,
                        selected = state.reportSessionId == session.id,
                        onClick = { onSelect(session.id) },
                    )
                }
            }
            Spacer(Modifier.height(BendeySpacing.md))
        }
    }
}

@Composable
private fun ReportPickerRow(
    session: CashSessionBrief,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(BendeyShapeTokens.sm)
            .then(if (selected) Modifier.background(BendeyColors.PrimaryContainer) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = BendeySpacing.xs, vertical = BendeySpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Sesión #${session.id}", fontWeight = FontWeight.SemiBold)
            val subtitle = sessionMomentAndOperator(session)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            }
        }
        BendeyStatusChip(
            label = if (session.status == CashSessionStatus.OPEN) "Abierta" else "Cerrada",
            accentColor = if (session.status == CashSessionStatus.OPEN) BendeyColors.Success else BendeyColors.OnSurfaceVariant,
        )
    }
}

/** "9 sep, 08:10 – 22:38 · María Torres" — o solo lo que haya disponible. */
private fun sessionMomentAndOperator(session: CashSessionBrief?): String? {
    if (session == null) return null
    val opened = formatSessionMoment(session.openedAt)
    val closed = formatSessionMoment(session.closedAt)
    val moment = when {
        opened != null && closed != null -> "$opened – $closed"
        opened != null -> "Abrió $opened"
        else -> null
    }
    return listOfNotNull(moment, session.openedByName?.takeIf { it.isNotBlank() }).joinToString(" · ").ifBlank { null }
}

/** Historial (izquierda) y Reporte (derecha) a la vez — solo en tablet ancho Expanded. */
@Composable
private fun HistoryReportTwoPane(
    state: CajaUiState,
    currency: NumberFormat,
    viewModel: CajaViewModel,
    context: android.content.Context,
    onNavigateToSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtered = remember(state.historySessions, state.historySearchQuery, state.historyDateFilter) {
        CajaHistoryFilters.apply(state.historySessions, state.historySearchQuery, state.historyDateFilter)
    }
    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .fillMaxHeight()
                .padding(BendeySpacing.sm),
        ) {
            BendeyTextField(
                value = state.historySearchQuery,
                onValueChange = viewModel::setHistorySearchQuery,
                label = "Buscar",
                placeholder = "N.° de sesión u operador",
            )
            BendeyHorizontalScrollRow(
                modifier = Modifier.padding(top = BendeySpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
            ) {
                HistoryDateFilter.entries.forEach { filter ->
                    BendeyFilterChip(
                        selected = state.historyDateFilter == filter,
                        onClick = { viewModel.setHistoryDateFilter(filter) },
                        text = filter.label,
                    )
                }
            }
            BendeyLazyColumn(
                state = rememberLazyListState(),
                contentPadding = rememberBendeyLazyListContentPadding(horizontal = 0.dp, top = BendeySpacing.sm),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
                modifier = Modifier.weight(1f),
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            if (state.historySessions.isEmpty()) "Sin historial de sesiones" else "Sin resultados para este filtro",
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                }
                items(filtered, key = { it.id }) { session ->
                    ReportPickerRow(
                        session = session,
                        selected = state.reportSessionId == session.id,
                        onClick = { viewModel.loadReport(session.id) },
                    )
                }
            }
        }
        VerticalDivider()
        ReportDetailContent(
            state = state,
            currency = currency,
            viewModel = viewModel,
            context = context,
            onNavigateToSubscription = onNavigateToSubscription,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(BendeySpacing.md),
        )
    }
}

@Composable
private fun ReportContent(
    report: CashSessionReport,
    products: List<com.bendey.restaurant.core.domain.cash.CashSessionProductSold>,
    comboComponents: List<com.bendey.restaurant.core.domain.cash.CashSessionComboComponent>,
    currency: NumberFormat,
) {
    val session = report.session
    // SIN `verticalScroll` propio — el scroll vive en el contenedor de ReportDetailContent, que
    // también incluye los botones de Compartir/Exportar PDF. Si este Column vuelve a scrollear
    // por su cuenta, esos botones quedan afuera del scroll otra vez (ver el comentario ahí).
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BendeyColors.Surface, BendeyShapeTokens.md)
            .padding(BendeySpacing.sm),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),
    ) {
        Text("Reporte sesión #${session.id}", fontWeight = FontWeight.Bold)
        session.branchName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        session.openedAt?.let { Text("Apertura: $it", style = MaterialTheme.typography.bodySmall) }
        session.closedAt?.let { Text("Cierre: $it", style = MaterialTheme.typography.bodySmall) }
        ReportRow("Ventas netas", currency.format(report.totalNetSales))
        if (report.totalVoidedSales > 0) {
            ReportRow("Ventas anuladas", currency.format(report.totalVoidedSales))
        }
        if (report.salesByMethod.isNotEmpty()) {
            Text("Ventas por método", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = BendeySpacing.xs))
            report.salesByMethod.forEach { row ->
                ReportRow(salePaymentMethodLabelEs(row.method), currency.format(row.total))
            }
        }
        if (report.nonCashSalesByMethod.isNotEmpty()) {
            Text("Ventas no efectivo", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = BendeySpacing.xs))
            report.nonCashSalesByMethod.forEach { row ->
                ReportRow(salePaymentMethodLabelEs(row.method), currency.format(row.total))
            }
        }
        // EFECTIVO EN CAJA, con su propio título y la cuenta completa a la vista.
        //
        // Antes eran filas sueltas ("Apertura", "Ingresos", "Egresos", "Saldo final") mezcladas con las
        // ventas, y ninguna decía que hablaban SOLO de efectivo. Un cliente leyó las ventas en efectivo
        // como el dinero de la gaveta y reportó su caja descuadrada: los dos números estaban bien, el
        // reporte no los enfrentaba. Faltaban además lo contado y la diferencia.
        Text("Efectivo en caja", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = BendeySpacing.xs))
        ReportRow("Saldo de apertura", currency.format(session.openingBalance))
        ReportRow("+ Ingresos en efectivo", currency.format(report.totalIncome))
        ReportRow("- Egresos en efectivo", currency.format(report.totalExpense))
        ReportRow("= Esperado en caja", currency.format(report.finalBalance), bold = true)
        session.closingBalance?.let { contado ->
            ReportRow("Contado al cerrar", currency.format(contado))
            val dif = contado - report.finalBalance
            // El signo se escribe siempre: un "0.00" pelado no distingue "cuadró" de "no se contó".
            ReportRow("Diferencia", (if (dif >= 0) "+" else "-") + currency.format(kotlin.math.abs(dif)), bold = true)
        }
        // EL FALTANTE NO PUEDE APARECER RECIEN AL CONTAR.
        //
        // Anular ya no saca plata sola: entregarle el dinero al cliente es un acto que alguien
        // registra. Si el cajero lo entrego y no lo registro, su caja espera un efectivo que ya no
        // esta. Va pegado a la diferencia a proposito: es la explicacion mas probable de ese numero.
        if (report.pendingRefunds.isNotEmpty()) {
            Text(
                "Ventas anuladas sin devolucion registrada",
                fontWeight = FontWeight.SemiBold,
                color = BendeyColors.Warning,
                modifier = Modifier.padding(top = BendeySpacing.xs),
            )
            Text(
                "Se cobraron en esta caja y despues se anularon, pero nadie registro que el dinero " +
                    "se devolviera. Si ya se entrego, registralo desde el comprobante; si no, el " +
                    "efectivo deberia seguir en la caja.",
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
            report.pendingRefunds.forEach { row ->
                ReportRow(row.docNumber, currency.format(row.amount))
            }
            ReportRow("Total pendiente", currency.format(report.totalPendingRefunds), bold = true)
        }
        if (report.nonCashByMethod.isNotEmpty()) {
            Text("Medios electrónicos (neto)", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = BendeySpacing.xs))
            Text(
                "Ventas − compras − egresos",
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
            report.nonCashByMethod.forEach { row ->
                ReportRow(salePaymentMethodLabelEs(row.method), currency.format(row.total))
            }
        }
        if (report.incomeDetail.isNotEmpty()) {
            Text("Ingresos detalle", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = BendeySpacing.xs))
            report.incomeDetail.take(15).forEach { row ->
                Text("${row.docNumber.ifBlank { row.type }} · ${currency.format(row.amount)}", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (report.expenseDetail.isNotEmpty()) {
            Text("Egresos detalle", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = BendeySpacing.xs))
            report.expenseDetail.take(15).forEach { row ->
                Text("${row.reference.ifBlank { row.type }} · ${currency.format(row.amount)}", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (report.cancelledSalesDetail.isNotEmpty()) {
            Text("Ventas anuladas", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = BendeySpacing.xs))
            report.cancelledSalesDetail.take(15).forEach { row ->
                Text("${row.docNumber} · ${row.paymentMethod} · ${currency.format(row.amount)}", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (products.isNotEmpty()) {
            Text("Productos vendidos", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = BendeySpacing.xs))
            products.forEach { product ->
                Text(
                    "${product.quantity}× ${product.description} · ${currency.format(product.total)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        // Los platos de combos van en su propia sección y sin importe: la plata ya está arriba, en la
        // línea del combo. Acá la pregunta es cuántos platos salieron de cocina, que es lo que el
        // reporte no podía responder.
        if (comboComponents.isNotEmpty()) {
            Text("Platos de combos", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = BendeySpacing.xs))
            Text(
                "Ya incluidos en el precio del combo",
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
            comboComponents.forEach { c ->
                Text("${c.quantity}× ${c.description}", style = MaterialTheme.typography.bodySmall)
            }
            ReportRow("Total platos de combos", formatCantidadCombo(comboComponents.sumOf { it.quantity }))
        }
    }
}

/** Cantidad sin ceros sobrantes: 2.0 → "2", 1.50 → "1.5". */
private fun formatCantidadCombo(q: Double): String =
    if (q % 1.0 == 0.0) q.toLong().toString() else q.toString().trimEnd('0').trimEnd('.')

@Composable
private fun ReportRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = BendeyColors.Primary)
    }
}

@Composable
private fun HistoryTab(
    state: CajaUiState,
    currency: NumberFormat,
    viewModel: CajaViewModel,
    modifier: Modifier = Modifier,
) {
    val filtered = remember(state.historySessions, state.historySearchQuery, state.historyDateFilter) {
        CajaHistoryFilters.apply(state.historySessions, state.historySearchQuery, state.historyDateFilter)
    }
    BendeyLazyColumn(state = rememberLazyListState(),
        contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.md, top = BendeySpacing.md),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
        modifier = modifier.fillMaxSize(),
    ) {
        if (state.historySessions.isNotEmpty()) {
            item {
                BendeyTextField(
                    value = state.historySearchQuery,
                    onValueChange = viewModel::setHistorySearchQuery,
                    label = "Buscar",
                    placeholder = "N.° de sesión u operador",
                )
            }
            item {
                BendeyHorizontalScrollRow(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                    HistoryDateFilter.entries.forEach { filter ->
                        BendeyFilterChip(
                            selected = state.historyDateFilter == filter,
                            onClick = { viewModel.setHistoryDateFilter(filter) },
                            text = filter.label,
                        )
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    if (state.historySessions.isEmpty()) "Sin historial de sesiones" else "Sin resultados para este filtro",
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
        }
        items(filtered, key = { it.id }) { session ->
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
    BendeyManagementCard {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Sesión #${session.id}", fontWeight = FontWeight.Bold)
                BendeyStatusChip(
                    label = if (session.status == CashSessionStatus.OPEN) "Abierta" else "Cerrada",
                    accentColor = if (session.status == CashSessionStatus.OPEN) BendeyColors.Success else BendeyColors.OnSurfaceVariant,
                )
            }
            val moment = sessionMomentAndOperator(session)
            if (!moment.isNullOrBlank()) {
                Text(moment, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            }
            Text("Apertura: ${currency.format(session.openingBalance)}", style = MaterialTheme.typography.bodySmall)
            session.closingBalance?.let { Text("Cierre: ${currency.format(it)}", style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(onClick = onOpenReport, modifier = Modifier.padding(top = BendeySpacing.xs)) {
                Text(if (session.status == CashSessionStatus.OPEN) "Ver estado" else "Ver cierre")
            }
        }
    }
}

@Composable
private fun ClosedCashCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(BendeySpacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Caja cerrada", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Debes abrir caja para registrar movimientos", color = BendeyColors.OnSurfaceVariant, modifier = Modifier.padding(top = BendeySpacing.xs, bottom = BendeySpacing.lg))
        BendeyPrimaryButton("Abrir caja", onOpen)
    }
}

@Composable
private fun MovementCard(movement: CashMovement, currency: NumberFormat) {
    val isIncome = movement.type == CashMovementType.INCOME
    val accent = if (isIncome) BendeyColors.Success else BendeyColors.Error
    BendeyManagementCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(cashMovementCategoryLabel(movement.category), fontWeight = FontWeight.Medium)
                movement.titular?.takeIf { it.isNotBlank() }?.let {
                    Text("Titular: $it", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                }
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
    BendeyAlertDialog(
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
        dismissButton = {
            if (!mandatory) {
                BendeyTextButton(text = "Cancelar", onClick = onDismiss)
            }
        },
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
    BendeyAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (form.type == CashMovementType.INCOME) "Registrar ingreso" else "Registrar egreso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BendeyHorizontalScrollRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        BendeyFilterChip(
                            selected = form.category == cat.value,
                            onClick = { onFormChange { it.copy(category = cat.value) } },
                            text = cat.label,
                        )
                    }
                }
                BendeyTextField(form.amount, { v -> onFormChange { it.copy(amount = v) } }, "Monto (S/)")
                if (paymentMethods.isNotEmpty()) {
                    Text("Método de pago", style = MaterialTheme.typography.labelMedium)
                    BendeyHorizontalScrollRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        paymentMethods.filter { it.active }.forEach { pm ->
                            BendeyFilterChip(
                                selected = form.paymentMethod == pm.code,
                                onClick = { onFormChange { it.copy(paymentMethod = pm.code) } },
                                text = pm.name,
                            )
                        }
                    }
                }
                BendeyTextField(
                    form.titular,
                    { v -> onFormChange { it.copy(titular = v) } },
                    if (form.type == CashMovementType.INCOME) "Titular (a nombre de quién ingresa)" else "Titular (a nombre de quién sale)",
                )
                BendeyTextField(form.reference, { v -> onFormChange { it.copy(reference = v) } }, "Referencia")
                BendeyTextField(form.notes, { v -> onFormChange { it.copy(notes = v) } }, "Notas", singleLine = false)
            }
        },
        confirmButton = { BendeyPrimaryButton(if (loading) "Guardando…" else "Guardar", onConfirm, enabled = !loading) },
        dismissButton = { BendeyTextButton(text = "Cancelar", onClick = onDismiss) },
    )
}

@Composable
private fun ConfigTab(
    state: CajaUiState,
    currency: NumberFormat,
    viewModel: CajaViewModel,
    modifier: Modifier = Modifier,
) {
    if (state.configLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    BendeyLazyColumn(state = rememberLazyListState(),
        contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.md, top = BendeySpacing.md),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        modifier = modifier.fillMaxSize(),
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
        items(state.bankAccounts, key = { "bank-account-${it.id}" }) { acc ->
            BankAccountCard(
                acc,
                currency,
                canManage = state.canManageCashSettings,
                onEdit = { viewModel.showEditBankAccount(acc) },
                onMovements = { viewModel.showBankMovements(acc) },
            )
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = BendeySpacing.xs), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Métodos de pago", fontWeight = FontWeight.Bold)
                if (state.canManageCashSettings) {
                    BendeyPrimaryButton("+ Método", viewModel::showCreatePaymentMethod)
                }
            }
        }
        if (state.paymentMethods.isEmpty()) {
            item { Text("Sin métodos de pago", color = BendeyColors.OnSurfaceVariant) }
        }
        items(state.paymentMethods, key = { "payment-method-${it.id}" }) { pm ->
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
    BendeyManagementCard {
        Column {
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
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs), modifier = Modifier.padding(top = BendeySpacing.xs)) {
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
    BendeyManagementCard {
        Column {
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
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs), modifier = Modifier.padding(top = BendeySpacing.xs)) {
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
    BendeyAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (form.id != null) "Editar método" else "Nuevo método de pago") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BendeyTextField(form.name, { v -> onFormChange { it.copy(name = v) } }, "Nombre")
                BendeyTextField(form.code, { v -> onFormChange { it.copy(code = v) } }, "Código")
                Text("Destino", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BendeyFilterChip(
                        selected = form.destinationType == "cash",
                        onClick = { onFormChange { it.copy(destinationType = "cash", bankAccountId = null) } },
                        text = "Caja",
                    )
                    BendeyFilterChip(
                        selected = form.destinationType == "bank_account",
                        onClick = { onFormChange { it.copy(destinationType = "bank_account") } },
                        text = "Cuenta",
                    )
                }
                if (form.destinationType == "bank_account") {
                    BendeyHorizontalScrollRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bankAccounts.forEach { acc ->
                            BendeyFilterChip(
                                selected = form.bankAccountId == acc.id,
                                onClick = { onFormChange { it.copy(bankAccountId = acc.id) } },
                                text = acc.name,
                            )
                        }
                    }
                }
                if (form.id != null) {
                    BendeyFilterChip(
                        selected = form.active,
                        onClick = { onFormChange { it.copy(active = !it.active) } },
                        text = if (form.active) "Activo" else "Inactivo",
                    )
                }
            }
        },
        confirmButton = { BendeyPrimaryButton(if (loading) "Guardando…" else "Guardar", onConfirm, enabled = !loading) },
        dismissButton = { BendeyTextButton(text = "Cancelar", onClick = onDismiss) },
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
    BendeyAlertDialog(
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
                    BendeyHorizontalScrollRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        paymentMethods.filter { it.active }.forEach { pm ->
                            BendeyFilterChip(
                                selected = form.paymentMethod == pm.code,
                                onClick = { onFormChange { it.copy(paymentMethod = pm.code) } },
                                text = pm.name,
                            )
                        }
                    }
                }
                if (form.id != null) {
                    BendeyFilterChip(
                        selected = form.active,
                        onClick = { onFormChange { it.copy(active = !it.active) } },
                        text = if (form.active) "Activa" else "Inactiva",
                    )
                }
            }
        },
        confirmButton = { BendeyPrimaryButton(if (loading) "Guardando…" else "Guardar", onConfirm, enabled = !loading) },
        dismissButton = { BendeyTextButton(text = "Cancelar", onClick = onDismiss) },
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
    BendeyAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Movimientos · ${accountName.orEmpty()}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BendeyFilterChip(
                        selected = form.type == "credit",
                        onClick = { onFormChange { it.copy(type = "credit") } },
                        text = "Ingreso",
                    )
                    BendeyFilterChip(
                        selected = form.type == "debit",
                        onClick = { onFormChange { it.copy(type = "debit") } },
                        text = "Egreso",
                    )
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
        dismissButton = { BendeyTextButton(text = "Cerrar", onClick = onDismiss) },
    )
}
