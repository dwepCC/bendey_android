package com.bendey.restaurant.feature.caja.propinas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.delivery.RangedReportState
import com.bendey.restaurant.core.domain.tips.MyTips
import com.bendey.restaurant.core.domain.tips.PendingTipStaff
import com.bendey.restaurant.core.domain.tips.PropinasTab
import com.bendey.restaurant.core.domain.tips.TipsReport
import com.bendey.restaurant.core.domain.tips.TipsText
import com.bendey.restaurant.core.domain.time.PeruDateTime
import com.bendey.restaurant.core.ui.components.BendeyAlert
import com.bendey.restaurant.core.ui.components.BendeyAlertSeverity
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyLoadError
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySkeletonListRow
import com.bendey.restaurant.core.ui.components.BendeySnackMessage
import com.bendey.restaurant.core.ui.layout.rememberBendeyLazyListContentPadding

/**
 * Pantalla «Propinas»: por pagar (o.ch / s.m), reporte (c.v / s.m) y «Mis propinas» (todos).
 * La propina es voluntaria: no va en el comprobante ni suma al total de la venta.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropinasScreen(
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PropinasViewModel = hiltViewModel(),
) {
    val st by viewModel.state.collectAsStateWithLifecycle()
    val rep by viewModel.report.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.start() }
    BendeySnackMessage(message = st.snack, onShow = onShowMessage, onConsume = viewModel::consumeSnack)

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "Propinas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = BendeySpacing.s16, vertical = BendeySpacing.s8),
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = BendeySpacing.s16),
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
        ) {
            st.tabs.forEach { t ->
                BendeyFilterChip(selected = st.tab == t, onClick = { viewModel.selectTab(t) }, text = t.label)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.s16, top = BendeySpacing.s8),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
        ) {
            item {
                Muted("La propina es voluntaria: no va en el comprobante ni suma al total de la venta. Es plata del personal.")
            }
            when (st.tab) {
                PropinasTab.POR_PAGAR -> porPagar(st, viewModel::refresh, viewModel::askPayout)
                PropinasTab.REPORTE -> reporte(rep, viewModel)
                PropinasTab.MIS_PROPINAS -> misPropinas(st.mine, st.mineLoading, st.mineError, viewModel::refresh)
            }
        }
    }

    st.confirm?.let { row ->
        BendeyFormDialog(
            onDismissRequest = viewModel::dismissPayout,
            title = "Pagar propinas",
            confirmText = "Pagar propinas",
            loading = st.paying,
            validationError = st.payError,
            onConfirm = viewModel::confirmPayout,
            onDismiss = viewModel::dismissPayout,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                Text(
                    TipsText.money(row.total),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.OnSurface,
                )
                Text(TipsText.payoutConfirm(row), style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurface)
                if (row.unbooked > 0) {
                    Muted("${row.unbooked} de estas propinas no tienen movimiento de caja anotado.")
                }
            }
        }
    }
}

private fun LazyListScope.porPagar(st: PropinasUiState, onRetry: () -> Unit, onPay: (PendingTipStaff) -> Unit) {
    val rows = st.pending
    when {
        st.pendingLoading && rows == null -> item {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) { repeat(3) { BendeySkeletonListRow() } }
        }
        st.pendingError != null && rows == null -> item { BendeyLoadError(onRetry = onRetry, message = st.pendingError) }
        rows != null -> {
            if (rows.isEmpty()) {
                item {
                    BendeyEmptyState(
                        title = "No hay propinas por pagar",
                        description = "Cuando un cliente deje propina al cobrar, aparecerá aquí para entregarla al mozo.",
                    )
                }
            } else {
                item {
                    StatCard("Total por pagar", TipsText.money(TipsText.totalPending(rows)), Modifier.fillMaxWidth())
                }
                items(rows, key = { it.staffId }) { row -> PendingCard(row, enabled = !st.paying, onPay = { onPay(row) }) }
            }
        }
    }
}

@Composable
private fun PendingCard(row: PendingTipStaff, enabled: Boolean, onPay: () -> Unit) {
    BendeyManagementCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    TipsText.staffName(row),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    TipsText.money(row.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BendeyColors.OnSurface,
                )
            }
            Muted(TipsText.count(row.count))
            if (row.staffId == 0) Muted("Estas propinas se anotaron sin mozo; se pagan a quien las entregue.")
            BendeyPrimaryButton(text = "Pagar propinas", onClick = onPay, enabled = enabled, fillWidth = true)
        }
    }
}

private fun LazyListScope.reporte(rep: RangedReportState<TipsReport>, vm: PropinasViewModel) {
    val report = rep.data
    val loading = rep.loading
    val error = rep.error
    item {
        RangePicker(
            state = rep,
            onPreset = vm.report::selectPreset,
            onFrom = vm.report::setFrom,
            onTo = vm.report::setTo,
            onApplyCustom = vm.report::applyCustom,
        )
    }
    when {
        loading && report == null -> item {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) { repeat(3) { BendeySkeletonListRow() } }
        }
        error != null && report == null -> item { BendeyLoadError(onRetry = vm.report::refresh, message = error) }
        else -> {
            if (error != null) item { BendeyAlert(message = error, severity = BendeyAlertSeverity.Danger) }
            if (report != null) reportBody(report, loading)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.reportBody(r: TipsReport, refreshing: Boolean) {
    if (refreshing) item { Muted("Actualizando…") }
    if (r.count == 0) {
        item {
            BendeyEmptyState(
                title = "Sin propinas en este rango",
                description = "No se anotaron propinas en las fechas elegidas.",
            )
        }
        return
    }
    item {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
        ) {
            StatCard("Total", TipsText.money(r.total), Modifier.fillMaxWidth(0.48f))
            StatCard("Propinas", r.count.toString(), Modifier.fillMaxWidth(0.48f))
            StatCard("Pagadas", TipsText.money(r.paid), Modifier.fillMaxWidth(0.48f))
            StatCard("Por pagar", TipsText.money(r.pending), Modifier.fillMaxWidth(0.48f))
        }
    }
    item {
        SectionCard("Por mozo") {
            if (r.staff.isEmpty()) Muted("Sin datos")
            r.staff.forEach { s ->
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4)) {
                    Text(
                        s.name.ifBlank { if (s.staffId == 0) "Sin mozo asignado" else "Sin nombre" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = BendeyColors.OnSurface,
                    )
                    Muted("${TipsText.count(s.count)} · Total ${TipsText.money(s.total)}")
                    Muted("Pagado ${TipsText.money(s.paid)} · Por pagar ${TipsText.money(s.pending)}")
                }
            }
        }
    }
    item {
        SectionCard("Por medio") {
            if (r.methods.isEmpty()) Muted("Sin datos")
            r.methods.forEach { m -> LineRow(TipsText.methodLabel(m.method), "${TipsText.count(m.count)} · ${TipsText.money(m.total)}") }
        }
    }
}

private fun LazyListScope.misPropinas(mine: MyTips?, loading: Boolean, error: String?, onRetry: () -> Unit) {
    when {
        loading && mine == null -> item {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) { repeat(2) { BendeySkeletonListRow() } }
        }
        error != null && mine == null -> item { BendeyLoadError(onRetry = onRetry, message = error) }
        mine != null -> {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                    StatCard("Por cobrar", TipsText.money(mine.pending), Modifier.weight(1f))
                    StatCard("Ya recibido", TipsText.money(mine.paid), Modifier.weight(1f))
                }
            }
            item {
                SectionCard("Mis últimas propinas") {
                    if (mine.recent.isEmpty()) Muted("Aún no tienes propinas registradas.")
                    mine.recent.forEach { t ->
                        val date = PeruDateTime.formatDateTime(t.createdAt) ?: t.createdAt.take(10)
                        LineRow(
                            date,
                            "${TipsText.money(t.amount)} · ${TipsText.methodLabel(t.method)} · " + if (t.paid) "Pagada" else "Por cobrar",
                        )
                    }
                }
            }
        }
    }
}
