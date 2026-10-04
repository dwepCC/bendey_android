# Catálogo de errores accionables: textos y acciones (R5, oleada 2)

Fuente: `core/network/src/main/java/com/bendey/restaurant/core/network/error/ErrorCatalog.kt` (Android).
Tauri debe copiar estas cadenas TAL CUAL a `src/utils/errorCatalog.ts` (mismas claves, mismo texto, mismo orden).
Este archivo se regenera desde el catálogo (`BENDEY_WRITE_ERROR_COPY=1 ./gradlew :core:network:testDebugUnitTest --tests '*ErrorCopyDocGenerator*'`) y `ErrorCatalogTest.copyDocumentMatchesCatalog` falla si se desalinea.

Reglas (UX-REDESIGN §15.2-§15.4): tuteo, sin jerga; todo error de comanda, cobro, caja y anulación dice SI LA OPERACIÓN SE REALIZÓ O NO; todo error lleva al menos una acción; el código de soporte (`CODE · HTTP n · clase`) va en un detalle plegable con Copiar, nunca en el mensaje. Nunca se reintenta solo una operación no idempotente (cobrar, enviar comanda).

## Orden de resolución

1. `code` del backend (tabla de abajo; `SUMMARY_VOID_*` comparte una entrada).
2. Tipo de fallo de red: sin conexión (la petición NO salió) / timeout o corte (pudo ejecutarse) / TLS / respuesta ilegible / otro.
3. HTTP: 5xx → "problema de nuestro lado" (nunca "conexión"); 401 → sesión vencida (en login: PIN/credenciales); 402 → servicio suspendido; 403/404/409/400 → texto legible del servidor si lo hay, si no la reserva; 429 → espera.
4. Texto `error` del servidor SOLO si es legible: una línea, ≤ 220 caracteres, sin HTML, llaves ni nombres de excepción.
5. Reserva por flujo (tablas de abajo). Nunca "Error" a secas.

## Acciones (etiqueta del botón)

| Clave | Etiqueta |
|---|---|
| `RETRY` | Reintentar |
| `KEEP_ORDER` | Mantener pedido en pantalla |
| `REFRESH` | Actualizar |
| `GO_TABLES` | Ir a Mesas |
| `GO_SALES` | Ver Ventas |
| `OPEN_CASH` | Abrir caja |
| `GO_MY_CASH` | Ir a mi caja |
| `GO_SERIES` | Ir a Series |
| `CHOOSE_BRANCH` | Elegir sucursal |
| `CHANGE_STATION` | Cambiar estación |
| `RELOGIN` | Iniciar sesión |
| `CREATE_PIN` | Crear PIN |
| `BACK_TO_TRY` | Volver a intentar |
| `VIEW_PLANS` | Ver planes |
| `CONTACT_ADMIN` | Hablar con el administrador |
| `CONTACT_SUPPORT` | Contactar a soporte |

## Códigos del backend (code → título → mensaje → acciones)

