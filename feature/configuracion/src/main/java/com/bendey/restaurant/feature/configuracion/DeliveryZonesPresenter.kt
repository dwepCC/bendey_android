package com.bendey.restaurant.feature.configuracion

import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.catalog.UbiItem
import com.bendey.restaurant.core.domain.delivery.DeliverySettingsUpdate
import com.bendey.restaurant.core.domain.delivery.DeliveryZone
import com.bendey.restaurant.core.domain.delivery.DeliveryZoneInput
import com.bendey.restaurant.core.domain.delivery.DeliveryZoneRules
import com.bendey.restaurant.core.domain.delivery.DeliveryZonesRepository
import com.bendey.restaurant.core.domain.delivery.canManageDeliverySettings
import com.bendey.restaurant.core.domain.delivery.restrictWouldBlockAll
import com.bendey.restaurant.core.domain.model.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Formulario de una zona. Región → Provincia → Distrito en cascada; el id del distrito es el código de 6 dígitos. */
data class ZoneFormState(
    /** null = zona nueva. */
    val editingId: Int? = null,
    val code: String = "",
    val regionId: String = "",
    val provinciaId: String = "",
    val regiones: List<UbiItem> = emptyList(),
    val provincias: List<UbiItem> = emptyList(),
    val distritos: List<UbiItem> = emptyList(),
    val name: String = "",
    val feeText: String = "0",
    val minOrderText: String = "0",
    val etaText: String = "0",
    val active: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
)

/** Estado de «Zonas de reparto» en Ajustes > Operación. */
data class DeliveryZonesState(
    val zones: List<DeliveryZone> = emptyList(),
    val restrictToZones: Boolean = false,
    val loading: Boolean = false,
    val loaded: Boolean = false,
    /** `s.m`: sin esto la tarjeta es de solo lectura. */
    val canEdit: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val form: ZoneFormState? = null,
    val deleteTarget: DeliveryZone? = null,
) {
    /** Reparto restringido y ninguna zona activa: nadie podría pedir delivery. */
    val restrictWarning: Boolean get() = restrictToZones && restrictWouldBlockAll(zones)
}

/**
 * Reductor de Ajustes > Operación > Zonas de reparto (§26). Sin Android: lo usa [ConfiguracionViewModel] y los tests.
 * Solo `s.m` cambia algo. Una zona por distrito: el servidor hace upsert por código, así que al CREAR se rechaza
 * (en local) un distrito que ya tiene zona, para no pisarla sin querer.
 */
