package com.bendey.restaurant.core.domain.cash

import com.bendey.restaurant.core.domain.model.AppResult

interface CashRepository {
    suspend fun getOpenSession(branchId: Int?): AppResult<CashSession?>
    suspend fun openSession(
        branchId: Int,
        openingBalance: Double,
        notes: String?,
    ): AppResult<CashSession>
    suspend fun closeSession(
        sessionId: Int,
        closingBalance: Double?,
        notes: String?,
    ): AppResult<CashSession>
    suspend fun listMovements(sessionId: Int): AppResult<List<CashMovement>>
    suspend fun addMovement(
        sessionId: Int,
        input: AddCashMovementInput,
    ): AppResult<CashMovement>
}
