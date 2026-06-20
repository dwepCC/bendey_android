package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.billing.BillSessionInput
import com.bendey.restaurant.core.domain.billing.BillSessionResult
import com.bendey.restaurant.core.domain.billing.BillingRepository
import com.bendey.restaurant.core.domain.billing.CheckoutMeta
import com.bendey.restaurant.core.domain.billing.ContactBrief
import com.bendey.restaurant.core.domain.billing.DocumentSeries
import com.bendey.restaurant.core.domain.billing.PaymentMethodOption
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.network.api.CashbankApi
import com.bendey.restaurant.core.network.api.CompanyApi
import com.bendey.restaurant.core.network.api.ContactsApi
import com.bendey.restaurant.core.network.api.RestaurantApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.BillSessionRequestDto
import com.bendey.restaurant.core.network.dto.BillPaymentDto
import com.bendey.restaurant.core.network.dto.ContactDto
import com.bendey.restaurant.core.network.dto.DocumentSeriesDto
import com.bendey.restaurant.core.network.dto.PaymentMethodDto
import com.bendey.restaurant.core.domain.billing.VoidCreditNoteResult
import com.bendey.restaurant.core.network.api.BillingApi
import com.bendey.restaurant.core.network.dto.VoidCreditNoteRequestDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : BillingRepository {

    override suspend fun loadCheckoutMeta(branchId: Int): AppResult<CheckoutMeta> = apiCall {
        val series = tenantRetrofitProvider.create<CompanyApi>()
            .listSeries(branchId = branchId, category = "venta")
            .data
            .filter { it.active }
            .map { it.toDomain() }
        val contacts = tenantRetrofitProvider.create<ContactsApi>()
            .listContacts(type = "customer")
            .data
            .filter { it.active }
            .map { it.toDomain() }
        val paymentMethods = tenantRetrofitProvider.create<CashbankApi>()
            .listPaymentMethods(all = "1")
            .data
            .filter { it.active }
            .map { it.toDomain() }
        CheckoutMeta(
            series = series,
            contacts = contacts,
            paymentMethods = paymentMethods,
        )
    }

    override suspend fun billSession(
        sessionId: Int,
        input: BillSessionInput,
    ): AppResult<BillSessionResult> = apiCall {
        val response = tenantRetrofitProvider.create<RestaurantApi>().billSession(
            sessionId = sessionId,
            body = BillSessionRequestDto(
                seriesId = input.seriesId,
                docType = input.docType,
                contactId = input.contactId,
                cashSessionId = input.cashSessionId,
                closeSession = input.closeSession,
                discountAmount = input.discountAmount?.takeIf { it > 0 },
                payments = input.payments.map {
                    BillPaymentDto(
                        method = it.method,
                        amount = it.amount,
                        reference = it.reference,
                    )
                },
            ),
        )
        val data = response.data ?: error("Venta no generada")
        BillSessionResult(
            saleId = data.id,
            number = data.number,
            total = data.total,
            printData = response.printData?.toDomain(),
        )
    }

    override suspend fun voidWithCreditNote(
        saleId: Int,
        reason: String,
    ): AppResult<VoidCreditNoteResult> = apiCall {
        val response = tenantRetrofitProvider.create<BillingApi>()
            .voidWithCreditNote(saleId, VoidCreditNoteRequestDto(reason = reason.trim()))
        VoidCreditNoteResult(
            message = response.message,
            async = response.async,
        )
    }
}

fun pickDefaultNotaVentaSeries(series: List<DocumentSeries>): DocumentSeries? {
    val active = series.filter { it.active }
    return active.firstOrNull { it.docType.contains("NOTA", ignoreCase = true) }
        ?: active.firstOrNull { it.sunatCode == "03" }
        ?: active.firstOrNull { it.series.startsWith("NV", ignoreCase = true) }
        ?: active.firstOrNull()
}

fun pickVariosContactId(contacts: List<ContactBrief>): Int? {
    return contacts.firstOrNull {
        it.docNumber.replace("-", "") == "00000000" ||
            it.businessName.contains("VARIOS", ignoreCase = true)
    }?.id ?: contacts.firstOrNull()?.id
}

fun defaultPaymentMethodCode(methods: List<PaymentMethodOption>): String {
    return methods.firstOrNull()?.code ?: "cash"
}

private inline fun <T> apiCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: Exception) {
    val mapped = NetworkErrorMapper.map(e)
    AppResult.Error(mapped.message ?: "Error de conexión", mapped)
}

private fun DocumentSeriesDto.toDomain() = DocumentSeries(
    id = id,
    branchId = branchId,
    docType = docType,
    series = series,
    category = category,
    sunatCode = sunatCode,
    active = active,
)

private fun ContactDto.toDomain() = ContactBrief(
    id = id,
    docType = docType,
    docNumber = docNumber,
    businessName = businessName.ifBlank { tradeName },
    active = active,
)

private fun PaymentMethodDto.toDomain() = PaymentMethodOption(
    id = id,
    name = name,
    code = code,
    destinationType = destinationType,
    active = active,
)
