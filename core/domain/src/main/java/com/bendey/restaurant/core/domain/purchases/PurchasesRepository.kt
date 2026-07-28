package com.bendey.restaurant.core.domain.purchases

import com.bendey.restaurant.core.domain.model.AppResult

interface PurchasesRepository {
    suspend fun listPurchases(query: String = ""): AppResult<List<Purchase>>
    suspend fun getPurchase(id: Int): AppResult<PurchaseDetail>
    suspend fun createPurchase(input: CreatePurchaseInput): AppResult<Purchase>
    suspend fun voidPurchase(id: Int): AppResult<String>
}
