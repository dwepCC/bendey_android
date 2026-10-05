package com.bendey.restaurant.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalCopy
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalLogic
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalOrder
import com.bendey.restaurant.core.realtime.pending.PendingApprovalState
import com.bendey.restaurant.core.realtime.pending.PendingApprovalStore
import com.bendey.restaurant.core.ui.components.BendeyAlert
import com.bendey.restaurant.core.ui.components.BendeyAlertAction
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyAlertSeverity
import com.bendey.restaurant.core.ui.components.BendeyBottomSheet
import com.bendey.restaurant.core.ui.components.BendeyDestructiveButton
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyLoadError
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySkeletonBlock
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PendingApprovalViewModel @Inject constructor(
    private val store: PendingApprovalStore,
) : ViewModel() {
    val state: StateFlow<PendingApprovalState> = store.state

    private val _busyOrderId = MutableStateFlow<Int?>(null)
    val busyOrderId: StateFlow<Int?> = _busyOrderId.asStateFlow()

    fun refresh() {
        viewModelScope.launch { store.refresh() }
    }

    fun approve(orderId: Int, onResult: (String, Boolean) -> Unit) {
        if (_busyOrderId.value != null) return
        _busyOrderId.value = orderId
        viewModelScope.launch {
            when (val r = store.approve(orderId)) {
                is AppResult.Success -> onResult(PendingApprovalCopy.APPROVED_OK, true)
                is AppResult.Error -> onResult(r.message, false)
                AppResult.Loading -> Unit
            }
            _busyOrderId.value = null
        }
    }

    fun reject(orderId: Int, reason: String, onResult: (String, Boolean) -> Unit) {
        if (_busyOrderId.value != null) return
        _busyOrderId.value = orderId
        viewModelScope.launch {
            when (val r = store.reject(orderId, reason)) {
                is AppResult.Success -> onResult(PendingApprovalCopy.REJECTED_OK, true)
                is AppResult.Error -> onResult(r.message, false)
                AppResult.Loading -> Unit
            }
            _busyOrderId.value = null
        }
    }
}

private fun money(value: Double) = String.format(Locale.US, "S/ %.2f", value)

private fun createdAtMs(iso: String): Long? =
    runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull()

