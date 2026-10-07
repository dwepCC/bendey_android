package com.bendey.restaurant.core.network.error

/**
 * Catalogo central de errores accionables (R5, UX-REDESIGN §15.3-§15.4). Funcion pura: sin Android,
 * sin Retrofit; el [NetworkErrorMapper] traduce la excepcion a [Failure] y este objeto decide el texto.
 *
 * Las cadenas son las MISMAS que `src/utils/errorCatalog.ts` de Tauri y estan documentadas en
 * `docs/ERROR_CATALOG_COPY.md`: no cambies una sin cambiar la otra. Espanol, tuteo, sin jerga.
 *
 * Reglas que cumple cada mensaje:
 *  - dice que paso y, en comanda/cobro/caja/anulacion, SI LA OPERACION SE REALIZO O NO;
 *  - lleva al menos una accion ([ErrorInfo.actions]) y la nombra tambien en el texto, porque no
 *    todas las pantallas dibujan botones;
 *  - nunca muestra excepciones, HTML, numeros HTTP ni "Error" a secas.
 *
 * Orden de resolucion: 1) `code` del backend; 2) tipo de fallo de red; 3) HTTP; 4) texto `error` del
 * servidor si es legible; 5) reserva por flujo.
 */
enum class ErrorFlow {
    GENERIC,
    SEND_COMANDA,
    CHARGE,
    OPEN_TABLE,
    VOID_REFUND,
    CASH,
    LOGIN_PIN,
    LOGIN_EMAIL,
}

/** Acciones que el usuario puede tomar ante el error. `label` es el texto del boton. */
enum class ErrorAction(val label: String) {
    RETRY("Reintentar"),
    KEEP_ORDER("Mantener pedido en pantalla"),
    REFRESH("Actualizar"),
    GO_TABLES("Ir a Mesas"),
    GO_SALES("Ver Ventas"),
    OPEN_CASH("Abrir caja"),
    GO_MY_CASH("Ir a mi caja"),
    GO_SERIES("Ir a Series"),
    CHOOSE_BRANCH("Elegir sucursal"),
    CHANGE_STATION("Cambiar estación"),
    RELOGIN("Iniciar sesión"),
    CREATE_PIN("Crear PIN"),
    BACK_TO_TRY("Volver a intentar"),
    VIEW_PLANS("Ver planes"),
    CONTACT_ADMIN("Hablar con el administrador"),
    CONTACT_SUPPORT("Contactar a soporte"),
}

/** Clase de fallo, ya independiente de la libreria de red. */
enum class FailureKind {
    /** No se pudo abrir la conexion (sin internet, DNS, servidor inalcanzable): la peticion NUNCA salio. */
    NO_CONNECTION,

    /** Timeout o corte a mitad de camino: la peticion pudo haber llegado y ejecutarse. */
    TIMEOUT,

    /** Fallo de certificado / canal seguro. */
    TLS,

    /** El servidor respondio con un HTTP de error (ver [Failure.status]). */
    HTTP,

    /** El servidor respondio pero el cuerpo no se pudo leer (HTML de proxy, JSON roto). */
    UNREADABLE,

    /** Cualquier otra cosa. */
    OTHER,
}

/** Lo que el mapper sabe del fallo. Todos los campos son opcionales salvo [kind]. */
data class Failure(
    val kind: FailureKind,
    val status: Int? = null,
    val code: String? = null,
    /** Texto `error` del backend, si lo hubo. */
    val serverMessage: String? = null,
)

data class ErrorInfo(
    val title: String,
    val message: String,
    val actions: List<ErrorAction>,
    /** Para el detalle plegable "Codigo de soporte" (nunca en el mensaje principal). */
    val supportCode: String? = null,
)

object ErrorCatalog {

    private class Entry(val title: String, val message: String, val actions: List<ErrorAction>)

    private fun e(title: String, message: String, vararg actions: ErrorAction) =
        Entry(title, message, actions.toList())

    private const val SESSION_EXPIRED = "Tu sesión venció. Vuelve a iniciar sesión para continuar."
    private const val CONTACT_SUPPORT_TAIL = "Vuelve a intentar; si sigue igual, avisa a soporte."

