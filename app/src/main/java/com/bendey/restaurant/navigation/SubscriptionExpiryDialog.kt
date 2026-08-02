package com.bendey.restaurant.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.subscription.BillingContext
import com.bendey.restaurant.core.domain.subscription.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Cada cuánto se revisa el estado de la suscripción con la app abierta. */
private const val SUBSCRIPTION_REFRESH_MS = 30 * 60 * 1000L

/** Estados donde el aviso ya no es recordatorio sino bloqueo: se muestra siempre. */
private val ALWAYS_SHOW_TIERS = setOf("suspended", "blocked")

@HiltViewModel
class SubscriptionExpiryViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
) : ViewModel() {

    private val _context = MutableStateFlow<BillingContext?>(null)
    val context: StateFlow<BillingContext?> = _context.asStateFlow()

    init {
        viewModelScope.launch {
            // Una tablet de mesero queda abierta todo el turno: sin refresco periódico no se
            // enteraría de que el plan venció mientras estaba encendida.
            while (isActive) {
                when (val result = repository.getHub()) {
                    is AppResult.Success -> _context.value = result.data.billingContext
                    else -> Unit
                }
                delay(SUBSCRIPTION_REFRESH_MS)
            }
        }
    }
}

/**
 * Aviso de vencimiento del plan.
 *
 * Cuándo aparecer lo decide el backend (`showExpiryModal`), igual que en Tauri, para que ambas
 * apps avisen en el mismo momento. Es cerrable y no vuelve a salir ese día, salvo cuando el plan
 * ya está suspendido o bloqueado: ahí el sistema no deja operar y el aviso explica por qué.
 */
@Composable
fun SubscriptionExpiryDialog(
    onGoToSubscription: () -> Unit,
    viewModel: SubscriptionExpiryViewModel = hiltViewModel(),
) {
    val context by viewModel.context.collectAsStateWithLifecycleCompat()
    var dismissedOn by remember { mutableStateOf<LocalDate?>(null) }

    val ctx = context ?: return
    if (!ctx.showExpiryModal) return

    val alwaysShow = ctx.urgencyTier in ALWAYS_SHOW_TIERS
    val today = LocalDate.now()
    if (!alwaysShow && dismissedOn == today) return

    AlertDialog(
        onDismissRequest = { dismissedOn = today },
        title = { Text(ctx.expiryModalTitle.ifBlank { "Tu plan está por vencer" }) },
        text = { Text(ctx.expiryModalMessage) },
        confirmButton = {
            TextButton(onClick = {
                dismissedOn = today
                onGoToSubscription()
            }) { Text("Ver mi plan") }
        },
        dismissButton = {
            TextButton(onClick = { dismissedOn = today }) { Text("Ahora no") }
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
