package com.bendey.restaurant.feature.ayuda

import com.bendey.restaurant.core.domain.help.HelpIndex
import com.bendey.restaurant.core.domain.help.HelpRole
import com.bendey.restaurant.core.domain.help.parseHelpMarkdown
import com.bendey.restaurant.core.domain.help.searchHelp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** El JSON de ayuda que viaja en la app (assets) y su relación con el de Tauri. */
class HelpCatalogTest {
    private val asset = File("src/main/assets/help/help.json")

    private fun raw(): String = asset.readText(Charsets.UTF_8)

    private fun tauriFile(): File? {
        var dir: File? = File("").absoluteFile
        repeat(8) {
            val f = File(dir, "front_tenant_restaurant_tauri/docs/help-export/help.json")
            if (f.isFile) return f
            dir = dir?.parentFile
        }
        return null
    }

    @Test fun el_asset_existe_y_se_lee() {
        assertTrue("falta ${asset.absolutePath}: corre node scripts/sync-help.mjs", asset.isFile)
        val catalog = HelpCatalogParser.parse(raw())
        assertEquals(61, catalog.articles.size)
        assertEquals(12, catalog.categories.size)
    }

    @Test fun cada_articulo_tiene_categoria_puesto_y_texto() {
        val catalog = HelpCatalogParser.parse(raw())
        val categoryIds = catalog.categories.map { it.id }.toSet()
        for (a in catalog.articles) {
            assertTrue("categoría desconocida en ${a.id}", a.categoryId in categoryIds)
            assertTrue("sin puestos: ${a.id}", a.roles.isNotEmpty())
            assertTrue("sin título/markdown: ${a.id}", a.title.isNotBlank() && a.markdown.isNotBlank())
            assertTrue("sin texto plano: ${a.id}", a.text.isNotBlank())
        }
        assertEquals(catalog.articles.size, catalog.articles.map { it.id }.toSet().size)
    }

    @Test fun el_markdown_de_todos_los_articulos_se_interpreta() {
        for (a in HelpCatalogParser.parse(raw()).articles) {
            assertTrue("sin bloques: ${a.id}", parseHelpMarkdown(a.markdown).isNotEmpty())
        }
    }

    @Test fun el_contenido_no_usa_usted() {
        val usted = Regex("\\busted(es)?\\b", RegexOption.IGNORE_CASE)
        for (a in HelpCatalogParser.parse(raw()).articles) {
            assertFalse("usted en ${a.id}", usted.containsMatchIn(a.markdown) || usted.containsMatchIn(a.title))
        }
    }

    @Test fun el_mozo_ve_lo_del_mozo_y_la_busqueda_funciona() {
        val catalog = HelpCatalogParser.parse(raw())
        val mozo = catalog.articles.filter { HelpRole.MOZO in it.roles }
        assertTrue(mozo.isNotEmpty() && mozo.size < catalog.articles.size)
        val hits = searchHelp(HelpIndex(catalog.articles), "comanda")
        assertTrue(hits.isNotEmpty())
        assertEquals(hits.sortedByDescending { it.score }.map { it.score }, hits.map { it.score })
    }

    @Test fun un_archivo_roto_se_rechaza_con_error_claro() {
        val e = runCatching { HelpCatalogParser.parse("no es json") }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException)
        val v = runCatching { HelpCatalogParser.parse("{\"schema\":99,\"version\":\"x\"}") }.exceptionOrNull()
        assertTrue(v is IllegalArgumentException)
    }

    @Test fun la_copia_coincide_con_la_de_tauri() {
        val tauri = tauriFile()
        assumeTrue("repo de Tauri no encontrado: comparación omitida", tauri != null)
        val a = raw().replace("\r\n", "\n")
        val b = tauri!!.readText(Charsets.UTF_8).replace("\r\n", "\n")
        assertTrue("La ayuda de Android no coincide con la de Tauri. Corre: node scripts/sync-help.mjs", a == b)
    }
}
