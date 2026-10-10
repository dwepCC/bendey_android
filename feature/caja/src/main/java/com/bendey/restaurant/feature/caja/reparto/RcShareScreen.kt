package com.bendey.restaurant.feature.caja.reparto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.delivery.ReportRangeRules
import com.bendey.restaurant.core.domain.rcshare.RcDistribution
import com.bendey.restaurant.core.domain.rcshare.RcPeriodPreset
import com.bendey.restaurant.core.domain.rcshare.RcShareRules
import com.bendey.restaurant.core.domain.rcshare.RcShareText
import com.bendey.restaurant.core.domain.rcshare.RcShareUiState
import com.bendey.restaurant.core.domain.time.PeruDateTime
import com.bendey.restaurant.core.ui.components.BendeyAlert
import com.bendey.restaurant.core.ui.components.BendeyAlertSeverity
import com.bendey.restaurant.core.ui.components.BendeyDateField
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyLoadError
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySkeletonListRow
import com.bendey.restaurant.core.ui.components.BendeySnackMessage
import com.bendey.restaurant.core.ui.components.BendeySwitchRow
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.layout.rememberBendeyLazyListContentPadding
import com.bendey.restaurant.feature.caja.propinas.LineRow
import com.bendey.restaurant.feature.caja.propinas.Muted
import com.bendey.restaurant.feature.caja.propinas.SectionCard
import com.bendey.restaurant.feature.caja.propinas.StatCard