| code | título | mensaje | acciones |
|---|---|---|---|
| `AUTH_TOKEN_INVALID` | Sesión vencida | Tu sesión venció. Vuelve a iniciar sesión para continuar. | [Iniciar sesión] |
| `AUTH_TOKEN_MISSING` | Sesión vencida | Tu sesión venció. Vuelve a iniciar sesión para continuar. | [Iniciar sesión] |
| `TOKEN_TENANT_INVALID` | Sesión vencida | Tu sesión venció. Vuelve a iniciar sesión para continuar. | [Iniciar sesión] |
| `TENANT_ISOLATION_VIOLATION` | Sesión vencida | Tu sesión venció. Vuelve a iniciar sesión para continuar. | [Iniciar sesión] |
| `AUTH_USER_NOT_FOUND` | Usuario no encontrado | Tu usuario ya no existe en este restaurante. Vuelve a iniciar sesión o habla con tu administrador. | [Iniciar sesión] · [Hablar con el administrador] |
| `BRANCH_REQUIRED` | Falta la sucursal | Elige tu sucursal para continuar. | [Elegir sucursal] |
| `BRANCH_FORBIDDEN` | Sucursal no permitida | No tienes acceso a esa sucursal. Elige otra o habla con tu administrador. | [Elegir sucursal] · [Hablar con el administrador] |
| `SUBSCRIPTION_REQUIRED` | Servicio suspendido | Tu restaurante tiene el servicio suspendido. Avisa al administrador. | [Hablar con el administrador] |
| `TENANT_BLOCKED` | Servicio suspendido | Tu restaurante tiene el servicio suspendido. Avisa al administrador. | [Hablar con el administrador] |
| `PAYMENT_BLOCKED` | Servicio suspendido | Tu restaurante tiene el servicio suspendido. Avisa al administrador. | [Hablar con el administrador] |
| `DOCUMENT_QUOTA_EXCEEDED` | Límite del plan | Llegaste al límite de comprobantes de tu plan este mes. No se emitió el comprobante. | [Ver planes] · [Hablar con el administrador] |
| `ELECTRONIC_BILLING_DISABLED` | Facturación no disponible | Tu plan solo permite notas de venta. No se cobró: elige nota de venta o revisa tu plan. | [Ver planes] · [Volver a intentar] |
| `menu_disabled` | Función desactivada | Esta función está desactivada en tu restaurante. Pídele al administrador que la active. | [Hablar con el administrador] |
| `takeaway_disabled` | Función desactivada | Esta función está desactivada en tu restaurante. Pídele al administrador que la active. | [Hablar con el administrador] |
| `delivery_disabled` | Función desactivada | Esta función está desactivada en tu restaurante. Pídele al administrador que la active. | [Hablar con el administrador] |
| `menu_digital_disabled` | Función desactivada | Esta función está desactivada en tu restaurante. Pídele al administrador que la active. | [Hablar con el administrador] |
| `IDEMPOTENCY_IN_PROGRESS` | La comanda se está enviando | La comanda anterior todavía se está enviando. Espera un momento y vuelve a intentar: no se duplicará. | [Reintentar] |
| `ORDER_EMPTY` | Pedido vacío | El pedido está vacío. Agrega al menos un producto antes de enviarlo a cocina. | [Volver a intentar] |
| `SESSION_CLOSED` | Mesa cerrada | Esta mesa ya fue cerrada o cobrada. Actualiza para ver el estado. | [Actualizar] · [Ir a Mesas] |
| `SESSION_UPDATED` | La mesa cambió | Otra persona modificó esta mesa mientras armabas el pedido. Actualiza para ver lo último y vuelve a intentar. | [Actualizar] |
| `SESSION_NOT_FOUND` | Cuenta no encontrada | No encontramos esta cuenta. Actualiza las mesas para ver el estado. | [Actualizar] · [Ir a Mesas] |
| `SESSION_NOT_OPEN` | Cuenta cerrada | Esta cuenta ya no está abierta. Actualiza para ver el estado. | [Actualizar] · [Ir a Mesas] |
| `SESSION_MOVED` | Cuenta movida | La cuenta ya no está en esa mesa. Actualiza e intenta de nuevo. | [Actualizar] |
| `TABLE_OCCUPIED` | Mesa ocupada | No se pudo abrir la mesa. Puede que otro mozo la acabe de ocupar. Actualiza las mesas. | [Actualizar] |
| `TABLE_NOT_FOUND` | Mesa no encontrada | Esa mesa ya no existe. Actualiza las mesas. | [Actualizar] |
| `TABLE_NOT_AVAILABLE` | Mesa no disponible | La mesa destino no está disponible. Elige otra mesa. | [Volver a intentar] |
| `TABLE_CLOSE_PENDING_BALANCE` | Consumo pendiente | Esta mesa tiene consumo pendiente de cobro. Cóbrala antes de cerrarla. | [Actualizar] |
| `TABLE_CLOSE_PENDING_COMANDAS` | Comandas pendientes | Quedan comandas pendientes en esta mesa. Cóbralas o anúlalas antes de cerrarla. | [Actualizar] |
| `COMANDA_NOT_FOUND` | Comanda no encontrada | No encontramos la comanda. Actualiza para ver el estado. | [Actualizar] |
| `COMANDA_CANCELLED` | Comanda anulada | Esta comanda ya fue anulada. Actualiza para ver el estado. | [Actualizar] |
| `COMANDA_STATUS_BACKWARDS` | Estado no válido | La comanda ya avanzó de estado y no puede retroceder. Actualiza para ver el estado. | [Actualizar] |
| `COMANDA_ALREADY_DELIVERED` | Comanda entregada | Esta comanda ya fue entregada y no se puede anular. | [Actualizar] |
| `COMANDA_ALREADY_BILLED` | Ya cobrada | Esta comanda ya fue cobrada. Revisa en Ventas el comprobante; no se cobró de nuevo. | [Ver Ventas] · [Actualizar] |
| `COMANDA_NOTHING_TO_BILL` | Nada por cobrar | No hay productos por cobrar en esta mesa. Actualiza para ver el estado. | [Actualizar] |
| `COMANDA_NOT_BILLABLE` | No se puede cobrar | Algunas comandas ya no están disponibles para cobro. No se cobró. Actualiza la mesa y vuelve a intentar. | [Actualizar] |
| `COMANDA_WRONG_SESSION` | No se puede cobrar | Algunas comandas son de otra cuenta. No se cobró. Actualiza la mesa y vuelve a intentar. | [Actualizar] |
| `SALE_EMPTY` | Venta vacía | La venta está vacía. Agrega al menos un producto. No se cobró. | [Volver a intentar] |
| `CART_EMPTY` | Venta vacía | La venta está vacía. Agrega al menos un producto. No se cobró. | [Volver a intentar] |
| `PAYMENT_BELOW_TOTAL` | Pago incompleto | Lo pagado es menor al total de la cuenta. No se cobró. Revisa los montos y vuelve a intentar. | [Volver a intentar] |
| `PAYMENT_REQUIRED` | Falta el pago | Elige al menos un método de pago. No se cobró. | [Volver a intentar] |
| `PAYMENT_METHOD_NO_ACCOUNT` | Método sin cuenta | Este método de pago no tiene una cuenta asociada. No se cobró. Pídele al administrador que lo configure en Caja → Cuentas. | [Hablar con el administrador] |
| `CREDIT_REQUIRES_CUSTOMER` | Falta el cliente | Para vender al crédito elige un cliente. No se cobró. | [Volver a intentar] |
| `CUSTOMER_RUC_INVALID` | RUC no válido | El RUC del cliente solo puede tener números. Corrígelo y vuelve a intentar. No se cobró. | [Volver a intentar] |
| `INVOICE_REQUIRES_RUC` | Falta el RUC | Para emitir factura el cliente necesita un RUC de 11 dígitos. Elige boleta o corrige el cliente. No se cobró. | [Volver a intentar] |
| `SERIES_REQUIRED` | Falta la serie | Esta sucursal no tiene una serie de boleta o factura activa. No se cobró. Pide al administrador que la active. | [Ir a Series] · [Hablar con el administrador] |
| `SERIES_INACTIVE` | Falta la serie | Esta sucursal no tiene una serie de boleta o factura activa. No se cobró. Pide al administrador que la active. | [Ir a Series] · [Hablar con el administrador] |
| `SERIES_NOT_FOUND` | Falta la serie | Esta sucursal no tiene una serie de boleta o factura activa. No se cobró. Pide al administrador que la active. | [Ir a Series] · [Hablar con el administrador] |
| `SERIES_WRONG_BRANCH` | Serie de otra sucursal | La serie elegida es de otra sucursal. Elige una serie de esta sucursal. No se cobró. | [Volver a intentar] · [Ir a Series] |
| `SERIES_INVALID` | Serie no válida | La serie elegida no sirve para este comprobante. Elige otra. | [Volver a intentar] · [Ir a Series] |
| `SERIES_DUPLICATE` | Serie repetida | Ese código de serie ya está en uso. Usa uno distinto. | [Volver a intentar] |
| `CASH_WAITER_NOT_ALLOWED` | Cobro en efectivo no permitido | Los mozos no cobran en efectivo. No se cobró: usa otro método de pago o pide a un cajero que cobre. | [Volver a intentar] |
| `CASH_SESSION_REQUIRED` | Caja cerrada | Para cobrar en efectivo necesitas abrir tu caja. | [Abrir caja] |
| `CASH_SESSION_CLOSED` | Caja cerrada | Tu caja ya está cerrada. Ábrela de nuevo para cobrar o registrar movimientos. | [Abrir caja] · [Actualizar] |
| `CASH_SESSION_FOREIGN_BRANCH` | Caja de otra sucursal | Esa caja es de otra sucursal. Usa la caja de tu sucursal. | [Ir a mi caja] |
| `CASH_SESSION_NOT_FOUND` | Caja no encontrada | No encontramos esa caja. Actualiza e intenta de nuevo. | [Actualizar] · [Ir a mi caja] |
| `CASH_NOT_OWNER` | Caja de otra persona | Solo puedes operar tu propia caja. Abre la tuya o pide a su dueño que lo haga. | [Ir a mi caja] |
| `CASH_ALREADY_OPEN` | Caja ya abierta | Ya tienes una caja abierta en este local. Ciérrala o continúa con ella. | [Ir a mi caja] |
| `CASH_ARQUEO_LOCKED` | Caja ya cerrada | Esta caja ya tiene el conteo registrado y no se puede modificar. | [Actualizar] |
| `CASH_OPENING_INVALID` | Monto no válido | El monto inicial no es un número válido. Corrígelo y vuelve a intentar. | [Volver a intentar] |
| `CASH_OPENING_NEGATIVE` | Monto no válido | El monto inicial no puede ser negativo. Corrígelo y vuelve a intentar. | [Volver a intentar] |
| `CASH_OPENING_TOO_LARGE` | Monto muy alto | El monto inicial es demasiado alto. Revisa que esté bien escrito. | [Volver a intentar] |
| `MOVEMENT_ALREADY_VOIDED` | Movimiento anulado | Este movimiento ya estaba anulado. | [Actualizar] |
| `MOVEMENT_AMOUNT_INVALID` | Monto no válido | El monto debe ser mayor a cero. | [Volver a intentar] |
| `MOVEMENT_NOT_FOUND` | Movimiento no encontrado | No encontramos el movimiento. Actualiza la lista. | [Actualizar] |
| `MOVEMENT_NOT_MANUAL` | No se puede anular aquí | Solo se anulan aquí los ingresos y egresos registrados a mano. Los de ventas o compras se anulan desde su documento. | [Actualizar] |
| `MOVEMENT_NO_CHANGES` | Sin cambios | No hay nada que corregir: los datos son los mismos. | [Volver a intentar] |
| `MOVEMENT_REASON_REQUIRED` | Falta el motivo | Escribe el motivo y vuelve a intentar. | [Volver a intentar] |
| `MOVEMENT_SESSION_CLOSED` | Caja cerrada | La caja de ese movimiento ya está cerrada. Registra un movimiento de corrección en la caja actual. | [Ir a mi caja] |
| `MOVEMENT_TYPE_INVALID` | Tipo no válido | Elige si es un ingreso o un egreso. | [Volver a intentar] |
| `PIN_INCORRECT` | PIN incorrecto | PIN incorrecto. No se hizo ningún cambio. Vuelve a intentar. | [Volver a intentar] |
| `PIN_REQUIRED` | Falta el PIN | Ingresa el PIN de autorización para continuar. | [Volver a intentar] |
| `PIN_NOT_CONFIGURED` | Sin PIN de autorización | Aún no hay PIN de autorización. Pídele al administrador que lo cree en Ajustes → Restaurante. | [Crear PIN] · [Hablar con el administrador] |
| `PIN_LOCKED` | PIN bloqueado | Demasiados intentos. Espera unos minutos o pídele al administrador que restablezca el PIN. | [Hablar con el administrador] |
| `PIN_UNAVAILABLE` | No se pudo verificar el PIN | No pudimos verificar el PIN. No se hizo ningún cambio. Revisa tu conexión y vuelve a intentar. | [Reintentar] |
| `PIN_DUPLICATE` | PIN repetido | Ese PIN ya lo usa otra persona del restaurante. Elige otro. | [Volver a intentar] |
| `PIN_FORMAT_INVALID` | PIN no válido | El PIN debe tener entre 4 y 6 dígitos, solo números. | [Volver a intentar] |
| `CANCEL_REASON_REQUIRED` | Falta el motivo | Escribe el motivo de la anulación y vuelve a intentar. | [Volver a intentar] |
| `ORDER_NOT_FOUND` | Pedido no encontrado | No encontramos el pedido. Actualiza para ver el estado. | [Actualizar] |
| `ORDER_NOT_OPEN` | Pedido no abierto | Solo se pueden anular pedidos abiertos. Actualiza para ver el estado. | [Actualizar] |
| `ORDER_ALREADY_BILLED` | Pedido ya cobrado | Este pedido ya fue cobrado y no se puede anular. Si hay que devolver el dinero, hazlo desde Ventas. | [Ver Ventas] |
| `ORDER_HAS_ITEMS` | El pedido tiene productos | Este pedido todavía tiene productos. Anúlalos primero o anula el pedido completo con el PIN. | [Volver a intentar] |
| `SALE_NOT_FOUND` | Venta no encontrada | No encontramos la venta. Actualiza Ventas para ver el estado. | [Actualizar] · [Ver Ventas] |
| `SALE_ALREADY_CANCELLED` | Venta ya anulada | Esta venta ya estaba anulada. No se hizo ningún cambio. | [Ver Ventas] |
| `SALE_ALREADY_ELECTRONIC` | Ya tiene comprobante | Esta nota ya tiene factura o boleta electrónica, por eso no se puede anular. Usa una nota de crédito. | [Ver Ventas] |
| `SALE_VOID_NEEDS_CREDIT_NOTE` | Usa nota de crédito | Las facturas y boletas se anulan con nota de crédito. No se anuló nada. | [Ver Ventas] |
| `REFUND_ALREADY_DONE` | Ya devuelta | Esta venta ya tiene una devolución registrada. No se devolvió dinero de nuevo. | [Ver Ventas] |
| `REFUND_CONFIRMATION_REQUIRED` | Falta confirmar | Confirma que entregaste el dinero al cliente. No se registró la devolución. | [Volver a intentar] |
| `REFUND_FAILED` | No se pudo devolver | No se pudo registrar la devolución. Antes de volver a intentar, revisa en Ventas si ya quedó registrada; si sigue igual, avisa a soporte. | [Ver Ventas] · [Contactar a soporte] |
| `REFUND_NO_PAYMENT` | Sin cobro que devolver | Esta venta no tiene un cobro registrado que se pueda devolver. | [Ver Ventas] |
| `REFUND_USER_REQUIRED` | Sesión incompleta | No pudimos saber quién registra la devolución. Vuelve a iniciar sesión e intenta de nuevo. | [Iniciar sesión] |
| `SUMMARY_ALREADY_EXISTS` | Ya enviado a SUNAT | La anulación de este comprobante ya fue enviada a SUNAT. Revisa su estado en Ventas. | [Ver Ventas] |
| `LOGIN_PIN_INCORRECT` | PIN incorrecto | PIN incorrecto o no corresponde a esta estación. Vuelve a intentar o cambia de estación. | [Volver a intentar] · [Cambiar estación] |
| `LOGIN_PIN_FORMAT` | PIN no válido | El PIN debe tener entre 4 y 6 dígitos, solo números. | [Volver a intentar] |
| `LOGIN_PIN_AMBIGUOUS` | PIN repetido | Este PIN está repetido en el sistema. Avisa a tu administrador para que lo cambie. | [Hablar con el administrador] |
| `LOGIN_PIN_RATE_LIMITED` | Demasiados intentos | Demasiados intentos. Espera unos minutos y vuelve a intentar. | [Volver a intentar] |
| `LOGIN_STATION_INVALID` | Estación no válida | Esa estación no existe. Elige tu estación de nuevo. | [Cambiar estación] |
| `SUMMARY_VOID_*` | No se pudo anular en SUNAT | No se pudo anular el comprobante en SUNAT. Revisa su estado en Ventas antes de volver a intentar. | [Ver Ventas] · [Contactar a soporte] |

