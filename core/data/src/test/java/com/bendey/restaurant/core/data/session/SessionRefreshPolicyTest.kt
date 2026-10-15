package com.bendey.restaurant.core.data.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRefreshPolicyTest {

    @Test fun guardaSiLaSesionSigueVivaYEsLaMisma() {
        assertTrue(
            SessionRefreshPolicy.puedeGuardarActualizacion(
                compuertaCerrada = false,
                tokenDeLaSesionGuardada = "A",
                tokenVigente = "A",
                tokenRefrescado = "A",
            ),
        )
    }

    /** El bucle de producción: un 401 cerró la compuerta mientras se refrescaban los permisos. */
    @Test fun noResucitaUnaSesionCerradaPorUn401() {
        assertFalse(
            SessionRefreshPolicy.puedeGuardarActualizacion(
                compuertaCerrada = true,
                tokenDeLaSesionGuardada = "A",
                tokenVigente = "A",
                tokenRefrescado = "A",
            ),
        )
    }

    /** La sesión ya se limpió (cierre por expiración) antes de que terminara el refresco. */
    @Test fun noEscribeSiLaSesionYaSeLimpio() {
        assertFalse(
            SessionRefreshPolicy.puedeGuardarActualizacion(
                compuertaCerrada = false,
                tokenDeLaSesionGuardada = null,
                tokenVigente = null,
                tokenRefrescado = "A",
            ),
        )
    }

    /** Entre medio hubo otro login: el refresco viejo no pisa la sesión nueva. */
    @Test fun noPisaUnaSesionNueva() {
        assertFalse(
            SessionRefreshPolicy.puedeGuardarActualizacion(
                compuertaCerrada = false,
                tokenDeLaSesionGuardada = "B",
                tokenVigente = "B",
                tokenRefrescado = "A",
            ),
        )
    }

    @Test fun noEscribeSiFaltaElTokenAunqueQuedeLaSesion() {
        assertFalse(
            SessionRefreshPolicy.puedeGuardarActualizacion(
                compuertaCerrada = false,
                tokenDeLaSesionGuardada = "A",
                tokenVigente = null,
                tokenRefrescado = "A",
            ),
        )
    }

    @Test fun noEscribeConTokenVacio() {
        assertFalse(
            SessionRefreshPolicy.puedeGuardarActualizacion(
                compuertaCerrada = false,
                tokenDeLaSesionGuardada = "",
                tokenVigente = "",
                tokenRefrescado = "",
            ),
        )
    }
}
