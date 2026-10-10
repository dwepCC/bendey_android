package com.bendey.restaurant.feature.configuracion

import com.bendey.restaurant.core.domain.catalog.DeliveryCompany
import com.bendey.restaurant.core.domain.catalog.DeliveryCompanyFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryDriver
import com.bendey.restaurant.core.domain.catalog.DeliveryDriverFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.catalog.UbiItem
import com.bendey.restaurant.core.domain.delivery.DeliveryBoardData
import com.bendey.restaurant.core.domain.delivery.DeliverySettings
import com.bendey.restaurant.core.domain.delivery.DeliverySettingsUpdate
import com.bendey.restaurant.core.domain.delivery.DeliveryZone
import com.bendey.restaurant.core.domain.delivery.DeliveryZoneInput
import com.bendey.restaurant.core.domain.delivery.DeliveryZonesData
import com.bendey.restaurant.core.domain.delivery.DeliveryZonesRepository
import com.bendey.restaurant.core.domain.delivery.SessionPayment
import com.bendey.restaurant.core.domain.model.AppResult
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** §26: Ajustes > Operación > Zonas de reparto. */
class DeliveryZonesPresenterTest {

    private class FakeZones(var zones: List<DeliveryZone>, var restrict: Boolean = false) : DeliveryZonesRepository {
        val saved = CopyOnWriteArrayList<DeliveryZoneInput>()
        val deleted = CopyOnWriteArrayList<Int>()
        @Volatile var saveError: String? = null
        @Volatile var deleteError: String? = null
        private var nextId = 100

        override suspend fun zones(): AppResult<DeliveryZonesData> = AppResult.Success(DeliveryZonesData(zones, restrict))

        override suspend fun saveZone(input: DeliveryZoneInput): AppResult<DeliveryZone> {
            saved += input
            saveError?.let { return AppResult.Error(it) }
            val id = zones.firstOrNull { it.ubigeoCode == input.ubigeoCode }?.id ?: nextId++
            val z = DeliveryZone(id, input.ubigeoCode, input.name, input.fee, input.minOrder, input.etaMinutes, input.active)
            zones = zones.filter { it.id != id } + z
            return AppResult.Success(z)
        }

        override suspend fun deleteZone(id: Int): AppResult<Unit> {
            deleteError?.let { return AppResult.Error(it) }
            deleted += id
            zones = zones.filter { it.id != id }
            return AppResult.Success(Unit)
        }
    }

