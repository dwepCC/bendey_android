package com.bendey.restaurant.feature.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Surface
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
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
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.ui.checkout.CheckoutDialog
import com.bendey.restaurant.core.ui.checkout.CheckoutSuccessDialog
import com.bendey.restaurant.core.ui.components.BendeyPosCatalogPane
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    modifier: Modifier = Modifier,
    viewModel: PosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    val widthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val isExpanded = widthClass != WindowWidthSizeClass.COMPACT
    var showCartSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.snackMessage) {
        if (state.snackMessage != null) viewModel.consumeSnackMessage()
    }

    Column(modifier = modifier.fillMaxSize()) {
        OrderTypeRow(
            selected = state.orderType,
            onSelect = viewModel::setOrderType,
            compact = isExpanded,
        )
        if (isExpanded && (state.checkoutRawTotal > 0 || state.activeSessionId != null)) {
            PosSessionBar(
                orderCode = state.orderCode,
                sessionTotal = state.sessionTotal,
                cartTotal = state.cartTotal,
                currency = currency,
                checkoutLoading = state.checkoutSubmitting,
                onCheckout = viewModel::openCheckout,
                canCheckout = state.canCheckout,
            )
        }
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                CatalogPane(
                    state = state,
                    currency = currency,
                    assetsBaseUrl = viewModel.assetsBaseUrl,
                    sidebarCategories = true,
                    onSearch = viewModel::setSearchQuery,
                    onCategory = viewModel::selectCategory,
                    onAdd = viewModel::addToCart,
                    modifier = Modifier.weight(0.62f),
                )
                CartPane(
                    state = state,
                    currency = currency,
                    onIncrement = viewModel::addToCart,
                    onDecrement = viewModel::decrementLine,
                    onRemove = viewModel::removeLine,
                    onSend = viewModel::sendComanda,
                    onCheckout = viewModel::openCheckout,
                    canCheckout = state.canCheckout,
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                        .background(BendeyColors.SurfaceVariant),
                )
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                CatalogPane(
                    state = state,
                    currency = currency,
                    assetsBaseUrl = viewModel.assetsBaseUrl,
                    onSearch = viewModel::setSearchQuery,
                    onCategory = viewModel::selectCategory,
                    onAdd = viewModel::addToCart,
                    modifier = Modifier.fillMaxSize(),
                )
                CompactCartBar(
                    payableTotal = state.checkoutRawTotal,
                    cartTotal = state.cartTotal,
                    count = state.cartCount,
                    currency = currency,
                    sending = state.sending,
                    checkoutLoading = state.checkoutSubmitting,
                    canCheckout = state.canCheckout,
                    onOpenCart = { showCartSheet = true },
                    onSend = viewModel::sendComanda,
                    onCheckout = viewModel::openCheckout,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        state.error?.let { error ->
            Text(
                text = error,
                color = BendeyColors.Error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (!isExpanded && showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            CartPane(
                state = state,
                currency = currency,
                onIncrement = { product -> viewModel.addToCart(product) },
                onDecrement = viewModel::decrementLine,
                onRemove = viewModel::removeLine,
                onSend = {
                    viewModel.sendComanda()
                    showCartSheet = false
                },
                onCheckout = {
                    viewModel.openCheckout()
                    showCartSheet = false
                },
                canCheckout = state.canCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )
        }
    }

    CheckoutDialog(
        open = state.checkoutOpen,
        title = "Cobrar venta",
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
            subtitle = "Registrado correctamente. SUNAT se actualiza en segundo plano; revisa el estado en Ventas.",
            printNote = state.checkoutPrintNote,
            onDismiss = viewModel::dismissCheckoutSuccess,
        )
    }
}

@Composable
private fun PosSessionBar(
    orderCode: String?,
    sessionTotal: Double,
    cartTotal: Double,
    currency: NumberFormat,
    checkoutLoading: Boolean,
    onCheckout: () -> Unit,
    canCheckout: Boolean,
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
            Text(orderCode ?: "Venta activa", style = MaterialTheme.typography.labelMedium)
            Text(
                currency.format(sessionTotal + cartTotal),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BendeyColors.Primary,
            )
        }
        BendeyPrimaryButton(
            text = if (checkoutLoading) "Cobrando…" else "Cobrar",
            onClick = onCheckout,
            enabled = canCheckout && !checkoutLoading,
        )
    }
}

