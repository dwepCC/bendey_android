package com.bendey.restaurant.core.domain.help

import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import java.text.Normalizer

/**
 * Buscador del centro de ayuda. Puerto de `src/utils/helpSearch.ts` de Tauri (mismas decisiones):
 * sin tildes ni mayúsculas, por prefijo/subcadena y TODOS los términos deben aparecer (AND).
 */

/** Quita tildes y pasa a minúsculas carácter por carácter, conservando la longitud (ñ -> n). */
fun normalizeHelpText(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
        val base = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD).firstOrNull() ?: ch
        val lower = base.toString().lowercase()
        sb.append(if (lower.length == 1) lower else ch.toString())
    }
    return sb.toString()
}

/** Divide la consulta en términos útiles; se descartan los de 1 letra. */
fun tokenizeHelpQuery(query: String): List<String> =
    normalizeHelpText(query).split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }

data class HelpSearchHit(val article: HelpArticle, val score: Int, val snippet: String)

private const val WEIGHT_TITLE = 12
private const val WEIGHT_KEYWORDS = 7
private const val WEIGHT_SUMMARY = 5
private const val WEIGHT_BODY = 1
private const val BODY_HITS_CAP = 4
private const val SNIPPET_BEFORE = 70
private const val SNIPPET_AFTER = 130

/** Índice precalculado: normalizar todos los artículos en cada tecla sería trabajo desperdiciado. */
class HelpIndex(articles: List<HelpArticle>) {
    internal class Entry(
        val article: HelpArticle,
        val title: String,
        val summary: String,
        val keywords: String,
        val body: String,
        val bodyOriginal: String,
    )

    internal val entries: List<Entry> = articles.map {
        val original = it.text.replace(Regex("\\s*\\n+\\s*"), " · ")
        Entry(
            article = it,
            title = normalizeHelpText(it.title),
            summary = normalizeHelpText(it.summary),
            keywords = normalizeHelpText(it.keywords.joinToString(" ")),
            body = normalizeHelpText(original),
            bodyOriginal = original,
        )
    }
}

/** Artículos que coinciden con [query], mejores primero. Consulta vacía = lista vacía. */
fun searchHelp(index: HelpIndex, query: String, limit: Int = 40): List<HelpSearchHit> {
    val tokens = tokenizeHelpQuery(query)
    if (tokens.isEmpty()) return emptyList()
    val hits = ArrayList<HelpSearchHit>()
    for (e in index.entries) {
        var score = 0
        var matchedAll = true
        for (token in tokens) {
            var tokenScore = 0
            if (e.title.contains(token)) tokenScore += WEIGHT_TITLE
            if (e.keywords.contains(token)) tokenScore += WEIGHT_KEYWORDS
            if (e.summary.contains(token)) tokenScore += WEIGHT_SUMMARY
            val bodyHits = countOccurrences(e.body, token)
            if (bodyHits > 0) tokenScore += minOf(bodyHits, BODY_HITS_CAP) * WEIGHT_BODY
            if (tokenScore == 0) {
                matchedAll = false
                break
            }
            score += tokenScore
        }
        if (!matchedAll) continue
        if (e.title.startsWith(tokens[0])) score += 6
        hits += HelpSearchHit(e.article, score, buildSnippet(e.bodyOriginal, e.body, tokens))
    }
    return hits.sortedWith(compareByDescending<HelpSearchHit> { it.score }.thenBy { it.article.title }).take(limit)
}

private fun countOccurrences(haystack: String, needle: String): Int {
    var count = 0
    var from = 0
    while (true) {
        val at = haystack.indexOf(needle, from)
        if (at == -1) return count
        count++
        from = at + needle.length
    }
}

