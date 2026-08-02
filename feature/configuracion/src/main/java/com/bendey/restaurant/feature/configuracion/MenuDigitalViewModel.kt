package com.bendey.restaurant.feature.configuracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.digitalmenu.BENDEY_OFFICIAL_COLOR_HEX
import com.bendey.restaurant.core.domain.digitalmenu.DigitalMenuRepository
import com.bendey.restaurant.core.domain.digitalmenu.MenuConfig
import com.bendey.restaurant.core.domain.digitalmenu.MenuStyleVariant
import com.bendey.restaurant.core.domain.digitalmenu.MenuThemeMode
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.session.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val HEX_COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6}$")

data class MenuDigitalUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val regenerating: Boolean = false,
    val menuEnabled: Boolean = false,
    val menuUrl: String = "",
    /** PNG en base64 del QR del menú general; null mientras no se haya cargado. */
    val menuQrBase64: String? = null,
    val welcomeTitle: String = "",
    val welcomeDescription: String = "",
    val showPrices: Boolean = true,
    val whatsapp: String = "",
    val publicTakeawayEnabled: Boolean = false,
    val publicDeliveryEnabled: Boolean = false,
    val themeMode: MenuThemeMode = MenuThemeMode.BENDEY_DEFAULT,
    val primaryColorHex: String = BENDEY_OFFICIAL_COLOR_HEX,
    val backgroundImageBase64: String = "",
    val styleVariant: MenuStyleVariant = MenuStyleVariant.GLASS,
    val fontFamily: String = "",
    val cardVariant: String = "",
    val cornerStyle: String = "",
    val showStockBadges: Boolean = false,
    val requireOrderApproval: Boolean = false,
    val heroImageBase64: String = "",
    val previewOpen: Boolean = false,
    val canManage: Boolean = false,
    val error: String? = null,
    val snackMessage: String? = null,
) {
    val isCustomTheme: Boolean get() = themeMode == MenuThemeMode.CUSTOM
    val isGlassStyle: Boolean get() = styleVariant == MenuStyleVariant.GLASS
    val previewColorHex: String get() = if (isCustomTheme) primaryColorHex else BENDEY_OFFICIAL_COLOR_HEX
}

