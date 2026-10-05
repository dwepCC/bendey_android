package com.bendey.restaurant.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.data.subscription.PlanNoticePreferencesStore
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.domain.subscription.BillingContext
import com.bendey.restaurant.core.domain.subscription.PlanNoticePolicy
import com.bendey.restaurant.core.realtime.connection.AppForeground
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.bendey.restaurant.core.domain.subscription.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SubscriptionExpiryViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
    private val preferences: PlanNoticePreferencesStore,
    sessionStore: UserSessionStore,
    appForeground: AppForeground,
) : ViewModel() {

    private val _context = MutableStateFlow<BillingContext?>(null)
    val context: StateFlow<BillingContext?> = _context.asStateFlow()

    /** Dia de Lima en que se cerro el aviso (persistido por tenant); null si no se cerro. */
    private val _dismissedDay = MutableStateFlow<String?>(null)
    val dismissedDay: StateFlow<String?> = _dismissedDay.asStateFlow()

    /** Cerrar siempre cierra, incluso en suspendido/bloqueado (ahi no se persiste). */
    private val _closedThisSession = MutableStateFlow(false)
    val closedThisSession: StateFlow<Boolean> = _closedThisSession.asStateFlow()

    /** Dia de Lima actual; se recalcula al volver a primer plano (turno que cruza medianoche). */
    private val _today = MutableStateFlow(PlanNoticePolicy.limaDay(System.currentTimeMillis()))
    val today: StateFlow<String> = _today.asStateFlow()

    /** Solo el administrador (s.m) ve el aviso, igual que Tauri. */
    val isAdmin: StateFlow<Boolean> = sessionStore.userSessionFlow
        .map { RestaurantPermissions.isRestaurantAdmin(it?.restaurantPermissions.orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch { _dismissedDay.value = preferences.dismissedDay() }
        viewModelScope.launch {
            // Cada media hora y cada vez que la app vuelve a primer plano: una tablet de mesero queda
            // abierta todo el turno y sin esto no se enteraria de que el plan vencio.
            appForeground.enPantalla.collectLatest { enPantalla ->
                if (!enPantalla) return@collectLatest
                _today.value = PlanNoticePolicy.limaDay(System.currentTimeMillis())
                _dismissedDay.value = preferences.dismissedDay()
                while (true) {
                    when (val result = repository.getHub()) {
                        is AppResult.Success -> _context.value = result.data.billingContext
                        else -> Unit
                    }
                    delay(PlanNoticePolicy.REFRESH_MS)
                }
            }
        }
    }

    fun close(persist: Boolean) {
        _closedThisSession.value = true
        if (persist) {
            val day = PlanNoticePolicy.limaDay(System.currentTimeMillis())
            _dismissedDay.value = day
            viewModelScope.launch { preferences.saveDismissedDay(day) }
        }
    }
}

/**
 * Aviso de vencimiento del plan (R10.6). Cadencia identica a Tauri (`SubscriptionExpiryModal.tsx`, ver
 * [PlanNoticePolicy]): lo decide el backend (`showExpiryModal`); solo el administrador lo ve; cerrar siempre
 * cierra; el cierre se recuerda por tenant + dia de Lima (no por la fecha del dispositivo), salvo en
 * suspendido/bloqueado, donde reaparece al reabrir.
 */
@Composable
fun SubscriptionExpiryDialog(
    onGoToSubscription: () -> Unit,
    viewModel: SubscriptionExpiryViewModel = hiltViewModel(),
) {
    val context by viewModel.context.collectAsStateWithLifecycleCompat()
    val dismissedDay by viewModel.dismissedDay.collectAsStateWithLifecycleCompat()
    val closed by viewModel.closedThisSession.collectAsStateWithLifecycleCompat()
    val today by viewModel.today.collectAsStateWithLifecycleCompat()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycleCompat()

    val ctx = context ?: return
    if (!PlanNoticePolicy.shouldShow(
            showExpiryModal = ctx.showExpiryModal,
            isAdmin = isAdmin,
            urgencyTier = ctx.urgencyTier,
            closedThisSession = closed,
            persistedDismissDay = dismissedDay,
            today = today,
        )
    ) return

    val cerrar = { viewModel.close(persist = PlanNoticePolicy.persistsDismissal(ctx.urgencyTier)) }

    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text(ctx.expiryModalTitle.ifBlank { "Tu plan está por vencer" }) },
        text = { Text(ctx.expiryModalMessage) },
        confirmButton = {
            TextButton(onClick = {
                cerrar()
                onGoToSubscription()
            }) { Text("Ver mi plan") }
        },
        dismissButton = {
            TextButton(onClick = cerrar) { Text("Ahora no") }
        },
    )
}

/**
 * Pequeño puente para no obligar a importar lifecycle-compose en este archivo: el estado ya es
 * un StateFlow simple y no necesita el manejo de ciclo de vida completo.
 */
@Composable
private fun <T> StateFlow<T>.collectAsStateWithLifecycleCompat(): androidx.compose.runtime.State<T> {
    val flow = this
    val state = remember(flow) { mutableStateOf(flow.value) }
    LaunchedEffect(flow) { flow.collect { state.value = it } }
    return state
}
