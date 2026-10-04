package com.bendey.restaurant.core.domain.copy

/**
 * Textos de los estados vacíos y de error de carga (R5, UX-REDESIGN §7.2). Un solo módulo para Android.
 * Las cadenas son las MISMAS que `src/content/emptyStates.ts` de Tauri: no cambies una sin cambiar la otra.
 * Español, tuteo, sin jerga. Cada vacío responde 4 cosas: qué es, por qué está vacío, qué hacer y el botón.
 */
data class EmptyCopy(
    val title: String,
    val description: String,
    /** Texto del botón principal (null si no hay acción posible). */
    val action: String? = null,
    /** Sustituto de [description] para quien no puede crear/administrar (p. ej. el mozo). */
    val noPermissionDescription: String? = null,
) {
    fun descriptionFor(canAct: Boolean): String =
        if (!canAct && noPermissionDescription != null) noPermissionDescription else description
}

object EmptyStatesCopy {
    val productos = EmptyCopy(
        title = "Tu carta está vacía",
        description = "Los mozos no pueden tomar pedidos hasta que cargues al menos un plato. Súbelos desde tu Excel o créalos uno por uno.",
        action = "Crear un producto",
    )
    val categorias = EmptyCopy(
        title = "Aún no tienes categorías",
        description = "Sirven para ordenar tu carta (Entradas, Platos de fondo, Bebidas) y que el mozo encuentre rápido cada plato.",
        action = "Crear categoría",
    )
    val salas = EmptyCopy(
        title = "Todavía no hay mesas",
        description = "Sin mesas no se pueden abrir cuentas en salón. Crea tu primera mesa o usa Vender para mostrador.",
        action = "Crear mi primera mesa",
        noPermissionDescription = "Pídele al administrador que cree las mesas.",
    )
    val ventas = EmptyCopy(
        title = "Aún no hay ventas en este periodo",
        description = "Cuando cobres, el comprobante aparecerá aquí para reimprimir, enviar o anular.",
        action = "Ir a Vender",
    )
    val clientes = EmptyCopy(
        title = "Aún no registras clientes",
        description = "Los necesitas para emitir facturas con RUC; en boletas simples no hace falta.",
        action = "Agregar cliente",
    )
    val repartidores = EmptyCopy(
        title = "Aún no tienes repartidores",
        description = "Regístralos para asignarles los pedidos de delivery.",
        action = "Agregar repartidor",
    )
    val cajaCerrada = EmptyCopy(
        title = "Tu caja está cerrada",
        description = "Necesitas abrirla para cobrar en efectivo y ver tus ventas del turno.",
        action = "Abrir caja",
    )
    val cajaMovimientos = EmptyCopy(
        title = "Esta caja aún no tiene movimientos",
        description = "Aparecerán cuando cobres o registres un ingreso o un gasto.",
    )
    val metodosPago = EmptyCopy(
        title = "No hay métodos de pago activos",
        description = "Sin ellos no se puede cobrar. Activa efectivo, Yape, Plin o tarjeta.",
        action = "Configurar métodos de pago",
    )
    val compras = EmptyCopy(
        title = "Aún no registras compras",
        description = "Regístralas para llevar tu stock de insumos y saber cuánto cuesta cada plato.",
        action = "Registrar compra",
    )
    val proveedores = EmptyCopy(
        title = "Aún no tienes proveedores",
        description = "Los usarás al registrar compras.",
        action = "Agregar proveedor",
    )
    val combos = EmptyCopy(
        title = "Aún no tienes combos",
        description = "Un combo agrupa platos (ej. Menú del día) a un precio especial.",
        action = "Crear combo",
    )
    val modificadores = EmptyCopy(
        title = "Aún no tienes modificadores",
        description = "Sirven para cosas como «Término de la carne» o «Extras».",
        action = "Crear grupo de opciones",
    )
    val areas = EmptyCopy(
        title = "Sin áreas, todos los pedidos salen a una sola impresora",
        description = "Crea áreas (Cocina, Barra) para que cada plato vaya a la suya.",
        action = "Crear área",
    )
    val transferencias = EmptyCopy(
        title = "Aún no mueves stock entre locales",
        description = "Aquí verás cada transferencia con su origen y destino.",
        action = "Nueva transferencia",
    )
    val personal = EmptyCopy(
        title = "Aún no tienes personal",
        description = "Crea un usuario con PIN para cada mozo, cajero o cocinero.",
        action = "Crear usuario",
    )
    val impresoras = EmptyCopy(
        title = "Aún no se imprimió nada desde este equipo",
        description = "Imprime una prueba para confirmar que todo funciona.",
        action = "Imprimir prueba",
    )
    val sucursales = EmptyCopy(
        title = "Tu local principal aún no está configurado",
        description = "Crea tu primera sucursal para empezar a vender.",
        action = "Crear sucursal",
    )
    val pedidosEnEspera = EmptyCopy(
        title = "No hay pedidos en espera",
        description = "Los pedidos para llevar y delivery sin cobrar aparecen aquí.",
    )
    val carrito = EmptyCopy(
        title = "Toca un plato para agregarlo al pedido",
        description = "Los platos que elijas aparecerán aquí con su total.",
    )
    val hoySinVentas = EmptyCopy(
        title = "Todavía no hay ventas hoy",
        description = "Tu resumen se llena en cuanto cobres la primera cuenta.",
        action = "Hacer mi primera venta",
    )
    val cocinaAlDia = EmptyCopy(
        title = "Cocina al día",
        description = "No hay platos pendientes. Cuando un mozo envíe un pedido aparecerá aquí con sonido.",
    )
    val reportes = EmptyCopy(
        title = "No hay datos para este periodo",
        description = "Prueba con otro rango de fechas o con «Este mes».",
        action = "Ver este mes",
    )

