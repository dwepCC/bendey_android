package com.bendey.restaurant.feature.productos.reportes

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.catalog.BranchItem
import com.bendey.restaurant.core.domain.inventory.CountResultLine
import com.bendey.restaurant.core.domain.inventory.PhysicalCountRules
import com.bendey.restaurant.core.domain.inventory.PhysicalCountResult
import com.bendey.restaurant.core.domain.inventory.PhysicalCountUiState
import com.bendey.restaurant.core.ui.components.BendeyAlert
import com.bendey.restaurant.core.ui.components.BendeyAlertSeverity
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyLoadError
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySearchableSelect
import com.bendey.restaurant.core.ui.components.BendeySelectOption
import com.bendey.restaurant.core.ui.components.BendeySkeletonListRow
import com.bendey.restaurant.core.ui.components.BendeySnackMessage
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.layout.rememberBendeyLazyListContentPadding
import com.bendey.restaurant.core.ui.subscription.BendeyExportActionsRow

@Composable
private fun BranchPicker(branches: List<BranchItem>, selected: Int?, onSelect: (Int) -> Unit) {
    if (branches.size < 2) return
    BendeySearchableSelect(
        options = branches.map { BendeySelectOption(it.id, it.name) },
        selectedId = selected,
        onSelect = onSelect,
        label = "Sucursal",
        placeholder = "Buscar sucursal…",
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    BendeyManagementCard(modifier = modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = BendeyColors.OnSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface)
        }
    }
}

// ───────────────────────── Valorización ─────────────────────────

/** Valorización de inventario: producto, cantidad, costo promedio y valor; total y saldos negativos marcados. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ValorizacionPanel(
    onNavigateToSubscription: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ValorizacionViewModel = hiltViewModel(),
) {
    val st by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) { viewModel.start() }
    BendeySnackMessage(message = st.snack, onShow = onShowMessage, onConsume = viewModel::consumeSnack)
    val data = st.data

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.s16, top = BendeySpacing.s8),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
    ) {
        item { BranchPicker(st.branches, st.branchId, viewModel::setBranch) }
        item {
            BendeyTextField(
                value = st.query,
                onValueChange = viewModel::setQuery,
                label = "Buscar producto o código",
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when {
            st.loading && data == null -> item {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) { repeat(4) { BendeySkeletonListRow() } }
            }
            st.error != null && data == null -> item { BendeyLoadError(onRetry = viewModel::load, message = st.error) }
            data != null -> {
                st.error?.let { msg -> item { BendeyAlert(message = msg, severity = BendeyAlertSeverity.Danger) } }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
                        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
                    ) {
                        Stat("Valor total", PhysicalCountRules.money(data.totalValue), Modifier.fillMaxWidth(0.48f))
                        Stat("Productos", data.totalProducts.toString(), Modifier.fillMaxWidth(0.48f))
                    }
                }
                if (data.negativeCount > 0) {
                    item {
                        BendeyAlert(
                            title = "Saldos negativos",
                            message = "${data.negativeCount} " + (if (data.negativeCount == 1) "producto tiene" else "productos tienen") +
                                " saldo negativo. Revisa tus compras y ventas, o haz un conteo físico para corregirlos.",
                            severity = BendeyAlertSeverity.Warning,
                        )
                    }
                }
                item {
                    BendeyExportActionsRow(
                        allowsExport = st.allowsExport,
                        exportBusy = st.exportBusy,
                        onExportPdf = { viewModel.export(context, "pdf") },
                        onExportExcel = { viewModel.export(context, "excel") },
                        onLockedClick = onNavigateToSubscription,
                        pdfLabel = "PDF",
                        excelLabel = "Excel",
                    )
                }
                if (st.rows.isEmpty()) {
                    item {
                        BendeyEmptyState(
                            title = if (st.query.isBlank()) "Sin saldos que valorizar" else "Sin coincidencias",
                            description = if (st.query.isBlank()) "No hay productos con control de stock y saldo distinto de cero." else "Prueba con otro nombre o código.",
                        )
                    }
                }
                items(st.rows, key = { PhysicalCountRules.valuationRowKey(it) }) { row ->
                    BendeyManagementCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    row.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BendeyColors.OnSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    PhysicalCountRules.money(row.value),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BendeyColors.OnSurface,
                                )
                            }
                            Muted("${row.code} · ${PhysicalCountRules.qty(row.quantity)} u. × ${PhysicalCountRules.money(row.averageCost)}")
                            if (row.negative) {
                                // Ícono + texto: el saldo negativo no se comunica solo con color.
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s4)) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = BendeyColors.Error, modifier = Modifier.padding(end = 2.dp))
                                    Text(
                                        "Saldo negativo",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BendeyColors.OnSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── Conteo físico ─────────────────────────

/** Conteo físico: contar, «Calcular diferencias» (vista previa) y «Aplicar ajustes». */
@Composable
fun ConteoPanel(
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConteoViewModel = hiltViewModel(),
) {
    val st by viewModel.state.collectAsStateWithLifecycle()
    val meta by viewModel.meta.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.start() }
    BendeySnackMessage(message = st.snack, onShow = onShowMessage, onConsume = viewModel::consumeSnack)

    if (!meta.canCount) {
        BendeyEmptyState(
            title = "Sin permiso para contar",
            description = "El conteo físico ajusta el stock. Pídele a un administrador que lo haga o que te dé el permiso de productos.",
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.s16, top = BendeySpacing.s8),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
    ) {
        item {
            Muted(
                "Anota lo que contaste en el local. «Calcular diferencias» solo compara con el sistema; " +
                    "el stock no cambia hasta que apliques los ajustes.",
            )
        }
        item { BranchPicker(meta.branches, st.branchId, viewModel::selectBranch) }
        item {
            BendeyTextField(
                value = st.query,
                onValueChange = viewModel::setQuery,
                label = "Buscar producto o código",
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (st.skippedByPresentation > 0) {
            item {
                Muted(
                    "${st.skippedByPresentation} producto(s) con stock por presentación no aparecen aquí; " +
                        "ajústalos desde Productos > Ajuste de stock.",
                )
            }
        }
        if (st.truncated) item { Muted("Se muestran los primeros productos del catálogo; usa el buscador para ubicar otros.") }
        st.actionError?.let { msg -> item { BendeyAlert(message = msg, severity = BendeyAlertSeverity.Danger) } }
        st.applyResult?.let { res -> item { ApplyResultCard(res) } }
        st.preview?.let { prev -> previewItems(prev) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8), modifier = Modifier.fillMaxWidth()) {
                BendeyPrimaryButton(
                    text = if (st.calculating) "Calculando…" else "Calcular diferencias",
                    onClick = viewModel::calculate,
                    enabled = st.canCalculate,
                    modifier = Modifier.weight(1f),
                )
                BendeyOutlinedButton(
                    text = "Aplicar ajustes",
                    onClick = viewModel::openConfirm,
                    enabled = st.canApply,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { Muted("Contados: ${st.countedCount}") }
        when {
            st.loading -> item {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) { repeat(4) { BendeySkeletonListRow() } }
            }
            st.error != null && st.products.isEmpty() -> item {
                BendeyLoadError(onRetry = { st.branchId?.let(viewModel::selectBranch) }, message = st.error)
            }
            st.visibleProducts.isEmpty() -> item {
                BendeyEmptyState(
                    title = "No hay productos para contar",
                    description = "Solo aparecen los productos activos con control de stock.",
                )
            }
            else -> items(st.visibleProducts, key = { it.productId }) { p ->
                BendeyManagementCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s12), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface)
                            Muted("${p.code} · ${p.unit}")
                        }
                        val text = st.counted[p.productId to 0].orEmpty()
                        BendeyTextField(
                            value = text,
                            onValueChange = { viewModel.setCounted(p.productId, it) },
                            label = "Contado",
                            isError = text.isNotBlank() && PhysicalCountRules.parseCounted(text) == null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.6f),
                        )
                    }
                }
            }
        }
    }

    if (st.confirmOpen) ApplyDialog(st, viewModel)
}