    /** code -> entrada. Las claves en minuscula (`menu_disabled`...) son las que el backend ya emitia. */
    private val byCode: Map<String, Entry> = mapOf(
        // --- Sesión ---
        "AUTH_TOKEN_INVALID" to e("Sesión vencida", SESSION_EXPIRED, ErrorAction.RELOGIN),
        "AUTH_TOKEN_MISSING" to e("Sesión vencida", SESSION_EXPIRED, ErrorAction.RELOGIN),
        "TOKEN_TENANT_INVALID" to e("Sesión vencida", SESSION_EXPIRED, ErrorAction.RELOGIN),
        "TENANT_ISOLATION_VIOLATION" to e("Sesión vencida", SESSION_EXPIRED, ErrorAction.RELOGIN),
        "AUTH_USER_NOT_FOUND" to e(
            "Usuario no encontrado",
            "Tu usuario ya no existe en este restaurante. Vuelve a iniciar sesión o habla con tu administrador.",
            ErrorAction.RELOGIN, ErrorAction.CONTACT_ADMIN,
        ),
        "BRANCH_REQUIRED" to e("Falta la sucursal", "Elige tu sucursal para continuar.", ErrorAction.CHOOSE_BRANCH),
        "BRANCH_FORBIDDEN" to e(
            "Sucursal no permitida",
            "No tienes acceso a esa sucursal. Elige otra o habla con tu administrador.",
            ErrorAction.CHOOSE_BRANCH, ErrorAction.CONTACT_ADMIN,
        ),
        // --- Plan ---
        "SUBSCRIPTION_REQUIRED" to e(
            "Servicio suspendido",
            "Tu restaurante tiene el servicio suspendido. Avisa al administrador.",
            ErrorAction.CONTACT_ADMIN,
        ),
        "TENANT_BLOCKED" to e(
            "Servicio suspendido",
            "Tu restaurante tiene el servicio suspendido. Avisa al administrador.",
            ErrorAction.CONTACT_ADMIN,
        ),
        "PAYMENT_BLOCKED" to e(
            "Servicio suspendido",
            "Tu restaurante tiene el servicio suspendido. Avisa al administrador.",
            ErrorAction.CONTACT_ADMIN,
        ),
        "DOCUMENT_QUOTA_EXCEEDED" to e(
            "Límite del plan",
            "Llegaste al límite de comprobantes de tu plan este mes. No se emitió el comprobante.",
            ErrorAction.VIEW_PLANS, ErrorAction.CONTACT_ADMIN,
        ),
        "ELECTRONIC_BILLING_DISABLED" to e(
            "Facturación no disponible",
            "Tu plan solo permite notas de venta. No se cobró: elige nota de venta o revisa tu plan.",
            ErrorAction.VIEW_PLANS, ErrorAction.BACK_TO_TRY,
        ),
        "menu_disabled" to e("Función desactivada", "Esta función está desactivada en tu restaurante. Pídele al administrador que la active.", ErrorAction.CONTACT_ADMIN),
        "takeaway_disabled" to e("Función desactivada", "Esta función está desactivada en tu restaurante. Pídele al administrador que la active.", ErrorAction.CONTACT_ADMIN),
        "delivery_disabled" to e("Función desactivada", "Esta función está desactivada en tu restaurante. Pídele al administrador que la active.", ErrorAction.CONTACT_ADMIN),
        "menu_digital_disabled" to e("Función desactivada", "Esta función está desactivada en tu restaurante. Pídele al administrador que la active.", ErrorAction.CONTACT_ADMIN),

        // --- Enviar comanda / mesa ---
        "IDEMPOTENCY_IN_PROGRESS" to e("La comanda se está enviando", "La comanda anterior todavía se está enviando. Espera un momento y vuelve a intentar: no se duplicará.", ErrorAction.RETRY),
        "GUESTS_INVALID" to e("Comensales no válidos", "El número de comensales debe estar entre 1 y 99.", ErrorAction.BACK_TO_TRY),
        "ORDER_EMPTY" to e("Pedido vacío", "El pedido está vacío. Agrega al menos un producto antes de enviarlo a cocina.", ErrorAction.BACK_TO_TRY),
        "SESSION_CLOSED" to e(
            "Mesa cerrada",
            "Esta mesa ya fue cerrada o cobrada. Actualiza para ver el estado.",
            ErrorAction.REFRESH, ErrorAction.GO_TABLES,
        ),
        "SESSION_UPDATED" to e(
            "La mesa cambió",
            "Otra persona modificó esta mesa mientras armabas el pedido. Actualiza para ver lo último y vuelve a intentar.",
            ErrorAction.REFRESH,
        ),
        "SESSION_NOT_FOUND" to e("Cuenta no encontrada", "No encontramos esta cuenta. Actualiza las mesas para ver el estado.", ErrorAction.REFRESH, ErrorAction.GO_TABLES),
        "SESSION_NOT_OPEN" to e("Cuenta cerrada", "Esta cuenta ya no está abierta. Actualiza para ver el estado.", ErrorAction.REFRESH, ErrorAction.GO_TABLES),
        "SESSION_MOVED" to e("Cuenta movida", "La cuenta ya no está en esa mesa. Actualiza e intenta de nuevo.", ErrorAction.REFRESH),
        "TABLE_OCCUPIED" to e(
            "Mesa ocupada",
            "No se pudo abrir la mesa. Puede que otro mozo la acabe de ocupar. Actualiza las mesas.",
            ErrorAction.REFRESH,
        ),
        "TABLE_NOT_FOUND" to e("Mesa no encontrada", "Esa mesa ya no existe. Actualiza las mesas.", ErrorAction.REFRESH),
        "TABLE_NOT_AVAILABLE" to e("Mesa no disponible", "La mesa destino no está disponible. Elige otra mesa.", ErrorAction.BACK_TO_TRY),
        "TABLE_CLOSE_PENDING_BALANCE" to e(
            "Consumo pendiente",
            "Esta mesa tiene consumo pendiente de cobro. Cóbrala antes de cerrarla.",
            ErrorAction.REFRESH,
        ),
        "TABLE_CLOSE_PENDING_COMANDAS" to e(
            "Comandas pendientes",
            "Quedan comandas pendientes en esta mesa. Cóbralas o anúlalas antes de cerrarla.",
            ErrorAction.REFRESH,
        ),
        "COMANDA_NOT_FOUND" to e("Comanda no encontrada", "No encontramos la comanda. Actualiza para ver el estado.", ErrorAction.REFRESH),
        "COMANDA_CANCELLED" to e("Comanda anulada", "Esta comanda ya fue anulada. Actualiza para ver el estado.", ErrorAction.REFRESH),
        "COMANDA_STATUS_BACKWARDS" to e("Estado no válido", "La comanda ya avanzó de estado y no puede retroceder. Actualiza para ver el estado.", ErrorAction.REFRESH),
        "COMANDA_NOT_IN_ORDER" to e("Comanda de otra ronda", "Una de las comandas ya no pertenece a esta ronda. Actualiza la cocina y vuelve a intentar.", ErrorAction.REFRESH),
        "COMANDA_ALREADY_DELIVERED" to e("Comanda entregada", "Esta comanda ya fue entregada y no se puede anular.", ErrorAction.REFRESH),

        // --- Cobrar ---
        "COMANDA_ALREADY_BILLED" to e(
            "Ya cobrada",
            "Esta comanda ya fue cobrada. Revisa en Ventas el comprobante; no se cobró de nuevo.",
            ErrorAction.GO_SALES, ErrorAction.REFRESH,
        ),
        "COMANDA_NOTHING_TO_BILL" to e("Nada por cobrar", "No hay productos por cobrar en esta mesa. Actualiza para ver el estado.", ErrorAction.REFRESH),
        "COMANDA_NOT_BILLABLE" to e(
            "No se puede cobrar",
            "Algunas comandas ya no están disponibles para cobro. No se cobró. Actualiza la mesa y vuelve a intentar.",
            ErrorAction.REFRESH,
        ),
        "COMANDA_WRONG_SESSION" to e(
            "No se puede cobrar",
            "Algunas comandas son de otra cuenta. No se cobró. Actualiza la mesa y vuelve a intentar.",
            ErrorAction.REFRESH,
        ),
        "SALE_EMPTY" to e("Venta vacía", "La venta está vacía. Agrega al menos un producto. No se cobró.", ErrorAction.BACK_TO_TRY),
        "CART_EMPTY" to e("Venta vacía", "La venta está vacía. Agrega al menos un producto. No se cobró.", ErrorAction.BACK_TO_TRY),
        "PAYMENT_BELOW_TOTAL" to e("Pago incompleto", "Lo pagado es menor al total de la cuenta. No se cobró. Revisa los montos y vuelve a intentar.", ErrorAction.BACK_TO_TRY),
        "PAYMENT_REQUIRED" to e("Falta el pago", "Elige al menos un método de pago. No se cobró.", ErrorAction.BACK_TO_TRY),
        "PAYMENT_METHOD_NO_ACCOUNT" to e(
            "Método sin cuenta",
            "Este método de pago no tiene una cuenta asociada. No se cobró. Pídele al administrador que lo configure en Caja → Cuentas.",
            ErrorAction.CONTACT_ADMIN,
        ),
        "CREDIT_REQUIRES_CUSTOMER" to e("Falta el cliente", "Para vender al crédito elige un cliente. No se cobró.", ErrorAction.BACK_TO_TRY),
        "CUSTOMER_RUC_INVALID" to e("RUC no válido", "El RUC del cliente solo puede tener números. Corrígelo y vuelve a intentar. No se cobró.", ErrorAction.BACK_TO_TRY),
        "INVOICE_REQUIRES_RUC" to e(
            "Falta el RUC",
            "Para emitir factura el cliente necesita un RUC de 11 dígitos. Elige boleta o corrige el cliente. No se cobró.",
            ErrorAction.BACK_TO_TRY,
        ),
        "SERIES_REQUIRED" to seriesMissing(),
        "SERIES_INACTIVE" to seriesMissing(),
        "SERIES_NOT_FOUND" to seriesMissing(),
        "SERIES_WRONG_BRANCH" to e("Serie de otra sucursal", "La serie elegida es de otra sucursal. Elige una serie de esta sucursal. No se cobró.", ErrorAction.BACK_TO_TRY, ErrorAction.GO_SERIES),
        "SERIES_INVALID" to e("Serie no válida", "La serie elegida no sirve para este comprobante. Elige otra.", ErrorAction.BACK_TO_TRY, ErrorAction.GO_SERIES),
        "SERIES_DUPLICATE" to e("Serie repetida", "Ese código de serie ya está en uso. Usa uno distinto.", ErrorAction.BACK_TO_TRY),
        "CASH_WAITER_NOT_ALLOWED" to e(
            "Cobro en efectivo no permitido",
            "Los mozos no cobran en efectivo. No se cobró: usa otro método de pago o pide a un cajero que cobre.",
            ErrorAction.BACK_TO_TRY,
        ),

        // --- Caja ---
        "CASH_SESSION_REQUIRED" to e("Caja cerrada", "Para cobrar en efectivo necesitas abrir tu caja.", ErrorAction.OPEN_CASH),
        "CASH_SESSION_CLOSED" to e("Caja cerrada", "Tu caja ya está cerrada. Ábrela de nuevo para cobrar o registrar movimientos.", ErrorAction.OPEN_CASH, ErrorAction.REFRESH),
        "CASH_SESSION_FOREIGN_BRANCH" to e("Caja de otra sucursal", "Esa caja es de otra sucursal. Usa la caja de tu sucursal.", ErrorAction.GO_MY_CASH),
        "CASH_SESSION_NOT_FOUND" to e("Caja no encontrada", "No encontramos esa caja. Actualiza e intenta de nuevo.", ErrorAction.REFRESH, ErrorAction.GO_MY_CASH),
        "CASH_NOT_OWNER" to e("Caja de otra persona", "Solo puedes operar tu propia caja. Abre la tuya o pide a su dueño que lo haga.", ErrorAction.GO_MY_CASH),
        "CASH_ALREADY_OPEN" to e("Caja ya abierta", "Ya tienes una caja abierta en este local. Ciérrala o continúa con ella.", ErrorAction.GO_MY_CASH),
        "CASH_ARQUEO_LOCKED" to e("Caja ya cerrada", "Esta caja ya tiene el conteo registrado y no se puede modificar.", ErrorAction.REFRESH),
        "CASH_OPENING_INVALID" to e("Monto no válido", "El monto inicial no es un número válido. Corrígelo y vuelve a intentar.", ErrorAction.BACK_TO_TRY),
        "CASH_OPENING_NEGATIVE" to e("Monto no válido", "El monto inicial no puede ser negativo. Corrígelo y vuelve a intentar.", ErrorAction.BACK_TO_TRY),
        "CASH_OPENING_TOO_LARGE" to e("Monto muy alto", "El monto inicial es demasiado alto. Revisa que esté bien escrito.", ErrorAction.BACK_TO_TRY),
        "MOVEMENT_ALREADY_VOIDED" to e("Movimiento anulado", "Este movimiento ya estaba anulado.", ErrorAction.REFRESH),
        "MOVEMENT_AMOUNT_INVALID" to e("Monto no válido", "El monto debe ser mayor a cero.", ErrorAction.BACK_TO_TRY),
        "MOVEMENT_NOT_FOUND" to e("Movimiento no encontrado", "No encontramos el movimiento. Actualiza la lista.", ErrorAction.REFRESH),
        "MOVEMENT_NOT_MANUAL" to e(
            "No se puede anular aquí",
            "Solo se anulan aquí los ingresos y egresos registrados a mano. Los de ventas o compras se anulan desde su documento.",
            ErrorAction.REFRESH,
        ),
        "MOVEMENT_NO_CHANGES" to e("Sin cambios", "No hay nada que corregir: los datos son los mismos.", ErrorAction.BACK_TO_TRY),
        "MOVEMENT_REASON_REQUIRED" to e("Falta el motivo", "Escribe el motivo y vuelve a intentar.", ErrorAction.BACK_TO_TRY),
        "MOVEMENT_SESSION_CLOSED" to e(
            "Caja cerrada",
            "La caja de ese movimiento ya está cerrada. Registra un movimiento de corrección en la caja actual.",
            ErrorAction.GO_MY_CASH,
        ),
        "MOVEMENT_TYPE_INVALID" to e("Tipo no válido", "Elige si es un ingreso o un egreso.", ErrorAction.BACK_TO_TRY),

        // --- Anular / devolver (PIN de autorización) ---
        "PIN_INCORRECT" to e("PIN incorrecto", "PIN incorrecto. No se hizo ningún cambio. Vuelve a intentar.", ErrorAction.BACK_TO_TRY),
        "PIN_REQUIRED" to e("Falta el PIN", "Ingresa el PIN de autorización para continuar.", ErrorAction.BACK_TO_TRY),
        "PIN_NOT_CONFIGURED" to e(
            "Sin PIN de autorización",
            "Aún no hay PIN de autorización. Pídele al administrador que lo cree en Ajustes → Restaurante.",
            ErrorAction.CREATE_PIN, ErrorAction.CONTACT_ADMIN,
        ),
        "PIN_LOCKED" to e(
            "PIN bloqueado",
            "Demasiados intentos. Espera unos minutos o pídele al administrador que restablezca el PIN.",
            ErrorAction.CONTACT_ADMIN,
        ),
        "PIN_UNAVAILABLE" to e(
            "No se pudo verificar el PIN",
            "No pudimos verificar el PIN. No se hizo ningún cambio. Revisa tu conexión y vuelve a intentar.",
            ErrorAction.RETRY,
        ),
        "PIN_DUPLICATE" to e("PIN repetido", "Ese PIN ya lo usa otra persona del restaurante. Elige otro.", ErrorAction.BACK_TO_TRY),
        "PIN_FORMAT_INVALID" to e("PIN no válido", "El PIN debe tener entre 4 y 6 dígitos, solo números.", ErrorAction.BACK_TO_TRY),
        "CANCEL_REASON_REQUIRED" to e("Falta el motivo", "Escribe el motivo de la anulación y vuelve a intentar.", ErrorAction.BACK_TO_TRY),
        "ORDER_NOT_FOUND" to e("Pedido no encontrado", "No encontramos el pedido. Actualiza para ver el estado.", ErrorAction.REFRESH),
        "ORDER_NOT_OPEN" to e("Pedido no abierto", "Solo se pueden anular pedidos abiertos. Actualiza para ver el estado.", ErrorAction.REFRESH),
        "ORDER_ALREADY_BILLED" to e(
            "Pedido ya cobrado",
            "Este pedido ya fue cobrado y no se puede anular. Si hay que devolver el dinero, hazlo desde Ventas.",
            ErrorAction.GO_SALES,
        ),
        "ORDER_HAS_ITEMS" to e(
            "El pedido tiene productos",
            "Este pedido todavía tiene productos. Anúlalos primero o anula el pedido completo con el PIN.",
            ErrorAction.BACK_TO_TRY,
        ),
        "SALE_NOT_FOUND" to e("Venta no encontrada", "No encontramos la venta. Actualiza Ventas para ver el estado.", ErrorAction.REFRESH, ErrorAction.GO_SALES),
        "SALE_ALREADY_CANCELLED" to e("Venta ya anulada", "Esta venta ya estaba anulada. No se hizo ningún cambio.", ErrorAction.GO_SALES),
        "SALE_ALREADY_ELECTRONIC" to e(
            "Ya tiene comprobante",
            "Esta nota ya tiene factura o boleta electrónica, por eso no se puede anular. Usa una nota de crédito.",
            ErrorAction.GO_SALES,
        ),
        "SALE_VOID_NEEDS_CREDIT_NOTE" to e(
            "Usa nota de crédito",
            "Las facturas y boletas se anulan con nota de crédito. No se anuló nada.",
            ErrorAction.GO_SALES,
        ),
        "REFUND_ALREADY_DONE" to e("Ya devuelta", "Esta venta ya tiene una devolución registrada. No se devolvió dinero de nuevo.", ErrorAction.GO_SALES),
        "REFUND_CONFIRMATION_REQUIRED" to e("Falta confirmar", "Confirma que entregaste el dinero al cliente. No se registró la devolución.", ErrorAction.BACK_TO_TRY),
        "REFUND_FAILED" to e(
            "No se pudo devolver",
            "No se pudo registrar la devolución. Antes de volver a intentar, revisa en Ventas si ya quedó registrada; si sigue igual, avisa a soporte.",
            ErrorAction.GO_SALES, ErrorAction.CONTACT_SUPPORT,
        ),
        "REFUND_NO_PAYMENT" to e("Sin cobro que devolver", "Esta venta no tiene un cobro registrado que se pueda devolver.", ErrorAction.GO_SALES),
        "REFUND_USER_REQUIRED" to e("Sesión incompleta", "No pudimos saber quién registra la devolución. Vuelve a iniciar sesión e intenta de nuevo.", ErrorAction.RELOGIN),
        "SUMMARY_ALREADY_EXISTS" to e("Ya enviado a SUNAT", "La anulación de este comprobante ya fue enviada a SUNAT. Revisa su estado en Ventas.", ErrorAction.GO_SALES),

        // --- Login por PIN ---
        "LOGIN_PIN_INCORRECT" to e(
            "PIN incorrecto",
            "PIN incorrecto o no corresponde a esta estación. Vuelve a intentar o cambia de estación.",
            ErrorAction.BACK_TO_TRY, ErrorAction.CHANGE_STATION,
        ),
        "LOGIN_PIN_FORMAT" to e("PIN no válido", "El PIN debe tener entre 4 y 6 dígitos, solo números.", ErrorAction.BACK_TO_TRY),
        "LOGIN_PIN_AMBIGUOUS" to e(
            "PIN repetido",
            "Este PIN está repetido en el sistema. Avisa a tu administrador para que lo cambie.",
            ErrorAction.CONTACT_ADMIN,
        ),
        "LOGIN_PIN_RATE_LIMITED" to e(
            "Demasiados intentos",
            "Demasiados intentos. Espera unos minutos y vuelve a intentar.",
            ErrorAction.BACK_TO_TRY,
        ),
        "LOGIN_STATION_INVALID" to e("Estación no válida", "Esa estación no existe. Elige tu estación de nuevo.", ErrorAction.CHANGE_STATION),
        // --- Delivery (D0/D1): bloque al final, alfabético por clave (mismo orden que Tauri) ---
        "DELIVERY_CANCEL_REASON_REQUIRED" to e("Falta el motivo", "Escribe el motivo de la cancelación (mínimo 3 letras) y vuelve a intentar.", ErrorAction.BACK_TO_TRY),
        "DRIVER_INACTIVE" to e("Repartidor dado de baja", "Ese repartidor ya no está activo. Elige a otro.", ErrorAction.BACK_TO_TRY),
        "DRIVER_UNAVAILABLE" to e("Repartidor no disponible", "Ese repartidor está marcado como no disponible. Elige a otro o espera a que se conecte.", ErrorAction.BACK_TO_TRY),
        "SESSION_ALREADY_CLOSED" to e("Pedido ya cerrado", "Este pedido ya fue cobrado o cerrado y no se puede cancelar. Actualiza para ver el estado.", ErrorAction.REFRESH),
        "SESSION_NOT_ASSIGNABLE" to e("No se puede asignar", "Este pedido ya no se puede asignar: está cerrado, entregado o cancelado. Actualiza para ver el estado.", ErrorAction.REFRESH),
        "SESSION_NOT_DELIVERY" to e("No es un pedido de delivery", "Este pedido no es de delivery, así que no se gestiona desde Delivery. Actualiza para ver el estado.", ErrorAction.REFRESH),
        "USE_DELIVERY_ASSIGNMENT" to e("Cambia el estado desde Delivery", "Este pedido ya tiene un repartidor. Cambia su estado desde Delivery.", ErrorAction.REFRESH),
        // D2.0: tarifa de delivery, al final del bloque (alfabético entre sí).
        "DELIVERY_FEE_FORBIDDEN" to e("Sin permiso para la tarifa", "No tienes permiso para cambiar la tarifa de delivery.", ErrorAction.CONTACT_ADMIN),
        "DELIVERY_FEE_INVALID" to e("Tarifa no válida", "Revisa la tarifa: usa un monto entre S/ 0 y S/ 999.99.", ErrorAction.BACK_TO_TRY),
        "DELIVERY_FEE_NOT_EDITABLE" to e("Tarifa no editable", "Este pedido ya no admite cambios en la tarifa de delivery.", ErrorAction.REFRESH),
        // D2b: efectivo contra entrega, al final del bloque (alfabético entre sí; textos exactos de D2B_COMMON §8).
        "CASH_TENDERED_TOO_LOW" to e("Monto no válido", "El monto con el que pagas debe cubrir el total del pedido.", ErrorAction.BACK_TO_TRY),
        "COLLECTION_REQUIRED" to e("Falta cobrar", "Primero marca «Cobrado» antes de marcar la entrega.", ErrorAction.REFRESH),
        "COLLECT_NOT_APPLICABLE" to e("Sin pago contra entrega", "Este pedido no es de pago contra entrega.", ErrorAction.REFRESH),
        "COLLECT_STATUS_INVALID" to e("Aún no puedes cobrar", "Solo puedes cobrar cuando ya recogiste el pedido.", ErrorAction.REFRESH),
        "PAYMENT_MODE_NOT_AVAILABLE" to e("Pago no disponible", "El pago contra entrega no está disponible en este restaurante.", ErrorAction.CONTACT_ADMIN),
        "PAYMENT_NOT_EDITABLE" to e("Pago no editable", "Este pedido ya no admite cambios en el pago.", ErrorAction.REFRESH),
    )

