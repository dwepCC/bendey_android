package com.bendey.restaurant.feature.productos.reportes



import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.DropdownMenuItem

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.ExposedDropdownMenuAnchorType

import androidx.compose.material3.ExposedDropdownMenuBox

import androidx.compose.material3.ExposedDropdownMenuDefaults

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.OutlinedButton

import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

import androidx.compose.runtime.snapshotFlow

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip

import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip

import com.bendey.restaurant.core.designsystem.theme.BendeyColors

import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

import com.bendey.restaurant.core.domain.sales.BILLING_FILTER_STATUSES

import com.bendey.restaurant.core.domain.sales.SaleSummary
import com.bendey.restaurant.core.domain.sales.SaleListSummary

import com.bendey.restaurant.core.domain.sales.billingStatusLabel

import com.bendey.restaurant.core.domain.sales.formatOtherPaymentMethods

import com.bendey.restaurant.core.domain.sales.formatPaymentsCompact

import com.bendey.restaurant.core.domain.sales.isSaleCancelled

import com.bendey.restaurant.core.domain.sales.salePaymentMethodLabel

import com.bendey.restaurant.core.domain.sales.saleStatusLabel

import com.bendey.restaurant.core.domain.sales.sumPaymentsForMethod

import com.bendey.restaurant.core.ui.components.BendeyHorizontalScrollRow

import com.bendey.restaurant.core.ui.components.BendeyLazyColumn

import com.bendey.restaurant.core.ui.components.BendeySearchableSelect

import com.bendey.restaurant.core.ui.components.BendeySelectOption

import com.bendey.restaurant.core.ui.components.BendeySnackMessage

import com.bendey.restaurant.core.ui.components.BendeyTextField

import com.bendey.restaurant.core.ui.components.SalesPaymentSummaryRow

import com.bendey.restaurant.core.ui.layout.BendeyListScreenLayout

import com.bendey.restaurant.core.ui.layout.rememberBendeyBottomBarScrollPadding

import java.text.NumberFormat

import java.util.Locale

import kotlinx.coroutines.flow.distinctUntilChanged



@Composable

fun ReportesScreen(

    onShowMessage: (String) -> Unit = {},

    modifier: Modifier = Modifier,

    viewModel: ReportesViewModel = hiltViewModel(),

) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val currency = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }

    val bottomPadding = rememberBendeyBottomBarScrollPadding()



    BendeySnackMessage(

        message = state.snackMessage,

        onShow = onShowMessage,

        onConsume = viewModel::consumeSnackMessage,

    )



    BendeyListScreenLayout(

        modifier = modifier.fillMaxSize(),

        isRefreshing = state.loading,

        onRefresh = viewModel::refresh,

        header = {

            Column(modifier = Modifier.fillMaxWidth()) {

                Text(

                    text = "Reportes",

                    style = MaterialTheme.typography.titleLarge,

                    fontWeight = FontWeight.Bold,

                    modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xs),

                )

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(horizontal = BendeySpacing.md),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically,

                ) {

                    BendeyHorizontalScrollRow(

                        modifier = Modifier.weight(1f),

                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),

                    ) {

                        ReportesTab.entries.forEach { tab ->

                            BendeyFilterChip(

                                selected = state.tab == tab,

                                onClick = { viewModel.selectTab(tab) },

                                text = tab.label,

                            )

                        }

                    }

                    if (state.tab == ReportesTab.VENTAS) {

                        BendeyHorizontalScrollRow(

                            horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs),

                        ) {

                            SalesReportSubTab.entries.forEach { subTab ->

                                BendeyFilterChip(

                                    selected = state.salesSubTab == subTab,

                                    onClick = { viewModel.selectSalesSubTab(subTab) },

                                    text = subTab.label,

                                )

                            }

                        }

                    }

                }

                when (state.tab) {

                    ReportesTab.KARDEX, ReportesTab.PRODUCTOS -> ReportFilters(

                        state = state,

                        onFromDate = viewModel::setFromDate,

                        onToDate = viewModel::setToDate,

                        onBranch = viewModel::setBranchId,

                        onCategory = viewModel::setCategoryId,

                        onProductQ = viewModel::setProductQ,

                        onRefNotesQ = viewModel::setRefNotesQ,

                        onStockLessThan = viewModel::setStockLessThan,

                    )

                    ReportesTab.VENTAS -> SalesReportFilters(

                        state = state,

                        onFromDate = viewModel::setFromDate,

                        onToDate = viewModel::setToDate,

                        onSearchChange = viewModel::setSalesQuery,

                        onPaymentMethodChange = viewModel::setPaymentMethodFilter,

                        onBillingStatusChange = viewModel::setBillingStatusFilter,

                    )

                }

                state.error?.let {
                    Text(
                        text = it,
                        color = BendeyColors.Error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
                    )
                }

            }

        },

    ) { contentModifier ->

        when (state.tab) {

            ReportesTab.KARDEX -> {

                if (state.loading && state.kardexRows.isEmpty()) {

                    Box(modifier = contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                        CircularProgressIndicator()

                    }

                } else {

                    KardexList(state.kardexRows, bottomPadding, contentModifier)

                }

            }

            ReportesTab.PRODUCTOS -> {

                if (state.loading && state.productRows.isEmpty()) {

                    Box(modifier = contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                        CircularProgressIndicator()

                    }

                } else {

                    ProductsList(state.productRows, currency, bottomPadding, contentModifier)

                }

            }

            ReportesTab.VENTAS -> {

                if (state.loading && state.salesList.isEmpty()) {

                    Box(modifier = contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                        CircularProgressIndicator()

                    }

                } else {

                    SalesDocumentsList(

                        sales = state.salesList,

                        subTab = state.salesSubTab,

                        summary = state.listSummary,

                        paymentMethodFilter = state.paymentMethodFilter,

                        paymentMethodNames = state.paymentMethodNames,

                        exportBusy = state.exportBusy,

                        onExport = { format -> viewModel.export(context, format) },

                        loadingMore = state.loading && state.salesList.isNotEmpty(),

                        currency = currency,

                        bottomPadding = bottomPadding,

                        onLoadMore = viewModel::loadMoreSales,

                        modifier = contentModifier,

                    )

                }

            }

        }

    }

}



