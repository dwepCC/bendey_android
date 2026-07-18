package com.bendey.restaurant.core.domain.subscription

import com.bendey.restaurant.core.domain.model.AppResult

interface SubscriptionRepository {
    suspend fun getHub(): AppResult<BillingHub>
    suspend fun getPlans(): AppResult<List<AvailablePlan>>
    suspend fun submitPayment(input: SubmitPaymentInput): AppResult<SubscriptionActionResult>
    suspend fun requestPlanChange(input: PlanChangeInput): AppResult<SubscriptionActionResult>
}
