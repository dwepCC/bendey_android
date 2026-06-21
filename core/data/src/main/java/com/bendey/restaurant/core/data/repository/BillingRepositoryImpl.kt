package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.data.cache.OperationalDataCache
import com.bendey.restaurant.core.domain.billing.BillQuickSaleInput
import com.bendey.restaurant.core.domain.billing.BillSessionInput
import com.bendey.restaurant.core.domain.billing.BillSessionResult
import com.bendey.restaurant.core.domain.billing.BillingDocumentKind
import com.bendey.restaurant.core.domain.billing.BillingRepository
import com.bendey.restaurant.core.domain.billing.BankAccountBrief
import com.bendey.restaurant.core.domain.billing.CheckoutMeta
import com.bendey.restaurant.core.domain.billing.ContactBrief
import com.bendey.restaurant.core.domain.billing.DocumentSeries
import com.bendey.restaurant.core.domain.billing.PaymentMethodOption
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.network.api.CashbankApi
import com.bendey.restaurant.core.network.api.SettingsApi
import com.bendey.restaurant.core.network.api.CompanyApi
import com.bendey.restaurant.core.network.api.ContactsApi
import com.bendey.restaurant.core.network.api.RestaurantApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.BillQuickSaleRequestDto
import com.bendey.restaurant.core.network.dto.BillSessionRequestDto
import com.bendey.restaurant.core.network.dto.BillPaymentDto
import com.bendey.restaurant.core.network.dto.OrderItemInputDto
import com.bendey.restaurant.core.network.dto.ContactDto
import com.bendey.restaurant.core.network.dto.DocumentSeriesDto
import com.bendey.restaurant.core.network.dto.PaymentMethodDto
import com.bendey.restaurant.core.domain.billing.VoidCreditNoteResult
import com.bendey.restaurant.core.domain.billing.BillingActionResult
import com.bendey.restaurant.core.network.api.BillingApi
import com.bendey.restaurant.core.network.dto.VoidCreditNoteRequestDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
    private val operationalDataCache: OperationalDataCache,
    @ApplicationContext private val context: Context,
) : BillingRepository {

    override suspend fun loadCheckoutMeta(branchId: Int): AppResult<CheckoutMeta> {
        operationalDataCache.getCheckoutMeta(branchId)?.let { return AppResult.Success(it) }
        return fetchCheckoutMeta(branchId)
    }

    override suspend fun refreshCheckoutMeta(branchId: Int): AppResult<CheckoutMeta> = fetchCheckoutMeta(branchId)

    private suspend fun fetchCheckoutMeta(branchId: Int): AppResult<CheckoutMeta> = apiCall {
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
        val bankAccounts = tenantRetrofitProvider.create<CashbankApi>()
            .listBankAccounts(all = "1")
            .data
            .map {
                BankAccountBrief(
                    id = it.id,
                    paymentMethod = it.paymentMethod,
                    active = it.active,
                )
            }
        val sunatEnabled = tenantRetrofitProvider.create<SettingsApi>()
            .getSunatConfig()
            .sunatEnabled
        CheckoutMeta(
            series = series,
            contacts = contacts,
            paymentMethods = paymentMethods,
            bankAccounts = bankAccounts,
            sunatEnabled = sunatEnabled,
        ).also { operationalDataCache.setCheckoutMeta(branchId, it) }
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

    override suspend fun billQuickSale(
        input: BillQuickSaleInput,
    ): AppResult<BillSessionResult> = apiCall {
        val response = tenantRetrofitProvider.create<RestaurantApi>().billQuickSale(
            body = BillQuickSaleRequestDto(
                seriesId = input.seriesId,
                docType = input.docType,
                contactId = input.contactId,
                cashSessionId = input.cashSessionId,
                discountAmount = input.discountAmount?.takeIf { it > 0 },
                notes = input.notes?.takeIf { it.isNotBlank() },
                items = input.items.map { it.toBillingDto() },
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

    override suspend fun sendToSunat(saleId: Int): AppResult<BillingActionResult> = apiCall {
        val response = tenantRetrofitProvider.create<BillingApi>().sendToSunat(saleId)
        BillingActionResult(
            message = response.message ?: response.sunatMessage,
            billingStatus = response.billingStatus,
            async = response.async,
        )
    }

    override suspend fun resendToSunat(saleId: Int): AppResult<BillingActionResult> = apiCall {
        val response = tenantRetrofitProvider.create<BillingApi>().resendToSunat(saleId)
        BillingActionResult(
            message = response.message ?: response.sunatMessage,
            billingStatus = response.billingStatus,
            async = response.async,
        )
    }

    override suspend fun downloadOfficialPdf(saleId: Int): AppResult<File> =
        downloadBillingDocument(saleId, BillingDocumentKind.PDF)

    override suspend fun downloadBillingDocument(saleId: Int, kind: BillingDocumentKind): AppResult<File> = apiCall {
        val body = tenantRetrofitProvider.create<BillingApi>().downloadDocument(saleId, kind.pathSegment)
        val dir = File(context.cacheDir, "sunat-docs").also { it.mkdirs() }
        val file = File(dir, "sunat-$saleId-${kind.pathSegment}.${fileExtension(kind)}")
        body.byteStream().use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file
    }

    override suspend fun loadBillingDocumentText(saleId: Int, kind: BillingDocumentKind): AppResult<String> = apiCall {
        val body = tenantRetrofitProvider.create<BillingApi>().downloadDocument(saleId, kind.pathSegment)
        body.string()
    }
}

private fun fileExtension(kind: BillingDocumentKind): String = when (kind) {
    BillingDocumentKind.CDR -> "zip"
    BillingDocumentKind.PDF -> "pdf"
    BillingDocumentKind.XML, BillingDocumentKind.XML_GENERATED -> "xml"
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

private fun DocumentSeriesDto.toDomain(): DocumentSeries {
    val correlativeValue = correlative ?: currentNumber
    val sales = salesCount ?: 0
    val computedLocked = locked ?: (correlativeValue > 1 || sales > 0)
    val computedCanDelete = canDelete ?: !computedLocked
    return DocumentSeries(
        id = id,
        branchId = branchId,
        docType = docType,
        series = series,
        category = category,
        sunatCode = sunatCode,
        active = active,
        currentNumber = correlativeValue,
        locked = computedLocked,
        canDelete = computedCanDelete,
    )
}

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
    bankAccountId = bankAccountId,
    active = active,
)

private fun com.bendey.restaurant.core.domain.restaurant.OrderItemInput.toBillingDto() = OrderItemInputDto(
    itemKind = itemKind,
    productId = productId,
    productCode = productCode.takeIf { it.isNotBlank() },
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    notes = notes?.takeIf { it.isNotBlank() },
    modifiersJson = modifiersJson?.takeIf { it.isNotBlank() },
    comboId = comboId,
    comboConfigJson = comboConfigJson?.takeIf { it.isNotBlank() },
    igvAffectationType = igvAffectationType,
    priceIncludesIgv = priceIncludesIgv,
)
