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
        arqueo: Map<String, Int>? = null,
    ): AppResult<CashSession>
    suspend fun listSessions(branchId: Int?): AppResult<List<CashSessionBrief>>
    suspend fun getSessionReport(sessionId: Int): AppResult<CashSessionReport>
    suspend fun saveArqueo(sessionId: Int, arqueo: Map<String, Int>): AppResult<Double>
    suspend fun listMovements(sessionId: Int): AppResult<List<CashMovement>>
    suspend fun addMovement(
        sessionId: Int,
        input: AddCashMovementInput,
    ): AppResult<CashMovement>
    suspend fun listPaymentMethods(): AppResult<List<CashPaymentMethod>>
    suspend fun createPaymentMethod(name: String, code: String, destinationType: String, bankAccountId: Int?): AppResult<Unit>
    suspend fun updatePaymentMethod(id: Int, name: String, code: String, destinationType: String, bankAccountId: Int?, active: Boolean): AppResult<Unit>
    suspend fun deletePaymentMethod(id: Int): AppResult<Unit>
    suspend fun listBankAccounts(): AppResult<List<CashBankAccount>>
    suspend fun createBankAccount(name: String, bankName: String, accountNumber: String, currency: String, type: String, paymentMethod: String, initialBalance: Double): AppResult<Unit>
    suspend fun updateBankAccount(id: Int, name: String, bankName: String, accountNumber: String, type: String, paymentMethod: String, active: Boolean): AppResult<Unit>
    suspend fun listBankMovements(accountId: Int): AppResult<List<CashBankMovement>>
    suspend fun addBankMovement(accountId: Int, type: String, description: String, reference: String, amount: Double, date: String): AppResult<Unit>
}