@Composable

private fun ReportFilters(

    state: ReportesUiState,

    onFromDate: (String) -> Unit,

    onToDate: (String) -> Unit,

    onBranch: (Int?) -> Unit,

    onCategory: (Int?) -> Unit,

    onProductQ: (String) -> Unit,

    onRefNotesQ: (String) -> Unit,

    onStockLessThan: (String) -> Unit,

) {

    Column(

        modifier = Modifier

            .fillMaxWidth()

            .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),

        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),

    ) {

        if (state.tab == ReportesTab.KARDEX) {

            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {

                BendeyTextField(

                    value = state.fromDate,

                    onValueChange = onFromDate,

                    label = "Desde",

                    modifier = Modifier.weight(1f),

                )

                BendeyTextField(

                    value = state.toDate,

                    onValueChange = onToDate,

                    label = "Hasta",

                    modifier = Modifier.weight(1f),

                )

            }

        }

        if (state.branches.size > 1) {

            val branchOptions = listOf(BendeySelectOption(-1, "Todas las sucursales")) +

                state.branches.map { BendeySelectOption(it.id, it.name) }

            BendeySearchableSelect(

                options = branchOptions,

                selectedId = state.branchId ?: -1,

                onSelect = { id -> onBranch(if (id == -1) null else id) },

                label = "Sucursal",

                placeholder = "Buscar sucursal…",

            )

        }

        when (state.tab) {

            ReportesTab.KARDEX -> {

                BendeyTextField(value = state.productQ, onValueChange = onProductQ, label = "Buscar producto")

                BendeyTextField(value = state.refNotesQ, onValueChange = onRefNotesQ, label = "Referencia o notas")

            }

            ReportesTab.PRODUCTOS -> {

                val categoryOptions = listOf(BendeySelectOption(-1, "Todas las categorías")) +

                    state.categories.map { BendeySelectOption(it.id, it.name) }

                BendeySearchableSelect(

                    options = categoryOptions,

                    selectedId = state.categoryId ?: -1,

                    onSelect = { id -> onCategory(if (id == -1) null else id) },

                    label = "Categoría",

                    placeholder = "Buscar categoría…",

                )

                BendeyTextField(value = state.productQ, onValueChange = onProductQ, label = "Buscar producto")

                BendeyTextField(value = state.stockLessThan, onValueChange = onStockLessThan, label = "Stock menor a")

            }

            ReportesTab.VENTAS -> Unit

        }

    }

}



@OptIn(ExperimentalMaterial3Api::class)

@Composable

