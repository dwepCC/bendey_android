package com.bendey.restaurant.core.domain.subscription

import com.bendey.restaurant.core.domain.model.AppResult

interface SubscriptionRepository {
    suspend fun getHub(): AppResult<BillingHub>
    suspend fun getPlans(): AppResult<List<AvailablePlan>>
    suspend fun submitPayment(input: SubmitPaymentInput): AppResult<SubscriptionActionResult>
    suspend fun requestPlanChange(input: PlanChangeInput): AppResult<SubscriptionActionResult>

    /**
     * Contrata el proximo periodo. `planId` nulo repite el plan actual.
     *
     * Renovar no es pagar: esto crea la obligacion, y el comprobante se presenta despues.
     */
    suspend fun renovar(planId: Int?): AppResult<SubscriptionActionResult>

    /** Base para resolver el QR de Yape/Plin: esas imagenes viven en el panel central. */
    fun assetsBaseUrl(): String?
}
