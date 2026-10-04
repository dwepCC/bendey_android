package com.bendey.restaurant.core.domain.auth

import com.bendey.restaurant.core.domain.onboarding.wizard.WizardCopy
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions

/** Resultado de crear el restaurante y entrar directo (DEC-12: sin pasar por éxito → inicio → login). */
sealed interface RegisterAndSignInOutcome {
    /** Creado y con sesión iniciada: se puede entrar a la app (al wizard). */
    data class SignedIn(val restaurantName: String, val initialRoute: String) : RegisterAndSignInOutcome

    /** No se creó nada (validación, RUC repetido, red): el formulario sigue y se puede reintentar. */
    data class RegistrationFailed(val message: String) : RegisterAndSignInOutcome

    /**
     * El tenant YA existe pero el inicio de sesión automático falló. Se va al login normal con el
     * correo prellenado; NUNCA se reintenta el registro (el RUC ya está tomado).
     */
    data class CreatedButLoginFailed(
        val restaurantName: String,
        val email: String,
        val message: String,
    ) : RegisterAndSignInOutcome
}

/**
 * Crea el restaurante y encadena `login()` con las MISMAS credenciales. El registro se hace una sola
 * vez por llamada: si el login falla, se devuelve [RegisterAndSignInOutcome.CreatedButLoginFailed].
 */
class RegisterAndSignIn(
    private val tenantRepository: TenantRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(input: RestaurantRegistrationInput): RegisterAndSignInOutcome {
        val registered = tenantRepository.registerRestaurant(input).getOrElse { error ->
            return RegisterAndSignInOutcome.RegistrationFailed(
                error.message?.takeIf { it.isNotBlank() } ?: "No se pudo crear tu restaurante",
            )
        }
        val email = input.email.trim()
        val loginFailed = RegisterAndSignInOutcome.CreatedButLoginFailed(
            restaurantName = registered.name,
            email = email,
            message = WizardCopy.REGISTER_LOGIN_FAILED_AFTER_CREATE,
        )
        val session = authRepository.loginWithEmail(email, input.password).getOrElse { return loginFailed }
        if (!RestaurantPermissions.hasOperationalAccess(session.restaurantPermissions)) {
            authRepository.logout()
            return loginFailed
        }
        val route = RestaurantPermissions.defaultRoute(session.restaurantPermissions, session.user.employeeType)
        return RegisterAndSignInOutcome.SignedIn(registered.name, route)
    }
}