class DeliveryZonesPresenter(
    private val scope: CoroutineScope,
    private val repository: DeliveryZonesRepository,
    private val deliveryRepository: DeliveryRepository,
    private val regiones: suspend () -> AppResult<List<UbiItem>>,
    private val provincias: suspend (String) -> AppResult<List<UbiItem>>,
    private val distritos: suspend (String) -> AppResult<List<UbiItem>>,
) {
    private val _state = MutableStateFlow(DeliveryZonesState())
    val state: StateFlow<DeliveryZonesState> = _state.asStateFlow()

    fun setPermissions(permissions: List<String>) {
        _state.update { it.copy(canEdit = canManageDeliverySettings(permissions)) }
    }

    fun load() {
        _state.update { it.copy(loading = true) }
        scope.launch {
            when (val r = repository.zones()) {
                is AppResult.Success -> _state.update {
                    it.copy(loading = false, loaded = true, zones = r.data.zones, restrictToZones = r.data.restrictToZones, error = null)
                }
                is AppResult.Error -> _state.update { it.copy(loading = false, error = r.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    // ---- «Repartir solo en estas zonas»
    fun toggleRestrict(value: Boolean) {
        val s = _state.value
        if (!s.canEdit || s.saving || value == s.restrictToZones) return
        _state.update { it.copy(saving = true, error = null) }
        scope.launch {
            when (val r = deliveryRepository.updateDeliverySettings(DeliverySettingsUpdate(restrictToZones = value))) {
                is AppResult.Success -> _state.update { it.copy(saving = false, restrictToZones = r.data.restrictToZones) }
                is AppResult.Error -> _state.update { it.copy(saving = false, error = r.message) }
                AppResult.Loading -> _state.update { it.copy(saving = false) }
            }
        }
    }

    // ---- formulario
    fun openCreate() {
        if (!_state.value.canEdit) return
        _state.update { it.copy(form = ZoneFormState(), error = null) }
        scope.launch {
            val reg = (regiones() as? AppResult.Success)?.data ?: return@launch
            updateForm { f -> f.copy(regiones = reg) }
        }
    }

    fun openEdit(zone: DeliveryZone) {
        if (!_state.value.canEdit) return
        _state.update {
            it.copy(
                error = null,
                form = ZoneFormState(
                    editingId = zone.id,
                    code = zone.ubigeoCode,
                    name = zone.name,
                    feeText = DeliveryZoneRules.formatAmount(zone.fee),
                    minOrderText = DeliveryZoneRules.formatAmount(zone.minOrder),
                    etaText = zone.etaMinutes.toString(),
                    active = zone.active,
                ),
            )
        }
    }

    fun dismissForm() = _state.update { if (it.form?.saving == true) it else it.copy(form = null) }

    private fun updateForm(change: (ZoneFormState) -> ZoneFormState) =
        _state.update { st -> st.copy(form = st.form?.let(change)) }

    fun selectRegion(regionId: String) {
        updateForm { it.copy(regionId = regionId, provinciaId = "", provincias = emptyList(), distritos = emptyList(), code = "", error = null) }
        if (regionId.isBlank()) return
        scope.launch {
            val prov = (provincias(regionId) as? AppResult.Success)?.data ?: return@launch
            updateForm { f -> if (f.regionId == regionId) f.copy(provincias = prov) else f }
        }
    }

    fun selectProvincia(provinciaId: String) {
        updateForm { it.copy(provinciaId = provinciaId, distritos = emptyList(), code = "", error = null) }
        if (provinciaId.isBlank()) return
        scope.launch {
            val dist = (distritos(provinciaId) as? AppResult.Success)?.data ?: return@launch
            updateForm { f -> if (f.provinciaId == provinciaId) f.copy(distritos = dist) else f }
        }
    }

    /** El nombre de la zona arranca con el del distrito si todavía está vacío. */
    fun selectDistrito(code: String) = updateForm { f ->
        val nombre = f.distritos.firstOrNull { it.id == code }?.nombre.orEmpty()
        f.copy(code = code, name = f.name.ifBlank { nombre }, error = null)
    }

    fun setName(text: String) = updateForm { it.copy(name = text.take(DeliveryZoneRules.MAX_NAME), error = null) }
    fun setFee(text: String) = updateForm { it.copy(feeText = amountFilter(text), error = null) }
    fun setMinOrder(text: String) = updateForm { it.copy(minOrderText = amountFilter(text), error = null) }
    fun setEta(text: String) = updateForm { it.copy(etaText = text.filter(Char::isDigit).take(3), error = null) }
    fun setActive(value: Boolean) = updateForm { it.copy(active = value, error = null) }

    private fun amountFilter(text: String) = text.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6)

    fun saveForm() {
        val s = _state.value
        val f = s.form ?: return
        if (f.saving || !s.canEdit) return
        val invalid = DeliveryZoneRules.validate(f.code, f.name, f.feeText, f.minOrderText, f.etaText)
            ?: if (f.editingId == null && s.zones.any { it.ubigeoCode == f.code }) {
                "Ya tienes una zona para ese distrito: edítala desde la lista."
            } else {
                null
            }
        if (invalid != null) {
            _state.update { it.copy(form = f.copy(error = invalid)) }
            return
        }
        val input = DeliveryZoneInput(
            ubigeoCode = f.code,
            name = f.name.trim(),
            fee = DeliveryZoneRules.parseAmount(f.feeText) ?: 0.0,
            minOrder = DeliveryZoneRules.parseAmount(f.minOrderText) ?: 0.0,
            etaMinutes = DeliveryZoneRules.parseEta(f.etaText) ?: 0,
            active = f.active,
        )
        _state.update { it.copy(form = f.copy(saving = true, error = null)) }
        scope.launch {
            when (val r = repository.saveZone(input)) {
                is AppResult.Success -> _state.update { st ->
                    st.copy(
                        form = null,
                        zones = (st.zones.filter { it.id != r.data.id && it.ubigeoCode != r.data.ubigeoCode } + r.data)
                            .sortedBy { it.name.lowercase() },
                    )
                }
                is AppResult.Error -> _state.update { it.copy(form = it.form?.copy(saving = false, error = r.message)) }
                AppResult.Loading -> Unit
            }
        }
    }

    // ---- activar / apagar sin abrir el formulario
    fun toggleActive(zone: DeliveryZone, active: Boolean) {
        val s = _state.value
        if (!s.canEdit || s.saving) return
        _state.update { it.copy(saving = true, error = null) }
        scope.launch {
            val input = DeliveryZoneInput(zone.ubigeoCode, zone.name, zone.fee, zone.minOrder, zone.etaMinutes, active)
            when (val r = repository.saveZone(input)) {
                is AppResult.Success -> _state.update { st ->
                    st.copy(saving = false, zones = st.zones.map { if (it.id == zone.id) r.data else it })
                }
                is AppResult.Error -> _state.update { it.copy(saving = false, error = r.message) }
                AppResult.Loading -> _state.update { it.copy(saving = false) }
            }
        }
    }

    // ---- borrar (con confirmación)
    fun askDelete(zone: DeliveryZone) {
        if (_state.value.canEdit) _state.update { it.copy(deleteTarget = zone) }
    }

    fun dismissDelete() = _state.update { if (it.saving) it else it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        scope.launch {
            when (val r = repository.deleteZone(target.id)) {
                is AppResult.Success -> _state.update {
                    it.copy(saving = false, deleteTarget = null, zones = it.zones.filter { z -> z.id != target.id })
                }
                is AppResult.Error -> _state.update { it.copy(saving = false, deleteTarget = null, error = r.message) }
                AppResult.Loading -> _state.update { it.copy(saving = false) }
            }
        }
    }
}
