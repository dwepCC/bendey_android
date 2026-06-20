package com.bendey.restaurant.feature.mesas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.accentColor
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.domain.restaurant.SessionOrderSummary
import com.bendey.restaurant.core.ui.checkout.CheckoutDialog
import com.bendey.restaurant.core.ui.checkout.CheckoutSuccessDialog
import com.bendey.restaurant.core.ui.components.BendeyPosCatalogPane
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.layout.bendeySafeDrawingPadding
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesaScreen(
    onBack: () -> Unit,
    onCheckoutSuccess: () -> Unit = onBack,
    modifier: Modifier = Modifier,
    viewModel: MesaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    val widthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val isExpanded = widthClass != WindowWidthSizeClass.COMPACT
    var showCartSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.snackMessage) {
        if (state.snackMessage != null) viewModel.consumeSnackMessage()
    }

    Column(modifier = modifier.fillMaxSize().bendeySafeDrawingPadding()) {
        BendeyScreenToolbar(
            title = state.session?.tableName ?: "Mesa",
            subtitle = buildSessionSubtitle(state.session?.floorName, state.session?.orderCode, state.session?.guests),
            onBack = onBack,
            actions = {
                IconButton(onClick = viewModel::refreshSession) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
            },
        )
        SessionSummaryBar(
            total = state.sessionTotal,
            currency = currency,
            onPrintPrecuenta = viewModel::printPrecuenta,
            onCheckout = viewModel::openCheckout,
            printing = state.printingPrecuenta,
            checkoutLoading = state.checkoutSubmitting,
        )
        if (isExpanded) {
            Row(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.weight(0.58f)) {
                    CatalogSection(state, currency, viewModel, sidebarCategories = true)
                }
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .background(BendeyColors.SurfaceVariant),
                ) {
                    CartSection(state, currency, viewModel, modifier = Modifier.weight(1f))
                    OrdersSection(
                        orders = state.session?.orders.orEmpty(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            CatalogSection(
                state = state,
                currency = currency,
                viewModel = viewModel,
                modifier = Modifier.weight(1f),
            )
            CompactMesaBar(
                cartTotal = state.cartTotal,
                sessionTotal = state.sessionTotal,
                cartCount = state.cartCount,
                currency = currency,
                sending = state.sending,
                onOpenCart = { showCartSheet = true },
                onSend = viewModel::sendComanda,
            )
        }
        state.error?.let {
            Text(it, color = BendeyColors.Error, modifier = Modifier.padding(16.dp))
        }
    }

    CheckoutDialog(
        open = state.checkoutOpen,
        title = "Cobrar y cerrar mesa",
        loading = state.checkoutSubmitting,
        metaLoading = state.checkoutMetaLoading,
        meta = state.checkoutMeta,
        rawTotal = state.checkoutRawTotal,
        discountMode = state.checkoutDiscountMode,
        discountValue = state.checkoutDiscountValue,
        allowDiscount = state.allowCheckoutDiscount,
        payments = state.checkoutPayments,
        seriesId = state.checkoutSeriesId,
        docType = state.checkoutDocType,
        contactId = state.checkoutContactId,
        error = if (state.checkoutOpen) state.error else null,
        onDismiss = viewModel::dismissCheckout,
        onSeriesChange = viewModel::setCheckoutSeries,
        onContactChange = viewModel::setCheckoutContact,
        onDiscountModeChange = viewModel::setCheckoutDiscountMode,
        onDiscountValueChange = viewModel::setCheckoutDiscountValue,
        onPaymentsChange = viewModel::setCheckoutPayments,
        onConfirm = viewModel::confirmCheckout,
    )

    state.checkoutSuccess?.let { sale ->
        CheckoutSuccessDialog(
            number = sale.number,
            total = sale.total,
            subtitle = "Mesa cerrada. El comprobante quedó registrado; SUNAT se actualiza en segundo plano.",
            printNote = state.checkoutPrintNote,
            onDismiss = {
                viewModel.dismissCheckoutSuccess()
                onCheckoutSuccess()
            },
        )
    }

    if (!isExpanded && showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                CartSection(
                    state = state,
                    currency = currency,
                    viewModel = viewModel,
                    onSend = {
                        viewModel.sendComanda()
                        showCartSheet = false
                    },
                )
                OrdersSection(
                    orders = state.session?.orders.orEmpty(),
                    modifier = Modifier.heightIn(max = 280.dp),
                )
            }
        }
    }
}