    private fun seriesMissing() = e(
        "Falta la serie",
        "Esta sucursal no tiene una serie de boleta o factura activa. No se cobró. Pide al administrador que la active.",
        ErrorAction.GO_SERIES, ErrorAction.CONTACT_ADMIN,
    )

    /** Codigos conocidos (para pruebas y para el documento de copia). */
    val knownCodes: Set<String> get() = byCode.keys

    fun has(code: String?): Boolean = code != null && (byCode.containsKey(code) || isSummaryVoid(code))

    private fun isSummaryVoid(code: String) = code.startsWith("SUMMARY_VOID_")

    private val SUMMARY_VOID_ENTRY = e(
        "No se pudo anular en SUNAT",
        "No se pudo anular el comprobante en SUNAT. Revisa su estado en Ventas antes de volver a intentar.",
        ErrorAction.GO_SALES, ErrorAction.CONTACT_SUPPORT,
    )

    /** Reserva por flujo y clase de fallo (§15.4 pasos 2, 3 y 5). */
    fun resolve(failure: Failure, flow: ErrorFlow = ErrorFlow.GENERIC): ErrorInfo {
        val support = supportCode(failure)

        // 1) code del backend.
        val code = failure.code?.takeIf { it.isNotBlank() }
        if (code != null) {
            val entry = byCode[code] ?: if (isSummaryVoid(code)) SUMMARY_VOID_ENTRY else null
            if (entry != null) {
                val message = if (code == "PIN_LOCKED") pinLockedMessage(failure.serverMessage, entry.message) else entry.message
                return ErrorInfo(entry.title, message, entry.actions, support)
            }
        }

        // 2) tipo de fallo (red, lectura).
        when (failure.kind) {
            FailureKind.NO_CONNECTION -> return noConnection(flow, support)
            FailureKind.TIMEOUT -> return uncertain(flow, support)
            FailureKind.TLS -> return ErrorInfo(
                "Conexión no segura",
                "No se pudo establecer una conexión segura con el servidor. Revisa la fecha y la hora de tu equipo y tu red, y vuelve a intentar.",
                listOf(ErrorAction.RETRY),
                support,
            )
            FailureKind.UNREADABLE -> return unreadable(flow, support)
            FailureKind.OTHER -> return other(flow, support)
            FailureKind.HTTP -> Unit
        }

        // 3) HTTP.
        val status = failure.status ?: return other(flow, support)
        if (status >= 500) return server(flow, support)
        val readable = readableServerMessage(failure.serverMessage)
        return when (status) {
            401 -> when (flow) {
                ErrorFlow.LOGIN_PIN -> ErrorInfo(
                    "PIN incorrecto",
                    "PIN incorrecto. Vuelve a intentar o cambia de estación.",
                    listOf(ErrorAction.BACK_TO_TRY, ErrorAction.CHANGE_STATION),
                    support,
                )
                ErrorFlow.LOGIN_EMAIL -> ErrorInfo(
                    "Datos incorrectos",
                    "Correo o contraseña incorrectos. Revisa tus datos y vuelve a intentar.",
                    listOf(ErrorAction.BACK_TO_TRY),
                    support,
                )
                else -> ErrorInfo("Sesión vencida", SESSION_EXPIRED, listOf(ErrorAction.RELOGIN), support)
            }
            402 -> ErrorInfo(
                "Servicio suspendido",
                "Tu restaurante tiene el servicio suspendido. Avisa al administrador.",
                listOf(ErrorAction.CONTACT_ADMIN),
                support,
            )
            403 -> if (readable != null) {
                ErrorInfo("Sin permiso", readable, listOf(ErrorAction.CONTACT_ADMIN), support)
            } else {
                ErrorInfo(
                    "Sin permiso",
                    "No tienes permiso para hacer esto. Pídele acceso a tu administrador.",
                    listOf(ErrorAction.CONTACT_ADMIN),
                    support,
                )
            }
            408 -> uncertain(flow, support)
            429 -> ErrorInfo(
                "Demasiados intentos",
                "Demasiados intentos seguidos. Espera un momento y vuelve a intentar.",
                listOf(ErrorAction.BACK_TO_TRY),
                support,
            )
            404 -> ErrorInfo(
                "No encontrado",
                readable ?: "No encontramos lo que buscabas. Actualiza e intenta de nuevo.",
                listOf(ErrorAction.REFRESH),
                support,
            )
            409 -> ErrorInfo(
                "Conflicto",
                readable ?: "Otra persona cambió esto al mismo tiempo. Actualiza y vuelve a intentar.",
                listOf(ErrorAction.REFRESH),
                support,
            )
            else -> {
                // 4) texto del servidor si es legible; 5) reserva por flujo.
                if (readable != null) {
                    ErrorInfo("No se pudo completar", readable, listOf(ErrorAction.BACK_TO_TRY), support)
                } else {
                    ErrorInfo(
                        "No se pudo completar",
                        "No se pudo completar la operación. $CONTACT_SUPPORT_TAIL",
                        listOf(ErrorAction.RETRY, ErrorAction.CONTACT_SUPPORT),
                        support,
                    )
                }
            }
        }
    }

