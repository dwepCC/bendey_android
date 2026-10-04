package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.billing.BankAccountBrief
import com.bendey.restaurant.core.domain.billing.CheckoutPaymentDraft
import com.bendey.restaurant.core.domain.billing.CheckoutPaymentLine
import com.bendey.restaurant.core.domain.billing.PaymentMethodOption
import com.bendey.restaurant.core.domain.billing.findPaymentMethodRecord
import com.bendey.restaurant.core.domain.billing.isPaymentMethodLinkedForSale
import com.bendey.restaurant.core.domain.billing.needsCashSessionForPayments
import com.bendey.restaurant.core.domain.billing.roundSunat

fun parseCheckoutPayments(drafts: List<CheckoutPaymentDraft>): List<CheckoutPaymentLine>? {
    if (drafts.isEmpty()) return null
    val lines = mutableListOf<CheckoutPaymentLine>()
    for (draft in drafts) {
        val amount = draft.amount.replace(',', '.').trim().toDoubleOrNull() ?: return null
        if (amount <= 0) return null
        lines += CheckoutPaymentLine(
            method = draft.method.ifBlank { "cash" },
            amount = roundSunat(amount),
            reference = draft.reference.trim(),
        )
    }
    return lines
}

fun needsOpenCashSessionForPayments(
    methods: List<PaymentMethodOption>,
    payments: List<CheckoutPaymentLine>,
): Boolean = payments.any { line ->
    val method = methods.firstOrNull { it.code == line.method }
    method?.isCash == true || (methods.isEmpty() && line.method.equals("cash", ignoreCase = true))
}

/** Cajeros deben tener caja abierta para cualquier cobro; otros roles solo si hay efectivo. */
fun requiresOpenCashSessionForCheckout(
    canOperateCash: Boolean,
    methods: List<PaymentMethodOption>,
    payments: List<CheckoutPaymentLine>,
): Boolean = canOperateCash || needsOpenCashSessionForPayments(methods, payments)

const val MSG_CASH_NOT_ALLOWED = "No tienes permiso para cobrar en efectivo; usa otro método o pide a un cajero"
const val MSG_METHOD_NOT_CONFIGURED = "Método de pago no configurado. Revísalo en Caja."

/**
 * Validaciones del cobro que deben correr ANTES de enviar la comanda pendiente a cocina: si una falla,
 * el pedido no debe haber salido ya (y el diálogo de cobro se queda abierto con lo tecleado).
 *
 * Orden: método configurado y con cuenta -> permiso de efectivo -> caja abierta. El permiso va antes
 * que la caja porque a quien no puede cobrar efectivo no le sirve el consejo "abre tu caja" (no puede).
 *
 * NO cambia qué métodos exigen caja: eso sigue en [requiresOpenCashSessionForCheckout] (los cajeros la
 * necesitan para cualquier método; los demás roles, solo con efectivo).
 *
 * @return mensaje para el usuario, o null si todo está en orden.
 */
fun checkoutPaymentPrecheckError(
    canOperateCash: Boolean,
    methods: List<PaymentMethodOption>,
    bankAccounts: List<BankAccountBrief>,
    payments: List<CheckoutPaymentLine>,
    hasOpenCashSession: Boolean,
): String? {
    if (methods.isNotEmpty()) {
        for (line in payments) {
            val method = findPaymentMethodRecord(methods, line.method) ?: return MSG_METHOD_NOT_CONFIGURED
            if (!isPaymentMethodLinkedForSale(method, bankAccounts)) {
                return "El método \"${method.name}\" no tiene una cuenta vinculada."
            }
        }
    }
    val hasCash = needsCashSessionForPayments(methods, payments)
    if (hasCash && !canOperateCash) return MSG_CASH_NOT_ALLOWED
    if (requiresOpenCashSessionForCheckout(canOperateCash, methods, payments) && !hasOpenCashSession) {
        return if (hasCash) "Abre tu caja para cobrar en efectivo" else "Abre tu caja para cobrar"
    }
    return null
}