`PIN_LOCKED`: si el servidor trae `espera N minuto(s)`, el mensaje pasa a "Demasiados intentos. Espera N minutos o pídele al administrador que restablezca el PIN." ("minuto" si N = 1).

## Reserva por clase de fallo y flujo (code desconocido o ausente)

### Sin conexión (la petición NO salió)

| flujo | mensaje | acciones |
|---|---|---|
| Genérico (lecturas, listas, ajustes) (`GENERIC`) | No hay conexión con el servidor. Revisa tu internet y vuelve a intentar. | [Reintentar] |
| Enviar comanda (`SEND_COMANDA`) | No se pudo enviar el pedido. No salió a cocina. Revisa tu conexión y vuelve a intentar. | [Reintentar] · [Mantener pedido en pantalla] |
| Cobrar (`CHARGE`) | No hay conexión con el servidor y no se registró el cobro. Revisa tu conexión y vuelve a intentar. | [Reintentar] |
| Abrir mesa (`OPEN_TABLE`) | No se pudo abrir la mesa porque no hay conexión. Intenta de nuevo. | [Reintentar] |
| Anular / devolver (`VOID_REFUND`) | No hay conexión con el servidor. No se anuló ni se devolvió nada. Revisa tu conexión y vuelve a intentar. | [Reintentar] |
| Caja (abrir / cerrar / mover) (`CASH`) | No hay conexión con el servidor y la operación de caja no se realizó. Tus ventas siguen guardadas. Revisa tu conexión y vuelve a intentar. | [Reintentar] |
| Login por PIN (`LOGIN_PIN`) | No hay conexión con el servidor. Revisa tu internet. | [Reintentar] |
| Login por correo (`LOGIN_EMAIL`) | No hay conexión con el servidor. Revisa tu internet. | [Reintentar] |

