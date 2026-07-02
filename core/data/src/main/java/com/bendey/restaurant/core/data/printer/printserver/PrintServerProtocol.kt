package com.bendey.restaurant.core.data.printer.printserver

import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.core.domain.restaurant.ComandaLine
import com.bendey.restaurant.core.domain.restaurant.PrecuentaData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PrintJobResponse(
    @SerialName("jobId") val jobId: String,
    val status: String,
    val message: String? = null,
    val duplicate: Boolean = false,
    val errorCode: String? = null,
) {
    val isSuccess: Boolean get() = status == "ok"
}

@Serializable
data class DiscoveredPrintServer(
    val serverId: String,
    val displayName: String,
    val host: String,
    val port: Int,
    val tenant: String,
    val branchName: String,
    val branchId: Int,
    val hostname: String,
    val appVersion: String,
    val latencyMs: Long = 0,
)

@Serializable
data class RemoteComandaLineDto(
    val id: Int? = null,
    val productName: String,
    val quantity: Double,
    val notes: String? = null,
    val modifiersJson: String? = null,
    val status: String = "pending",
    val preparationArea: String? = null,
    val comboSnapshotJson: String? = null,
)

@Serializable
data class RemoteComandaJobRequest(
    val jobId: String,
    val tableName: String? = null,
    val orderNumber: Int,
    val waiterName: String? = null,
    val comandas: List<RemoteComandaLineDto>,
)

@Serializable
data class RemotePrecuentaLineDto(
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
)

@Serializable
data class RemotePrecuentaJobRequest(
    val jobId: String,
    val tableName: String? = null,
    val lines: List<RemotePrecuentaLineDto>,
    val total: Double,
)

@Serializable
data class RemoteDocumentLineDto(
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double,
)

@Serializable
data class RemoteDocumentPaymentDto(
    val method: String,
    val amount: Double,
)

@Serializable
data class RemoteDocumentJobRequest(
    val jobId: String,
    val docType: String,
    val sunatCode: String = "",
    val number: String,
    val issueDate: String,
    val issueTime: String? = null,
    val companyName: String,
    val companyLegalName: String? = null,
    val companyRuc: String,
    val companyAddress: String? = null,
    val companyPhone: String? = null,
    val companyEmail: String? = null,
    val companyWebsite: String? = null,
    val companyLogoUrl: String? = null,
    val branchName: String? = null,
    val clientName: String? = null,
    val clientDocNumber: String? = null,
    val items: List<RemoteDocumentLineDto>,
    val subtotal: Double,
    val taxAmount: Double,
    val total: Double,
    val currency: String,
    val payments: List<RemoteDocumentPaymentDto>,
    val legendText: String? = null,
    val qrData: String? = null,
    val sunatHash: String? = null,
)

@Serializable
data class RemoteTestJobRequest(
    val jobId: String,
    val kind: String,
)

fun newPrintJobId(): String = UUID.randomUUID().toString()

fun ComandaLine.toRemoteDto() = RemoteComandaLineDto(
    id = id,
    productName = productName,
    quantity = quantity,
    notes = notes,
    modifiersJson = modifiersJson,
    status = status.backendValue,
    preparationArea = preparationArea,
    comboSnapshotJson = comboSnapshotJson,
)

fun PrecuentaData.toRemoteJob(jobId: String) = RemotePrecuentaJobRequest(
    jobId = jobId,
    tableName = tableName,
    lines = lines.map {
        RemotePrecuentaLineDto(
            productName = it.productName,
            quantity = it.quantity,
            unitPrice = it.unitPrice,
        )
    },
    total = total,
)

fun SalePrintData.toRemoteJob(jobId: String) = RemoteDocumentJobRequest(
    jobId = jobId,
    docType = docType,
    sunatCode = sunatCode,
    number = number,
    issueDate = issueDate,
    issueTime = issueTime,
    companyName = companyName,
    companyLegalName = companyLegalName,
    companyRuc = companyRuc,
    companyAddress = companyAddress,
    companyPhone = companyPhone,
    companyEmail = companyEmail,
    companyWebsite = companyWebsite,
    companyLogoUrl = companyLogoUrl,
    branchName = branchName,
    clientName = clientName,
    clientDocNumber = clientDocNumber,
    items = items.map {
        RemoteDocumentLineDto(
            description = it.description,
            quantity = it.quantity,
            unitPrice = it.unitPrice,
            total = it.total,
        )
    },
    subtotal = subtotal,
    taxAmount = taxAmount,
    total = total,
    currency = currency,
    payments = payments.map { RemoteDocumentPaymentDto(method = it.method, amount = it.amount) },
    legendText = legendText,
    qrData = qrData,
    sunatHash = sunatHash,
)