/**
 * Pantalla «Reparto del recargo al consumo». Ver: c.v o s.m. Repartir: solo s.m.
 * No mueve el RC de las ventas (ya va en el comprobante); deja constancia de cuánto recibió cada trabajador.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RcShareScreen(
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RcShareViewModel = hiltViewModel(),
) {
    val st by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.start() }
    BendeySnackMessage(message = st.snack, onShow = onShowMessage, onConsume = viewModel::consumeSnack)
    val summary = st.summary

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.s16, top = BendeySpacing.s8),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
    ) {
        item {
            Text(
                "Reparto del recargo al consumo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BendeyColors.OnSurface,
            )
        }
        item { BendeyAlert(message = RcShareText.HELP, severity = BendeyAlertSeverity.Info) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4),
                ) {
                    RcPeriodPreset.entries.forEach { p ->
                        BendeyFilterChip(selected = st.preset == p, onClick = { viewModel.selectPreset(p) }, text = p.label)
                    }
                }
                if (st.preset == RcPeriodPreset.CUSTOM) {
                    BendeyDateField(value = st.fromText, onValueChange = viewModel::setFrom, label = "Desde")
                    BendeyDateField(value = st.toText, onValueChange = viewModel::setTo, label = "Hasta")
                    Muted("Máximo ${RcShareRules.MAX_DAYS} días, sin fechas futuras.")
                    BendeyOutlinedButton(text = "Consultar", onClick = viewModel::applyCustom, enabled = !st.loading)
                }
                st.shownRange?.let { r ->
                    Muted("Del ${ReportRangeRules.display(r.from)} al ${ReportRangeRules.display(r.to)}")
                }
            }
        }
        when {
            st.loading && summary == null -> item {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) { repeat(3) { BendeySkeletonListRow() } }
            }
            st.error != null && summary == null -> item { BendeyLoadError(onRetry = viewModel::refresh, message = st.error) }
            summary != null -> {
                st.error?.let { msg -> item { BendeyAlert(message = msg, severity = BendeyAlertSeverity.Danger) } }
                if (RcShareRules.isOverdue(summary, ReportRangeRules.todayLima())) {
                    item {
                        BendeyAlert(
                            title = "RC sin repartir",
                            message = RcShareText.overdueNotice(summary.pending),
                            severity = BendeyAlertSeverity.Warning,
                        )
                    }
                }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
                        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
                    ) {
                        StatCard("Cobrado", RcShareText.money(summary.collected), Modifier.fillMaxWidth(0.48f))
                        StatCard("Repartido", RcShareText.money(summary.distributed), Modifier.fillMaxWidth(0.48f))
                        StatCard("Por repartir", RcShareText.money(summary.pending), Modifier.fillMaxWidth(0.48f))
                        StatCard("Porcentaje promedio", RcShareText.percent(summary.avgRate), Modifier.fillMaxWidth(0.48f))
                        StatCard("Ventas con recargo", RcShareText.salesCount(summary.salesCount), Modifier.fillMaxWidth(0.48f))
                    }
                }
                if (summary.collected <= 0.0) {
                    item {
                        BendeyEmptyState(
                            title = "Sin recargo al consumo en este período",
                            description = "No hay ventas con recargo al consumo en las fechas elegidas.",
                        )
                    }
                }
                if (st.canDistribute && summary.pending > 0.009) {
                    item { BendeyPrimaryButton(text = "Registrar reparto", onClick = viewModel::openDistribute, fillWidth = true) }
                } else if (!st.canDistribute && summary.pending > 0.009) {
                    item { Muted("Solo quien administra el negocio puede registrar el reparto.") }
                }
                item { Text("Repartos registrados", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface) }
                if (summary.history.isEmpty()) item { Muted("Aún no se registró ningún reparto para este período.") }
                summary.history.forEach { h -> item(key = "h${h.id}") { HistoryCard(h) } }
            }
        }
    }

    if (st.sheetOpen) DistributeDialog(st, viewModel)
}

@Composable
private fun HistoryCard(h: RcDistribution) {
    SectionCard(RcShareText.money(h.amount)) {
        val created = PeruDateTime.formatDateTime(h.createdAt)
        Muted(
            "Período ${PeruDateTime.formatDate(h.periodFrom) ?: h.periodFrom} al ${PeruDateTime.formatDate(h.periodTo) ?: h.periodTo}" +
                (created?.let { " · registrado $it" } ?: ""),
        )
        Muted(if (h.paidFromCash) "Pagado en efectivo desde caja" else "Solo constancia (pagado por otro medio)")
        h.lines.forEach { l -> LineRow(l.name, RcShareText.money(l.amount)) }
        if (h.notes.isNotBlank()) Muted("Notas: ${h.notes}")
    }
}

@Composable
private fun DistributeDialog(st: RcShareUiState, vm: RcShareViewModel) {
    BendeyFormDialog(
        onDismissRequest = vm::closeDistribute,
        title = "Registrar reparto",
        confirmText = "Registrar reparto",
        confirmEnabled = st.canSubmit,
        loading = st.submitting,
        enableContentScroll = true,
        validationError = st.submitError,
        onConfirm = vm::submit,
        onDismiss = vm::closeDistribute,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
            Muted("Indica cuánto recibió cada trabajador por el recargo al consumo de este período.")
            if (st.staffLoading) Muted("Cargando personal…")
            if (!st.staffLoading && st.staff.isEmpty()) Muted("No hay personal registrado. Agrégalo en Configuración.")
            st.staff.forEach { person ->
                val line = st.draft.firstOrNull { it.staffId == person.id }
                BendeyTextField(
                    value = line?.amount.orEmpty(),
                    onValueChange = { vm.setAmount(person.id, it) },
                    label = "${person.name} (S/)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            BendeyOutlinedButton(
                text = "Dividir en partes iguales lo que falta",
                onClick = vm::splitRemainder,
                enabled = st.staff.isNotEmpty(),
                fillWidth = true,
            )
            TotalsBox(st)
            BendeyTextField(
                value = st.notes,
                onValueChange = vm::setNotes,
                label = "Notas (opcional)",
                modifier = Modifier.fillMaxWidth(),
            )
            BendeySwitchRow(
                label = "Pagar en efectivo desde mi caja",
                checked = st.payFromCash,
                onCheckedChange = vm::setPayFromCash,
            )
            Muted(
                if (st.payFromCash) "El efectivo sale de tu caja abierta como «Reparto de recargo al consumo»."
                else "Solo queda la constancia (por ejemplo, si lo pagaste por planilla u otro medio).",
            )
        }
    }
}

/** Total en vivo contra lo pendiente; si se pasa se avisa con texto e ícono de alerta, no solo con color. */
@Composable
private fun TotalsBox(st: RcShareUiState) {
    BendeyManagementCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total a repartir", style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurface)
                Text(
                    "${RcShareText.money(st.draftTotal)} de ${RcShareText.money(st.pending)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurface,
                )
            }
            if (st.overLimit) {
                BendeyAlert(
                    message = "Te pasas por ${RcShareText.money(-st.leftover)}: el reparto no puede superar lo que queda por repartir.",
                    severity = BendeyAlertSeverity.Danger,
                )
            } else {
                Muted("Falta asignar ${RcShareText.money(st.leftover)}.")
            }
        }
    }
}