private fun LazyListScope.previewItems(prev: PhysicalCountResult) {
    val withDiff = PhysicalCountRules.withDifference(prev)
    item {
        BendeyManagementCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4)) {
                Text("Diferencias", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface)
                if (withDiff.isEmpty()) {
                    Muted("Todo lo contado coincide con el sistema. No hay nada que ajustar.")
                } else {
                    Muted("${withDiff.size} producto(s) con diferencia · Valor de la diferencia ${PhysicalCountRules.signedMoney(prev.totalValueDiff)}")
                }
            }
        }
    }
    items(withDiff, key = { "d${it.productId}-${it.presentationId}" }) { line -> DiffLine(line) }
}

@Composable
private fun DiffLine(line: CountResultLine) {
    BendeyManagementCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s4)) {
            Text(line.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = BendeyColors.OnSurface)
            Muted(
                "Sistema ${PhysicalCountRules.qty(line.system)} · Contado ${PhysicalCountRules.qty(line.counted)} · " +
                    PhysicalCountRules.differenceLabel(line.difference),
            )
            Muted("Valor de la diferencia ${PhysicalCountRules.signedMoney(line.valueDiff)}")
            line.error?.let { msg ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s4)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = BendeyColors.Error)
                    Text("No se pudo ajustar: $msg", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurface)
                }
            }
        }
    }
}

@Composable
private fun ApplyResultCard(res: PhysicalCountResult) {
    BendeyAlert(
        title = if (res.failedCount == 0) "Ajustes aplicados" else "Ajustes aplicados con avisos",
        message = "Se ajustaron ${res.adjustedCount} producto(s)" +
            (if (res.failedCount > 0) " y ${res.failedCount} no se pudieron ajustar (siguen abajo para reintentar)" else "") +
            ". Valor total de los ajustes: ${PhysicalCountRules.signedMoney(res.totalValueDiff)}.",
        severity = if (res.failedCount == 0) BendeyAlertSeverity.Success else BendeyAlertSeverity.Warning,
    )
}

@Composable
private fun ApplyDialog(st: PhysicalCountUiState, vm: ConteoViewModel) {
    val prev = st.preview
    BendeyFormDialog(
        onDismissRequest = vm::dismissConfirm,
        title = "Aplicar ajustes",
        confirmText = "Aplicar ajustes",
        loading = st.applying,
        validationError = st.actionError,
        onConfirm = vm::apply,
        onDismiss = vm::dismissConfirm,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
            Text(
                "Se generará un ajuste de entrada o salida por cada diferencia y quedará en el kardex" +
                    (prev?.let { " (${PhysicalCountRules.withDifference(it).size} productos, ${PhysicalCountRules.signedMoney(it.totalValueDiff)})" } ?: "") +
                    ". Esto cambia el stock y no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = BendeyColors.OnSurface,
            )
            BendeyTextField(
                value = st.note,
                onValueChange = vm::setNote,
                label = "Nota (opcional)",
                placeholder = "Ej.: inventario de fin de mes",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
