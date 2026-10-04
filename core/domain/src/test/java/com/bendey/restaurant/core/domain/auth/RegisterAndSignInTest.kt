package com.bendey.restaurant.core.domain.auth

import com.bendey.restaurant.core.domain.model.AuthUser
import com.bendey.restaurant.core.domain.model.PinStation
import com.bendey.restaurant.core.domain.model.TenantBinding
import com.bendey.restaurant.core.domain.model.UserSession
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RegisterAndSignInTest {

    private class FakeTenantRepository(private val result: Result<RestaurantRegistrationResult>) : TenantRepository {
        var registerCalls = 0
        var lastInput: RestaurantRegistrationInput? = null
        override suspend fun resolveTenantByRuc(ruc: String): Result<TenantBinding> = error("no se usa")
        override suspend fun validateRucWithSunat(ruc: String): Result<SunatRucValidation> = error("no se usa")
        override suspend fun bindTenant(binding: TenantBinding) = Unit
        override suspend fun registerRestaurant(input: RestaurantRegistrationInput): Result<RestaurantRegistrationResult> {
            registerCalls++
            lastInput = input
            return result
        }
        override suspend fun clearTenant() = Unit
    }

    private class FakeAuthRepository(private val result: Result<UserSession>) : AuthRepository {
        var loginCalls = 0
        var lastEmail: String? = null
        var lastPassword: String? = null
        var logoutCalls = 0
        override suspend fun loginWithEmail(email: String, password: String): Result<UserSession> {
            loginCalls++
            lastEmail = email
            lastPassword = password
            return result
        }
        override suspend fun loginWithPin(pin: String, station: PinStation): Result<UserSession> = error("no se usa")
        override suspend fun refreshRestaurantPermissions(): Result<UserSession> = error("no se usa")
        override suspend fun logout() {
            logoutCalls++
        }
    }

    private val input = RestaurantRegistrationInput(
        name = "Sabores SAC",
        razonSocial = "Sabores SAC",
        ruc = "20123456789",
        email = " dueno@sabores.pe ",
        phone = "",
        password = "secreto1",
    )

    private fun session(permissions: List<String>) = UserSession(
        token = "t",
        user = AuthUser(id = 1, name = "Dueño", email = "dueno@sabores.pe", role = "admin", employeeType = "admin"),
        restaurantPermissions = permissions,
    )

    @Test
    fun exitoRegistraUnaVezYEntraConLasMismasCredenciales() = runBlocking {
        val tenants = FakeTenantRepository(Result.success(RestaurantRegistrationResult("Sabores")))
        val auth = FakeAuthRepository(Result.success(session(listOf("restaurant.admin", "restaurant.caja"))))

        val out = RegisterAndSignIn(tenants, auth)(input)

        assertIs<RegisterAndSignInOutcome.SignedIn>(out)
        assertEquals("Sabores", out.restaurantName)
        assertEquals(1, tenants.registerCalls)
        assertEquals(1, auth.loginCalls)
        assertEquals("dueno@sabores.pe", auth.lastEmail)
        assertEquals("secreto1", auth.lastPassword)
    }

    @Test
    fun fallaElRegistroNoIntentaLogin() = runBlocking {
        val tenants = FakeTenantRepository(Result.failure(IllegalStateException("Ya existe una empresa registrada con ese RUC")))
        val auth = FakeAuthRepository(Result.success(session(listOf("x"))))

        val out = RegisterAndSignIn(tenants, auth)(input)

        assertIs<RegisterAndSignInOutcome.RegistrationFailed>(out)
        assertEquals("Ya existe una empresa registrada con ese RUC", out.message)
        assertEquals(0, auth.loginCalls)
    }

    @Test
    fun fallaElLoginTrasRegistrarVaAlLoginConElCorreoYNuncaReintentaElRegistro() = runBlocking {
        val tenants = FakeTenantRepository(Result.success(RestaurantRegistrationResult("Sabores")))
        val auth = FakeAuthRepository(Result.failure(RuntimeException("sin conexión")))

        val out = RegisterAndSignIn(tenants, auth)(input)

        assertIs<RegisterAndSignInOutcome.CreatedButLoginFailed>(out)
        assertEquals("dueno@sabores.pe", out.email)
        assertTrue(out.message.contains("ya está creado"))
        assertEquals(1, tenants.registerCalls)
        assertEquals(1, auth.loginCalls)
    }

    @Test
    fun sinPermisosOperativosCierraSesionYVaAlLogin() = runBlocking {
        val tenants = FakeTenantRepository(Result.success(RestaurantRegistrationResult("Sabores")))
        val auth = FakeAuthRepository(Result.success(session(emptyList())))

        val out = RegisterAndSignIn(tenants, auth)(input)

        assertIs<RegisterAndSignInOutcome.CreatedButLoginFailed>(out)
        assertEquals(1, auth.logoutCalls)
        assertEquals(1, tenants.registerCalls)
    }
}