### Timeout o corte (pudo ejecutarse)

| flujo | mensaje | acciones |
|---|---|---|
| Genérico (lecturas, listas, ajustes) (`GENERIC`) | El servidor tardó demasiado en responder. Revisa tu conexión y vuelve a intentar. | [Reintentar] |
| Enviar comanda (`SEND_COMANDA`) | No pudimos confirmar el envío del pedido. Puede que sí haya llegado a cocina: revisa la mesa antes de volver a enviarlo para no duplicarlo. | [Actualizar] · [Reintentar] |
| Cobrar (`CHARGE`) | No pudimos confirmar el cobro. Antes de intentar de nuevo, revisa en Ventas si ya se registró para no cobrar dos veces. | [Ver Ventas] · [Reintentar] |
| Abrir mesa (`OPEN_TABLE`) | No pudimos confirmar si la mesa se abrió. Actualiza las mesas y vuelve a intentar. | [Actualizar] · [Reintentar] |
| Anular / devolver (`VOID_REFUND`) | No pudimos confirmar la operación. Antes de volver a intentar, revisa en Ventas si ya quedó registrada. | [Ver Ventas] · [Reintentar] |
| Caja (abrir / cerrar / mover) (`CASH`) | No pudimos confirmar la operación de caja. Actualiza tu caja para ver si se registró antes de reintentar. Tus ventas siguen guardadas. | [Actualizar] · [Reintentar] |
| Login por PIN (`LOGIN_PIN`) | El servidor tardó demasiado en responder. Revisa tu internet y vuelve a intentar. | [Reintentar] |
| Login por correo (`LOGIN_EMAIL`) | El servidor tardó demasiado en responder. Revisa tu internet y vuelve a intentar. | [Reintentar] |