private fun buildSessionSubtitle(floor: String?, orderCode: String?, guests: Int?): String {
    val parts = mutableListOf<String>()
    floor?.let { parts += it }
    orderCode?.let { parts += it }
    guests?.takeIf { it > 0 }?.let { parts += "$it comensales" }
    return parts.joinToString(" · ").ifBlank { "Sesión activa" }
}

@Composable
private fun SessionSummaryBar(
    total: Double,
    currency: NumberFormat,
    onPrintPrecuenta: () -> Unit,
    onCheckout: () -> Unit,
    printing: Boolean,
    checkoutLoading: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BendeyColors.PrimaryContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Total mesa", style = MaterialTheme.typography.labelMedium)
            Text(
                currency.format(total),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BendeyColors.Primary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPrintPrecuenta,
                enabled = !printing && total > 0,
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Text(if (printing) "Imprimiendo…" else "Precuenta")
            }
            BendeyPrimaryButton(
                text = if (checkoutLoading) "Cobrando…" else "Cobrar",
                onClick = onCheckout,
                enabled = total > 0 && !checkoutLoading && !printing,
            )
        }
    }
}

@Composable
private fun CatalogSection(
    state: MesaUiState,
    currency: NumberFormat,
    viewModel: MesaViewModel,
    modifier: Modifier = Modifier,
    sidebarCategories: Boolean = false,
) {
    BendeyPosCatalogPane(
        searchQuery = state.searchQuery,
        onSearchChange = viewModel::setSearchQuery,
        categories = state.categories,
        selectedCategoryId = state.selectedCategoryId,
        onCategorySelect = viewModel::selectCategory,
        products = state.products,
        currency = currency,
        assetsBaseUrl = viewModel.assetsBaseUrl,
        onProductClick = viewModel::addToCart,
        modifier = modifier,
        sidebarCategories = sidebarCategories,
    )
}

@Composable
private fun CartSection(
    state: MesaUiState,
    currency: NumberFormat,
    viewModel: MesaViewModel,
    modifier: Modifier = Modifier,
    onSend: (() -> Unit)? = null,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Nuevo pedido (${state.cartCount})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        if (state.cart.isEmpty()) {
            Text("Agrega productos", color = BendeyColors.OnSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(state.cart, key = { it.product.id }) { line ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(line.product.name, fontWeight = FontWeight.Medium)
                            Text(currency.format(line.lineTotal), color = BendeyColors.OnSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.decrementLine(line.product.id) }) {
                            Icon(Icons.Default.Remove, contentDescription = "Menos")
                        }
                        Text(line.quantity.toString(), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.addToCart(line.product) }) {
                            Icon(Icons.Default.Add, contentDescription = "Más")
                        }
                    }
                }
            }
        }
        Text(
            currency.format(state.cartTotal),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = BendeyColors.Primary,
        )
        BendeyPrimaryButton(
            text = if (state.sending) "Enviando…" else "Enviar comanda",
            onClick = { onSend?.invoke() ?: viewModel.sendComanda() },
            enabled = state.cart.isNotEmpty() && !state.sending,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun OrdersSection(
    orders: List<SessionOrderSummary>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Pedidos enviados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        if (orders.isEmpty()) {
            Text("Sin pedidos aún", color = BendeyColors.OnSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(orders, key = { it.id }) { order ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Comanda #${order.orderNumber}", fontWeight = FontWeight.SemiBold)
                            order.comandas.forEach { comanda ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        "${comanda.quantity.toInt()}× ${comanda.productName}",
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    BendeyStatusChip(
                                        label = comanda.status.label,
                                        accentColor = comanda.status.accentColor(),
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

@Composable
private fun CompactMesaBar(
    cartTotal: Double,
    sessionTotal: Double,
    cartCount: Int,
    currency: NumberFormat,
    sending: Boolean,
    onOpenCart: () -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BendeyColors.Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenCart)) {
            Text("$cartCount en carrito · Mesa ${currency.format(sessionTotal)}")
            Text(currency.format(cartTotal), fontWeight = FontWeight.Bold, color = BendeyColors.Primary)
        }
        BendeyPrimaryButton(
            text = if (sending) "…" else "Comanda",
            onClick = onSend,
            enabled = cartCount > 0 && !sending,
            modifier = Modifier.fillMaxWidth(0.4f),
        )
    }
}