    // ---- Reservas por clase de fallo y flujo ----

    private fun noConnection(flow: ErrorFlow, support: String?): ErrorInfo = when (flow) {
        ErrorFlow.SEND_COMANDA -> ErrorInfo(
            "No se envió el pedido",
            "No se pudo enviar el pedido. No salió a cocina. Revisa tu conexión y vuelve a intentar.",
            listOf(ErrorAction.RETRY, ErrorAction.KEEP_ORDER),
            support,
        )
        ErrorFlow.CHARGE -> ErrorInfo(
            "No se cobró",
            "No hay conexión con el servidor y no se registró el cobro. Revisa tu conexión y vuelve a intentar.",
            listOf(ErrorAction.RETRY),
            support,
        )
        ErrorFlow.OPEN_TABLE -> ErrorInfo(
            "No se abrió la mesa",
            "No se pudo abrir la mesa porque no hay conexión. Intenta de nuevo.",
            listOf(ErrorAction.RETRY),
            support,
        )
        ErrorFlow.VOID_REFUND -> ErrorInfo(
            "No se completó",
            "No hay conexión con el servidor. No se anuló ni se devolvió nada. Revisa tu conexión y vuelve a intentar.",
            listOf(ErrorAction.RETRY),
            support,
        )
        ErrorFlow.CASH -> ErrorInfo(
            "Sin conexión",
            "No hay conexión con el servidor y la operación de caja no se realizó. Tus ventas siguen guardadas. Revisa tu conexión y vuelve a intentar.",
            listOf(ErrorAction.RETRY),
            support,
        )
        ErrorFlow.LOGIN_PIN, ErrorFlow.LOGIN_EMAIL -> ErrorInfo(
            "Sin conexión",
            "No hay conexión con el servidor. Revisa tu internet.",
            listOf(ErrorAction.RETRY),
            support,
        )
        ErrorFlow.GENERIC -> ErrorInfo(
            "Sin conexión",
            "No hay conexión con el servidor. Revisa tu internet y vuelve a intentar.",
            listOf(ErrorAction.RETRY),
            support,
        )
    }