@Composable
private fun OrderTypeRow(
    selected: PosOrderType,
    onSelect: (PosOrderType) -> Unit,
    compact: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 4.dp else 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PosOrderType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(type.label, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BendeyColors.Primary,
                    selectedLabelColor = BendeyColors.OnPrimary,
                ),
            )
        }
    }
}

@Composable
private fun CatalogPane(
    state: PosUiState,
    currency: NumberFormat,
    assetsBaseUrl: String?,
    onSearch: (String) -> Unit,
    onCategory: (Int?) -> Unit,
    onAdd: (PosProduct) -> Unit,
    modifier: Modifier = Modifier,
    sidebarCategories: Boolean = false,
) {
    BendeyPosCatalogPane(
        searchQuery = state.searchQuery,
        onSearchChange = onSearch,
        categories = state.categories,
        selectedCategoryId = state.selectedCategoryId,
        onCategorySelect = onCategory,
        products = state.products,
        currency = currency,
        assetsBaseUrl = assetsBaseUrl,
        onProductClick = onAdd,
        modifier = modifier,
        sidebarCategories = sidebarCategories,
    )
}

@Composable
private fun CartPane(
    state: PosUiState,
    currency: NumberFormat,
    onIncrement: (PosProduct) -> Unit,
    onDecrement: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onSend: () -> Unit,
    onCheckout: () -> Unit,
    canCheckout: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Carrito (${state.cartCount})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
        if (state.cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Sin productos", color = BendeyColors.OnSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.cart, key = { it.product.id }) { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(line.product.name, fontWeight = FontWeight.Medium)
                            Text(
                                currency.format(line.lineTotal),
                                color = BendeyColors.OnSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDecrement(line.product.id) }) {
                            Icon(Icons.Default.Remove, contentDescription = "Menos")
                        }
                        Text(line.quantity.toString(), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onIncrement(line.product) }) {
                            Icon(Icons.Default.Add, contentDescription = "Más")
                        }
                    }
                }
            }
        }
        Text(
            text = currency.format(state.cartTotal),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = BendeyColors.Primary,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        BendeyPrimaryButton(
            text = if (state.sending) "Enviando…" else "Enviar comanda",
            onClick = onSend,
            enabled = state.cart.isNotEmpty() && !state.sending,
            modifier = Modifier.fillMaxWidth(),
        )
        if (canCheckout) {
            BendeyPrimaryButton(
                text = "Cobrar venta",
                onClick = onCheckout,
                enabled = !state.sending && !state.checkoutSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun CompactCartBar(
    payableTotal: Double,
    cartTotal: Double,
    count: Int,
    currency: NumberFormat,
    sending: Boolean,
    checkoutLoading: Boolean,
    canCheckout: Boolean,
    onOpenCart: () -> Unit,
    onSend: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BendeyColors.Surface,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = BendeyColors.Outline)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenCart)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BadgedBox(
                    badge = {
                        if (count > 0) {
                            Badge { Text(count.coerceAtMost(99).toString()) }
                        }
                    },
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Carrito",
                        tint = BendeyColors.Primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column {
                    Text(
                        text = if (count > 0) "Carrito · $count" else "Ver carrito",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        currency.format(if (payableTotal > 0) payableTotal else cartTotal),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BendeyColors.Primary,
                    )
                }
            }
            if (canCheckout && payableTotal > 0) {
                BendeyPrimaryButton(
                    text = if (checkoutLoading) "…" else "Cobrar",
                    onClick = onCheckout,
                    enabled = !checkoutLoading && !sending,
                    fillWidth = false,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            BendeyPrimaryButton(
                text = if (sending) "…" else "Comanda",
                onClick = onSend,
                enabled = count > 0 && !sending,
                fillWidth = false,
            )
        }
    }
}