    private class FakeDelivery : DeliveryRepository {
        val updates = CopyOnWriteArrayList<DeliverySettingsUpdate>()
        @Volatile var error: String? = null
        var settings = DeliverySettings()
        override suspend fun getDeliverySettings(forceRefresh: Boolean): AppResult<DeliverySettings> = AppResult.Success(settings)
        override suspend fun updateDeliverySettings(update: DeliverySettingsUpdate): AppResult<DeliverySettings> {
            updates += update
            error?.let { return AppResult.Error(it) }
            settings = settings.copy(restrictToZones = update.restrictToZones ?: settings.restrictToZones)
            return AppResult.Success(settings)
        }
        override fun peekDeliverySettings(): DeliverySettings? = settings
        override suspend fun setSessionPayment(sessionId: Int, mode: String, cashTendered: Double?): AppResult<SessionPayment?> = AppResult.Success(null)
        override suspend fun collectAssignment(assignmentId: Int): AppResult<SessionPayment?> = AppResult.Success(null)
        override suspend fun setSessionDeliveryFee(sessionId: Int, amount: Double): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getDeliveryBoard(): AppResult<DeliveryBoardData> = AppResult.Success(DeliveryBoardData())
        override suspend fun assignDriver(sessionId: Int, driverId: Int, deliveryFee: Double?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun cancelDeliveryOrder(sessionId: Int, reason: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateAssignmentStatus(assignmentId: Int, status: String, failedReason: String?, forceReason: String?): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun listDrivers(): AppResult<List<DeliveryDriver>> = AppResult.Success(emptyList())
        override suspend fun createDriver(input: DeliveryDriverFormInput): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateDriver(id: Int, input: DeliveryDriverFormInput): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteDriver(id: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun listCompanies(): AppResult<List<DeliveryCompany>> = AppResult.Success(emptyList())
        override suspend fun createCompany(input: DeliveryCompanyFormInput): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateCompany(id: Int, name: String, active: Boolean): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteCompany(id: Int): AppResult<Unit> = AppResult.Success(Unit)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val zonesRepo = FakeZones(listOf(DeliveryZone(1, "080400", "Calca", fee = 4.0)))
    private val deliveryRepo = FakeDelivery()

    private fun presenter(canEdit: Boolean = true): DeliveryZonesPresenter {
        val p = DeliveryZonesPresenter(
            scope, zonesRepo, deliveryRepo,
            regiones = { AppResult.Success(listOf(UbiItem("080000", "Cusco"))) },
            provincias = { AppResult.Success(listOf(UbiItem("080400", "Calca"))) },
            distritos = { AppResult.Success(listOf(UbiItem("080401", "Calca"), UbiItem("080402", "Coya"))) },
        )
        p.setPermissions(if (canEdit) listOf("s.m") else listOf("o.ch"))
        return p
    }

    @After fun tearDown() = scope.cancel()
    private fun await(cond: () -> Boolean) = runBlocking { withTimeout(3000) { while (!cond()) delay(10) } }

    private fun loaded(p: DeliveryZonesPresenter): DeliveryZonesPresenter {
        p.load()
        await { p.state.value.loaded }
        return p
    }

    /** Abre el formulario nuevo y baja la cascada hasta el distrito 080402. */
    private fun cascade(p: DeliveryZonesPresenter) {
        p.openCreate()
        await { p.state.value.form?.regiones?.isNotEmpty() == true }
        p.selectRegion("080000")
        await { p.state.value.form?.provincias?.isNotEmpty() == true }
        p.selectProvincia("080400")
        await { p.state.value.form?.distritos?.isNotEmpty() == true }
        p.selectDistrito("080402")
    }

    @Test fun cargaLasZonasYElInterruptor() {
        zonesRepo.restrict = true
        val p = loaded(presenter())
        assertEquals(1, p.state.value.zones.size)
        assertTrue(p.state.value.restrictToZones)
    }

    @Test fun laCascadaArrastraElNombreDelDistritoYLimpiaAlCambiarDeRegion() {
        val p = loaded(presenter())
        cascade(p)
        assertEquals("080402", p.state.value.form?.code)
        assertEquals("Coya", p.state.value.form?.name)
        p.selectRegion("150000")
        assertEquals("", p.state.value.form?.code)
        assertTrue(p.state.value.form?.distritos?.isEmpty() == true)
    }

    @Test fun guardarUnaZonaNuevaLaEnvia() {
        val p = loaded(presenter())
        cascade(p)
        p.setFee("0")
        p.setMinOrder("15,5")
        p.setEta("40")
        p.saveForm()
        await { p.state.value.form == null }
        val sent = zonesRepo.saved.single()
        assertEquals(DeliveryZoneInput("080402", "Coya", 0.0, 15.5, 40, true), sent)
        assertEquals(2, p.state.value.zones.size)
    }

    @Test fun sinDistritoNoSeEnvia() {
        val p = loaded(presenter())
        p.openCreate()
        p.setName("Centro")
        p.saveForm()
        assertTrue(zonesRepo.saved.isEmpty())
        assertNotNull(p.state.value.form?.error)
    }

    @Test fun noPisaPorErrorUnaZonaExistenteAlCrear() {
        val p = loaded(presenter())
        p.openCreate()
        await { p.state.value.form?.regiones?.isNotEmpty() == true }
        p.selectDistrito("080400")
        p.setName("Otra")
        p.saveForm()
        assertTrue(zonesRepo.saved.isEmpty())
        assertTrue(p.state.value.form?.error!!.contains("Ya tienes una zona"))
    }

    @Test fun editarUsaElMismoGuardado() {
        val p = loaded(presenter())
        p.openEdit(p.state.value.zones[0])
        assertEquals("4.00", p.state.value.form?.feeText)
        p.setFee("6")
        p.saveForm()
        await { p.state.value.form == null }
        assertEquals(6.0, zonesRepo.saved.single().fee, 0.0)
        assertEquals("080400", zonesRepo.saved.single().ubigeoCode)
        assertEquals(1, p.state.value.zones.size)
        assertEquals(6.0, p.state.value.zones[0].fee, 0.0)
    }

    @Test fun unErrorDelServidorQuedaEnElFormulario() {
        val p = loaded(presenter())
        zonesRepo.saveError = "Revisa la tarifa y el pedido mínimo (entre S/ 0 y S/ 999.99) y el tiempo (0 a 240 minutos)."
        p.openEdit(p.state.value.zones[0])
        p.saveForm()
        await { p.state.value.form?.error != null && p.state.value.form?.saving == false }
        assertNotNull(p.state.value.form)
    }

    @Test fun borrarPideConfirmacion() {
        val p = loaded(presenter())
        p.askDelete(p.state.value.zones[0])
        assertNotNull(p.state.value.deleteTarget)
        assertTrue(zonesRepo.deleted.isEmpty())
        p.dismissDelete()
        assertNull(p.state.value.deleteTarget)
        p.askDelete(p.state.value.zones[0])
        p.confirmDelete()
        await { zonesRepo.deleted.isNotEmpty() && !p.state.value.saving }
        assertTrue(p.state.value.zones.isEmpty())
    }

    @Test fun apagarUnaZonaGuardaElResto() {
        val p = loaded(presenter())
        p.toggleActive(p.state.value.zones[0], false)
        await { zonesRepo.saved.isNotEmpty() && !p.state.value.saving }
        assertFalse(zonesRepo.saved.single().active)
        assertEquals(4.0, zonesRepo.saved.single().fee, 0.0)
    }

    @Test fun elInterruptorEnviaSoloRestrictToZones() {
        val p = loaded(presenter())
        p.toggleRestrict(true)
        await { p.state.value.restrictToZones }
        assertEquals(DeliverySettingsUpdate(restrictToZones = true), deliveryRepo.updates.single())
    }

    @Test fun encenderSinZonasActivasAvisaQueNadieVaAPoderPedir() {
        zonesRepo.zones = emptyList()
        val p = loaded(presenter())
        p.toggleRestrict(true)
        await { p.state.value.restrictToZones }
        assertTrue(p.state.value.restrictWarning)
    }

    @Test fun conUnaZonaActivaNoHayAdvertencia() {
        val p = loaded(presenter())
        p.toggleRestrict(true)
        await { p.state.value.restrictToZones }
        assertFalse(p.state.value.restrictWarning)
    }

    @Test fun unRechazoDelInterruptorMuestraElErrorYLoDejaApagado() {
        val p = loaded(presenter())
        deliveryRepo.error = "Las reglas de delivery aún no están disponibles."
        p.toggleRestrict(true)
        await { p.state.value.error != null && !p.state.value.saving }
        assertFalse(p.state.value.restrictToZones)
    }

    @Test fun sinPermisoDeAdministradorNadaSeCambia() {
        val p = loaded(presenter(canEdit = false))
        p.openCreate()
        assertNull(p.state.value.form)
        p.openEdit(p.state.value.zones[0])
        assertNull(p.state.value.form)
        p.askDelete(p.state.value.zones[0])
        assertNull(p.state.value.deleteTarget)
        p.toggleRestrict(true)
        p.toggleActive(p.state.value.zones[0], false)
        assertTrue(deliveryRepo.updates.isEmpty())
        assertTrue(zonesRepo.saved.isEmpty())
    }
}