### 5xx (el servidor falló; NO es falta de red)

| flujo | mensaje | acciones |
|---|---|---|
| Genérico (lecturas, listas, ajustes) (`GENERIC`) | Tuvimos un problema de nuestro lado. No se perdió nada de lo que ya cobraste. Vuelve a intentar en unos minutos. | [Reintentar] |
| Enviar comanda (`SEND_COMANDA`) | Tuvimos un problema de nuestro lado y no pudimos confirmar el pedido. Revisa la mesa antes de volver a enviarlo para no duplicarlo. | [Actualizar] · [Reintentar] |
| Cobrar (`CHARGE`) | Tuvimos un problema de nuestro lado y no pudimos confirmar el cobro. Antes de intentar de nuevo, revisa en Ventas si ya se registró. | [Ver Ventas] · [Reintentar] |
| Abrir mesa (`OPEN_TABLE`) | Tuvimos un problema de nuestro lado. Actualiza las mesas y vuelve a intentar. | [Actualizar] · [Reintentar] |
| Anular / devolver (`VOID_REFUND`) | Tuvimos un problema de nuestro lado y no pudimos confirmar la operación. Revisa en Ventas si ya quedó registrada antes de volver a intentar. | [Ver Ventas] · [Reintentar] |
| Caja (abrir / cerrar / mover) (`CASH`) | Tuvimos un problema de nuestro lado. Tus ventas siguen guardadas. Actualiza tu caja y vuelve a intentar en unos minutos. | [Actualizar] · [Reintentar] |
| Login por PIN (`LOGIN_PIN`) | Tuvimos un problema de nuestro lado. Vuelve a intentar en unos minutos. | [Reintentar] |
| Login por correo (`LOGIN_EMAIL`) | Tuvimos un problema de nuestro lado. Vuelve a intentar en unos minutos. | [Reintentar] |

