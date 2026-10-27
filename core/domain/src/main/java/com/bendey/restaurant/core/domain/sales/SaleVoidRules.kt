package com.bendey.restaurant.core.domain.sales

import com.bendey.restaurant.core.domain.locale.LabelKey

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
enum class VentasTab(val sunatCodes: String) {
    TODAS("00,01,03"),
    NOTAS("00"),
    BOLETAS("03"),
    FACTURAS("01"),
    FACTURACION("01,03"),
    CREDITOS(""),
    ;

    /**
     * Texto del filtro, con las etiquetas del país (I5). En Perú: «Todas las ventas», «Solo notas de
     * venta», «Solo boletas», «Solo facturas», «Facturas y boletas», «Notas de crédito». Los plurales
     * «notas de venta» / «Notas de crédito» no salen de la forma corta y quedan fijos.
     */
    val label: String
        get() {
            val boletas = "${LabelKey.DOC_RECEIPT.text.lowercase()}s"
            val facturas = "${LabelKey.DOC_INVOICE.text.lowercase()}s"
            return when (this) {
                TODAS -> "Todas las ventas"
                NOTAS -> "Solo notas de venta"
                BOLETAS -> "Solo $boletas"
                FACTURAS -> "Solo $facturas"
                FACTURACION -> "${facturas.replaceFirstChar { it.uppercase() }} y $boletas"
                CREDITOS -> "Notas de crédito"
            }
        }
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

/**
 * «Reabrir para cobrar de nuevo»: la venta ya está anulada (nota de venta anulada o comprobante con nota de
 * crédito aceptada), viene de un pedido del restaurante y quien mira puede cobrar (`o.ch`).
 */
fun SaleDetail.canReopenSession(canCharge: Boolean): Boolean = com.bendey.restaurant.core.domain.ops.ReopenRules.isOffered(
    cancelled = isSaleCancelled(status) || billingStatus.equals("voided", ignoreCase = true),
    restaurantSessionId = restaurantSessionId,
    canCharge = canCharge,
)

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
