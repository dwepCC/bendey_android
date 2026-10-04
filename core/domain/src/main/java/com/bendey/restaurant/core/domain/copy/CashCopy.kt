package com.bendey.restaurant.core.domain.copy

/**
 * Textos de Caja (R9). Mismas claves y mismo texto que `src/content/cashCopy.ts` de Tauri: no cambies
 * uno sin cambiar el otro. Vocabulario fijo: "Abrir caja", "Contar el dinero", "Efectivo esperado"
 * (nunca "Saldo"), Ingreso/Gasto, "Caja cerrada". Tuteo.
 */
object CashCopy {
    const val CHIP_OPEN = "Caja abierta"
    const val CHIP_CLOSED = "Caja cerrada"
    const val CHIP_CLOSED_ACTION = "Abrir"
    const val CHIP_UNKNOWN = "Caja…"
    const val CHIP_UNKNOWN_HINT = "No pudimos leer el estado de la caja. Toca para reintentar."

    const val POPOVER_TITLE = "Tu caja"
    const val POPOVER_OPENING = "Apertura"
    const val POPOVER_CASHIER = "Cajero"
    const val POPOVER_SOLD_NET = "Vendido (neto)"
    const val POPOVER_BY_METHOD = "Por método"
    const val POPOVER_INCOME = "Ingresos"
    const val POPOVER_EXPENSE = "Gastos"
    const val POPOVER_EXPECTED = "Efectivo esperado"
    const val POPOVER_EXPECTED_HINT = "Lo que debería haber en el cajón según el sistema."
    const val POPOVER_CLOSE = "Cerrar caja"
    const val POPOVER_PARTIAL_COUNT = "Arqueo parcial"
    const val POPOVER_LOADING = "Cargando el resumen…"
    const val POPOVER_ERROR = "No pudimos cargar el resumen. Intenta de nuevo."

    const val OPEN_TITLE = "Abrir caja"
    const val OPEN_AMOUNT_LABEL = "Efectivo con el que abres"
    const val OPEN_ZERO = "Abrir con S/ 0"
    const val OPEN_SUBMIT = "Abrir caja"
    const val OPEN_SUCCESS = "Caja abierta"

    const val CLOSE_MODE_COUNT = "Con arqueo"
    const val CLOSE_MODE_MANUAL = "Sin arqueo — digitar efectivo contado"
    const val CLOSE_EXPECTED = "Efectivo esperado"
    const val CLOSE_EMPTY_ERROR = "Ingresa el conteo de efectivo antes de cerrar."
    const val CLOSE_EMPTY_ERROR_HINT = "Si no hay efectivo en la caja, digita S/ 0."
    const val CLOSE_ALREADY_CLOSED = "Esta caja ya estaba cerrada. Actualizamos la pantalla."
    const val CLOSE_SUCCESS = "Caja cerrada"
    const val CLOSE_NOT_COUNTED = "Cerrada sin contar el efectivo"

    const val BANNER_CLOSED = "Caja cerrada. Abre tu caja para cobrar en efectivo."
    const val BANNER_ACTION = "Abrir caja"
    const val CHECKOUT_CASH_DISABLED_CLOSED =
        "Efectivo no disponible: la caja está cerrada. Abre tu caja o cobra con otro método."
    const val CHECKOUT_CASH_DISABLED_ROLE = "Tu usuario no cobra en efectivo. Usa otro método o pide a un cajero."
    const val CHECKOUT_NEED_OPEN = "Para cobrar en efectivo necesitas abrir tu caja."

    fun openPrefillHint(amount: String) =
        "Prellenado con el efectivo que contaste al cerrar la última caja ($amount)."

    fun chipOpenFull(time: String, amount: String) = "Caja abierta · desde $time · Vendido $amount"
    fun chipOpenShort(amount: String) = "Vendido $amount"
}
