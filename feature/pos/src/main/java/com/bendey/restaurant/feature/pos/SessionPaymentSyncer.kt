package com.bendey.restaurant.feature.pos

import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.delivery.CashTenderedCheck
import com.bendey.restaurant.core.domain.delivery.DeliverySettings
import com.bendey.restaurant.core.domain.delivery.PaymentSync
import com.bendey.restaurant.core.domain.delivery.SessionPayment
import com.bendey.restaurant.core.domain.delivery.SessionPaymentMode
import com.bendey.restaurant.core.domain.delivery.paymentSyncNeeded
import com.bendey.restaurant.core.domain.delivery.showsPosPaymentForm
import com.bendey.restaurant.core.domain.model.AppResult

/**
 * D2b: guarda el pago contra entrega en la sesión YA creada (`PUT /sessions/:id/payment`) y lo vuelve a mandar si
 * el cajero lo cambió. Sin Android: lo usa [PosViewModel] tras `ensureSession` y los tests con un repositorio falso.
 */
internal class SessionPaymentSyncer(private val repository: DeliveryRepository) {

    /**
     * `null` = no había nada que mandar (no es delivery, `cod_enabled` apagado sin pago previo, o el servidor ya
     * tiene lo mismo): NO se hace ninguna llamada. Si se llamó, devuelve el resultado: el pago guardado (`null` si
     * se quitó) o el error ya traducido.
     */
    suspend fun sync(
        sessionId: Int,
        isDelivery: Boolean,
        settings: DeliverySettings?,
        current: SessionPayment?,
        codSelected: Boolean,
        tenderedCheck: CashTenderedCheck,
    ): AppResult<SessionPayment?>? {
        if (!isDelivery) return null
        // `Exact` (campo vacío) y `Stale` (monto ya guardado) = sin monto nuevo; solo `Ok` lleva un monto.
        val tendered = (tenderedCheck as? CashTenderedCheck.Ok)?.amount
            ?: (tenderedCheck as? CashTenderedCheck.Stale)?.amount
        val action = paymentSyncNeeded(
            formShown = showsPosPaymentForm(true, settings, current),
            codSelected = codSelected,
            tendered = tendered,
            current = current,
        ) ?: return null
        return when (action) {
            is PaymentSync.SetCod -> repository.setSessionPayment(sessionId, SessionPaymentMode.CASH_ON_DELIVERY, action.tendered)
            PaymentSync.Clear -> repository.setSessionPayment(sessionId, SessionPaymentMode.NONE)
        }
    }
}
