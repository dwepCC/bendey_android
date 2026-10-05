package com.bendey.restaurant.feature.ayuda

import com.bendey.restaurant.core.domain.help.HelpArticle
import com.bendey.restaurant.core.domain.help.HelpCatalog
import com.bendey.restaurant.core.domain.help.HelpCategory
import com.bendey.restaurant.core.domain.help.HelpRole
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lee el JSON que exporta Tauri (`docs/help-export/help.json`, schema 1). Los artículos con un puesto
 * desconocido lo ignoran (no se pierden), y un artículo sin ningún puesto reconocible queda para todos.
 */
@Serializable
private data class HelpFile(
    val schema: Int = 0,
    val version: String = "",
    val categories: List<CategoryDto> = emptyList(),
    val articles: List<ArticleDto> = emptyList(),
)

@Serializable
private data class CategoryDto(val id: String, val title: String, val description: String = "")

@Serializable
private data class ArticleDto(
    val id: String,
    val title: String,
    val summary: String = "",
    val categoryId: String,
    val roles: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val markdown: String = "",
    val text: String = "",
)

object HelpCatalogParser {
    const val SUPPORTED_SCHEMA = 1

    private val json = Json { ignoreUnknownKeys = true }

    /** @throws IllegalArgumentException si el archivo no es un catálogo de ayuda que esta versión entienda. */
    fun parse(raw: String): HelpCatalog {
        val file = try {
            json.decodeFromString(HelpFile.serializer(), raw)
        } catch (e: Exception) {
            throw IllegalArgumentException("El archivo de ayuda no se puede leer", e)
        }
        require(file.schema == SUPPORTED_SCHEMA) { "Versión de ayuda no compatible" }
        val categories = file.categories.map { HelpCategory(it.id, it.title, it.description) }
        val articles = file.articles.map { a ->
            val roles = a.roles.mapNotNull(HelpRole::fromId).toSet()
            HelpArticle(
                id = a.id,
                title = a.title,
                summary = a.summary,
                categoryId = a.categoryId,
                roles = roles.ifEmpty { HelpRole.entries.toSet() },
                keywords = a.keywords,
                markdown = a.markdown,
                text = a.text,
            )
        }
        return HelpCatalog(version = file.version, categories = categories, articles = articles)
    }
}