    /** «No hay nada con este filtro» (distinto de «no hay nada creado»). */
    val filtrado = EmptyCopy(
        title = "No encontramos resultados",
        description = "Prueba con otra búsqueda o quita los filtros.",
        action = "Quitar filtros",
    )

    /** Todas las entradas (para tests de microcopy). */
    val all: List<EmptyCopy> = listOf(
        productos, categorias, salas, ventas, clientes, repartidores, cajaCerrada, cajaMovimientos,
        metodosPago, compras, proveedores, combos, modificadores, areas, transferencias, personal,
        impresoras, sucursales, pedidosEnEspera, carrito, hoySinVentas, cocinaAlDia, reportes, filtrado,
    )
}

/** Fallo de carga: se muestra en vez de «Sin X», con botón Reintentar. */
object LoadErrorCopy {
    const val TITLE = "No pudimos cargar esta lista"
    const val DESCRIPTION = "Revisa tu conexión a internet e inténtalo de nuevo. Tus datos no se perdieron."
    const val RETRY = "Reintentar"
}

/** Jerga y «usted» prohibidos en textos visibles (UX-REDESIGN §14). */
object ForbiddenTerms {
    val patterns: List<Regex> = listOf(
        Regex("localStorage", RegexOption.IGNORE_CASE),
        Regex("consola de Tauri", RegexOption.IGNORE_CASE),
        Regex("base de datos", RegexOption.IGNORE_CASE),
        Regex("panel tenant", RegexOption.IGNORE_CASE),
        Regex("aperturar", RegexOption.IGNORE_CASE),
        Regex("income", RegexOption.IGNORE_CASE),
        Regex("expense", RegexOption.IGNORE_CASE),
        Regex("\\busted(es)?\\b", RegexOption.IGNORE_CASE),
        // Imperativo de «usted»; se permite el subjuntivo tras «que» (p. ej. «que cree las mesas»).
        Regex("(?<!que )\\b(agregue|cree|configure|indique|ingrese|seleccione|abra su|revise su)\\b", RegexOption.IGNORE_CASE),
        Regex("\\barqueo\\b", RegexOption.IGNORE_CASE),
    )

    fun find(text: String): List<String> = patterns.filter { it.containsMatchIn(text) }.map { it.pattern }
}

/** Qué debe mostrar una pantalla de lista. Regla clave: un fallo de carga NUNCA se muestra como «Sin X». */
sealed interface ListViewState {
    data object Loading : ListViewState
    data class Error(val message: String) : ListViewState
    data object EmptyCreated : ListViewState
    data object EmptyFiltered : ListViewState
    data object Content : ListViewState
}

/** true si en lugar de la lista hay que mostrar un error o un vacío. */
val ListViewState.showsPlaceholder: Boolean
    get() = this is ListViewState.Error || this === ListViewState.EmptyCreated || this === ListViewState.EmptyFiltered

object ListStateDecider {
    /**
     * @param error mensaje del último fallo de carga (null/blank = sin fallo)
     * @param hasActiveFilters hay búsqueda o filtros que podrían explicar la lista vacía
     */
    fun decide(loading: Boolean, error: String?, itemCount: Int, hasActiveFilters: Boolean = false): ListViewState = when {
        itemCount > 0 -> ListViewState.Content
        loading -> ListViewState.Loading
        !error.isNullOrBlank() -> ListViewState.Error(error)
        hasActiveFilters -> ListViewState.EmptyFiltered
        else -> ListViewState.EmptyCreated
    }
}
