package com.bendey.restaurant.core.domain.help

import com.bendey.restaurant.core.domain.copy.ForbiddenTerms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpLogicTest {
    private fun article(
        id: String,
        title: String,
        roles: Set<HelpRole>,
        category: String = "salon",
        summary: String = "",
        keywords: List<String> = emptyList(),
        text: String = "",
    ) = HelpArticle(id, title, summary, category, roles, keywords, markdown = text, text = text)

    private val catalog = HelpCatalog(
        version = "v",
        categories = listOf(HelpCategory("salon", "Salón y mesas", ""), HelpCategory("caja", "Caja y cobros", "")),
        articles = listOf(
            article("abrir-mesa", "Abrir una mesa", setOf(HelpRole.MOZO, HelpRole.CAJERO), "salon", "Cómo abrir la cuenta de una mesa", listOf("sentar")),
            article("cobrar", "Cobrar una cuenta", setOf(HelpRole.CAJERO, HelpRole.ADMIN), "caja", "Efectivo, Yape y tarjeta", listOf("plata", "vuelto"), "Elige el método y cobra. El vuelto se calcula solo."),
            article("series", "Series de comprobantes", setOf(HelpRole.ADMIN), "caja", "Configura boletas y facturas"),
        ),
    )
    private val index = HelpIndex(catalog.articles)

    // ---- normalización y búsqueda ----
    @Test fun normaliza_tildes_y_mayusculas_sin_cambiar_la_longitud() {
        val s = "Cómo ANULO una Precuenta, año"
        assertEquals(s.length, normalizeHelpText(s).length)
        assertEquals("como anulo una precuenta, ano", normalizeHelpText(s))
    }

    @Test fun busca_sin_tildes_ni_mayusculas_y_por_prefijo() {
        assertEquals(listOf("cobrar"), searchHelp(index, "COBR").map { it.article.id })
        assertEquals(listOf("abrir-mesa"), searchHelp(index, "abrír").map { it.article.id })
    }

    @Test fun busca_por_palabra_clave_aunque_no_este_en_el_texto() {
        assertEquals(listOf("cobrar"), searchHelp(index, "plata").map { it.article.id })
    }

    @Test fun todos_los_terminos_deben_aparecer() {
        assertEquals(listOf("cobrar"), searchHelp(index, "cobrar vuelto").map { it.article.id })
        assertTrue(searchHelp(index, "cobrar mesa").isEmpty())
    }

    @Test fun consulta_vacia_o_de_una_letra_no_devuelve_nada() {
        assertTrue(searchHelp(index, "").isEmpty())
        assertTrue(searchHelp(index, "   ").isEmpty())
        assertTrue(searchHelp(index, "a").isEmpty())
    }

    @Test fun el_titulo_pesa_mas_que_el_cuerpo() {
        val hits = searchHelp(index, "cuenta")
        assertEquals(listOf("cobrar", "abrir-mesa"), hits.map { it.article.id })
    }

    @Test fun snippet_alrededor_de_la_coincidencia() {
        val hit = searchHelp(index, "calcula").single()
        assertTrue(hit.snippet.contains("calcula"))
    }

    // ---- puesto ----
    @Test fun el_mozo_ve_lo_del_mozo() {
        val ids = articlesForRole(catalog.articles, HelpRole.MOZO).map { it.id }
        assertEquals(listOf("abrir-mesa"), ids)
        assertEquals(3, articlesForRole(catalog.articles, null).size)
    }

    @Test fun puesto_a_partir_de_permisos_y_tipo_de_empleado() {
        assertEquals(HelpRole.MOZO, helpRoleFor(listOf("t.o"), "waiter"))
        assertEquals(HelpRole.MOZO, helpRoleFor(listOf("t.v"), null))
        assertEquals(HelpRole.CAJERO, helpRoleFor(listOf("c.v"), "cashier"))
        assertEquals(HelpRole.COCINA, helpRoleFor(listOf("k.v"), "cook"))
        assertEquals(HelpRole.REPARTIDOR, helpRoleFor(listOf("d.v"), "driver"))
        assertEquals(HelpRole.ADMIN, helpRoleFor(listOf("s.m", "t.o"), "waiter"))
        assertNull(helpRoleFor(emptyList(), null))
    }

    @Test fun filtro_inicial_el_admin_y_el_desconocido_ven_todo() {
        assertEquals(HelpRole.MOZO, defaultHelpRoleFilter(HelpRole.MOZO))
        assertNull(defaultHelpRoleFilter(HelpRole.ADMIN))
        assertNull(defaultHelpRoleFilter(null))
    }

    // ---- estados de la lista ----
    @Test fun sin_busqueda_agrupa_por_categoria_y_respeta_el_puesto() {
        val s = helpListState(catalog, index, "", HelpRole.MOZO) as HelpListState.Browse
        assertEquals(listOf("salon"), s.groups.map { it.first.id })
        val all = helpListState(catalog, index, "", null) as HelpListState.Browse
        assertEquals(listOf("salon", "caja"), all.groups.map { it.first.id })
        assertEquals(2, all.groups[1].second.size)
    }

    @Test fun busqueda_con_resultados_y_sin_resultados() {
        assertTrue(helpListState(catalog, index, "cobrar", null) is HelpListState.Results)
        assertEquals(HelpListState.NoResults, helpListState(catalog, index, "zzzzzz", null))
    }

    @Test fun la_busqueda_respeta_el_filtro_de_puesto() {
        // "cobrar" existe, pero no es del mozo: para el mozo es "sin resultados", no un error.
        assertEquals(HelpListState.NoResults, helpListState(catalog, index, "cobrar", HelpRole.MOZO))
    }

    @Test fun puesto_sin_articulos_es_un_vacio_propio() {
        assertEquals(HelpListState.EmptyRole, helpListState(catalog, index, "", HelpRole.REPARTIDOR))
    }

    @Test fun textos_de_la_pantalla_sin_usted_ni_jerga() {
        val texts = listOf(
            HelpCopy.TITLE, HelpCopy.SEARCH_PLACEHOLDER, HelpCopy.LOAD_ERROR_TITLE, HelpCopy.LOAD_ERROR_DESCRIPTION,
            HelpCopy.NO_RESULTS_TITLE, HelpCopy.NO_RESULTS_DESCRIPTION, HelpCopy.EMPTY_ROLE_TITLE,
            HelpCopy.EMPTY_ROLE_DESCRIPTION, HelpCopy.resultsCount(1), HelpCopy.resultsCount(5),
            HelpCopy.forRole(HelpRole.MOZO),
        )
        for (t in texts) assertTrue("$t -> ${ForbiddenTerms.find(t)}", ForbiddenTerms.find(t).isEmpty())
        assertEquals("1 artículo", HelpCopy.resultsCount(1))
    }

    // ---- markdown ----
    @Test fun parsea_los_bloques_del_markdown_de_tauri() {
        val md = listOf(
            "Texto con **negrita** y `codigo`.",
            "",
            "### Si no te deja entrar",
            "",
            "1. Abre la app.",
            "2. Toca **Mozo**.",
            "",
            "- Uno",
            "- Dos",
            "",
            "> Dato: Una estación no es un equipo.",
            "",
            "> Ojo: Cuidado con el PIN.",
            "",
            "| Palabra | Qué es |",
            "| --- | --- |",
            "| **Mesa** | El mueble \\| físico |",
        ).joinToString("\n")
        val b = parseHelpMarkdown(md)
        assertEquals(7, b.size)
        val p = b[0] as HelpBlock.Paragraph
        assertEquals(listOf(HelpSpan("Texto con "), HelpSpan("negrita", bold = true), HelpSpan(" y "), HelpSpan("codigo", code = true), HelpSpan(".")), p.spans)
        assertEquals("Si no te deja entrar", (b[1] as HelpBlock.Heading).text)
        assertEquals(2, (b[2] as HelpBlock.Steps).items.size)
        assertEquals(2, (b[3] as HelpBlock.Bullets).items.size)
        assertEquals(HelpCalloutKind.NOTE, (b[4] as HelpBlock.Callout).kind)
        assertEquals("Una estación no es un equipo.", (b[4] as HelpBlock.Callout).spans.single().text)
        assertEquals(HelpCalloutKind.WARN, (b[5] as HelpBlock.Callout).kind)
        val t = b[6] as HelpBlock.Table
        assertEquals(listOf("Palabra", "Qué es"), t.head)
        assertEquals(listOf(listOf("**Mesa**", "El mueble | físico")), t.rows)
    }

    @Test fun negrita_sin_cerrar_queda_como_texto() {
        assertEquals(listOf(HelpSpan("2 ** 3")), parseHelpSpans("2 ** 3"))
        assertEquals(listOf(HelpSpan("a `b")), parseHelpSpans("a `b"))
    }

    @Test fun markdown_vacio_no_da_bloques() {
        assertTrue(parseHelpMarkdown("").isEmpty())
        assertNotNull(parseHelpMarkdown("hola").single())
        assertFalse(parseHelpMarkdown("   \n\n").isNotEmpty())
    }
}
