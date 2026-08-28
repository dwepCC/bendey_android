package com.bendey.restaurant.core.domain.sales

/**
 * FILTRO DE DOCUMENTO DEL LISTADO DE VENTAS (paridad web).
 *
 * Eran tres pestañas estancas y no había forma de ver el día completo: un negocio que emite boleta a
 * quien la pide y nota de venta al resto tenía que sumar dos pantallas a mano.
 *
 * «Todas» son las VENTAS, no todos los documentos: la nota de crédito no es una venta sino su
 * anulación, y mezclarla haría que el total del listado no cuadre con lo que entró a caja. Por eso
 * tiene su propia opción y queda fuera del resto.
 *
 * `sunatCodes` viaja tal cual al backend en `sunat_code`: 00 nota de venta, 01 factura, 03 boleta.
 */
enum class VentasTab(val label: String, val sunatCodes: String) {
    TODAS("Todas las ventas", "00,01,03"),
    NOTAS("Solo notas de venta", "00"),
    BOLETAS("Solo boletas", "03"),
    FACTURAS("Solo facturas", "01"),
    FACTURACION("Facturas y boletas", "01,03"),
    CREDITOS("Notas de crédito", ""),
}

/** Si el filtro puede traer comprobantes electrónicos, y con ellos el estado de SUNAT. */
fun VentasTab.incluyeElectronicos(): Boolean = this != VentasTab.NOTAS

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