    /** Timeout o corte a mitad: la peticion pudo ejecutarse. Nunca se reintenta sola. */
    private fun uncertain(flow: ErrorFlow, support: String?): ErrorInfo = when (flow) {
        ErrorFlow.SEND_COMANDA -> ErrorInfo(
            "No pudimos confirmar el envío",
            "No pudimos confirmar el envío del pedido. Puede que sí haya llegado a cocina: revisa la mesa antes de volver a enviarlo para no duplicarlo.",
            listOf(ErrorAction.REFRESH, ErrorAction.RETRY),
            support,
        )
        ErrorFlow.CHARGE -> ErrorInfo(
            "No pudimos confirmar el cobro",
            "No pudimos confirmar el cobro. Antes de intentar de nuevo, revisa en Ventas si ya se registró para no cobrar dos veces.",
            listOf(ErrorAction.GO_SALES, ErrorAction.RETRY),
            support,
        )
        ErrorFlow.OPEN_TABLE -> ErrorInfo(
            "No pudimos confirmar",
            "No pudimos confirmar si la mesa se abrió. Actualiza las mesas y vuelve a intentar.",
            listOf(ErrorAction.REFRESH, ErrorAction.RETRY),
            support,
        )
        ErrorFlow.VOID_REFUND -> ErrorInfo(
            "No pudimos confirmar",
            "No pudimos confirmar la operación. Antes de volver a intentar, revisa en Ventas si ya quedó registrada.",
            listOf(ErrorAction.GO_SALES, ErrorAction.RETRY),
            support,
        )
        ErrorFlow.CASH -> ErrorInfo(
            "No pudimos confirmar",
            "No pudimos confirmar la operación de caja. Actualiza tu caja para ver si se registró antes de reintentar. Tus ventas siguen guardadas.",
            listOf(ErrorAction.REFRESH, ErrorAction.RETRY),
            support,
        )
        ErrorFlow.LOGIN_PIN, ErrorFlow.LOGIN_EMAIL -> ErrorInfo(
            "El servidor tardó en responder",
            "El servidor tardó demasiado en responder. Revisa tu internet y vuelve a intentar.",
            listOf(ErrorAction.RETRY),
            support,
        )
        ErrorFlow.GENERIC -> ErrorInfo(
            "El servidor tardó en responder",
            "El servidor tardó demasiado en responder. Revisa tu conexión y vuelve a intentar.",
            listOf(ErrorAction.RETRY),
            support,
        )
    }

