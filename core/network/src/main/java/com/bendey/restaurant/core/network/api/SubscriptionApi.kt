package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.AvailablePlansResponseDto
import com.bendey.restaurant.core.network.dto.BillingHubDto
import retrofit2.http.GET

interface SubscriptionApi {
    @GET("/api/subscription/summary")
    suspend fun getSummary(): BillingHubDto

    @GET("/api/subscription/plans")
    suspend fun getPlans(): AvailablePlansResponseDto
}