### Respuesta ilegible (HTML de proxy en un 2xx, JSON roto)

| flujo | mensaje | acciones |
|---|---|---|
| Cobrar (`CHARGE`) | No pudimos leer la respuesta del servidor. Antes de intentar de nuevo, revisa en Ventas si el cobro ya se registró. | [Ver Ventas] · [Reintentar] |
| Enviar comanda (`SEND_COMANDA`) | No pudimos leer la respuesta del servidor. Revisa la mesa antes de volver a enviar el pedido para no duplicarlo. | [Actualizar] · [Reintentar] |
| Genérico (lecturas, listas, ajustes) (`GENERIC`) | No pudimos leer la respuesta del servidor. Vuelve a intentar; si sigue igual, avisa a soporte. | [Reintentar] · [Contactar a soporte] |

### Otros fallos

| caso | mensaje | acciones |
|---|---|---|
| TLS / certificado | No se pudo establecer una conexión segura con el servidor. Revisa la fecha y la hora de tu equipo y tu red, y vuelve a intentar. | [Reintentar] |
| Otro fallo (cobrar, `CHARGE`) | No se pudo completar el cobro. Antes de intentar de nuevo, revisa en Ventas si ya se registró. | [Ver Ventas] · [Reintentar] |
| Otro fallo (enviar comanda, `SEND_COMANDA`) | No se pudo enviar el pedido. Revisa la mesa antes de volver a enviarlo. Vuelve a intentar; si sigue igual, avisa a soporte. | [Actualizar] · [Reintentar] |
| Otro fallo (resto de flujos) | No se pudo completar la operación. Vuelve a intentar; si sigue igual, avisa a soporte. | [Reintentar] · [Contactar a soporte] |
| HTTP 401 sin code, fuera de login (sesión) | Tu sesión venció. Vuelve a iniciar sesión para continuar. | [Iniciar sesión] |
| HTTP 401 sin code, login por PIN | PIN incorrecto. Vuelve a intentar o cambia de estación. | [Volver a intentar] · [Cambiar estación] |
| HTTP 401 sin code, login por correo | Correo o contraseña incorrectos. Revisa tus datos y vuelve a intentar. | [Volver a intentar] |
| HTTP 402 (suscripción) | Tu restaurante tiene el servicio suspendido. Avisa al administrador. | [Hablar con el administrador] |
| HTTP 403 sin texto legible | No tienes permiso para hacer esto. Pídele acceso a tu administrador. | [Hablar con el administrador] |
| HTTP 404 sin texto legible | No encontramos lo que buscabas. Actualiza e intenta de nuevo. | [Actualizar] |
| HTTP 409 sin texto legible | Otra persona cambió esto al mismo tiempo. Actualiza y vuelve a intentar. | [Actualizar] |
| HTTP 429 | Demasiados intentos seguidos. Espera un momento y vuelve a intentar. | [Volver a intentar] |
| HTTP 4xx desconocido sin texto legible | No se pudo completar la operación. Vuelve a intentar; si sigue igual, avisa a soporte. | [Reintentar] · [Contactar a soporte] |

Textos fijos de login que no vienen del catálogo (validación local): "Ingresa al menos 4 dígitos", "Escribe tu correo y tu contraseña.", "Tu usuario no tiene permisos operativos".

