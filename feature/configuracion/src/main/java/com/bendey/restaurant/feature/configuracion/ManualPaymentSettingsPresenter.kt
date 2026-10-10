package com.bendey.restaurant.feature.configuracion

import com.bendey.restaurant.core.domain.cash.CashPaymentMethod
import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.DeliverySettings
import com.bendey.restaurant.core.domain.delivery.DeliverySettingsUpdate
import com.bendey.restaurant.core.domain.delivery.ManualPaymentRepository
import com.bendey.restaurant.core.domain.delivery.ManualPaymentRules
import com.bendey.restaurant.core.domain.delivery.PayMethod
import com.bendey.restaurant.core.domain.delivery.PayMethodInput
import com.bendey.restaurant.core.domain.delivery.canManageDeliverySettings
import com.bendey.restaurant.core.domain.model.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Medio de Caja y Bancos (no efectivo) donde puede entrar la plata de un pago con captura. */
data class CashbankMethodOption(val code: String, val name: String)

/** Imagen elegida del sistema, aún sin subir. */
class PendingImage(val bytes: ByteArray, val mimeType: String)

data class PayMethodFormState(
    /** null = medio nuevo. */
    val editingId: Int? = null,
    val methodCode: String = "",
    val label: String = "",
    val holder: String = "",
    val accountNumber: String = "",
    val cci: String = "",
    val instructions: String = "",
    val active: Boolean = true,
    val existingQrUrl: String = "",
    val pendingQr: PendingImage? = null,
    val saving: Boolean = false,
    val error: String? = null,
) {
    val input: PayMethodInput
        get() = PayMethodInput(methodCode, label, holder, accountNumber, cci, instructions, active)
}

/** Estado de «Pago con captura» (D2a) en Ajustes > Operación. */
data class ManualPaymentSettingsState(
    val settings: DeliverySettings? = null,
    val methods: List<PayMethod> = emptyList(),
    val cashbankOptions: List<CashbankMethodOption> = emptyList(),
    val loading: Boolean = false,
    /** `s.m`: sin esto la tarjeta es de solo lectura. */
    val canEdit: Boolean = false,
    val saving: Boolean = false,
    val minutesText: String = ManualPaymentRules.DEFAULT_REVIEW_MINUTES.toString(),
    val error: String? = null,
    val form: PayMethodFormState? = null,
    val deleteTarget: PayMethod? = null,
) {
    val enabled: Boolean get() = settings?.manualPaymentEnabled == true
    val activeMethods: Int get() = methods.count { it.active }

    val summary: String
        get() = when {
            settings == null && loading -> "Cargando…"
            enabled -> "Encendido · ${settings?.paymentReviewMinutes ?: ManualPaymentRules.DEFAULT_REVIEW_MINUTES} min para pagar · $activeMethods medio(s) activo(s)"
            else -> "Apagado"
        }

    /** Códigos de Caja y Bancos que se pueden elegir: al crear, los que aún no se ofrecen; al editar, el propio. */
    fun codeOptionsFor(editingId: Int?): List<CashbankMethodOption> {
        val used = methods.filter { it.id != editingId }.map { it.methodCode }.toSet()
        return cashbankOptions.filter { it.code !in used }
    }
}

/**
 * Reductor de Ajustes > Operación > Pago con captura. Sin Android: lo usa [ConfiguracionViewModel] y los tests.
 *
 * Reglas: solo `s.m` cambia algo; el interruptor y los minutos se guardan con un PUT PARCIAL (no pisan la tarifa);
 * el código del medio sale de los medios NO efectivo de Caja y Bancos; la imagen QR se sube justo después de
 * guardar el medio (necesita su id).
 */