/**
 * R10.1: cola de pedidos del cliente (QR) por revisar. Mismos textos que `PendingApprovalPanel.tsx` (Tauri).
 * Se abre desde la campana de la barra; solo se monta para quien puede ver la cola.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingApprovalSheet(
    permissions: List<String>,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit,
    viewModel: PendingApprovalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busyId by viewModel.busyOrderId.collectAsStateWithLifecycle()
    var rejectTarget by remember { mutableStateOf<PendingApprovalOrder?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var rejectError by remember { mutableStateOf(false) }
    val canApprove = PendingApprovalLogic.canApprove(permissions)
    val canReject = PendingApprovalLogic.canReject(permissions)

    // Al abrir la hoja siempre se pide la lista fresca.
    LaunchedEffect(Unit) { viewModel.refresh() }

    BendeyBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BendeySpacing.s16)
                .padding(bottom = BendeySpacing.s16),
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
        ) {
            Text(
                text = if (state.count > 0) PendingApprovalLogic.headerTitle(state.count) else PendingApprovalCopy.SHEET_TITLE,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BendeyColors.OnSurface,
            )
            if (state.error && state.orders.isNotEmpty()) {
                BendeyAlert(
                    message = PendingApprovalCopy.STALE_ERROR,
                    severity = BendeyAlertSeverity.Warning,
                    primaryAction = BendeyAlertAction(PendingApprovalCopy.RETRY) { viewModel.refresh() },
                )
            }
            when {
                !state.loaded -> {
                    // Cargando: la lista aun no llego (no se muestra "sin pedidos").
                    Text(PendingApprovalCopy.LOADING, color = BendeyColors.OnSurfaceVariant)
                    BendeySkeletonBlock(Modifier.fillMaxWidth().heightIn(min = 96.dp))
                }
                state.error && state.orders.isEmpty() -> BendeyLoadError(
                    onRetry = { viewModel.refresh() },
                    message = PendingApprovalCopy.LOAD_ERROR,
                )
                state.orders.isEmpty() -> BendeyEmptyState(
                    title = PendingApprovalCopy.EMPTY_TITLE,
                    description = PendingApprovalCopy.EMPTY_DESCRIPTION,
                )
                else -> LazyColumn(
                    modifier = Modifier.heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.s12),
                ) {
                    items(state.orders, key = { it.orderId }) { order ->
                        PendingOrderCard(
                            order = order,
                            busy = busyId == order.orderId,
                            anyBusy = busyId != null,
                            canApprove = canApprove,
                            canReject = canReject,
                            onApprove = {
                                viewModel.approve(order.orderId) { msg, _ -> onShowMessage(msg) }
                            },
                            onReject = {
                                rejectReason = ""
                                rejectError = false
                                rejectTarget = order
                            },
                        )
                    }
                }
            }
            if (state.orders.isNotEmpty() && canApprove && !canReject) {
                Text(
                    PendingApprovalCopy.NO_PERMISSION_REJECT,
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
        }
    }

    rejectTarget?.let { target ->
        BendeyAlertDialog(
            onDismissRequest = { if (busyId == null) rejectTarget = null },
            title = { Text(PendingApprovalCopy.REJECT_TITLE) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                    if (target.tableName.isNotBlank()) Text("Mesa ${target.tableName}")
                    BendeyTextField(
                        value = rejectReason,
                        onValueChange = {
                            rejectReason = it
                            rejectError = false
                        },
                        label = PendingApprovalCopy.REJECT_PLACEHOLDER,
                        singleLine = false,
                        isError = rejectError,
                        supportingText = if (rejectError) PendingApprovalCopy.REJECT_REASON_REQUIRED else null,
                    )
                }
            },
            confirmButton = {
                BendeyDestructiveButton(
                    text = PendingApprovalCopy.REJECT,
                    loading = busyId == target.orderId,
                    fillWidth = false,
                    onClick = {
                        val reason = rejectReason.trim()
                        if (reason.isEmpty()) {
                            rejectError = true
                        } else {
                            viewModel.reject(target.orderId, reason) { msg, ok ->
                                if (ok) rejectTarget = null
                                onShowMessage(msg)
                            }
                        }
                    },
                )
            },
            dismissButton = {
                BendeyTextButton(
                    text = PendingApprovalCopy.CANCEL,
                    enabled = busyId == null,
                    onClick = { rejectTarget = null },
                )
            },
        )
    }
}

@Composable
private fun PendingOrderCard(
    order: PendingApprovalOrder,
    busy: Boolean,
    anyBusy: Boolean,
    canApprove: Boolean,
    canReject: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val wait = PendingApprovalLogic.waitingLabel(
        PendingApprovalLogic.waitingMinutes(createdAtMs(order.createdAt), System.currentTimeMillis()),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BendeyColors.SurfaceVariant.copy(alpha = 0.45f), BendeyShapeTokens.md)
            .padding(BendeySpacing.s12),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.s8),
    ) {
        Text(
            text = PendingApprovalLogic.orderTitle(order.tableName, order.orderNumber),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = BendeyColors.OnSurface,
        )
        val meta = listOfNotNull(
            PendingApprovalLogic.orderTypeLabel(order.orderType),
            wait,
            order.customerName,
            order.customerPhone,
        ).joinToString(" · ")
        if (meta.isNotEmpty()) {
            Text(meta, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        }
        order.notes?.let {
            Text(
                "${PendingApprovalCopy.NOTE_PREFIX}: $it",
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.OnSurfaceVariant,
            )
        }
        order.items.forEach { item ->
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                    Text(
                        "${formatQty(item.quantity)}×",
                        fontWeight = FontWeight.Medium,
                        color = BendeyColors.OnSurface,
                    )
                    Text(item.productName, color = BendeyColors.OnSurface)
                }
                item.modifierLines.forEach {
                    Text(
                        it,
                        modifier = Modifier.padding(start = BendeySpacing.s24),
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
                item.notes?.let {
                    Text(
                        "($it)",
                        modifier = Modifier.padding(start = BendeySpacing.s24),
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                }
            }
        }
        if (order.total > 0.0) {
            Text(
                money(order.total),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = BendeyColors.OnSurface,
            )
        }
        if (canApprove || canReject) {
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.s8)) {
                if (canReject) {
                    BendeyOutlinedButton(
                        text = PendingApprovalCopy.REJECT,
                        onClick = onReject,
                        enabled = !anyBusy,
                    )
                }
                if (canApprove) {
                    BendeyPrimaryButton(
                        text = PendingApprovalCopy.APPROVE,
                        onClick = onApprove,
                        loading = busy,
                        enabled = !anyBusy,
                        fillWidth = false,
                    )
                }
            }
        }
    }
}

private fun formatQty(q: Double): String = if (q % 1.0 == 0.0) q.toInt().toString() else q.toString()