    /** 5xx: el servidor fallo; NO es "sin conexion". */
    private fun server(flow: ErrorFlow, support: String?): ErrorInfo = when (flow) {
        ErrorFlow.SEND_COMANDA -> ErrorInfo(
            "Problema de nuestro lado",
            "Tuvimos un problema de nuestro lado y no pudimos confirmar el pedido. Revisa la mesa antes de volver a enviarlo para no duplicarlo.",
            listOf(ErrorAction.REFRESH, ErrorAction.RETRY),
            support,
        )
        ErrorFlow.CHARGE -> ErrorInfo(
            "Problema de nuestro lado",
            "Tuvimos un problema de nuestro lado y no pudimos confirmar el cobro. Antes de intentar de nuevo, revisa en Ventas si ya se registró.",
            listOf(ErrorAction.GO_SALES, ErrorAction.RETRY),
            support,
        )
        ErrorFlow.OPEN_TABLE -> ErrorInfo(
            "Problema de nuestro lado",
            "Tuvimos un problema de nuestro lado. Actualiza las mesas y vuelve a intentar.",
            listOf(ErrorAction.REFRESH, ErrorAction.RETRY),
            support,
        )
        ErrorFlow.VOID_REFUND -> ErrorInfo(
            "Problema de nuestro lado",
            "Tuvimos un problema de nuestro lado y no pudimos confirmar la operación. Revisa en Ventas si ya quedó registrada antes de volver a intentar.",
            listOf(ErrorAction.GO_SALES, ErrorAction.RETRY),
            support,
        )
        ErrorFlow.CASH -> ErrorInfo(
            "Problema de nuestro lado",
            "Tuvimos un problema de nuestro lado. Tus ventas siguen guardadas. Actualiza tu caja y vuelve a intentar en unos minutos.",
            listOf(ErrorAction.REFRESH, ErrorAction.RETRY),
            support,
        )
        ErrorFlow.LOGIN_PIN, ErrorFlow.LOGIN_EMAIL -> ErrorInfo(
            "Problema de nuestro lado",
            "Tuvimos un problema de nuestro lado. Vuelve a intentar en unos minutos.",
            listOf(ErrorAction.RETRY),
            support,
        )
        ErrorFlow.GENERIC -> ErrorInfo(
            "Problema de nuestro lado",
            "Tuvimos un problema de nuestro lado. No se perdió nada de lo que ya cobraste. Vuelve a intentar en unos minutos.",
            listOf(ErrorAction.RETRY),
            support,
        )
    }

