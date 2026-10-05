package com.bendey.restaurant.core.domain.help

/**
 * Centro de ayuda (R10.7). La fuente es el JSON que exporta Tauri (`docs/help-export/help.json`):
 * MISMOS artículos, mismo texto. Android no escribe contenido propio; solo lo muestra.
 */
enum class HelpRole(val id: String, val label: String) {
    MOZO("mozo", "Mozo"),
    CAJERO("cajero", "Cajero"),
    COCINA("cocina", "Cocina"),
    REPARTIDOR("repartidor", "Repartidor"),
    ADMIN("admin", "Administrador"),
    ;

    companion object {
        fun fromId(id: String): HelpRole? = entries.firstOrNull { it.id == id }
    }
}

data class HelpCategory(
    val id: String,
    val title: String,
    val description: String,
)

data class HelpArticle(
    val id: String,
    val title: String,
    /** Una línea que responde "¿esto es lo que busco?" sin abrir el artículo. */
    val summary: String,
    val categoryId: String,
    /** A quién le sirve (permite filtrar por puesto de trabajo). */
    val roles: Set<HelpRole>,
    /** Palabras con las que el usuario buscaría esto aunque no estén en el texto. */
    val keywords: List<String>,
    /** Artículo completo en Markdown simple (el campo `markdown` del JSON). */
    val markdown: String,
    /** Texto plano del artículo (el campo `text` del JSON), para el buscador. */
    val text: String,
)

data class HelpCatalog(
    val version: String,
    val categories: List<HelpCategory>,
    val articles: List<HelpArticle>,
) {
    fun category(id: String): HelpCategory? = categories.firstOrNull { it.id == id }
    fun article(id: String): HelpArticle? = articles.firstOrNull { it.id == id }
}

/** Texto de la pantalla "Ayuda". Español, tuteo, sin jerga. */
object HelpCopy {
    const val TITLE = "Ayuda"
    const val SEARCH_PLACEHOLDER = "Busca por palabra: cobrar, anular, mesa…"
    const val ROLE_ALL = "Todo"
    const val LOAD_ERROR_TITLE = "No pudimos abrir la ayuda"
    const val LOAD_ERROR_DESCRIPTION = "Cierra la pantalla y vuelve a entrar. Si sigue igual, actualiza la app."
    const val NO_RESULTS_TITLE = "No encontramos nada con esas palabras"
    const val NO_RESULTS_DESCRIPTION = "Prueba con otra palabra o quita el filtro de puesto."
    const val CLEAR_SEARCH = "Borrar búsqueda"
    const val SHOW_ALL = "Ver todos los puestos"
    const val EMPTY_ROLE_TITLE = "Aún no hay ayuda para este puesto"
    const val EMPTY_ROLE_DESCRIPTION = "Mira la ayuda de todos los puestos."
    const val FOR_WHO = "Sirve para:"
    const val BACK_TO_LIST = "Volver a la ayuda"

    fun resultsCount(n: Int): String = if (n == 1) "1 artículo" else "$n artículos"
    fun forRole(role: HelpRole): String = "Mostrando la ayuda de ${role.label}"
}