@HiltViewModel
class MenuDigitalViewModel @Inject constructor(
    private val repository: DigitalMenuRepository,
    private val sessionStore: UserSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuDigitalUiState())
    val uiState: StateFlow<MenuDigitalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.userSessionFlow.collect { user ->
                val perms = user?.restaurantPermissions.orEmpty()
                _uiState.update {
                    it.copy(canManage = RestaurantPermissions.canManageRestaurantSettings(perms))
                }
            }
        }
        load()
    }

    fun clearSnack() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            when (val result = repository.getSettings()) {
                is AppResult.Success -> {
                    val settings = result.data
                    val config = settings.menuConfig
                    _uiState.update {
                        it.copy(
                            loading = false,
                            menuEnabled = settings.menuEnabled,
                            menuUrl = settings.menuUrl,
                            // El QR se pide aparte: es una imagen que genera el
                            // servidor y no tiene por qué viajar con los ajustes.
                            menuQrBase64 = null,
                            welcomeTitle = config.welcomeTitle,
                            welcomeDescription = config.welcomeDescription,
                            showPrices = config.showPrices,
                            whatsapp = config.whatsapp,
                            publicTakeawayEnabled = config.publicTakeawayEnabled,
                            publicDeliveryEnabled = config.publicDeliveryEnabled,
                            themeMode = config.themeMode,
                            primaryColorHex = config.primaryColorHex,
                            backgroundImageBase64 = config.backgroundImageBase64,
                            styleVariant = config.styleVariant,
                            fontFamily = config.fontFamily,
                            cardVariant = config.cardVariant,
                            cornerStyle = config.cornerStyle,
                            showStockBadges = config.showStockBadges,
                            requireOrderApproval = config.requireOrderApproval,
                            heroImageBase64 = config.heroImageBase64,
                        )
                    }
                    loadMenuQr()
                }
                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun setMenuEnabled(enabled: Boolean) {
        _uiState.update { it.copy(menuEnabled = enabled) }
    }

    fun setWelcomeTitle(value: String) {
        _uiState.update { it.copy(welcomeTitle = value) }
    }

    fun setWelcomeDescription(value: String) {
        _uiState.update { it.copy(welcomeDescription = value) }
    }

    fun setShowPrices(value: Boolean) {
        _uiState.update { it.copy(showPrices = value) }
    }

    fun setWhatsapp(value: String) {
        _uiState.update { it.copy(whatsapp = value) }
    }

    fun setPublicTakeawayEnabled(value: Boolean) {
        _uiState.update { it.copy(publicTakeawayEnabled = value) }
    }

    fun setPublicDeliveryEnabled(value: Boolean) {
        _uiState.update { it.copy(publicDeliveryEnabled = value) }
    }

    fun setThemeMode(mode: MenuThemeMode) {
        _uiState.update {
            it.copy(
                themeMode = mode,
                primaryColorHex = it.primaryColorHex.ifBlank { BENDEY_OFFICIAL_COLOR_HEX },
            )
        }
    }

    fun setPrimaryColorHex(hex: String) {
        _uiState.update { it.copy(primaryColorHex = hex) }
    }

    fun setBackgroundImageBase64(dataUrl: String) {
        _uiState.update { it.copy(backgroundImageBase64 = dataUrl) }
    }

    fun clearBackgroundImage() {
        _uiState.update { it.copy(backgroundImageBase64 = "") }
    }

    fun setStyleVariant(variant: MenuStyleVariant) {
        _uiState.update { it.copy(styleVariant = variant) }
    }

    fun setFontFamily(value: String) {
        _uiState.update { it.copy(fontFamily = value) }
    }

    fun setCardVariant(value: String) {
        _uiState.update { it.copy(cardVariant = value) }
    }

    fun setCornerStyle(value: String) {
        _uiState.update { it.copy(cornerStyle = value) }
    }

    fun setShowStockBadges(value: Boolean) {
        _uiState.update { it.copy(showStockBadges = value) }
    }

    fun setRequireOrderApproval(value: Boolean) {
        _uiState.update { it.copy(requireOrderApproval = value) }
    }

    fun setHeroImage(dataUrl: String) {
        _uiState.update { it.copy(heroImageBase64 = dataUrl) }
    }

    fun clearHeroImage() {
        _uiState.update { it.copy(heroImageBase64 = "") }
    }

    fun openPreview() {
        _uiState.update { it.copy(previewOpen = true) }
    }

    fun closePreview() {
        _uiState.update { it.copy(previewOpen = false) }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canManage) {
            _uiState.update { it.copy(error = "No tiene permiso para modificar el menú digital") }
            return
        }
        if (state.isCustomTheme && !HEX_COLOR_REGEX.matches(state.primaryColorHex)) {
            _uiState.update { it.copy(error = "El color principal debe tener el formato #RRGGBB") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            val config = MenuConfig(
                welcomeTitle = state.welcomeTitle,
                welcomeDescription = state.welcomeDescription,
                showPrices = state.showPrices,
                whatsapp = state.whatsapp,
                publicTakeawayEnabled = state.publicTakeawayEnabled,
                publicDeliveryEnabled = state.publicDeliveryEnabled,
                themeMode = state.themeMode,
                primaryColorHex = state.primaryColorHex,
                backgroundImageBase64 = state.backgroundImageBase64,
                styleVariant = state.styleVariant,
                fontFamily = state.fontFamily,
                cardVariant = state.cardVariant,
                cornerStyle = state.cornerStyle,
                showStockBadges = state.showStockBadges,
                requireOrderApproval = state.requireOrderApproval,
                heroImageBase64 = state.heroImageBase64,
            )
            when (val result = repository.updateSettings(state.menuEnabled, config)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        saving = false,
                        menuUrl = result.data.menuUrl,
                        snackMessage = "Menú digital guardado",
                    )
                }
                is AppResult.Error -> _uiState.update { it.copy(saving = false, error = result.message) }
                AppResult.Loading -> Unit
            }
        }
    }

    /**
     * Carga la imagen del QR del menú general. Un fallo aquí no es bloqueante: la
     * pantalla sigue mostrando el enlace, solo se queda sin el QR descargable.
     */
    private fun loadMenuQr() {
        viewModelScope.launch {
            when (val result = repository.getMenuQr()) {
                is AppResult.Success -> _uiState.update { it.copy(menuQrBase64 = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(menuQrBase64 = null) }
                AppResult.Loading -> Unit
            }
        }
    }

    fun regenerateToken() {
        if (!_uiState.value.canManage) {
            _uiState.update { it.copy(error = "No tiene permiso para modificar el menú digital") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(regenerating = true, error = null) }
            when (val result = repository.regenerateMenuToken()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        regenerating = false,
                        menuUrl = result.data.second,
                        snackMessage = "Enlace regenerado",
                    )
                }
                is AppResult.Error -> _uiState.update { it.copy(regenerating = false, error = result.message) }
                AppResult.Loading -> Unit
            }
            // El token cambió: el QR anterior ya no lleva a ningún lado.
            loadMenuQr()
        }
    }
}
