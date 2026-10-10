package com.bendey.restaurant.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Atajo desde el aviso push «Pago por validar» (data.type = payment_review): la app navega a Entregas y la pantalla
 * abre la pestaña Pagos. Es una bandera de un solo uso: quien la atiende llama a [consume].
 */
object DeliveryDeepLink {
    private val _openPayments = MutableStateFlow(false)
    val openPayments: StateFlow<Boolean> = _openPayments.asStateFlow()

    const val TYPE_PAYMENT_REVIEW = "payment_review"

    fun requestPayments() {
        _openPayments.value = true
    }

    fun consume() {
        _openPayments.value = false
    }

    /** Interpreta el data.type de un push; devuelve true si lo reconoció y dejó la bandera. */
    fun handlePushType(type: String?): Boolean {
        if (type != TYPE_PAYMENT_REVIEW) return false
        requestPayments()
        return true
    }
}
