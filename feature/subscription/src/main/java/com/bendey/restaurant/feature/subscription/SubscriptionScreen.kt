package com.bendey.restaurant.feature.subscription

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyCard
import com.bendey.restaurant.core.designsystem.components.BendeyStatusChip
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.subscription.AvailablePlan
import com.bendey.restaurant.core.domain.subscription.SubscriptionPayment
import com.bendey.restaurant.core.ui.components.BendeyEmptyState
import com.bendey.restaurant.core.ui.components.BendeyFormDialog
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.layout.rememberBendeyLazyListContentPadding
import com.bendey.restaurant.core.ui.components.BendeyOption
import com.bendey.restaurant.core.ui.components.BendeyOutlinedButton
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyScreenToolbar
import com.bendey.restaurant.core.ui.components.BendeySimpleSelect
import com.bendey.restaurant.core.ui.components.BendeySnackMessage
import com.bendey.restaurant.core.ui.components.BendeyTextField
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BendeySnackMessage(
        message = state.snackMessage,
        onShow = onShowMessage,
        onConsume = viewModel::consumeSnackMessage,
    )

    Column(modifier.fillMaxSize()) {
        BendeyScreenToolbar(title = "Suscripción", onBack = onBack)

        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            val hub = state.hub
            if (hub == null && !state.loading) {
                BendeyEmptyState(
                    title = "No se pudo cargar tu suscripción",
                    description = state.error,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (hub != null) {
                BendeyLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = rememberLazyListState(),
                    contentPadding = rememberBendeyLazyListContentPadding(
                        includeBottomBar = false,
                        top = BendeySpacing.md,
                    ),
                    verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                ) {
                    item(key = "banner") {
                        BendeyCard(contentPadding = PaddingValues(BendeySpacing.md)) {
                            Text(
                                hub.statusBanner.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = statusBannerColor(hub.statusBanner.variant),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    item(key = "plan-header") {
                        BendeyCard(contentPadding = PaddingValues(BendeySpacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        hub.subscription.planName.ifBlank { "Sin plan" },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "Vence: ${hub.subscription.nextBillingDate ?: "—"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BendeyColors.OnSurfaceVariant,
                                    )
                                }
                                BendeyStatusChip(
                                    label = subscriptionStatusLabel(hub.subscription.status),
                                    accentColor = statusBannerColor(hub.statusBanner.variant),
                                )
                            }
                            if (hub.subscription.pendingAmount > 0) {
                                Text(
                                    "Pendiente: S/ ${"%.2f".format(hub.subscription.pendingAmount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BendeyColors.Error,
                                    modifier = Modifier.padding(top = BendeySpacing.xs),
                                )
                            }
                            if (hub.subscription.hasPendingPaymentReview) {
                                Text(
                                    "Tienes un pago en validación.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BendeyColors.OnSurfaceVariant,
                                    modifier = Modifier.padding(top = BendeySpacing.xxs),
                                )
                            }
                            if (hub.subscription.canSubmitPayment) {
                                BendeyPrimaryButton(
                                    text = "Reportar pago",
                                    onClick = viewModel::openPaymentDialog,
                                    modifier = Modifier.padding(top = BendeySpacing.sm),
                                )
                            }
                        }
                    }
                    hub.documents?.let { docs ->
                        item(key = "documents") {
                            BendeyCard(contentPadding = PaddingValues(BendeySpacing.md)) {
                                Text(
                                    "Documentos electrónicos",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    if (docs.isUnlimited) {
                                        "Ilimitados"
                                    } else {
                                        "${docs.totalConsumed} / ${docs.totalAvailable} usados este ciclo"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BendeyColors.OnSurfaceVariant,
                                    modifier = Modifier.padding(top = BendeySpacing.xxs),
                                )
                                docs.warningMessage?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BendeyColors.Error,
                                        modifier = Modifier.padding(top = BendeySpacing.xxs),
                                    )
                                }
                            }
                        }
                    }
                    if (state.plans.isNotEmpty()) {
                        item(key = "plans-title") {
                            Text(
                                "Planes disponibles",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = BendeySpacing.xs),
                            )
                        }
                        items(state.plans, key = { "plan-${it.id}" }) { plan ->
                            PlanCard(
                                plan = plan,
                                isCurrent = plan.id == hub.subscription.planId,
                                onSelect = { viewModel.openPlanChangeDialog(plan) },
                            )
                        }
                    }
                    if (hub.payments.isNotEmpty()) {
                        item(key = "payments-title") {
                            Text(
                                "Historial de pagos",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = BendeySpacing.xs),
                            )
                        }
                        items(hub.payments, key = { "payment-${it.id}" }) { payment ->
                            PaymentRow(payment)
                        }
                    }
                }
            }
        }
    }

    if (state.paymentDialogOpen) {
        PaymentFormDialog(
            title = "Reportar pago",
            state = state,
            onDismiss = viewModel::dismissPaymentDialog,
            onConfirm = viewModel::submitPayment,
            onAmountChange = { v -> viewModel.updatePaymentForm { it.copy(amount = v) } },
            onMethodChange = { v -> viewModel.updatePaymentForm { it.copy(paymentMethod = v) } },
            onReferenceChange = { v -> viewModel.updatePaymentForm { it.copy(reference = v) } },
            onNotesChange = { v -> viewModel.updatePaymentForm { it.copy(notes = v) } },
            onReceiptPicked = viewModel::pickReceipt,
        )
    }

    if (state.planChangeDialogOpen) {
        PaymentFormDialog(
            title = "Cambiar a ${state.planChangeTarget?.name.orEmpty()}",
            state = state,
            onDismiss = viewModel::dismissPlanChangeDialog,
            onConfirm = viewModel::submitPlanChange,
            onAmountChange = { v -> viewModel.updatePaymentForm { it.copy(amount = v) } },
            onMethodChange = { v -> viewModel.updatePaymentForm { it.copy(paymentMethod = v) } },
            onReferenceChange = { v -> viewModel.updatePaymentForm { it.copy(reference = v) } },
            onNotesChange = { v -> viewModel.updatePaymentForm { it.copy(notes = v) } },
            onReceiptPicked = viewModel::pickReceipt,
        )
    }
}