private fun SalesReportFilters(

    state: ReportesUiState,

    onFromDate: (String) -> Unit,

    onToDate: (String) -> Unit,

    onSearchChange: (String) -> Unit,

    onPaymentMethodChange: (String) -> Unit,

    onBillingStatusChange: (String) -> Unit,

) {

    Column(

        modifier = Modifier

            .fillMaxWidth()

            .padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),

        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xxs),

    ) {

        Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {

            BendeyTextField(

                value = state.salesQuery,

                onValueChange = onSearchChange,

                label = "Buscar nº…",

                modifier = Modifier.weight(1f),

            )

            if (state.salesSubTab == SalesReportSubTab.DOCUMENTOS) {

                ReportFilterDropdown(

                    label = "Estado SUNAT",

                    value = state.billingStatusFilter,

                    options = listOf("" to "Todos") + BILLING_FILTER_STATUSES.map { it to billingStatusLabel(it) },

                    onSelect = onBillingStatusChange,

                    modifier = Modifier.weight(1f),

                )

            }

        }

        Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {

            ReportFilterDropdown(

                label = "Método",

                value = state.paymentMethodFilter,

                options = listOf("" to "Todos") +

                    state.paymentMethods.map { it.code to it.name },

                onSelect = onPaymentMethodChange,

                modifier = Modifier.weight(1.5f),

            )

            BendeyTextField(

                value = state.fromDate,

                onValueChange = onFromDate,

                label = "Desde",

                modifier = Modifier.weight(1f),

            )

            BendeyTextField(

                value = state.toDate,

                onValueChange = onToDate,

                label = "Hasta",

                modifier = Modifier.weight(1f),

            )

        }

    }

}



@OptIn(ExperimentalMaterial3Api::class)

@Composable

private fun ReportFilterDropdown(

    label: String,

    value: String,

    options: List<Pair<String, String>>,

    onSelect: (String) -> Unit,

    modifier: Modifier = Modifier,

) {

    var expanded by remember { mutableStateOf(false) }

    val selectedLabel = options.firstOrNull { it.first == value }?.second ?: label



    ExposedDropdownMenuBox(

        expanded = expanded,

        onExpandedChange = { expanded = it },

        modifier = modifier,

    ) {

        OutlinedTextField(

            value = selectedLabel,

            onValueChange = {},

            readOnly = true,

            singleLine = true,

            label = { Text(label) },

            modifier = Modifier

                .fillMaxWidth()

                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),

            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },

        )

        ExposedDropdownMenu(

            expanded = expanded,

            onDismissRequest = { expanded = false },

        ) {

            options.forEach { (code, text) ->

                DropdownMenuItem(

                    text = { Text(text) },

                    onClick = {

                        onSelect(code)

                        expanded = false

                    },

                )

            }

        }

    }

}



@Composable

private fun KardexList(

    rows: List<com.bendey.restaurant.core.domain.inventory.StockMovementItem>,

    bottomPadding: androidx.compose.ui.unit.Dp,

    modifier: Modifier = Modifier,

) {

    val listState = rememberLazyListState()

    BendeyLazyColumn(

        modifier = modifier.fillMaxSize(),

        state = listState,

        contentPadding = PaddingValues(

            start = BendeySpacing.md,

            end = BendeySpacing.md,

            top = BendeySpacing.sm,

            bottom = BendeySpacing.md + bottomPadding,

        ),

        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),

    ) {

        items(rows, key = { it.id }) { row ->

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {

                Text(row.productName.orEmpty(), fontWeight = FontWeight.SemiBold)

                Text("${row.createdAt.take(16)} · ${row.type} · ${row.quantity}", style = MaterialTheme.typography.bodySmall)

                Text(row.notes.orEmpty(), style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)

            }

        }

    }

}



@Composable

private fun ProductsList(

    rows: List<com.bendey.restaurant.core.domain.products.ProductReportItem>,

    currency: NumberFormat,

    bottomPadding: androidx.compose.ui.unit.Dp,

    modifier: Modifier = Modifier,

) {

    val listState = rememberLazyListState()

    BendeyLazyColumn(

        modifier = modifier.fillMaxSize(),

        state = listState,

        contentPadding = PaddingValues(

            start = BendeySpacing.md,

            end = BendeySpacing.md,

            top = BendeySpacing.sm,

            bottom = BendeySpacing.md + bottomPadding,

        ),

        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),

    ) {

        items(rows, key = { it.id }) { row ->

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {

                Text(row.name, fontWeight = FontWeight.SemiBold)

                Text("${row.code} · ${currency.format(row.salePrice)}", style = MaterialTheme.typography.bodySmall)

                if (row.manageStock) {

                    Text(

                        "Stock: ${row.stockTotal} · Mín: ${row.minStock}",

                        style = MaterialTheme.typography.bodySmall,

                        color = BendeyColors.OnSurfaceVariant,

                    )

                }

            }

        }

    }

}



@Composable

