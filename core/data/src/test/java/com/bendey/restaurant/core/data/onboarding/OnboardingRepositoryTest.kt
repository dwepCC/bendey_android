package com.bendey.restaurant.core.data.onboarding

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.onboarding.OnboardingPreferencesUpdate
import com.bendey.restaurant.core.domain.onboarding.OnboardingTier
import com.bendey.restaurant.core.domain.onboarding.OnboardingUnavailableException
import com.bendey.restaurant.core.domain.onboarding.SunatRequestState
import com.bendey.restaurant.core.network.api.OnboardingApi
import com.bendey.restaurant.core.network.dto.OnboardingPatchDto
import com.bendey.restaurant.core.network.dto.OnboardingStateDto
import com.bendey.restaurant.core.network.dto.SampleDataDeleteResponseDto
import com.bendey.restaurant.core.network.dto.SampleMenuResponseDto
import com.bendey.restaurant.core.network.dto.SunatRequestStatusDto
import com.bendey.restaurant.core.network.dto.TelemetryEventDto
import com.bendey.restaurant.core.network.serialization.ApiJson
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** Repositorio de onboarding probado con un API falso: contrato, mapeo de errores y mensajes en tuteo. */
class OnboardingRepositoryTest {

    private class FakeApi : OnboardingApi {
        var stateResult: () -> OnboardingStateDto = { OnboardingStateDto() }
        var patchResult: () -> Unit = {}
        var sampleMenuResult: () -> SampleMenuResponseDto = { SampleMenuResponseDto(10) }
        var deleteResult: () -> SampleDataDeleteResponseDto = { SampleDataDeleteResponseDto(8, 2) }
        var sunatResult: () -> SunatRequestStatusDto = { SunatRequestStatusDto("requested", "2026-10-04T10:00:00Z") }

        val patches = mutableListOf<OnboardingPatchDto>()
        var getCalls = 0
        val events = mutableListOf<TelemetryEventDto>()

        override suspend fun getState(): OnboardingStateDto {
            getCalls++
            return stateResult()
        }

        override suspend fun patch(body: OnboardingPatchDto) {
            patches += body
            patchResult()
        }

        override suspend fun loadSampleMenu(): SampleMenuResponseDto = sampleMenuResult()
        override suspend fun deleteSampleData(): SampleDataDeleteResponseDto = deleteResult()
        override suspend fun requestSunat(): SunatRequestStatusDto = sunatResult()
        override suspend fun getSunatStatus(): SunatRequestStatusDto = sunatResult()
        override suspend fun postTelemetryEvent(body: TelemetryEventDto) {
            events += body
        }
    }

    private val api = FakeApi()
    private val repo = OnboardingRepositoryImpl { api }

