package com.bendey.restaurant.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.auth.InitialAdminPinHolder
import com.bendey.restaurant.core.domain.catalog.BulkImportProgress
import com.bendey.restaurant.core.domain.catalog.BulkImportValidationResult
import com.bendey.restaurant.core.domain.catalog.PreparationAreasRepository
import com.bendey.restaurant.core.domain.catalog.ProductImportRepository
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.onboarding.OnboardingPreferencesUpdate
import com.bendey.restaurant.core.domain.onboarding.OnboardingRepository
import com.bendey.restaurant.core.domain.onboarding.OnboardingState
import com.bendey.restaurant.core.domain.onboarding.wizard.BusinessSubtype
import com.bendey.restaurant.core.domain.onboarding.wizard.PasteMenu
import com.bendey.restaurant.core.domain.onboarding.wizard.PastePreviewRow
import com.bendey.restaurant.core.domain.onboarding.wizard.ServiceMode
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardCoach
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardCopy
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardLocalState
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardPreferences
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardStep
import com.bendey.restaurant.core.domain.onboarding.wizard.buildPastePreview
import com.bendey.restaurant.core.domain.onboarding.wizard.edited
import com.bendey.restaurant.core.domain.onboarding.wizard.hasSales
import com.bendey.restaurant.core.domain.onboarding.wizard.importResultMessage
import com.bendey.restaurant.core.domain.onboarding.wizard.menuDone
import com.bendey.restaurant.core.domain.onboarding.wizard.parsePasteMenu
import com.bendey.restaurant.core.domain.onboarding.wizard.parsePrice
import com.bendey.restaurant.core.domain.onboarding.wizard.pasteImportRows
import com.bendey.restaurant.core.domain.products.ProductListQuery
import com.bendey.restaurant.core.domain.products.ProductsRepository
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Subpantallas de W2 (la carta). */
enum class MenuMode { CHOOSE, PASTE, PREVIEW, MANUAL }

data class WizardUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val server: OnboardingState? = null,
    val local: WizardLocalState = WizardLocalState(),
    /** Solo se muestra: no hay endpoint de empresa usado por la app para editarlo. */
    val businessName: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    /** PIN inicial del administrador (solo en memoria, se muestra UNA vez). */
    val initialPin: String? = null,
    val menuMode: MenuMode = MenuMode.CHOOSE,
    // Pegar lista
    val pasteText: String = "",
    val pasteLimitReached: Boolean = false,
    val preview: List<PastePreviewRow> = emptyList(),
    // Excel
    val excelOpen: Boolean = false,
    val excelValidation: BulkImportValidationResult? = null,
    val excelProgress: BulkImportProgress? = null,
    val excelLoading: Boolean = false,
    // A mano
    val manualName: String = "",
    val manualPrice: String = "",
    val manualCategory: String = "",
    val categories: List<String> = emptyList(),
    val manualCreated: Int = 0,
) {
    val menuDone: Boolean get() = server?.menuDone() == true
    val step: WizardStep get() = local.step
    val selectedSubtype: BusinessSubtype? get() = BusinessSubtype.fromApi(server?.businessSubtype)
    val importableCount: Int get() = preview.count { it.importable }
}

