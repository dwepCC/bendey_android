package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.AvailablePlansResponseDto
import com.bendey.restaurant.core.network.dto.BillingHubDto
import com.bendey.restaurant.core.network.dto.RenewResponseDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface SubscriptionApi {
    @GET("/api/subscription/summary")
    suspend fun getSummary(): BillingHubDto

    @GET("/api/subscription/plans")
    suspend fun getPlans(): AvailablePlansResponseDto

    /** Contrata el próximo período. Sin `plan_id` el backend repite el plan actual, que es el caso normal. */
    @FormUrlEncoded
    @POST("/api/subscription/renew")
    suspend fun renovar(@Field("plan_id") planId: Int?): RenewResponseDto
}
