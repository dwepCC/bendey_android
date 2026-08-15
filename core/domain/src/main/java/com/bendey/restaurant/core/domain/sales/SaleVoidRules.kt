package com.bendey.restaurant.core.domain.sales

/** Pestañas del listado de ventas (paridad web). */
enum class VentasTab(val label: String) {
    NOTAS("Notas venta"),
    FACTURACION("Boletas/Facturas"),
    CREDITOS("Notas crédito"),
}

fun isSaleCancelled(status: String): Boolean =
    status.equals("cancelled", ignoreCase = true)

fun isNotaVenta(docType: String, sunatCode: String?): Boolean {
    val code = sunatCode?.trim().orEmpty()
    if (code == "00") return true
    return docType.contains("NOTA DE VENTA", ignoreCase = true)
}

fun isCreditNoteDoc(docType: String): Boolean =
    docType.contains("CREDITO", ignoreCase = true) ||
        docType.contains("CRÉDITO", ignoreCase = true)

fun isElectronicAccepted(billingStatus: String?): Boolean =
    billingStatus.equals("accepted", ignoreCase = true)

fun SaleDetail.canVoidWithCreditNote(): Boolean =
    !isSaleCancelled(status) &&
        isElectronicAccepted(billingStatus) &&
        !isCreditNoteDoc(docType) &&
        !isNotaVenta(docType, sunatCode)

fun SaleDetail.canCancelNotaVenta(): Boolean =
    !isSaleCancelled(status) &&
        isNotaVenta(docType, sunatCode) &&
        !isConverted()

fun SaleSummary.canVoidWithCreditNote(): Boolean =
    !isSaleCancelled(status) &&
        isElectronicAccepted(billingStatus) &&
        !isCreditNoteDoc(docType) &&
        !isNotaVenta(docType, sunatCode)

fun SaleSummary.canCancelNotaVenta(): Boolean =
    !isSaleCancelled(status) &&
        isNotaVenta(docType, sunatCode) &&
        !isConverted()

/**
 * ANULAR NO ES DEVOLVER.
 *
 * Anular deja la venta sin efecto; devolver saca plata de la caja. Pueden pasar juntos, separados o
 * solo uno: SUNAT acepta una nota de credito dias despues sin que nadie haya abierto el cajon.
 *
 * `refundable` lo calcula el backend cruzando caja y banco. Aca no se suman los pagos ni se busca el
 * movimiento: si la app dedujera la condicion por su cuenta, el boton y la API podrian discrepar.
 * Lo unico que agrega esta regla es que la venta ya este fuera de circulacion, porque devolver el
 * dinero de una venta vigente no es una devolucion sino un descuadre.
 */
fun canRegisterRefund(status: String, billingStatus: String?, refundable: Boolean): Boolean {
    if (!refundable) return false
    return isSaleCancelled(status) || billingStatus.equals("voided", ignoreCase = true)
}

fun SaleSummary.canRegisterRefund(): Boolean = canRegisterRefund(status, billingStatus, refundable)

fun SaleDetail.canRegisterRefund(): Boolean = canRegisterRefund(status, billingStatus, refundable)

fun SaleSummary.isRefunded(): Boolean = refundedAmount > 0

fun SaleDetail.isRefunded(): Boolean = refundedAmount > 0