    /** El servidor respondio pero no se pudo leer (HTML de proxy, JSON roto). */
    private fun unreadable(flow: ErrorFlow, support: String?): ErrorInfo = when (flow) {
        ErrorFlow.CHARGE -> ErrorInfo(
            "Respuesta ilegible",
            "No pudimos leer la respuesta del servidor. Antes de intentar de nuevo, revisa en Ventas si el cobro ya se registró.",
            listOf(ErrorAction.GO_SALES, ErrorAction.RETRY),
            support,
        )
        ErrorFlow.SEND_COMANDA -> ErrorInfo(
            "Respuesta ilegible",
            "No pudimos leer la respuesta del servidor. Revisa la mesa antes de volver a enviar el pedido para no duplicarlo.",
            listOf(ErrorAction.REFRESH, ErrorAction.RETRY),
            support,
        )
        else -> ErrorInfo(
            "Respuesta ilegible",
            "No pudimos leer la respuesta del servidor. $CONTACT_SUPPORT_TAIL",
            listOf(ErrorAction.RETRY, ErrorAction.CONTACT_SUPPORT),
            support,
        )
    }

    private fun other(flow: ErrorFlow, support: String?): ErrorInfo = when (flow) {
        ErrorFlow.SEND_COMANDA -> ErrorInfo(
            "No se pudo enviar",
            "No se pudo enviar el pedido. Revisa la mesa antes de volver a enviarlo. $CONTACT_SUPPORT_TAIL",
            listOf(ErrorAction.REFRESH, ErrorAction.RETRY),
            support,
        )
        ErrorFlow.CHARGE -> ErrorInfo(
            "No se pudo cobrar",
            "No se pudo completar el cobro. Antes de intentar de nuevo, revisa en Ventas si ya se registró.",
            listOf(ErrorAction.GO_SALES, ErrorAction.RETRY),
            support,
        )
        else -> ErrorInfo(
            "No se pudo completar",
            "No se pudo completar la operación. $CONTACT_SUPPORT_TAIL",
            listOf(ErrorAction.RETRY, ErrorAction.CONTACT_SUPPORT),
            support,
        )
    }