/** Extracto alrededor de la primera coincidencia, cortado en espacios para no partir palabras. */
fun buildSnippet(original: String, normalized: String, tokens: List<String>): String {
    var at = -1
    for (token in tokens) {
        val found = normalized.indexOf(token)
        if (found != -1 && (at == -1 || found < at)) at = found
    }
    if (at == -1 || normalized.length != original.length) return original.take(SNIPPET_AFTER).trim()
    var start = maxOf(0, at - SNIPPET_BEFORE)
    var end = minOf(original.length, at + SNIPPET_AFTER)
    if (start > 0) {
        val space = original.indexOf(' ', start)
        if (space != -1 && space < at) start = space + 1
    }
    if (end < original.length) {
        val space = original.lastIndexOf(' ', end)
        if (space > at) end = space
    }
    return (if (start > 0) "…" else "") + original.substring(start, end).trim() + (if (end < original.length) "…" else "")
}

/** Puesto del usuario para la ayuda, a partir de lo que ya sabe la app (permisos y tipo de empleado). */
fun helpRoleFor(permissions: List<String>, employeeType: String?): HelpRole? {
    val et = employeeType?.lowercase().orEmpty()
    return when {
        RestaurantPermissions.isRestaurantAdmin(permissions) || et == "admin" || et == "supervisor" -> HelpRole.ADMIN
        et == "waiter" || et == "mozo" -> HelpRole.MOZO
        et == "cashier" || et == "cajero" -> HelpRole.CAJERO
        et == "cook" || et == "cocinero" || et == "cocina" -> HelpRole.COCINA
        et == "driver" || et == "repartidor" -> HelpRole.REPARTIDOR
        RestaurantPermissions.hasPermission(permissions, RestaurantPermissions.PERM_ORDERS_CHARGE) ||
            RestaurantPermissions.hasPermission(permissions, RestaurantPermissions.PERM_CAJA) -> HelpRole.CAJERO
        RestaurantPermissions.hasPermission(permissions, RestaurantPermissions.PERM_MESA) ||
            RestaurantPermissions.hasPermission(permissions, RestaurantPermissions.PERM_SALAS) ||
            RestaurantPermissions.hasPermission(permissions, RestaurantPermissions.PERM_POS) -> HelpRole.MOZO
        RestaurantPermissions.hasPermission(permissions, RestaurantPermissions.PERM_COMANDAS) -> HelpRole.COCINA
        RestaurantPermissions.hasPermission(permissions, RestaurantPermissions.PERM_REPARTIDORES) -> HelpRole.REPARTIDOR
        else -> null
    }
}

/**
 * Filtro inicial de la ayuda: cada puesto ve lo suyo (el mozo ve lo del mozo). El administrador ve todo,
 * y quien no tiene un puesto reconocible también.
 */
fun defaultHelpRoleFilter(role: HelpRole?): HelpRole? = if (role == null || role == HelpRole.ADMIN) null else role

/** Artículos visibles con el filtro de puesto ([role] null = todos). */
fun articlesForRole(articles: List<HelpArticle>, role: HelpRole?): List<HelpArticle> =
    if (role == null) articles else articles.filter { role in it.roles }

/** Estado de la lista de ayuda, para decidir qué dibuja la pantalla. */
sealed interface HelpListState {
    /** Hay artículos para mostrar agrupados por categoría (sin búsqueda). */
    data class Browse(val groups: List<Pair<HelpCategory, List<HelpArticle>>>) : HelpListState
    /** Hay búsqueda y resultados. */
    data class Results(val hits: List<HelpSearchHit>) : HelpListState
    /** La búsqueda no encontró nada. */
    data object NoResults : HelpListState
    /** El filtro de puesto deja la ayuda sin artículos. */
    data object EmptyRole : HelpListState
}

fun helpListState(catalog: HelpCatalog, index: HelpIndex, query: String, role: HelpRole?): HelpListState {
    val visible = articlesForRole(catalog.articles, role)
    if (visible.isEmpty()) return HelpListState.EmptyRole
    if (tokenizeHelpQuery(query).isEmpty()) {
        val groups = catalog.categories.mapNotNull { c ->
            visible.filter { it.categoryId == c.id }.takeIf { it.isNotEmpty() }?.let { c to it }
        }
        return HelpListState.Browse(groups)
    }
    val ids = visible.map { it.id }.toSet()
    val hits = searchHelp(index, query).filter { it.article.id in ids }
    return if (hits.isEmpty()) HelpListState.NoResults else HelpListState.Results(hits)
}
