package com.bendey.restaurant.core.domain.onboarding.wizard

/**
 * UN solo módulo de textos del wizard (R4). Las claves y las cadenas son IDÉNTICAS a las de Tauri
 * (`wizardCopy.ts`); si cambias un texto aquí, cámbialo allá. Tuteo siempre.
 */
object WizardCopy {
    const val APP_NAME = "Bendey Resto"

    // Registro
    const val REGISTER_TITLE = "Crear mi restaurante"
    const val REGISTER_CTA = "Crear mi restaurante"
    const val REGISTER_CREATING = "Creando tu restaurante… esto toma unos segundos"
    const val REGISTER_SHOW_PASSWORD = "Mostrar"
    const val REGISTER_HIDE_PASSWORD = "Ocultar"
    const val REGISTER_LOGIN_FAILED_AFTER_CREATE =
        "Tu restaurante ya está creado. Inicia sesión con tu correo y tu contraseña para continuar."

    // W0
    const val W0_TITLE = "Bienvenido a Bendey Resto"
    const val W0_BODY = "Vamos a dejar tu restaurante listo para vender. Te toma unos 8 minutos."
    const val W0_START = "Comenzar"
    const val W0_EXPLORE = "Explorar primero"
    const val PROGRESS_FORMAT = "Progreso: %d de 3"

    // W1
    const val W1_TITLE = "Tu restaurante"
    const val W1_BUSINESS_NAME = "Nombre comercial"
    const val W1_SUBTYPE_TITLE = "¿Qué tipo de local es?"
    const val W1_SERVICE_TITLE = "¿Cómo atiendes?"
    const val W1_TABLES_NOTICE = "Ya creamos 10 mesas; puedes cambiarlas cuando quieras."
    const val W1_NEXT = "Siguiente"

    // W2
    const val W2_TITLE = "Tu carta"
    const val W2_INTRO = "Tu carta es lo que tus mozos van a vender. Elige cómo empezar:"
    const val W2_SAMPLE_TITLE = "Usar una carta de ejemplo"
    const val W2_SAMPLE_BODY =
        "12 platos de ejemplo para probar. Son de ejemplo: puedes editarlos o borrarlos cuando quieras."
    const val W2_PASTE_TITLE = "Pegar mi lista"
    const val W2_PASTE_BODY = "Pega tus platos y precios; antes de crear nada, tú los revisas."
    const val W2_EXCEL_TITLE = "Subir mi Excel"
    const val W2_EXCEL_BODY = "Con una plantilla simple de 4 columnas."
    const val W2_MANUAL_TITLE = "Crear a mano"
    const val W2_MANUAL_BODY = "Un plato a la vez: nombre, precio y categoría."
    const val W2_LATER = "Más tarde"
    const val W2_LATER_NOTICE =
        "Sin carta no se puede vender. Cuando quieras, está en tu lista de primeros pasos."
    const val W2_DONE = "Tu carta ya está lista."

    // Pegar lista
    const val PASTE_TITLE = "Pega tu lista"
    const val PASTE_HINT =
        "Un plato por línea, por ejemplo: Ceviche clásico 35. Para agrupar, escribe la categoría en su propia línea: Entradas:"
    const val PASTE_PREVIEW = "Revisa antes de crear"
    const val PASTE_DOUBTFUL_NOTICE = "Las filas con aviso vienen sin marcar. Revísalas y márcalas si están bien."
    const val PASTE_CREATE = "Crear mi carta"
    const val PASTE_EXISTING_EXCLUDED = "Ya existe en tu carta; no se importará"
    const val PASTE_REMOVE = "Quitar"

    // Excel
    const val EXCEL_DOWNLOAD_SIMPLE = "Descargar plantilla simple"
    const val EXCEL_DOWNLOAD_ADVANCED = "Descargar plantilla avanzada"
    const val EXCEL_IMPORT_VALID_ONLY = "Importar solo las filas válidas (%d)"
    const val EXCEL_DOWNLOAD_ERRORS = "Descargar errores"

    // Resultado de una importación
    const val RESULT_CREATED = "Creamos %d platos."
    const val RESULT_FAILED = "%d no se pudieron crear."

