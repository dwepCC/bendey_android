package com.bendey.restaurant.feature.productos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.onboarding.OnboardingRefreshPolicy
import com.bendey.restaurant.core.domain.onboarding.OnboardingRepository
import com.bendey.restaurant.core.domain.onboarding.canSeeOnboarding
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.ui.components.BendeyAlert
import com.bendey.restaurant.core.ui.components.BendeyAlertAction
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyAlertSeverity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SampleMenuBannerState(
    val sampleDataLoaded: Boolean = false,
    val confirmOpen: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
)

/**
 * Aviso "carta de ejemplo" de Productos. Solo lo consulta el administrador de sesión completa; para
 * el resto (o si el backend no responde) simplemente no aparece: nunca estorba a la pantalla.
 */
@HiltViewModel
class SampleMenuBannerViewModel @Inject constructor(
    private val repository: OnboardingRepository,
    sessionStore: UserSessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SampleMenuBannerState())
    val state: StateFlow<SampleMenuBannerState> = _state.asStateFlow()

    private var canSee = false
    private var lastLoadedAtMs: Long? = null

    init {
        viewModelScope.launch {
            sessionStore.userSessionFlow
                .map { it.canSeeOnboarding() }
                .distinctUntilChanged()
                .collect { allowed ->
                    canSee = allowed
                    lastLoadedAtMs = null
                    if (allowed) refresh(force = true) else _state.value = SampleMenuBannerState()
                }
        }
    }

    fun refresh(force: Boolean = false) {
        if (!canSee) return
        if (!OnboardingRefreshPolicy.shouldRefresh(lastLoadedAtMs, System.currentTimeMillis(), force)) return
        viewModelScope.launch {
            val result = repository.getState()
            if (result is AppResult.Success) {
                lastLoadedAtMs = System.currentTimeMillis()
                _state.update { it.copy(sampleDataLoaded = result.data.sampleDataLoaded) }
            }
        }
    }

    fun askDelete() = _state.update { it.copy(confirmOpen = true, message = null) }

    fun dismissConfirm() = _state.update { it.copy(confirmOpen = false) }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    /** Borra los ejemplos y avisa al llamador para que recargue la lista. */
    fun confirmDelete(onDeleted: () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(confirmOpen = false, busy = true, message = null) }
        viewModelScope.launch {
            when (val result = repository.deleteSampleData()) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            busy = false,
                            sampleDataLoaded = false,
                            message = result.data.userMessage(),
                            messageIsError = false,
                        )
                    }
                    onDeleted()
                    refresh(force = true)
                }
                is AppResult.Error -> _state.update {
                    it.copy(busy = false, message = result.message, messageIsError = true)
                }
                AppResult.Loading -> Unit
            }
        }
    }
}

/**
 * Mientras `sample_data_loaded` sea true y existan productos: "Estás usando una carta de ejemplo".
 * [onChanged] se llama tras borrar, para que la pantalla recargue su lista.
 */
@Composable
fun SampleMenuBanner(
    hasProducts: Boolean,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SampleMenuBannerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val showBanner = state.sampleDataLoaded && hasProducts
    if (!showBanner && state.message == null) return

    Column(
        modifier = modifier.padding(horizontal = BendeySpacing.md, vertical = BendeySpacing.xxs),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        state.message?.let {
            BendeyAlert(
                message = it,
                severity = if (state.messageIsError) BendeyAlertSeverity.Danger else BendeyAlertSeverity.Success,
                onDismiss = viewModel::dismissMessage,
            )
        }
        if (showBanner) {
            BendeyAlert(
                message = "Estás usando una carta de ejemplo. Edítala o bórrala cuando quieras.",
                severity = BendeyAlertSeverity.Info,
                primaryAction = if (state.busy) null else BendeyAlertAction("Borrar ejemplos", viewModel::askDelete),
            )
        }
    }

    if (state.confirmOpen) {
        BendeyAlertDialog(
            onDismissRequest = viewModel::dismissConfirm,
            title = "¿Borrar los ejemplos?",
            message = "Se borrarán los productos de ejemplo. Los que ya tienen ventas no se borran: " +
                "se desactivan. Esta acción no se puede deshacer.",
            confirmText = "Borrar ejemplos",
            onConfirm = { viewModel.confirmDelete(onChanged) },
            onDismiss = viewModel::dismissConfirm,
            destructive = true,
        )
    }
}