@Composable
private fun PlanCard(plan: AvailablePlan, isCurrent: Boolean, onSelect: () -> Unit) {
    BendeyCard(contentPadding = PaddingValues(BendeySpacing.md)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Row {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (plan.featured) {
                        Text(
                            "  ★ Popular",
                            style = MaterialTheme.typography.labelMedium,
                            color = BendeyColors.Primary,
                        )
                    }
                }
                Text(
                    plan.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            Text(
                "S/ ${"%.0f".format(plan.price)}/mes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (isCurrent) {
            Row(
                modifier = Modifier.padding(top = BendeySpacing.sm),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BendeyColors.Primary)
                Text(
                    " Plan actual",
                    style = MaterialTheme.typography.labelMedium,
                    color = BendeyColors.Primary,
                )
            }
        } else {
            BendeyOutlinedButton(
                text = "Cambiar a este plan",
                onClick = onSelect,
                fillWidth = true,
                modifier = Modifier.padding(top = BendeySpacing.sm),
            )
        }
    }
}

@Composable
private fun PaymentRow(payment: SubscriptionPayment) {
    BendeyCard(contentPadding = PaddingValues(BendeySpacing.md)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    payment.newPlanName?.let { "Cambio a $it" } ?: "Renovación",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    payment.createdAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text("S/ ${"%.2f".format(payment.amount)}", style = MaterialTheme.typography.bodyMedium)
                BendeyStatusChip(
                    label = paymentStatusLabel(payment.status),
                    accentColor = paymentStatusColor(payment.status),
                )
            }
        }
        payment.rejectReason?.takeIf { it.isNotBlank() }?.let {
            Text(
                "Motivo: $it",
                style = MaterialTheme.typography.bodySmall,
                color = BendeyColors.Error,
                modifier = Modifier.padding(top = BendeySpacing.xxs),
            )
        }
    }
}