@HiltViewModel
class WizardViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val preferences: WizardPreferences,
    private val productsRepository: ProductsRepository,
    private val importRepository: ProductImportRepository,
    private val areasRepository: PreparationAreasRepository,
    private val sessionStore: UserSessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(WizardUiState(initialPin = InitialAdminPinHolder.peek()))
    val state: StateFlow<WizardUiState> = _state.asStateFlow()

    /** Nombres de platos ya en la carta (para no actualizar sin querer lo que ya existe). */
    private var existingNames: Set<String> = emptySet()
    private var areaNames: List<String> = emptyList()

    init {
        viewModelScope.launch {
            preferences.state.collect { local -> _state.update { it.copy(local = local) } }
        }
        viewModelScope.launch {
            val name = sessionStore.tenantFlow.first()?.name.orEmpty()
            _state.update { it.copy(businessName = name) }
        }
        refresh()
    }

    // ── Estado del servidor ──────────────────────────────────────────────────────────────────────

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = it.server == null, loadError = null) }
            when (val result = onboardingRepository.getState()) {
                is AppResult.Success -> _state.update { it.copy(loading = false, server = result.data) }
                is AppResult.Error -> _state.update { it.copy(loading = false, loadError = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    private suspend fun refreshQuiet() {
        (onboardingRepository.getState() as? AppResult.Success)?.let { r ->
            _state.update { it.copy(server = r.data) }
        }
    }

    // ── Navegación entre pasos ───────────────────────────────────────────────────────────────────

    fun goTo(step: WizardStep) {
        viewModelScope.launch { preferences.setStep(step) }
        _state.update { it.copy(error = null, notice = null, menuMode = MenuMode.CHOOSE) }
    }

    fun start() = goTo(WizardStep.RESTAURANT)

    /** Salir ("Explorar primero", "Más tarde" o atrás): no vuelve a abrirse solo; se retoma desde el checklist. */
    fun close(onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.setClosed(true)
            onDone()
        }
    }

    fun dismissPin() {
        InitialAdminPinHolder.clear()
        _state.update { it.copy(initialPin = null) }
    }

    // ── W1 ───────────────────────────────────────────────────────────────────────────────────────

    fun selectSubtype(subtype: BusinessSubtype) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            when (val result = onboardingRepository.updatePreferences(OnboardingPreferencesUpdate(businessSubtype = subtype.apiKey))) {
                is AppResult.Success -> _state.update { it.copy(busy = false, server = result.data) }
                is AppResult.Error -> _state.update { it.copy(busy = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun toggleServiceMode(mode: ServiceMode) {
        val current = _state.value.local.serviceModes
        val next = if (mode in current) current - mode else current + mode
        // Siempre queda al menos un modo: sin ninguno, la primera venta no sabría por dónde empezar.
        if (next.isEmpty()) return
        viewModelScope.launch { preferences.setServiceModes(next) }
    }

    // ── W2: carta de ejemplo ─────────────────────────────────────────────────────────────────────

    fun useSampleMenu() {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            when (val result = onboardingRepository.loadSampleMenu()) {
                is AppResult.Success -> {
                    refreshQuiet()
                    val n = result.data.created
                    _state.update {
                        it.copy(
                            busy = false,
                            notice = (if (n == 1) "Cargamos 1 plato de ejemplo." else "Cargamos $n platos de ejemplo.") +
                                " Edítalos o bórralos cuando quieras.",
                        )
                    }
                }
                is AppResult.Error -> _state.update { it.copy(busy = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    // ── W2: pegar mi lista ───────────────────────────────────────────────────────────────────────

    fun openPaste() = _state.update { it.copy(menuMode = MenuMode.PASTE, error = null, notice = null) }

    fun backToChoose() = _state.update { it.copy(menuMode = MenuMode.CHOOSE, error = null) }

    fun onPasteTextChange(text: String) = _state.update { it.copy(pasteText = text, error = null) }

    /** Lee el texto y arma la vista previa. NO crea nada. */
    fun buildPreview() {
        val text = _state.value.pasteText
        if (text.isBlank()) {
            _state.update { it.copy(error = "Pega primero tu lista de platos.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            loadExistingNames()
            val parsed = parsePasteMenu(text)
            _state.update {
                it.copy(
                    busy = false,
                    preview = buildPastePreview(parsed, existingNames),
                    pasteLimitReached = parsed.truncated,
                    menuMode = if (parsed.rows.isEmpty()) MenuMode.PASTE else MenuMode.PREVIEW,
                    error = if (parsed.rows.isEmpty()) "No encontramos platos con precio. Revisa el formato del ejemplo." else null,
                )
            }
        }
    }

    fun editPreviewRow(id: Int, name: String? = null, priceText: String? = null, category: String? = null) {
        _state.update { s ->
            s.copy(
                preview = s.preview.map { row ->
                    if (row.id != id) row else row.edited(
                        name = name ?: row.name,
                        priceText = priceText ?: row.priceText,
                        category = category ?: row.category,
                        existingNames = existingNames,
                    )
                },
            )
        }
    }

    fun togglePreviewRow(id: Int, selected: Boolean) {
        _state.update { s ->
            s.copy(preview = s.preview.map { if (it.id == id && it.error == null) it.copy(selected = selected) else it })
        }
    }

    fun removePreviewRow(id: Int) {
        _state.update { s -> s.copy(preview = s.preview.filterNot { it.id == id }) }
    }

    /** "Crear mi carta": solo filas sin error y marcadas (las dudosas, solo si las marcó el usuario). */
    fun createFromPreview() {
        val s = _state.value
        val rows = pasteImportRows(s.preview)
        if (rows.isEmpty() || s.busy) {
            if (rows.isEmpty()) _state.update { it.copy(error = "No hay platos marcados para crear.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            when (val result = importRepository.importRows(rows, emptyMap())) {
                is AppResult.Success -> {
                    val p = result.data
                    refreshQuiet()
                    existingNames = existingNames + rows.map { it.name }
                    _state.update {
                        it.copy(
                            busy = false,
                            menuMode = MenuMode.CHOOSE,
                            pasteText = "",
                            preview = emptyList(),
                            notice = importResultMessage(p.created, p.failed.size),
                            error = p.failed.takeIf { f -> f.isNotEmpty() }
                                ?.joinToString("\n") { f -> "Fila ${f.row}: ${f.message}" },
                        )
                    }
                }
                is AppResult.Error -> _state.update { it.copy(busy = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    // ── W2: Excel ────────────────────────────────────────────────────────────────────────────────

    fun openExcel() {
        viewModelScope.launch {
            loadAreaNames()
            _state.update {
                it.copy(excelOpen = true, excelValidation = null, excelProgress = null, error = null, notice = null)
            }
        }
    }

    fun closeExcel() {
        _state.update { it.copy(excelOpen = false, excelValidation = null, excelProgress = null, excelLoading = false) }
        viewModelScope.launch { refreshQuiet() }
    }

    fun simpleTemplateBytes(): ByteArray = importRepository.generateSimpleTemplateBytes()
    fun advancedTemplateBytes(): ByteArray = importRepository.generateTemplateBytes()

    fun validateExcel(bytes: ByteArray) {
        viewModelScope.launch {
            val validation = importRepository.validateExcel(bytes, areaNames)
            _state.update { it.copy(excelValidation = validation, excelProgress = null, error = null) }
        }
    }

    /** Importa SOLO las filas válidas (el lector ya dejó fuera las que tienen error). */
    fun importExcel() {
        val validation = _state.value.excelValidation ?: return
        if (validation.rows.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(excelLoading = true, error = null) }
            when (val result = importRepository.importRows(validation.rows, emptyMap())) {
                is AppResult.Success -> {
                    refreshQuiet()
                    _state.update { it.copy(excelLoading = false, excelProgress = result.data) }
                }
                is AppResult.Error -> _state.update { it.copy(excelLoading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    // ── W2: crear a mano ─────────────────────────────────────────────────────────────────────────

    fun openManual() {
        viewModelScope.launch {
            _state.update { it.copy(menuMode = MenuMode.MANUAL, error = null, notice = null, manualCreated = 0) }
            if (_state.value.categories.isEmpty()) {
                (productsRepository.listCategories() as? AppResult.Success)?.let { r ->
                    _state.update { it.copy(categories = r.data.map { c -> c.name }) }
                }
            }
        }
    }

    fun onManualChange(name: String? = null, price: String? = null, category: String? = null) {
        _state.update {
            it.copy(
                manualName = name ?: it.manualName,
                manualPrice = price ?: it.manualPrice,
                manualCategory = category ?: it.manualCategory,
                error = null,
            )
        }
    }

    fun saveManual() {
        val s = _state.value
        val name = s.manualName.trim()
        val price = parsePrice(s.manualPrice)
        when {
            s.busy -> return
            name.isEmpty() -> return _state.update { it.copy(error = PasteMenu.MSG_MISSING_NAME) }
            price == null -> return _state.update { it.copy(error = PasteMenu.MSG_MISSING_PRICE) }
            price <= 0 -> return _state.update { it.copy(error = "El precio debe ser mayor a 0.") }
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            loadExistingNames()
            if (name.lowercase() in existingNames.map { it.lowercase() }) {
                _state.update { it.copy(busy = false, error = "Ya tienes un plato llamado \"$name\". Usa otro nombre.") }
                return@launch
            }
            val category = s.manualCategory.trim().ifEmpty { PasteMenu.DEFAULT_CATEGORY }
            val row = com.bendey.restaurant.core.domain.catalog.BulkImportRow(
                rowNumber = 1,
                name = name,
                code = "",
                description = "",
                salePrice = price!!,
                unit = "NIU",
                categoryName = category,
                preparationArea = "",
                igvAffectationType = "10",
                priceIncludesIgv = true,
                manageStock = false,
                initialStock = 0.0,
            )
            when (val result = importRepository.importRows(listOf(row), emptyMap())) {
                is AppResult.Success -> {
                    val failed = result.data.failed.firstOrNull()
                    if (failed != null) {
                        _state.update { it.copy(busy = false, error = failed.message) }
                    } else {
                        existingNames = existingNames + name
                        refreshQuiet()
                        _state.update {
                            it.copy(
                                busy = false,
                                manualName = "",
                                manualPrice = "",
                                manualCreated = it.manualCreated + 1,
                                categories = (it.categories + category).distinct(),
                                notice = "Creamos \"$name\". Puedes agregar otro.",
                            )
                        }
                    }
                }
                is AppResult.Error -> _state.update { it.copy(busy = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    // ── W3 ───────────────────────────────────────────────────────────────────────────────────────

    /** Inicia la guía de la primera venta sobre las pantallas reales. */
    fun startFirstSaleGuide(onStarted: () -> Unit) {
        viewModelScope.launch {
            preferences.setStep(WizardStep.FIRST_SALE)
            preferences.setClosed(true)
            WizardCoach.start(_state.value.local.usesTables)
            onStarted()
        }
    }

    val firstSaleDone: Boolean get() = _state.value.server?.hasSales() == true

    // ── Datos auxiliares ─────────────────────────────────────────────────────────────────────────

    private suspend fun loadAreaNames() {
        if (areaNames.isNotEmpty()) return
        (areasRepository.listPreparationAreas(activeOnly = true) as? AppResult.Success)?.let { r ->
            areaNames = r.data.map { it.name }
        }
    }

    /** Todos los nombres del catálogo (activos), paginando con tope. Si falla, no se bloquea el flujo. */
    private suspend fun loadExistingNames() {
        val names = mutableSetOf<String>()
        var page = 1
        while (page <= MAX_CATALOG_PAGES) {
            val result = productsRepository.listProducts(ProductListQuery(page = page, perPage = PAGE_SIZE))
            if (result !is AppResult.Success) break
            val (items, total) = result.data
            names += items.map { it.name }
            if (items.isEmpty() || page * PAGE_SIZE >= total) break
            page++
        }
        existingNames = names
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_CATALOG_PAGES = 20
    }
}