    // W3
    const val W3_TITLE = "Tu primera venta"
    const val W3_REAL_NOTICE = "Esta venta es real. Si te equivocas, puedes anularla con tu PIN de autorización."
    const val W3_STEP_CASH = "Abre la caja"
    const val W3_STEP_CASH_BODY =
        "Abre con S/ 0 si vas a cobrar en efectivo. Así sabrás cuánto efectivo debería haber al cerrar."
    const val W3_STEP_TABLE = "Toca una mesa"
    const val W3_STEP_COUNTER = "Vender en mostrador"
    const val W3_STEP_ADD = "Agrega un plato"
    const val W3_STEP_KITCHEN = "Envía a cocina"
    const val W3_STEP_KITCHEN_BODY = "Así lo ve tu cocina."
    const val W3_STEP_CHARGE = "Cobra como nota de venta"
    const val W3_OPEN_CASH = "Abrir con S/ 0"
    const val W3_NEXT = "Siguiente"
    const val W3_CLOSE = "Cerrar guía"

    // Comunes
    const val CONTINUE_SETUP = "Continuar configuración"
    const val PIN_NOTICE_TITLE = "Tu PIN para cocina y delivery"
    const val PIN_NOTICE_GOT_IT = "Ya lo anoté"

    // R6: primera venta (mismas cadenas que Tauri: firstSaleBand*/next* de wizardCopy.ts)
    const val FIRST_SALE_BAND_TITLE = "¡Tu primera venta! 🎉"
    const val FIRST_SALE_BAND_BODY = "Quedó registrada. Así de simple es vender con Bendey."
    const val NEXT_TITLE = "Lo que sigue"
    const val NEXT_CLOSE = "Cerrar"
    const val NEXT_PRINTER_TITLE = "Conecta tu impresora y entrega el ticket"
    const val NEXT_PRINTER_ACTION = "Ir a Impresoras"
    const val NEXT_QR_TITLE = "Que tus clientes pidan desde la mesa"
    const val NEXT_QR_ACTION = "Ir a Menú digital"
    const val NEXT_SUNAT_TITLE = "Emite boletas y facturas electrónicas"
    const val NEXT_SUNAT_ACTION = "Solicitar activación"
    const val NEXT_SUNAT_CONFIRM_TITLE = "¿Solicitar la activación de SUNAT?"
    const val NEXT_SUNAT_CONFIRM_MESSAGE =
        "Enviaremos tu solicitud para emitir boletas y facturas electrónicas. Te avisamos cuando esté lista."
    const val NEXT_SUNAT_DONE = "Recibimos tu solicitud. Te avisamos cuando esté lista."
    const val NEXT_TEAM_TITLE = "Crea el PIN de tu mozo y tu cocinero"
    const val NEXT_TEAM_ACTION = "Crear usuario"
    const val NEXT_AUTH_PIN_TITLE = "¿Fue una prueba? Crea tu PIN y anúlala"
    const val NEXT_AUTH_PIN_ACTION = "Crear mi PIN"
    const val NEXT_TABLE_TITLE = "Prueba una venta con mesa"
    const val NEXT_TABLE_ACTION = "Ir a Mesas"

    fun progress(done: Int): String = PROGRESS_FORMAT.format(done)
}

/** Tipos de local (clave de la API → etiqueta). Mismas claves que el backend. */
enum class BusinessSubtype(val apiKey: String, val label: String) {
    RESTAURANTE("restaurante", "Restaurante"),
    CEVICHERIA("cevicheria", "Cevichería"),
    POLLERIA_PARRILLA("polleria_parrilla", "Pollería / Parrilla"),
    CAFETERIA("cafeteria", "Cafetería"),
    PIZZERIA("pizzeria", "Pizzería"),
    BAR("bar", "Bar"),
    PASTELERIA("pasteleria", "Pastelería"),
    OTRO("otro", "Otro"),
    ;

    companion object {
        fun fromApi(value: String?): BusinessSubtype? = entries.firstOrNull { it.apiKey == value }
    }
}

/** Modos de atención de W1. Solo preferencia local: decide si W3 usa mesa o mostrador. */
enum class ServiceMode(val key: String, val label: String) {
    DINE_IN("mesas", "En mesas"),
    TAKEAWAY("llevar", "Para llevar"),
    DELIVERY("delivery", "Delivery"),
}
