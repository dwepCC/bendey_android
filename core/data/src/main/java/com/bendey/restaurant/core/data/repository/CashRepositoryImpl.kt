package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.data.session.SessionManager
import com.bendey.restaurant.core.domain.cash.AddCashMovementInput
import com.bendey.restaurant.core.domain.cash.CashBankAccount
import com.bendey.restaurant.core.domain.cash.CashBankMovement
import com.bendey.restaurant.core.domain.cash.CashCancelledSaleRow
import com.bendey.restaurant.core.domain.cash.CashMethodTotal
import com.bendey.restaurant.core.domain.cash.CashMovement
import com.bendey.restaurant.core.domain.cash.CashMovementType
import com.bendey.restaurant.core.domain.cash.CashPaymentMethod
import com.bendey.restaurant.core.domain.cash.CashReportRow
import com.bendey.restaurant.core.domain.cash.CashRepository
import com.bendey.restaurant.core.domain.cash.CashSession
import com.bendey.restaurant.core.domain.cash.CashSessionBrief
import com.bendey.restaurant.core.domain.cash.CashSessionReport
import com.bendey.restaurant.core.domain.cash.CashSessionStatus
import com.bendey.restaurant.core.domain.cash.CashFilterUser
import com.bendey.restaurant.core.domain.cash.CashMethodTotalWithCount
import com.bendey.restaurant.core.domain.cash.CashMovementReportRow
import com.bendey.restaurant.core.domain.cash.CashMovementsReportPage
import com.bendey.restaurant.core.domain.cash.CashMovementsReportQuery
import com.bendey.restaurant.core.domain.cash.CashMovementsReportSummary
import com.bendey.restaurant.core.domain.cash.CashPaymentDetailRow
import com.bendey.restaurant.core.domain.cash.CashPaymentsReport
import com.bendey.restaurant.core.domain.cash.CashSessionProductSold
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.model.CashSessionSnapshot
import com.bendey.restaurant.core.network.api.CashbankApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.AddCashMovementRequestDto
import com.bendey.restaurant.core.network.dto.AddBankMovementRequestDto
import com.bendey.restaurant.core.network.dto.BankAccountDto
import com.bendey.restaurant.core.network.dto.BankMovementDto
import com.bendey.restaurant.core.network.dto.BankAccountUpsertRequestDto
import com.bendey.restaurant.core.network.dto.CashCancelledSaleRowDto
import com.bendey.restaurant.core.network.dto.CashMethodTotalDto
import com.bendey.restaurant.core.network.dto.PaymentMethodDto
import com.bendey.restaurant.core.network.dto.PaymentMethodUpsertRequestDto
import com.bendey.restaurant.core.network.dto.CashMovementDto
import com.bendey.restaurant.core.network.dto.CashReportRowDto
import com.bendey.restaurant.core.network.dto.CashSessionDto
import com.bendey.restaurant.core.network.dto.CashSessionReportDto
import com.bendey.restaurant.core.network.dto.CloseCashSessionRequestDto
import com.bendey.restaurant.core.network.dto.OpenCashSessionRequestDto
import com.bendey.restaurant.core.network.api.RestaurantApi
import com.bendey.restaurant.core.network.dto.MovementReportRowDto
import com.bendey.restaurant.core.network.dto.MovementsReportSummaryDto
import com.bendey.restaurant.core.network.dto.SaveArqueoRequestDto
import com.bendey.restaurant.core.network.dto.SessionProductSoldDto
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
        arqueo: Map<String, Int>?,
    ): AppResult<CashSession> = apiCall {
        val dto = tenantRetrofitProvider.create<CashbankApi>().closeSession(
            sessionId = sessionId,
            body = CloseCashSessionRequestDto(
                closingBalance = closingBalance,
                notes = notes?.takeIf { it.isNotBlank() },
                arqueo = arqueo?.takeIf { it.values.any { qty -> qty > 0 } },
            ),
        ).data ?: error("No se pudo cerrar la caja")
        sessionManager.setCashSession(null)
        dto.toDomain()
    }

    override suspend fun listSessions(branchId: Int?): AppResult<List<CashSessionBrief>> = apiCall {
        tenantRetrofitProvider.create<CashbankApi>()
            .listSessions(branchId?.takeIf { it > 0 })
            .data
            .map { it.toBrief() }
    }

    override suspend fun getSessionReport(sessionId: Int): AppResult<CashSessionReport> = apiCall {
        tenantRetrofitProvider.create<CashbankApi>()
            .getSessionReport(sessionId)
            .data
            ?.toDomain()
            ?: error("Reporte no disponible")
    }

    override suspend fun saveArqueo(sessionId: Int, arqueo: Map<String, Int>): AppResult<Double> = apiCall {
        tenantRetrofitProvider.create<CashbankApi>()
            .saveArqueo(sessionId, SaveArqueoRequestDto(arqueo = arqueo))
            .sum ?: 0.0
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
                paymentMethod = input.paymentMethod.ifBlank { "cash" },
                amount = input.amount,
                notes = input.notes.takeIf { it.isNotBlank() },
                titular = input.titular.takeIf { it.isNotBlank() },
            ),
        ).data ?: error("Movimiento no registrado")
        dto.toDomain()
    }

    override suspend fun listPaymentMethods(): AppResult<List<CashPaymentMethod>> = apiCall {
        tenantRetrofitProvider.create<CashbankApi>()
            .listPaymentMethods()
            .data
            .map { it.toPaymentMethodDomain() }
    }

    override suspend fun createPaymentMethod(
        name: String,
        code: String,
        destinationType: String,
        bankAccountId: Int?,
    ): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<CashbankApi>().createPaymentMethod(
            PaymentMethodUpsertRequestDto(
                name = name,
                code = code,
                destinationType = destinationType,
                bankAccountId = bankAccountId,
            ),
        )
        Unit
    }

    override suspend fun updatePaymentMethod(
        id: Int,
        name: String,
        code: String,
        destinationType: String,
        bankAccountId: Int?,
        active: Boolean,
    ): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<CashbankApi>().updatePaymentMethod(
            id = id,
            body = PaymentMethodUpsertRequestDto(
                name = name,
                code = code,
                destinationType = destinationType,
                bankAccountId = bankAccountId,
                active = active,
            ),
        )
        Unit
    }

    override suspend fun deletePaymentMethod(id: Int): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<CashbankApi>().deletePaymentMethod(id)
        Unit
    }

    override suspend fun listBankAccounts(): AppResult<List<CashBankAccount>> = apiCall {
        tenantRetrofitProvider.create<CashbankApi>()
            .listBankAccounts()
            .data
            .map { it.toBankAccountDomain() }
    }

    override suspend fun createBankAccount(
        name: String,
        bankName: String,
        accountNumber: String,
        currency: String,
        type: String,
        paymentMethod: String,
        initialBalance: Double,
    ): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<CashbankApi>().createBankAccount(
            BankAccountUpsertRequestDto(
                name = name,
                bankName = bankName,
                accountNumber = accountNumber,
                currency = currency,
                type = type,
                paymentMethod = paymentMethod,
                initialBalance = initialBalance,
            ),
        )
        Unit
    }

    override suspend fun updateBankAccount(
        id: Int,
        name: String,
        bankName: String,
        accountNumber: String,
        type: String,
        paymentMethod: String,
        active: Boolean,
    ): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<CashbankApi>().updateBankAccount(
            id = id,
            body = BankAccountUpsertRequestDto(
                name = name,
                bankName = bankName,
                accountNumber = accountNumber,
                type = type,
                paymentMethod = paymentMethod,
                active = active,
            ),
        )
        Unit
    }

    override suspend fun listBankMovements(accountId: Int): AppResult<List<CashBankMovement>> = apiCall {
        tenantRetrofitProvider.create<CashbankApi>()
            .listBankMovements(accountId)
            .data
            .map { it.toMovementDomain() }
    }

    override suspend fun addBankMovement(
        accountId: Int,
        type: String,
        description: String,
        reference: String,
        amount: Double,
        date: String,
    ): AppResult<Unit> = apiCall {
        tenantRetrofitProvider.create<CashbankApi>().addBankMovement(
            id = accountId,
            body = AddBankMovementRequestDto(
                type = type,
                description = description,
                reference = reference.takeIf { it.isNotBlank() },
                amount = amount,
                date = date,
            ),
        )
        Unit
    }

    override suspend fun listMovementsReport(query: CashMovementsReportQuery): AppResult<CashMovementsReportPage> =
        fetchMovementsReport(query)

    override suspend fun listMovementsReportAll(query: CashMovementsReportQuery): AppResult<CashMovementsReportPage> =
        fetchMovementsReport(query.copy(page = 1, perPage = 0))

    override suspend fun getPaymentsReport(
        from: String,
        to: String,
        method: String?,
        userId: Int?,
        sessionId: Int?,
    ): AppResult<CashPaymentsReport> = apiCall {
        val response = tenantRetrofitProvider.create<RestaurantApi>().getPaymentsReport(
            from = from,
            to = to,
            method = method?.trim()?.takeIf { it.isNotEmpty() },
            userId = userId,
            sessionId = sessionId,
        )
        CashPaymentsReport(
            byMethod = response.byMethod.map {
                CashMethodTotalWithCount(method = it.method, total = it.total, count = it.count)
            },
            totalIncome = response.totalIncome,
            totalCount = response.totalCount,
            detail = response.detail.map {
                CashPaymentDetailRow(
                    date = it.date,
                    saleNumber = it.saleNumber,
                    orderCode = it.orderCode,
                    orderType = it.orderType,
                    userName = it.userName,
                    method = it.method,
                    amount = it.amount,
                    reference = it.reference,
                )
            },
        )
    }

    override suspend fun getSessionProductsReport(sessionId: Int): AppResult<List<CashSessionProductSold>> = apiCall {
        tenantRetrofitProvider.create<CashbankApi>()
            .getSessionProductsReport(sessionId)
            .data
            .map { it.toProductSoldDomain() }
    }

    override suspend fun listCashFilterUsers(): AppResult<List<CashFilterUser>> = apiCall {
        tenantRetrofitProvider.create<RestaurantApi>()
            .listStaff()
            .data
            .map { staff ->
                CashFilterUser(
                    userId = staff.userId,
                    name = staff.displayName?.takeIf { it.isNotBlank() }
                        ?: staff.staffCode?.takeIf { it.isNotBlank() }
                        ?: "#${staff.userId}",
                )
            }
    }

    private suspend fun fetchMovementsReport(query: CashMovementsReportQuery): AppResult<CashMovementsReportPage> = apiCall {
        val response = tenantRetrofitProvider.create<CashbankApi>().listMovementsReport(
            branchId = query.branchId?.takeIf { it > 0 },
            userId = query.userId,
            dateFrom = query.dateFrom?.takeIf { it.isNotBlank() },
            dateTo = query.dateTo?.takeIf { it.isNotBlank() },
            sessionId = query.sessionId,
            type = query.type?.takeIf { it.isNotBlank() },
            paymentMethod = query.paymentMethod?.takeIf { it.isNotBlank() },
            page = query.page.takeIf { query.perPage > 0 },
            perPage = query.perPage.takeIf { it > 0 },
        )
        CashMovementsReportPage(
            rows = response.data.map { it.toMovementReportDomain() },
            total = response.total,
            summary = response.summary?.toSummaryDomain() ?: CashMovementsReportSummary(),
        )
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
    closedAt = closedAt,
    closingBalance = closingBalance,
    difference = difference,
    notes = notes,
    arqueoJson = arqueoJson,
)

