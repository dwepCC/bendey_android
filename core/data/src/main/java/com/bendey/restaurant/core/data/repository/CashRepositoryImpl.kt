package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.data.session.SessionManager
import com.bendey.restaurant.core.domain.cash.AddCashMovementInput
import com.bendey.restaurant.core.domain.cash.CashMovement
import com.bendey.restaurant.core.domain.cash.CashMovementType
import com.bendey.restaurant.core.domain.cash.CashRepository
import com.bendey.restaurant.core.domain.cash.CashSession
import com.bendey.restaurant.core.domain.cash.CashSessionStatus
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.model.CashSessionSnapshot
import com.bendey.restaurant.core.network.api.CashbankApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.AddCashMovementRequestDto
import com.bendey.restaurant.core.network.dto.CashMovementDto
import com.bendey.restaurant.core.network.dto.CashSessionDto
import com.bendey.restaurant.core.network.dto.CloseCashSessionRequestDto
import com.bendey.restaurant.core.network.dto.OpenCashSessionRequestDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CashRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
    private val sessionManager: SessionManager,
) : CashRepository {

    override suspend fun getOpenSession(branchId: Int?): AppResult<CashSession?> = apiCall {
        val dto = tenantRetrofitProvider.create<CashbankApi>()
            .getOpenSession(branchId?.takeIf { it > 0 })
            .data
        if (dto == null) {
            sessionManager.setCashSession(null)
            null
        } else {
            dto.toDomain().also { persistSnapshot(it) }
        }
    }

    override suspend fun openSession(
        branchId: Int,
        openingBalance: Double,
        notes: String?,
    ): AppResult<CashSession> = apiCall {
        val dto = tenantRetrofitProvider.create<CashbankApi>().openSession(
            OpenCashSessionRequestDto(
                branchId = branchId,
                openingBalance = openingBalance,
                notes = notes?.takeIf { it.isNotBlank() },
            ),
        ).data ?: error("No se pudo abrir la caja")
        dto.toDomain().also { persistSnapshot(it) }
    }

    override suspend fun closeSession(
        sessionId: Int,
        closingBalance: Double?,
        notes: String?,
    ): AppResult<CashSession> = apiCall {
        val dto = tenantRetrofitProvider.create<CashbankApi>().closeSession(
            sessionId = sessionId,
            body = CloseCashSessionRequestDto(
                closingBalance = closingBalance,
                notes = notes?.takeIf { it.isNotBlank() },
            ),
        ).data ?: error("No se pudo cerrar la caja")
        sessionManager.setCashSession(null)
        dto.toDomain()
    }

    override suspend fun listMovements(sessionId: Int): AppResult<List<CashMovement>> = apiCall {
        tenantRetrofitProvider.create<CashbankApi>()
            .listMovements(sessionId)
            .data
            .map { it.toDomain() }
    }

    override suspend fun addMovement(
        sessionId: Int,
        input: AddCashMovementInput,
    ): AppResult<CashMovement> = apiCall {
        val dto = tenantRetrofitProvider.create<CashbankApi>().addMovement(
            sessionId = sessionId,
            body = AddCashMovementRequestDto(
                type = input.type.apiValue,
                category = input.category,
                reference = input.reference.takeIf { it.isNotBlank() },
                paymentMethod = "cash",
                amount = input.amount,
                notes = input.notes.takeIf { it.isNotBlank() },
            ),
        ).data ?: error("Movimiento no registrado")
        dto.toDomain()
    }

    private suspend fun persistSnapshot(session: CashSession) {
        if (session.status != CashSessionStatus.OPEN) return
        sessionManager.setCashSession(
            CashSessionSnapshot(
                sessionId = session.id,
                branchId = session.branchId,
                openedAtEpochMs = System.currentTimeMillis(),
                openingAmount = session.openingBalance,
                label = session.branchName ?: "Caja abierta",
                expectedBalance = session.expectedBalance,
            ),
        )
    }
}

private inline fun <T> apiCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: Exception) {
    val mapped = NetworkErrorMapper.map(e)
    AppResult.Error(mapped.message ?: "Error de conexión", mapped)
}

private fun CashSessionDto.toDomain() = CashSession(
    id = id,
    branchId = branchId,
    branchName = branchName,
    openedByName = openedByName,
    openingBalance = openingBalance,
    expectedBalance = expectedBalance ?: currentBalance ?: openingBalance,
    totalIncome = totalIncome ?: 0.0,
    totalExpense = totalExpense ?: 0.0,
    status = CashSessionStatus.fromApi(status),
    openedAt = openedAt,
    notes = notes,
)

private fun CashMovementDto.toDomain() = CashMovement(
    id = id,
    type = CashMovementType.fromApi(type),
    category = category,
    reference = reference,
    amount = amount,
    notes = notes,
    createdAt = createdAt,
)
