package com.bendey.restaurant.core.navigation

/** Verifica sesión de caja antes de abrir checkout (inyectado desde MainShell). */
fun interface CashCheckoutGate {
    /** Devuelve true si puede continuar; si no, dispara el modal de caja. */
    fun ensureForCheckout(): Boolean
}

val CashCheckoutGateNoOp: CashCheckoutGate = CashCheckoutGate { true }