private val PAYMENT_METHOD_OPTIONS = listOf(
    BendeyOption("yape", "Yape"),
    BendeyOption("plin", "Plin"),
    BendeyOption("transfer", "Transferencia"),
    BendeyOption("deposit", "Depósito"),
)

@Composable
private fun PaymentFormDialog(
    title: String,
    state: SubscriptionUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onAmountChange: (String) -> Unit,
    onMethodChange: (String) -> Unit,
    onReferenceChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onReceiptPicked: (ByteArray, String, String) -> Unit,
) {
    val context = LocalContext.current
    val form = state.paymentForm
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bytes = readReceiptBytes(context, it) ?: return@let
            val mime = context.contentResolver.getType(it) ?: "image/jpeg"
            val ext = if (mime.contains("pdf")) "pdf" else "jpg"
            onReceiptPicked(bytes, mime, "comprobante.$ext")
        }
    }

    BendeyFormDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = "Enviar",
        loading = state.submitting,
        confirmEnabled = form.amount.isNotBlank() && form.receipt != null,
        enableContentScroll = true,
        validationError = state.error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        BendeyTextField(
            value = form.amount,
            onValueChange = onAmountChange,
            label = "Monto (S/)",
            modifier = Modifier.fillMaxWidth(),
        )
        BendeySimpleSelect(
            options = PAYMENT_METHOD_OPTIONS,
            selectedValue = form.paymentMethod,
            onSelect = onMethodChange,
            label = "Método de pago",
            modifier = Modifier.fillMaxWidth(),
        )
        BendeyTextField(
            value = form.reference,
            onValueChange = onReferenceChange,
            label = "Referencia (opcional)",
            modifier = Modifier.fillMaxWidth(),
        )
        BendeyTextField(
            value = form.notes,
            onValueChange = onNotesChange,
            label = "Notas (opcional)",
            modifier = Modifier.fillMaxWidth(),
        )
        HorizontalDivider(color = BendeyColors.Outline.copy(alpha = 0.5f))
        BendeyOutlinedButton(
            text = form.receipt?.fileName ?: "Adjuntar comprobante",
            onClick = { launcher.launch("*/*") },
            fillWidth = true,
        )
        if (form.receipt != null) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.AttachFile, contentDescription = null, tint = BendeyColors.Primary)
                Text(
                    " Comprobante adjunto",
                    style = MaterialTheme.typography.bodySmall,
                    color = BendeyColors.OnSurfaceVariant,
                )
            }
        }
    }
}

private fun readReceiptBytes(context: android.content.Context, uri: Uri): ByteArray? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { input ->
        ByteArrayOutputStream().apply { input.copyTo(this) }.toByteArray()
    }
}.getOrNull()

private fun statusBannerColor(variant: String) = when (variant) {
    "danger" -> BendeyColors.Error
    "warning" -> BendeyColors.Warning
    "success" -> BendeyColors.Primary
    else -> BendeyColors.OnSurfaceVariant
}

private fun paymentStatusColor(status: String) = when (status.lowercase()) {
    "approved" -> BendeyColors.Primary
    "rejected" -> BendeyColors.Error
    else -> BendeyColors.Warning
}

private fun paymentStatusLabel(status: String) = when (status.lowercase()) {
    "approved" -> "Aprobado"
    "rejected" -> "Rechazado"
    "pending" -> "En revisión"
    else -> status.ifBlank { "—" }
}

private fun subscriptionStatusLabel(status: String) = when (status.lowercase()) {
    "active" -> "Activo"
    "grace" -> "Periodo de gracia"
    "suspended" -> "Suspendido"
    "blocked" -> "Bloqueado"
    "provisional_active" -> "Provisional"
    else -> status.ifBlank { "—" }
}
