package com.bendey.restaurant.core.data.auth

import com.bendey.restaurant.core.domain.auth.AuthRepository
import com.bendey.restaurant.core.domain.auth.RegisterAndSignIn
import com.bendey.restaurant.core.domain.auth.RegisterAndSignInOutcome
import com.bendey.restaurant.core.domain.auth.RestaurantRegistrationInput
import com.bendey.restaurant.core.domain.auth.TenantRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Tras crear el restaurante, el login automático falló: ir al login normal con el correo prellenado. */
data class RegistrationLoginHint(val email: String, val restaurantName: String, val message: String)

data class RegistrationState(
    /** Creando (y entrando): mientras sea true la app NO debe cambiar de pantalla raíz. */
    val inFlight: Boolean = false,
    /** El registro falló (no se creó nada): se muestra en el formulario y se puede reintentar. */
    val error: String? = null,
)

/**
 * Corre "crear restaurante + entrar" FUERA del ciclo de vida de la pantalla. Al registrar, la app
 * vincula el restaurante y eso cambia la pantalla raíz: si la corrutina viviera en el ViewModel del
 * formulario, se cancelaría a mitad (después de crear, antes de entrar). El registro corre UNA vez
 * por llamada y nunca se reintenta solo.
 */
@Singleton
class RegistrationCoordinator @Inject constructor(
    tenantRepository: TenantRepository,
    authRepository: AuthRepository,
) {
    private val useCase = RegisterAndSignIn(tenantRepository, authRepository)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(RegistrationState())
    val state: StateFlow<RegistrationState> = _state.asStateFlow()

    private val _loginHint = MutableStateFlow<RegistrationLoginHint?>(null)
    val loginHint: StateFlow<RegistrationLoginHint?> = _loginHint.asStateFlow()

    fun start(input: RestaurantRegistrationInput) {
        if (_state.value.inFlight) return
        _state.value = RegistrationState(inFlight = true)
        _loginHint.value = null
        scope.launch {
            when (val outcome = useCase(input)) {
                is RegisterAndSignInOutcome.SignedIn -> _state.value = RegistrationState()
                is RegisterAndSignInOutcome.RegistrationFailed ->
                    _state.value = RegistrationState(error = outcome.message)
                is RegisterAndSignInOutcome.CreatedButLoginFailed -> {
                    _loginHint.value = RegistrationLoginHint(outcome.email, outcome.restaurantName, outcome.message)
                    _state.value = RegistrationState()
                }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    /** El login normal ya se hizo (o el usuario descartó el aviso). */
    fun consumeLoginHint() {
        _loginHint.value = null
    }
}
