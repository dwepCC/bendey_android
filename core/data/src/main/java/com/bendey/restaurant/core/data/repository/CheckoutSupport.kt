package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.billing.BankAccountBrief
import com.bendey.restaurant.core.domain.billing.CheckoutPaymentDraft
import com.bendey.restaurant.core.domain.billing.CheckoutPaymentLine
import com.bendey.restaurant.core.domain.billing.PaymentMethodOption
import com.bendey.restaurant.core.domain.billing.findPaymentMethodRecord
import com.bendey.restaurant.core.domain.billing.isPaymentMethodLinkedForSale
import com.bendey.restaurant.core.domain.billing.needsCashSessionForPayments
import com.bendey.restaurant.core.domain.billing.roundSunat
import com.bendey.restaurant.core.domain.copy.CashCopy

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

/**
 * Regla ÚNICA de caja en el cobro (R9, igual que Tauri): la caja abierta se exige SOLO si algún pago es en
 * efectivo. Verificado con el servidor: `CashBankService.ResolveCashSessionForPayments` solo exige sesión
 * cuando hay un pago a destino efectivo; Yape/tarjeta/transferencia no la exigen.
 */
fun requiresOpenCashSessionForCheckout(
    methods: List<PaymentMethodOption>,
    payments: List<CheckoutPaymentLine>,
): Boolean = needsOpenCashSessionForPayments(methods, payments)

/**
 * Qué sesión de caja se manda con el cobro. Quien opera caja la manda siempre que la tenga (aunque pague
 * con Yape) para que la venta quede en SU turno y salga en el reporte de cierre por método; quien no
 * opera caja solo la manda si el cobro la exige (y el servidor lo rechazará si no es suya).
 */
fun cashSessionIdForCheckout(canOperateCash: Boolean, requiresCash: Boolean, openSessionId: Int?): Int? =
    if (canOperateCash || requiresCash) openSessionId else null

const val MSG_CASH_NOT_ALLOWED = CashCopy.CHECKOUT_CASH_DISABLED_ROLE
const val MSG_METHOD_NOT_CONFIGURED = "Método de pago no configurado. Revísalo en Caja."

/**
 * Validaciones del cobro que deben correr ANTES de enviar la comanda pendiente a cocina: si una falla,
 * el pedido no debe haber salido ya (y el diálogo de cobro se queda abierto con lo tecleado).
 *
 * Orden: método configurado y con cuenta -> permiso de efectivo -> caja abierta. El permiso va antes
 * que la caja porque a quien no puede cobrar efectivo no le sirve el consejo "abre tu caja" (no puede).
 *
 * La caja abierta solo se exige para EFECTIVO ([requiresOpenCashSessionForCheckout]). Al que no puede cobrar
 * efectivo (mozo) nunca se le dice "abre tu caja": no puede; se le dice que use otro método o pida a un cajero.
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
    if (hasCash && !hasOpenCashSession) return CashCopy.CHECKOUT_NEED_OPEN
    return null
}
