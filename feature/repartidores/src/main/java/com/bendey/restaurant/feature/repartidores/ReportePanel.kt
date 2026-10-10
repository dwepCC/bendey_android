package com.bendey.restaurant.feature.repartidores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.delivery.DeliveryReport
import com.bendey.restaurant.core.domain.delivery.DeliveryReportText
import com.bendey.restaurant.core.domain.delivery.ReportPreset
import com.bendey.restaurant.core.domain.delivery.ReportRangeRules
import com.bendey.restaurant.core.ui.components.BendeyAlert
import com.bendey.restaurant.core.ui.components.BendeyAlertSeverity
import com.bendey.restaurant.core.ui.components.BendeyDateField
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyLoadError
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeySkeletonListRow
import com.bendey.restaurant.core.ui.layout.rememberBendeyLazyListContentPadding
import java.util.Locale

/** Pestaña «Reporte» de Entregas (D5): resumen, canales, tiempos, cancelaciones, pagos y repartidores. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportePanel(modifier: Modifier = Modifier, viewModel: ReporteViewModel = hiltViewModel()) {
    val st by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.start() }
    val report = st.report
    val error = st.error

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.s16, top = BendeySpacing.s8),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
    ) {
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
                verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4),
            ) {
                ReportPreset.entries.forEach { p ->
                    BendeyFilterChip(selected = st.preset == p, onClick = { viewModel.selectPreset(p) }, text = p.label)
                }
            }
        }
        if (st.preset == ReportPreset.CUSTOM) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                    BendeyDateField(value = st.fromText, onValueChange = viewModel::setFrom, label = "Desde")
                    BendeyDateField(value = st.toText, onValueChange = viewModel::setTo, label = "Hasta")
                    Text(
                        "Máximo ${ReportRangeRules.MAX_DAYS} días.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                    BendeyOutlinedButton(text = "Consultar", onClick = viewModel::applyCustom, enabled = !st.loading)
                }
            }
        }
        st.shownRange?.let { r ->
            item {
                Text(
                    if (r.days == 1) "Del ${ReportRangeRules.display(r.from)}"
                    else "Del ${ReportRangeRules.display(r.from)} al ${ReportRangeRules.display(r.to)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
        }
        when {
            st.loading && report == null -> item {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) { repeat(3) { BendeySkeletonListRow() } }
            }
            error != null && report == null && st.preset != ReportPreset.CUSTOM -> item {
                BendeyLoadError(onRetry = viewModel::refresh, message = error)
            }
            else -> {
                if (error != null) item { BendeyAlert(message = error, severity = BendeyAlertSeverity.Danger) }
                if (report != null) {
                    if (st.isEmpty) {
                        item {
                            BendeyEmptyState(
                                title = "Sin pedidos en este rango",
                                description = "No hubo pedidos de delivery ni para recoger en las fechas elegidas.",
                            )
                        }
                    }
                    reportBody(report, st.loading)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.reportBody(r: DeliveryReport, refreshing: Boolean) {
    if (refreshing) {
        item { Text("Actualizando…", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant) }
    }
    if (r.truncated) item { BendeyAlert(message = DeliveryReportText.TRUNCATED_NOTICE, severity = BendeyAlertSeverity.Warning) }
    DeliveryReportText.refundsNotice(r.refunds)?.let { msg ->
        item { BendeyAlert(message = msg, severity = BendeyAlertSeverity.Warning) }
    }
    val s = r.summary
    item {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
        ) {
            Stat("Pedidos", s.orders.toString())
            Stat("Entregados", s.delivered.toString())
            Stat("En curso", s.inProgress.toString())
            Stat("Cancelados", "${s.cancelled} (${DeliveryReportText.percent(s.cancelRate)})")
            Stat("Ventas", DeliveryReportText.money(s.gross))
            Stat("Ticket promedio", DeliveryReportText.money(s.avgTicket))
            Stat("Monto perdido", DeliveryReportText.money(s.lostAmount))
        }
    }
    item {
        ReportCard("Por canal") {
            if (r.channels.isEmpty()) Muted("Sin pedidos")
            r.channels.forEach { c ->
                Line(
                    DeliveryReportText.channelLabel(c.source, c.type),
                    "${c.orders} pedidos · ${c.cancelled} cancelados · ${DeliveryReportText.money(c.amount)}",
                )
            }
        }
    }
    item {
        ReportCard("Tiempos") {
            Line("Aceptación", DeliveryReportText.time(r.times.accept))
            Line("Preparación", DeliveryReportText.time(r.times.prep))
            Line("Entrega", DeliveryReportText.time(r.times.delivery))
            Line("Total", DeliveryReportText.time(r.times.total))
        }
    }
    item {
        ReportCard("Cancelaciones por causa") {
            if (r.cancellations.isEmpty()) Muted("Sin cancelaciones")
            r.cancellations.forEach { c ->
                Line(c.label, "${c.orders} pedidos · ${DeliveryReportText.money(c.amount)}")
                if (c.sample.isNotBlank()) Muted("Ejemplo: «${c.sample}»")
            }
        }
    }
    item {
        ReportCard("Cómo pagaron") {
            if (r.payments.isEmpty()) Muted("Sin pagos")
            r.payments.forEach { p -> Line(p.label, "${p.orders} pedidos · ${DeliveryReportText.money(p.amount)}") }
        }
    }
    item {
        ReportCard("Repartidores") {
            if (r.drivers.isEmpty()) Muted("Sin repartidores con pedidos")
            r.drivers.forEach { d ->
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4)) {
                    Text(d.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface)
                    Muted("Asignados ${d.assigned} · Entregados ${d.delivered} · Fallidos ${d.failed} · Rechazados ${d.rejected}")
                    Muted(
                        "Tiempo promedio ${if (d.delivered > 0) String.format(Locale.US, "%.0f", d.avgDeliveryMinutes) + " min" else "sin datos"} · " +
                            "Ganancia ${DeliveryReportText.money(d.earnings)}",
                    )
                    Muted("Ganancia pagada ${DeliveryReportText.money(d.earningsPaid)} · Por pagar ${DeliveryReportText.money(d.earningsPending)}")
                    Muted("Efectivo cobrado ${DeliveryReportText.money(d.cashCollected)} · Por rendir ${DeliveryReportText.money(d.cashPending)}")
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    BendeyManagementCard(modifier = Modifier.fillMaxWidth(0.48f)) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = BendeyColors.OnSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface)
        }
    }
}

@Composable
private fun ReportCard(title: String, content: @Composable () -> Unit) {
    BendeyManagementCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
            BendeySectionTitle(text = title)
            content()
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = BendeyColors.OnSurface,
            modifier = Modifier.weight(0.4f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, color = BendeyColors.OnSurface, modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
}
