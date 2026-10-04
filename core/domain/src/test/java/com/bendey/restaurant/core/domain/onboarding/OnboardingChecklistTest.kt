package com.bendey.restaurant.core.domain.onboarding

import com.bendey.restaurant.core.domain.model.AuthUser
import com.bendey.restaurant.core.domain.model.UserSession
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingChecklistTest {

    private fun step(
        key: String,
        tier: OnboardingTier,
        done: Boolean = false,
        skipped: Boolean = false,
        localOnly: Boolean = false,
        count: Int? = null,
    ) = OnboardingStep(key, tier, done, skipped, localOnly, count)

    /** Estado base del servidor: los 11 pasos, todo pendiente. */
    private fun allSteps(
        menu: Boolean = false,
        firstSale: Boolean = false,
        tables: Boolean = false,
        printer: Boolean = false,
        authPin: Boolean = false,
        team: Boolean = false,
        qr: Boolean = false,
        skipped: Set<String> = emptySet(),
        menuCount: Int? = null,
    ): List<OnboardingStep> = listOf(
        step("menu", OnboardingTier.REQUIRED, done = menu, count = menuCount),
        step("first_sale", OnboardingTier.REQUIRED, done = firstSale),
        step("tables", OnboardingTier.RECOMMENDED, done = tables),
        step("printer", OnboardingTier.RECOMMENDED, done = printer, localOnly = true),
        step("auth_pin", OnboardingTier.RECOMMENDED, done = authPin),
        step("team", OnboardingTier.RECOMMENDED, done = team),
        step("qr_menu", OnboardingTier.RECOMMENDED, done = qr),
        step("sunat", OnboardingTier.ADVANCED),
        step("recipes", OnboardingTier.ADVANCED),
        step("delivery", OnboardingTier.ADVANCED),
        step("branches", OnboardingTier.ADVANCED),
    ).map { if (it.key in skipped) it.copy(skipped = true) else it }

    private fun state(
        steps: List<OnboardingStep>,
        sellReady: Boolean = false,
        operateDone: Int = 0,
        operateTotal: Int = 5,
        dismissed: Boolean = false,
    ) = OnboardingState(
        dismissed = dismissed,
        progress = OnboardingServerProgress(sellReady, operateDone, operateTotal, 0),
        steps = steps,
    )

    private fun OnboardingChecklist.item(key: OnboardingStepKey) =
        sections.flatMap { it.items }.first { it.key == key }

    // ---- Tenant nuevo ----

    @Test
    fun `tenant nuevo - para vender 0 de 2 y titulo por defecto`() {
        val c = assertNotNull(buildOnboardingChecklist(state(allSteps()), localPrinterConfigured = false))
        assertEquals("🚀 Tu restaurante", c.title)
        assertEquals("Para operar: 0 de 5", c.progressLabel)
        assertEquals(0, c.percent)
        assertFalse(c.sellReady)
        assertEquals(
            listOf("PARA VENDER", "PARA OPERAR", "PARA CRECER (cuando quieras)"),
            c.sections.map { it.title },
        )
    }

    @Test
    fun `orden fijo del catalogo aunque el servidor los mande desordenados`() {
        val shuffled = allSteps().reversed()
        val c = assertNotNull(buildOnboardingChecklist(state(shuffled), false))
        assertEquals(
            listOf("menu", "first_sale"),
            c.sections[0].items.map { it.key.apiKey },
        )
        assertEquals(
            listOf("tables", "printer", "auth_pin", "team", "qr_menu"),
            c.sections[1].items.map { it.key.apiKey },
        )
        assertEquals(
            listOf("sunat", "recipes", "delivery", "branches"),
            c.sections[2].items.map { it.key.apiKey },
        )
    }

    @Test
    fun `sin carta ofrece carta de ejemplo y el CTA va a Productos`() {
        val c = assertNotNull(buildOnboardingChecklist(state(allSteps()), false))
        val menu = c.item(OnboardingStepKey.MENU)
        assertTrue(menu.offersSampleMenu)
        assertEquals("Subir mi carta", menu.primaryLabel)
        assertEquals(OnboardingAction.Navigate(OnboardingDestination.PRODUCTOS), menu.primaryAction)
        assertFalse(menu.canSkip, "los pasos para vender no se omiten")
    }

    // ---- Con carta / con ventas ----

    @Test
    fun `con carta - ya no ofrece ejemplo y muestra la cantidad de platos`() {
        val c = assertNotNull(buildOnboardingChecklist(state(allSteps(menu = true, menuCount = 12)), false))
        val menu = c.item(OnboardingStepKey.MENU)
        assertTrue(menu.done)
        assertFalse(menu.offersSampleMenu)
        assertNull(menu.primaryLabel)
        assertEquals("Tu carta (12 platos)", menu.title)
        assertEquals("Para operar: 0 de 5", c.progressLabel)
        assertEquals(0, c.percent)
        assertFalse(c.sellReady)
    }

    @Test
    fun `un solo plato va en singular`() {
        val c = assertNotNull(buildOnboardingChecklist(state(allSteps(menu = true, menuCount = 1)), false))
        assertEquals("Tu carta (1 plato)", c.item(OnboardingStepKey.MENU).title)
    }

    @Test
    fun `con ventas y sell_ready - titulo celebra y el avance pasa a operar`() {
        val s = state(
            allSteps(menu = true, firstSale = true, tables = true),
            sellReady = true,
            operateDone = 1,
            operateTotal = 5,
        )
        val c = assertNotNull(buildOnboardingChecklist(s, localPrinterConfigured = false))
        assertEquals("Ya puedes vender 🎉", c.title)
        assertEquals("Para operar: 1 de 5", c.progressLabel)
        assertEquals(20, c.percent)
        assertTrue(c.sellReady)
        // Las dos primeras siguen visibles, marcadas como hechas.
        assertTrue(c.item(OnboardingStepKey.FIRST_SALE).done)
        assertEquals("Hacer mi primera venta", allLabel(state(allSteps()), OnboardingStepKey.FIRST_SALE))
    }

    private fun allLabel(s: OnboardingState, key: OnboardingStepKey) =
        buildOnboardingChecklist(s, false)!!.item(key).primaryLabel

    @Test
    fun `primera venta va al POS`() {
        val c = assertNotNull(buildOnboardingChecklist(state(allSteps(menu = true)), false))
        assertEquals(
            OnboardingAction.Navigate(OnboardingDestination.POS),
            c.item(OnboardingStepKey.FIRST_SALE).primaryAction,
        )
    }

    // ---- Impresora local ----

    @Test
    fun `impresora local configurada suma al avance aunque el servidor diga false`() {
        val s = state(
            allSteps(menu = true, firstSale = true, tables = true),
            sellReady = true,
            operateDone = 1,
            operateTotal = 5,
        )
        val sin = assertNotNull(buildOnboardingChecklist(s, localPrinterConfigured = false))
        val con = assertNotNull(buildOnboardingChecklist(s, localPrinterConfigured = true))

        assertFalse(sin.item(OnboardingStepKey.PRINTER).done)
        assertEquals("Para operar: 1 de 5", sin.progressLabel)

        val printer = con.item(OnboardingStepKey.PRINTER)
        assertTrue(printer.done)
        assertNull(printer.primaryLabel)
        assertEquals("Para operar: 2 de 5", con.progressLabel)
        assertEquals(40, con.percent)
    }

    @Test
    fun `impresora no cuenta doble si el servidor ya la trae hecha`() {
        val s = state(
            allSteps(menu = true, firstSale = true, printer = true),
            sellReady = true,
            operateDone = 1,
            operateTotal = 5,
        )
        val c = assertNotNull(buildOnboardingChecklist(s, localPrinterConfigured = true))
        assertEquals("Para operar: 1 de 5", c.progressLabel)
    }

    @Test
    fun `impresora esta marcada como de este equipo y su CTA va a Impresoras`() {
        val c = assertNotNull(buildOnboardingChecklist(state(allSteps()), false))
        val printer = c.item(OnboardingStepKey.PRINTER)
        assertTrue(printer.thisDevice)
        assertEquals("Conecta tu impresora (este equipo)", printer.title)
        assertEquals("Configurar impresora", printer.primaryLabel)
        assertEquals(OnboardingAction.Navigate(OnboardingDestination.IMPRESORAS), printer.primaryAction)
        assertFalse(c.item(OnboardingStepKey.TABLES).thisDevice)
    }

    @Test
    fun `impresora local no suma si la omitieron`() {
        val s = state(
            allSteps(menu = true, firstSale = true, skipped = setOf("printer")),
            sellReady = true,
            operateDone = 0,
            operateTotal = 4,
        )
        val c = assertNotNull(buildOnboardingChecklist(s, localPrinterConfigured = true))
        assertEquals("Para operar: 0 de 4", c.progressLabel)
    }

    @Test
    fun `el avance se calcula en el cliente e ignora progress operate y percent del servidor`() {
        val s = state(
            allSteps(menu = true, firstSale = true, tables = true),
            sellReady = true,
            operateDone = 4,
            operateTotal = 4,
        ).let { it.copy(progress = it.progress.copy(percent = 100)) }
        val c = assertNotNull(buildOnboardingChecklist(s, localPrinterConfigured = true))
        // tables + printer(local) = 2 de 5
        assertEquals("Para operar: 2 de 5", c.progressLabel)
        assertEquals(40, c.percent)
    }

    @Test
    fun `sell_ready se deduce de carta y primera venta aunque el servidor diga false`() {
        val c = assertNotNull(
            buildOnboardingChecklist(state(allSteps(menu = true, firstSale = true), sellReady = false), false),
        )
        assertTrue(c.sellReady)
        assertEquals("Ya puedes vender 🎉", c.title)
        assertEquals("Para operar: 0 de 5", c.progressLabel)
    }

    @Test
    fun `el resumen nunca habla de Para vender`() {
        listOf(allSteps(), allSteps(menu = true), allSteps(menu = true, firstSale = true)).forEach {
            assertFalse(buildOnboardingChecklist(state(it), false)!!.progressLabel.contains("vender"))
        }
    }

    // ---- Omitidos / ocultar / completo ----

    @Test
    fun `los omitidos desaparecen`() {
        val s = state(allSteps(skipped = setOf("team", "qr_menu", "sunat")))
        val c = assertNotNull(buildOnboardingChecklist(s, false))
        val keys = c.sections.flatMap { it.items }.map { it.key.apiKey }
        assertFalse("team" in keys)
        assertFalse("qr_menu" in keys)
        assertFalse("sunat" in keys)
        assertTrue("tables" in keys)
    }

    @Test
    fun `una seccion sin items no se muestra`() {
        val s = state(allSteps(skipped = setOf("sunat", "recipes", "delivery", "branches")))
        val c = assertNotNull(buildOnboardingChecklist(s, false))
        assertEquals(listOf(OnboardingSectionKind.SELL, OnboardingSectionKind.OPERATE), c.sections.map { it.kind })
    }

    @Test
    fun `dismissed - no hay tarjeta`() {
        assertNull(buildOnboardingChecklist(state(allSteps(), dismissed = true), false))
    }

    @Test
    fun `todo completo - no hay tarjeta aunque queden avanzados`() {
        val s = state(
            allSteps(menu = true, firstSale = true, tables = true, printer = true, authPin = true, team = true, qr = true),
            sellReady = true,
            operateDone = 5,
        )
        assertNull(buildOnboardingChecklist(s, false))
    }

    @Test
    fun `todo completo contando impresora local y omitidos - no hay tarjeta`() {
        val s = state(
            allSteps(menu = true, firstSale = true, tables = true, authPin = true, skipped = setOf("team", "qr_menu")),
            sellReady = true,
            operateDone = 2,
            operateTotal = 3,
        )
        assertNull(buildOnboardingChecklist(s, localPrinterConfigured = true))
        assertNotNull(buildOnboardingChecklist(s, localPrinterConfigured = false))
    }

    @Test
    fun `sin pasos o con claves desconocidas no revienta ni muestra nada`() {
        assertNull(buildOnboardingChecklist(state(emptyList()), false))
        val raro = state(listOf(step("futuro", OnboardingTier.RECOMMENDED)))
        assertNull(buildOnboardingChecklist(raro, false))
    }

    @Test
    fun `claves desconocidas se ignoran y el resto se muestra`() {
        val s = state(allSteps() + step("futuro", OnboardingTier.RECOMMENDED))
        val c = assertNotNull(buildOnboardingChecklist(s, false))
        assertEquals(11, c.sections.sumOf { it.items.size })
    }

    // ---- Avanzados ----

    @Test
    fun `los avanzados no cuentan para el porcentaje`() {
        val s = state(
            allSteps(menu = true, firstSale = true, tables = true, printer = true, authPin = true),
            sellReady = true,
            operateDone = 4,
            operateTotal = 5,
        ).let { it.copy(steps = it.steps.map { st -> if (st.tier == OnboardingTier.ADVANCED) st.copy(done = true) else st }) }
        val c = assertNotNull(buildOnboardingChecklist(s, false))
        assertEquals("Para operar: 3 de 5", c.progressLabel)
        assertEquals(60, c.percent)
    }

    @Test
    fun `avanzados llevan a la pantalla real y solo sunat es accion especial`() {
        val c = assertNotNull(buildOnboardingChecklist(state(allSteps()), false))
        fun act(k: OnboardingStepKey) = c.item(k).primaryAction
        assertEquals(OnboardingAction.Navigate(OnboardingDestination.PRODUCTOS), act(OnboardingStepKey.RECIPES))
        assertEquals(OnboardingAction.Navigate(OnboardingDestination.REPARTIDORES), act(OnboardingStepKey.DELIVERY))
        assertEquals(OnboardingAction.Navigate(OnboardingDestination.CONFIG_SUCURSALES), act(OnboardingStepKey.BRANCHES))
        assertEquals(OnboardingAction.Navigate(OnboardingDestination.CONFIG_MENU_DIGITAL), act(OnboardingStepKey.QR_MENU))
        assertEquals(OnboardingAction.Navigate(OnboardingDestination.CONFIG_OPERACION), act(OnboardingStepKey.AUTH_PIN))
        assertEquals(OnboardingAction.Navigate(OnboardingDestination.CONFIG_OPERACION), act(OnboardingStepKey.TEAM))
        assertEquals(OnboardingAction.Navigate(OnboardingDestination.MESAS_ADMIN), act(OnboardingStepKey.TABLES))
        assertEquals(OnboardingAction.RequestSunat, act(OnboardingStepKey.SUNAT))
    }

    @Test
    fun `los recomendados y avanzados se pueden omitir, los hechos no`() {
        val c = assertNotNull(buildOnboardingChecklist(state(allSteps(tables = true)), false))
        assertTrue(c.item(OnboardingStepKey.PRINTER).canSkip)
        assertTrue(c.item(OnboardingStepKey.SUNAT).canSkip)
        assertFalse(c.item(OnboardingStepKey.TABLES).canSkip)
    }

    // ---- SUNAT ----

    private fun sunatItem(status: SunatRequestStatus?, serverDone: Boolean = false): OnboardingItem {
        val steps = allSteps().map { if (it.key == "sunat") it.copy(done = serverDone) else it }
        return assertNotNull(buildOnboardingChecklist(state(steps), false, status)).item(OnboardingStepKey.SUNAT)
    }

    @Test
    fun `sunat sin solicitar ofrece el boton`() {
        listOf(null, SunatRequestStatus(SunatRequestState.NONE)).forEach {
            val item = sunatItem(it)
            assertEquals("Solicitar activación", item.primaryLabel)
            assertNull(item.statusText)
            assertFalse(item.done)
        }
    }

    @Test
    fun `sunat solicitada muestra la fecha y quita el boton`() {
        val item = sunatItem(SunatRequestStatus(SunatRequestState.REQUESTED, "2026-10-04T15:30:00Z"))
        assertEquals("Solicitada el 04/10/2026", item.statusText)
        assertNull(item.primaryLabel)
        assertNull(item.primaryAction)
        assertFalse(item.canSkip)
        assertFalse(item.done)
    }

    @Test
    fun `sunat solicitada sin fecha legible dice solo Solicitada`() {
        assertEquals("Solicitada", sunatItem(SunatRequestStatus(SunatRequestState.REQUESTED, null)).statusText)
        assertEquals("Solicitada", sunatItem(SunatRequestStatus(SunatRequestState.REQUESTED, "ayer")).statusText)
    }

    @Test
    fun `sunat en revision y activa`() {
        val review = sunatItem(SunatRequestStatus(SunatRequestState.IN_REVIEW, "2026-10-04"))
        assertEquals("En revisión", review.statusText)
        assertNull(review.primaryLabel)

        val active = sunatItem(SunatRequestStatus(SunatRequestState.ACTIVE, "2026-10-04"))
        assertEquals("Activa", active.statusText)
        assertTrue(active.done)
        assertNull(active.primaryLabel)

        val serverDone = sunatItem(null, serverDone = true)
        assertTrue(serverDone.done)
        assertEquals("Activa", serverDone.statusText)
    }

    @Test
    fun `estado de sunat desconocido se trata como sin solicitar`() {
        assertEquals(SunatRequestState.NONE, SunatRequestState.fromApi("lo-que-sea"))
        assertEquals(SunatRequestState.IN_REVIEW, SunatRequestState.fromApi("in_review"))
    }

    // ---- Tuteo / copys ----

    @Test
    fun `copys exactos en tuteo`() {
        val c = assertNotNull(buildOnboardingChecklist(state(allSteps()), false))
        val expected = mapOf(
            OnboardingStepKey.MENU to Triple("Tu carta", "Carga tus platos para que tus mozos puedan vender.", "Subir mi carta"),
            OnboardingStepKey.FIRST_SALE to Triple("Tu primera venta", "Cobra un pedido para ver cómo funciona todo junto.", "Hacer mi primera venta"),
            OnboardingStepKey.TABLES to Triple("Revisa tus mesas", "Ya creamos 10 mesas. Ponles el nombre que usas en tu local.", "Revisar mesas"),
            OnboardingStepKey.AUTH_PIN to Triple("Crea tu PIN de autorización", "Lo necesitas para anular o devolver una venta.", "Crear mi PIN"),
            OnboardingStepKey.TEAM to Triple("Agrega a tu equipo", "Crea un PIN para cada mozo y cocinero.", "Agregar equipo"),
            OnboardingStepKey.QR_MENU to Triple("Activa tu carta QR", "Para que tus clientes pidan desde la mesa.", "Activar carta QR"),
            OnboardingStepKey.SUNAT to Triple("Emite boletas y facturas con SUNAT", "Te avisamos cuando esté lista.", "Solicitar activación"),
            OnboardingStepKey.RECIPES to Triple("Calcula el costo de tus platos", "Agrega ingredientes a tus platos.", "Ver mis platos"),
            OnboardingStepKey.DELIVERY to Triple("Configura tu delivery", "Registra a tus repartidores.", "Agregar repartidor"),
            OnboardingStepKey.BRANCHES to Triple("Agrega otro local", "Si tienes más de una sucursal.", "Ver sucursales"),
        )
        expected.forEach { (key, copy) ->
            val item = c.item(key)
            assertEquals(copy, Triple(item.title, item.description, item.primaryLabel), key.apiKey)
        }
    }

    // ---- Quién ve el checklist ----

    private fun session(perms: List<String>, authMethod: String?) = UserSession(
        token = "t",
        user = AuthUser(1, "Ana", "a@b.c", "admin", authMethod = authMethod),
        restaurantPermissions = perms,
    )

    @Test
    fun `solo el administrador de sesion completa ve el checklist`() {
        val admin = listOf(RestaurantPermissions.PERM_ADMIN)
        assertTrue(session(admin, authMethod = null).canSeeOnboarding())
        assertTrue(session(admin, authMethod = "password").canSeeOnboarding())
        // Admin entrando por PIN de turno: token sin permisos de configuración.
        assertFalse(session(admin, authMethod = "pin").canSeeOnboarding())
        // Mozo / cajero / cocina.
        assertFalse(session(listOf(RestaurantPermissions.PERM_POS), null).canSeeOnboarding())
        assertFalse(session(listOf(RestaurantPermissions.PERM_CAJA, RestaurantPermissions.PERM_COMANDAS), null).canSeeOnboarding())
        assertFalse(session(emptyList(), null).canSeeOnboarding())
        val none: UserSession? = null
        assertFalse(none.canSeeOnboarding())
    }
}