class ManualPaymentSettingsPresenter(
    private val scope: CoroutineScope,
    private val deliveryRepository: DeliveryRepository,
    private val repository: ManualPaymentRepository,
    /** Medios de Caja y Bancos (de ahi sale el codigo y la cuenta donde entra la plata). */
    private val cashbankMethods: suspend () -> AppResult<List<CashPaymentMethod>>,
) {
    private val _state = MutableStateFlow(ManualPaymentSettingsState(settings = deliveryRepository.peekDeliverySettings()))
    val state: StateFlow<ManualPaymentSettingsState> = _state.asStateFlow()

    init {
        _state.update { it.copy(minutesText = (it.settings?.paymentReviewMinutes ?: ManualPaymentRules.DEFAULT_REVIEW_MINUTES).toString()) }
    }

    fun setPermissions(permissions: List<String>) {
        _state.update { it.copy(canEdit = canManageDeliverySettings(permissions)) }
    }

    fun load(forceRefresh: Boolean = false) {
        _state.update { it.copy(loading = true) }
        scope.launch {
            val s = (deliveryRepository.getDeliverySettings(forceRefresh) as? AppResult.Success)?.data
            val methods = (repository.payMethods() as? AppResult.Success)?.data
            val cash = (cashbankMethods() as? AppResult.Success)?.data
            _state.update { st ->
                st.copy(
                    loading = false,
                    settings = s ?: st.settings,
                    methods = methods ?: st.methods,
                    cashbankOptions = cash
                        ?.filter { ManualPaymentRules.isOfferableCashbankMethod(it.code, it.destinationType, it.active) }
                        ?.map { CashbankMethodOption(it.code, it.name.ifBlank { it.code }) }
                        ?: st.cashbankOptions,
                    minutesText = (s ?: st.settings)?.paymentReviewMinutes?.toString() ?: st.minutesText,
                )
            }
        }
    }

    fun toggle(value: Boolean) {
        val s = _state.value
        if (!s.canEdit || s.saving || value == s.enabled) return
        saveSettings(DeliverySettingsUpdate(manualPaymentEnabled = value))
    }

    fun setMinutesText(text: String) = _state.update { it.copy(minutesText = text.filter(Char::isDigit).take(3), error = null) }

    fun saveMinutes() {
        val s = _state.value
        if (!s.canEdit || s.saving) return
        val minutes = ManualPaymentRules.parseReviewMinutes(s.minutesText)
        if (minutes == null) {
            _state.update { it.copy(error = "El plazo para pagar debe estar entre ${ManualPaymentRules.MIN_REVIEW_MINUTES} y ${ManualPaymentRules.MAX_REVIEW_MINUTES} minutos.") }
            return
        }
        if (minutes == s.settings?.paymentReviewMinutes) return
        saveSettings(DeliverySettingsUpdate(paymentReviewMinutes = minutes))
    }

    private fun saveSettings(update: DeliverySettingsUpdate) {
        _state.update { it.copy(saving = true, error = null) }
        scope.launch {
            when (val r = deliveryRepository.updateDeliverySettings(update)) {
                is AppResult.Success -> _state.update {
                    it.copy(saving = false, settings = r.data, minutesText = r.data.paymentReviewMinutes.toString(), error = null)
                }
                is AppResult.Error -> _state.update {
                    it.copy(saving = false, error = r.message, minutesText = it.settings?.paymentReviewMinutes?.toString() ?: it.minutesText)
                }
                AppResult.Loading -> _state.update { it.copy(saving = false) }
            }
        }
    }

    // ---- formulario de un medio
    fun openCreate() {
        val s = _state.value
        if (!s.canEdit) return
        _state.update { it.copy(form = PayMethodFormState(methodCode = s.codeOptionsFor(null).firstOrNull()?.code.orEmpty())) }
    }

    fun openEdit(method: PayMethod) {
        if (!_state.value.canEdit) return
        _state.update {
            it.copy(
                form = PayMethodFormState(
                    editingId = method.id,
                    methodCode = method.methodCode,
                    label = method.label,
                    holder = method.holder,
                    accountNumber = method.accountNumber,
                    cci = method.cci,
                    instructions = method.instructions,
                    active = method.active,
                    existingQrUrl = method.qrUrl,
                ),
            )
        }
    }

    fun dismissForm() = _state.update { if (it.form?.saving == true) it else it.copy(form = null) }

    fun updateForm(change: (PayMethodFormState) -> PayMethodFormState) =
        _state.update { st -> st.copy(form = st.form?.let { change(it).copy(error = null) }) }

    fun pickQr(bytes: ByteArray, mimeType: String) = updateForm { it.copy(pendingQr = PendingImage(bytes, mimeType)) }

    fun clearPendingQr() = updateForm { it.copy(pendingQr = null) }

    fun saveForm() {
        val f = _state.value.form ?: return
        if (f.saving || !_state.value.canEdit) return
        ManualPaymentRules.validateMethod(f.input)?.let { msg ->
            _state.update { it.copy(form = f.copy(error = msg)) }
            return
        }
        _state.update { it.copy(form = f.copy(saving = true, error = null)) }
        scope.launch {
            val saved = if (f.editingId == null) repository.createPayMethod(f.input) else repository.updatePayMethod(f.editingId, f.input)
            when (saved) {
                is AppResult.Success -> {
                    // El medio ya quedó guardado; si la imagen falla se avisa pero no se pierde el medio.
                    var qrError: String? = null
                    f.pendingQr?.let { img ->
                        val up = repository.uploadQr(saved.data.id, img.bytes, img.mimeType)
                        if (up is AppResult.Error) qrError = "El medio se guardó, pero no se pudo subir el QR: ${up.message}"
                    }
                    val methods = (repository.payMethods() as? AppResult.Success)?.data
                    _state.update { st ->
                        st.copy(
                            methods = methods ?: (st.methods.filter { it.id != saved.data.id } + saved.data),
                            form = if (qrError == null) null else st.form?.copy(
                                editingId = saved.data.id, saving = false, pendingQr = null, error = qrError,
                            ),
                        )
                    }
                }
                is AppResult.Error -> _state.update { it.copy(form = it.form?.copy(saving = false, error = saved.message)) }
                AppResult.Loading -> Unit
            }
        }
    }

    // ---- borrar
    fun askDelete(method: PayMethod) {
        if (_state.value.canEdit) _state.update { it.copy(deleteTarget = method) }
    }

    fun dismissDelete() = _state.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        scope.launch {
            when (val r = repository.deletePayMethod(target.id)) {
                is AppResult.Success -> _state.update {
                    it.copy(saving = false, deleteTarget = null, methods = it.methods.filter { m -> m.id != target.id })
                }
                is AppResult.Error -> _state.update { it.copy(saving = false, deleteTarget = null, error = r.message) }
                AppResult.Loading -> _state.update { it.copy(saving = false) }
            }
        }
    }

    /** Activa o desactiva un medio sin abrir el formulario. */
    fun toggleActive(method: PayMethod, active: Boolean) {
        if (!_state.value.canEdit || _state.value.saving) return
        _state.update { it.copy(saving = true, error = null) }
        scope.launch {
            val input = PayMethodInput(
                method.methodCode, method.label, method.holder, method.accountNumber, method.cci, method.instructions, active,
            )
            when (val r = repository.updatePayMethod(method.id, input)) {
                is AppResult.Success -> _state.update {
                    it.copy(saving = false, methods = it.methods.map { m -> if (m.id == method.id) r.data else m })
                }
                is AppResult.Error -> _state.update { it.copy(saving = false, error = r.message) }
                AppResult.Loading -> _state.update { it.copy(saving = false) }
            }
        }
    }
}