private fun CashSessionDto.toBrief() = CashSessionBrief(
    id = id,
    branchName = branchName,
    openedByName = openedByName,
    openingBalance = openingBalance,
    closingBalance = closingBalance,
    expectedBalance = expectedBalance ?: currentBalance ?: openingBalance,
    status = CashSessionStatus.fromApi(status),
    openedAt = openedAt,
    closedAt = closedAt,
)

private fun CashSessionReportDto.toDomain(): CashSessionReport {
    val totalsDto = totals
    return CashSessionReport(
        session = session.toBrief(),
        incomeDetail = incomeDetail.map { it.toDomain() },
        expenseDetail = expenseDetail.map { it.toDomain() },
        cancelledSalesDetail = cancelledSalesDetail.map { it.toDomain() },
        salesByMethod = totalsByMethod?.sales?.map { it.toDomain() }.orEmpty(),
        nonCashSalesByMethod = nonCashSalesByMethod.map { it.toDomain() },
        totalIncome = totalsDto.totalIncome,
        totalExpense = totalsDto.totalExpense,
        totalSales = totalsDto.totalSales,
        finalBalance = totalsDto.finalBalance,
        totalNetSales = totalsDto.totalNetSales ?: totalsDto.totalSales,
        totalVoidedSales = totalsDto.totalVoidedSales ?: 0.0,
    )
}

