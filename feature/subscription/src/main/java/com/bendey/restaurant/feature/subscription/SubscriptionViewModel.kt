package com.bendey.restaurant.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.subscription.AvailablePlan
import com.bendey.restaurant.core.domain.subscription.BillingHub
import com.bendey.restaurant.core.domain.subscription.PlanChangeInput
import com.bendey.restaurant.core.domain.subscription.SubmitPaymentInput
import com.bendey.restaurant.core.domain.subscription.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiptDraft(
    val bytes: ByteArray,
    val mimeType: String,
    val fileName: String,
)

data class PaymentFormState(
    val amount: String = "",
    val paymentMethod: String = "yape",
    val reference: String = "",
    val notes: String = "",
    val receipt: ReceiptDraft? = null,
)

data class SubscriptionUiState(
    val loading: Boolean = false,
    val hub: BillingHub? = null,
    val plans: List<AvailablePlan> = emptyList(),
    val plansLoading: Boolean = false,
    val error: String? = null,
    val snackMessage: String? = null,
    val paymentDialogOpen: Boolean = false,
    val planChangeDialogOpen: Boolean = false,
    val planChangeTarget: AvailablePlan? = null,
    val paymentForm: PaymentFormState = PaymentFormState(),
    val submitting: Boolean = false,
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        refresh()
        loadPlans()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val result = repository.getHub()) {
                is AppResult.Success -> _uiState.update { it.copy(loading = false, hub = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    private fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(plansLoading = true) }
            when (val result = repository.getPlans()) {
                is AppResult.Success -> _uiState.update { it.copy(plansLoading = false, plans = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(plansLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun openPaymentDialog() {
        val amount = _uiState.value.hub?.subscription?.pendingAmount
            ?: _uiState.value.hub?.billingContext?.planAmount
            ?: 0.0
        _uiState.update {
            it.copy(
                paymentDialogOpen = true,
                paymentForm = PaymentFormState(amount = if (amount > 0) formatAmount(amount) else ""),
            )
        }
    }

    fun dismissPaymentDialog() {
        _uiState.update { it.copy(paymentDialogOpen = false, paymentForm = PaymentFormState()) }
    }

    fun openPlanChangeDialog(plan: AvailablePlan) {
        _uiState.update {
            it.copy(
                planChangeDialogOpen = true,
                planChangeTarget = plan,
                paymentForm = PaymentFormState(amount = formatAmount(plan.price)),
            )
        }
    }

    fun dismissPlanChangeDialog() {
        _uiState.update { it.copy(planChangeDialogOpen = false, planChangeTarget = null, paymentForm = PaymentFormState()) }
    }

    fun updatePaymentForm(transform: (PaymentFormState) -> PaymentFormState) {
        _uiState.update { it.copy(paymentForm = transform(it.paymentForm)) }
    }

    fun pickReceipt(bytes: ByteArray, mimeType: String, fileName: String) {
        updatePaymentForm { it.copy(receipt = ReceiptDraft(bytes, mimeType, fileName)) }
    }

    fun consumeSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    fun submitPayment() {
        val form = _uiState.value.paymentForm
        val receipt = form.receipt ?: run {
            _uiState.update { it.copy(error = "Adjunta el comprobante de pago") }
            return
        }
        val amount = form.amount.replace(',', '.').trim().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Ingresa un monto válido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true, error = null) }
            val result = repository.submitPayment(
                SubmitPaymentInput(
                    billingCycleId = null,
                    amount = amount,
                    paymentMethod = form.paymentMethod,
                    reference = form.reference,
                    notes = form.notes,
                    paymentDate = null,
                    receiptBytes = receipt.bytes,
                    receiptMimeType = receipt.mimeType,
                    receiptFileName = receipt.fileName,
                ),
            )
            when (result) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        submitting = false,
                        paymentDialogOpen = false,
                        paymentForm = PaymentFormState(),
                        hub = result.data.hub ?: it.hub,
                        snackMessage = result.data.message ?: "Pago enviado; pendiente de validación",
                    )
                }
                is AppResult.Error -> _uiState.update { it.copy(submitting = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun submitPlanChange() {
        val plan = _uiState.value.planChangeTarget ?: return
        val form = _uiState.value.paymentForm
        val receipt = form.receipt ?: run {
            _uiState.update { it.copy(error = "Adjunta el comprobante de pago") }
            return
        }
        val amount = form.amount.replace(',', '.').trim().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Ingresa un monto válido") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true, error = null) }
            val result = repository.requestPlanChange(
                PlanChangeInput(
                    planId = plan.id,
                    amount = amount,
                    paymentMethod = form.paymentMethod,
                    reference = form.reference,
                    notes = form.notes,
                    paymentDate = null,
                    receiptBytes = receipt.bytes,
                    receiptMimeType = receipt.mimeType,
                    receiptFileName = receipt.fileName,
                ),
            )
            when (result) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        submitting = false,
                        planChangeDialogOpen = false,
                        planChangeTarget = null,
                        paymentForm = PaymentFormState(),
                        hub = result.data.hub ?: it.hub,
                        snackMessage = result.data.message ?: "Solicitud de cambio de plan enviada",
                    )
                }
                is AppResult.Error -> _uiState.update { it.copy(submitting = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }
}

private fun formatAmount(value: Double): String = if (value == value.toLong().toDouble()) {
    value.toLong().toString()
} else {
    "%.2f".format(value)
}