    private fun http(code: Int, body: String = "") = HttpException(
        Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())),
    )

    private fun <T> AppResult<T>.error(): AppResult.Error = this as AppResult.Error
    private fun <T> AppResult<T>.data(): T = (this as AppResult.Success).data

    private val contractJson = """
        {"dismissed":false,"business_subtype":"restaurante","sample_data_loaded":true,
         "started_at":"2026-10-01T10:00:00Z","completed_at":null,"first_sale_seen":false,
         "campo_nuevo_que_no_conocemos":123,
         "progress":{"sell_ready":true,"operate_done":2,"operate_total":5,"percent":40},
         "steps":[
           {"key":"menu","tier":"required","done":true,"skipped":false,"local_only":false,"count":12},
           {"key":"printer","tier":"recommended","done":false,"skipped":false,"local_only":true,"count":null},
           {"key":"sunat","tier":"advanced","done":false,"skipped":true,"local_only":false},
           {"key":"x","tier":"otro","done":false}
         ]}
    """.trimIndent()

    // ---- GET ----

    @Test
    fun `el JSON del contrato se parsea completo e ignora campos desconocidos`() = runBlocking {
        api.stateResult = { ApiJson.decodeFromString<OnboardingStateDto>(contractJson) }
        val s = repo.getState().data()

        assertFalse(s.dismissed)
        assertEquals("restaurante", s.businessSubtype)
        assertTrue(s.sampleDataLoaded)
        assertEquals("2026-10-01T10:00:00Z", s.startedAt)
        assertNull(s.completedAt)
        assertTrue(s.progress.sellReady)
        assertEquals(2, s.progress.operateDone)
        assertEquals(5, s.progress.operateTotal)
        assertEquals(40, s.progress.percent)
        assertEquals(4, s.steps.size)

        val menu = s.steps[0]
        assertEquals(OnboardingTier.REQUIRED, menu.tier)
        assertTrue(menu.done)
        assertEquals(12, menu.count)

        val printer = s.steps[1]
        assertTrue(printer.localOnly)
        assertNull(printer.count)

        assertTrue(s.steps[2].skipped)
        assertNull(s.steps[3].tier) // tier desconocido: no revienta
    }

    @Test
    fun `un cuerpo vacio usa los valores por defecto`() = runBlocking {
        api.stateResult = { ApiJson.decodeFromString<OnboardingStateDto>("{}") }
        val s = repo.getState().data()
        assertTrue(s.steps.isEmpty())
        assertFalse(s.dismissed)
    }

    @Test
    fun `403 y 404 se marcan como no disponible y no como error a reintentar`() = runBlocking {
        listOf(403, 404).forEach { code ->
            api.stateResult = { throw http(code) }
            val e = repo.getState().error()
            assertTrue("HTTP $code", e.cause is OnboardingUnavailableException)
            assertEquals(OnboardingRepositoryImpl.MSG_UNAVAILABLE, e.message)
        }
    }

    @Test
    fun `500 devuelve un mensaje accionable en tuteo`() = runBlocking {
        api.stateResult = { throw http(500) }
        val e = repo.getState().error()
        assertEquals(OnboardingRepositoryImpl.MSG_LOAD, e.message)
        assertFalse(e.cause is OnboardingUnavailableException)
        assertTrue(e.message!!.contains("Inténtalo"))
    }

    @Test
    fun `sin red el mensaje habla de conexion`() = runBlocking {
        api.stateResult = { throw IOException("Unable to resolve host") }
        assertEquals(OnboardingRepositoryImpl.MSG_CONNECTION, repo.getState().error().message)
    }

    @Test
    fun `cuerpo ilegible - mensaje de respuesta inesperada`() = runBlocking {
        api.stateResult = { ApiJson.decodeFromString<OnboardingStateDto>("no es json {{") }
        assertEquals(OnboardingRepositoryImpl.MSG_UNEXPECTED, repo.getState().error().message)
    }

    // ---- PATCH ----

    @Test
    fun `patch envia solo los campos pedidos y refresca con GET`() = runBlocking {
        api.stateResult = { OnboardingStateDto(dismissed = true) }
        val result = repo.updatePreferences(OnboardingPreferencesUpdate(dismissed = true))

        assertEquals(1, api.patches.size)
        assertEquals(OnboardingPatchDto(dismissed = true), api.patches[0])
        assertEquals(1, api.getCalls)
        assertTrue(result.data().dismissed)
    }

    @Test
    fun `el cuerpo del patch omite lo que es null`() {
        val json = ApiJson.encodeToString(OnboardingPatchDto(skip = listOf("printer", "team")))
        assertEquals("""{"skip":["printer","team"]}""", json)
        assertEquals("""{"dismissed":false}""", ApiJson.encodeToString(OnboardingPatchDto(dismissed = false)))
        assertEquals(
            """{"unskip":["sunat"],"business_subtype":"cafeteria"}""",
            ApiJson.encodeToString(OnboardingPatchDto(unskip = listOf("sunat"), businessSubtype = "cafeteria")),
        )
    }

    @Test
    fun `si el patch falla no se hace GET y el mensaje es de guardado`() = runBlocking {
        api.patchResult = { throw http(500) }
        val e = repo.updatePreferences(OnboardingPreferencesUpdate(skip = listOf("printer"))).error()
        assertEquals(OnboardingRepositoryImpl.MSG_SAVE, e.message)
        assertEquals(0, api.getCalls)
    }

    // ---- sample menu / sample data ----

    @Test
    fun `carta de ejemplo devuelve cuantos platos se crearon`() = runBlocking {
        assertEquals(10, repo.loadSampleMenu().data().created)
    }

    @Test
    fun `carta de ejemplo con catalogo no vacio da el mensaje exacto del 409`() = runBlocking {
        api.sampleMenuResult = { throw http(409, """{"error":"loquesea del backend"}""") }
        val e = repo.loadSampleMenu().error()
        assertEquals("Ya tienes productos; la carta de ejemplo solo se carga en una carta vacía.", e.message)
    }

    @Test
    fun `el 201 de sample-menu es exito y 422 y 403 dan mensaje de fallo`() = runBlocking {
        // Retrofit trata todo 2xx (incluido 201) como exito: el cuerpo {"created":N} se parsea igual.
        api.sampleMenuResult = { ApiJson.decodeFromString<SampleMenuResponseDto>("""{"created":7}""") }
        assertEquals(7, repo.loadSampleMenu().data().created)
        listOf(422, 403).forEach { code ->
            api.sampleMenuResult = { throw http(code) }
            val e = repo.loadSampleMenu().error()
            assertFalse("HTTP $code", e.message!!.contains("Ya tienes productos"))
        }
    }

    @Test
    fun `count nulo o ausente no rompe el parseo`() {
        val dto = ApiJson.decodeFromString<OnboardingStateDto>(
            """{"steps":[{"key":"menu","count":3},{"key":"tables","count":null},{"key":"qr_menu"}]}""",
        )
        assertEquals(listOf<Int?>(3, null, null), dto.steps.map { it.count })
    }

    @Test
    fun `carta de ejemplo con otro fallo da un mensaje propio`() = runBlocking {
        api.sampleMenuResult = { throw http(500) }
        assertEquals(OnboardingRepositoryImpl.MSG_SAMPLE_LOAD, repo.loadSampleMenu().error().message)
    }

    @Test
    fun `borrar ejemplos devuelve borrados y desactivados`() = runBlocking {
        val r = repo.deleteSampleData().data()
        assertEquals(8, r.deleted)
        assertEquals(2, r.deactivated)
    }

    @Test
    fun `borrar ejemplos con fallo da un mensaje accionable`() = runBlocking {
        api.deleteResult = { throw http(500) }
        assertEquals(OnboardingRepositoryImpl.MSG_SAMPLE_DELETE, repo.deleteSampleData().error().message)
    }

    // ---- SUNAT ----

    @Test
    fun `sunat mapea el estado y la fecha`() = runBlocking {
        val r = repo.requestSunatActivation().data()
        assertEquals(SunatRequestState.REQUESTED, r.state)
        assertEquals("2026-10-04T10:00:00Z", r.requestedAt)

        api.sunatResult = { SunatRequestStatusDto("in_review", "2026-10-04T10:00:00Z") }
        assertEquals(SunatRequestState.IN_REVIEW, repo.getSunatStatus().data().state)

        api.sunatResult = { SunatRequestStatusDto("none", null) }
        val none = repo.getSunatStatus().data()
        assertEquals(SunatRequestState.NONE, none.state)
        assertNull(none.requestedAt)

        api.sunatResult = { SunatRequestStatusDto("estado_raro") }
        assertEquals(SunatRequestState.NONE, repo.getSunatStatus().data().state)
    }

    @Test
    fun `sunat con fallo da un mensaje accionable`() = runBlocking {
        api.sunatResult = { throw http(500) }
        assertEquals(OnboardingRepositoryImpl.MSG_SUNAT_REQUEST, repo.requestSunatActivation().error().message)
    }

    @Test
    fun `403 fuera de GET explica que es solo del administrador`() = runBlocking {
        api.deleteResult = { throw http(403) }
        assertEquals(OnboardingRepositoryImpl.MSG_ADMIN_ONLY, repo.deleteSampleData().error().message)
    }

    // ---- Telemetría ----

    @Test
    fun `el evento printer_configured viaja con device_kind android`() {
        val telemetry = OnboardingTelemetry(
            apiSource = { api },
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            log = { _, _ -> },
        )
        telemetry.reportPrinterConfigured()
        assertEquals(listOf(TelemetryEventDto("printer_configured", "android")), api.events)
        assertEquals(
            """{"key":"printer_configured","device_kind":"android"}""",
            ApiJson.encodeToString(api.events[0]),
        )
    }

    @Test
    fun `si el evento falla no lanza nada y solo va al log`() {
        val logged = mutableListOf<String>()
        val failing = object : OnboardingApi by api {
            override suspend fun postTelemetryEvent(body: TelemetryEventDto) {
                throw IOException("sin red")
            }
        }
        val telemetry = OnboardingTelemetry(
            apiSource = { failing },
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            log = { message, _ -> logged += message },
        )
        telemetry.reportPrinterConfigured() // no debe lanzar
        assertEquals(1, logged.size)
        assertTrue(logged[0].contains("printer_configured"))
    }

    @Test
    fun `si ni siquiera hay API configurada el evento no rompe a quien lo llama`() {
        val logged = mutableListOf<String>()
        val telemetry = OnboardingTelemetry(
            apiSource = { error("Tenant API URL no configurada") },
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            log = { message, _ -> logged += message },
        )
        telemetry.reportPrinterConfigured()
        assertEquals(1, logged.size)
    }
}