private fun CashReportRowDto.toDomain() = CashReportRow(
    date = date,
    type = type,
    docNumber = docNumber,
    reference = reference,
    amount = amount,
    paymentMethod = paymentMethod,
)

private fun CashMethodTotalDto.toDomain() = CashMethodTotal(method = method, total = total)

private fun CashCancelledSaleRowDto.toDomain() = CashCancelledSaleRow(
    date = date,
    docNumber = docNumber,
    amount = amount,
    paymentMethod = paymentMethod,
    reason = reason,
)

private fun PaymentMethodDto.toPaymentMethodDomain() = CashPaymentMethod(
    id = id,
    name = name,
    code = code,
    destinationType = destinationType,
    bankAccountId = bankAccountId,
    active = active,
)

private fun BankAccountDto.toBankAccountDomain() = CashBankAccount(
    id = id,
    name = name,
    bankName = bankName,
    accountNumber = accountNumber,
    currency = currency,
    balance = balance,
    type = type,
    paymentMethod = paymentMethod,
    active = active,
)

private fun BankMovementDto.toMovementDomain() = CashBankMovement(
    id = id,
    type = type,
    amount = amount,
    description = description,
    reference = reference,
    date = date,
)

private fun CashMovementDto.toDomain() = CashMovement(
    id = id,
    type = CashMovementType.fromApi(type),
    category = category,
    reference = reference,
    amount = amount,
    notes = notes,
    titular = titular,
    createdAt = createdAt,
)

private fun MovementReportRowDto.toMovementReportDomain() = CashMovementReportRow(
    date = date,
    type = type,
    docNumber = docNumber,
    contactName = contactName,
    userName = userName,
    branchName = branchName,
    paymentMethod = paymentMethod,
    amount = amount,
    movementId = movementId,
    cashSessionId = cashSessionId,
    category = category,
    cashReference = cashReference,
    notesDetail = notesDetail,
)

private fun MovementsReportSummaryDto.toSummaryDomain() = CashMovementsReportSummary(
    totalRows = totalRows,
    sumIncome = sumIncome,
    sumExpense = sumExpense,
    netMovement = netMovement,
)

private fun SessionProductSoldDto.toProductSoldDomain() = CashSessionProductSold(
    productId = productId,
    code = code,
    description = description,
    quantity = quantity,
    total = total,
)