private fun SalesDocumentsList(

    sales: List<SaleSummary>,

    subTab: SalesReportSubTab,

    summary: SaleListSummary,

    paymentMethodFilter: String,

    paymentMethodNames: Map<String, String>,

    exportBusy: String?,

    onExport: (String) -> Unit,

    loadingMore: Boolean,

    currency: NumberFormat,

    bottomPadding: androidx.compose.ui.unit.Dp,

    onLoadMore: () -> Unit,

    modifier: Modifier = Modifier,

) {

    val listState = rememberLazyListState()



    LaunchedEffect(listState, sales.size) {

        snapshotFlow {

            val layoutInfo = listState.layoutInfo

            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisible >= layoutInfo.totalItemsCount - 3

        }

            .distinctUntilChanged()

            .collect { nearEnd ->

                if (nearEnd) onLoadMore()

            }

    }



    BendeyLazyColumn(

        modifier = modifier.fillMaxSize(),

        state = listState,

        contentPadding = PaddingValues(

            start = BendeySpacing.md,

            end = BendeySpacing.md,

            top = BendeySpacing.xs,

            bottom = BendeySpacing.md + bottomPadding,

        ),

        verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),

    ) {

        if (summary.paymentTotals.isNotEmpty()) {

            item(key = "sales-summary") {

                SalesPaymentSummaryRow(

                    summary = summary,

                    paymentMethodNames = paymentMethodNames,

                    currency = currency,

                    modifier = Modifier.padding(bottom = BendeySpacing.xxs),

                )

            }

        }

        item(key = "sales-export") {

            Row(

                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),

            ) {

                OutlinedButton(

                    onClick = { onExport("pdf") },

                    enabled = exportBusy == null,

                    modifier = Modifier.weight(1f),

                ) {

                    Text(if (exportBusy == "pdf") "Exportando…" else "PDF")

                }

                OutlinedButton(

                    onClick = { onExport("excel") },

                    enabled = exportBusy == null,

                    modifier = Modifier.weight(1f),

                ) {

                    Text(if (exportBusy == "excel") "Exportando…" else "Excel")

                }

            }

        }

        if (sales.isEmpty()) {

            item(key = "sales-empty") {

                Box(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(vertical = BendeySpacing.xl),

                    contentAlignment = Alignment.Center,

                ) {

                    Text(

                        "No hay comprobantes para los filtros seleccionados.",

                        color = BendeyColors.OnSurfaceVariant,

                    )

                }

            }

        } else {

            items(sales, key = { it.id }) { sale ->

                SalesDocumentRow(

                    sale = sale,

                    showBillingStatus = subTab == SalesReportSubTab.DOCUMENTOS,

                    paymentMethodFilter = paymentMethodFilter,

                    paymentMethodNames = paymentMethodNames,

                    currency = currency,

                )

            }

        }

        if (loadingMore) {

            item(key = "sales-loading-more") {

                Box(

                    modifier = Modifier.fillMaxWidth().padding(BendeySpacing.md),

                    contentAlignment = Alignment.Center,

                ) {

                    CircularProgressIndicator()

                }

            }

        }

    }

}



@Composable

private fun SalesDocumentRow(

    sale: SaleSummary,

    showBillingStatus: Boolean,

    paymentMethodFilter: String,

    paymentMethodNames: Map<String, String>,

    currency: NumberFormat,

) {

    Column(

        modifier = Modifier

            .fillMaxWidth()

            .padding(vertical = 4.dp),

        verticalArrangement = Arrangement.spacedBy(2.dp),

    ) {

        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.SpaceBetween,

            verticalAlignment = Alignment.Top,

        ) {

            Column(modifier = Modifier.weight(1f)) {

                Text(

                    "${sale.docType} ${sale.displayNumber}".trim(),

                    fontWeight = FontWeight.SemiBold,

                )

                Text(

                    sale.issueDate.take(10),

                    style = MaterialTheme.typography.bodySmall,

                    color = BendeyColors.OnSurfaceVariant,

                )

                sale.contactName?.let {

                    Text(it, style = MaterialTheme.typography.bodySmall)

                }

            }

            Column(horizontalAlignment = Alignment.End) {

                Text(currency.format(sale.total), fontWeight = FontWeight.Bold)

                val statusLabel = if (isSaleCancelled(sale.status)) "Anulada" else saleStatusLabel(sale.status)

                BendeyStatusChip(
                    label = statusLabel,
                    accentColor = if (isSaleCancelled(sale.status)) BendeyColors.Error else BendeyColors.Success,
                )

            }

        }

        Text(

            formatPaymentsCompact(sale, paymentMethodNames),

            style = MaterialTheme.typography.bodySmall,

            color = BendeyColors.OnSurfaceVariant,

        )

        if (paymentMethodFilter.isNotBlank()) {

            val filteredLabel = salePaymentMethodLabel(paymentMethodFilter, paymentMethodNames)

            Text(

                "$filteredLabel: ${currency.format(sumPaymentsForMethod(sale, paymentMethodFilter))} · " +

                    "Otros: ${formatOtherPaymentMethods(sale, paymentMethodFilter, paymentMethodNames)}",

                style = MaterialTheme.typography.labelSmall,

                color = BendeyColors.Primary,

            )

        }

        if (showBillingStatus) {

            BendeyStatusChip(
                label = billingStatusLabel(sale.billingStatus),
                accentColor = BendeyColors.OnSurfaceVariant,
            )

        }

    }

}