    // ---- Utilidades puras ----

    private fun supportCode(f: Failure): String? {
        val parts = buildList {
            f.code?.takeIf { it.isNotBlank() }?.let { add(it) }
            f.status?.let { add("HTTP $it") }
            if (f.kind != FailureKind.HTTP) add(f.kind.name)
        }
        return parts.joinToString(" · ").ifBlank { null }
    }

    /** "espera N minuto(s)" del backend -> "Espera N minutos"; si no hay numero, el texto base. */
    private fun pinLockedMessage(serverMessage: String?, base: String): String {
        val minutes = serverMessage?.let { Regex("(\\d+)\\s*minuto").find(it)?.groupValues?.get(1)?.toIntOrNull() }
            ?: return base
        val unit = if (minutes == 1) "minuto" else "minutos"
        return "Demasiados intentos. Espera $minutes $unit o pídele al administrador que restablezca el PIN."
    }

    private val JARGON = listOf(
        "exception", "stacktrace", "java.", "kotlin.", "javax.", "okhttp", "retrofit", "sqlstate", "gorm",
        "nil pointer", "panic", "goroutine", "null", "<html", "<!doctype", "<body", "{", "}", "http ", "err_",
    )

    /**
     * Texto `error` del servidor que se puede mostrar tal cual: corto, de una linea, sin HTML ni
     * excepciones. Si no, null (cae a la reserva por flujo).
     */
    fun readableServerMessage(raw: String?): String? {
        val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (text.length > 220 || text.contains('\n')) return null
        val lower = text.lowercase()
        if (JARGON.any { lower.contains(it) }) return null
        return text
    }
}
