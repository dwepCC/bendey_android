package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseDto(
    val id: Int,
    @SerialName("doc_type") val docType: String,
    val series: String,
    val number: String,
    @SerialName("issue_date") val issueDate: String,
    @SerialName("contact_id") val contactId: Int? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    val subtotal: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Double = 0.0,
    val total: Double = 0.0,
    val currency: String = "PEN",
    val status: String = "received",
    val notes: String? = null,
)

@Serializable
data class PurchaseListResponseDto(
    val data: List<PurchaseDto> = emptyList(),
    val total: Int? = null,
)

@Serializable
data class PurchaseItemDto(
    @SerialName("product_id") val productId: Int? = null,
    val code: String = "",
    val description: String = "",
    val unit: String = "",
    val quantity: Double = 0.0,
    @SerialName("unit_cost") val unitCost: Double = 0.0,
    @SerialName("igv_affectation_type") val igvAffectationType: String = "10",
    @SerialName("price_includes_igv") val priceIncludesIgv: Boolean = false,
    val serials: List<String> = emptyList(),
)

@Serializable
data class PurchaseDetailDataDto(
    val id: Int,
    @SerialName("doc_type") val docType: String,
    val series: String,
    val number: String,
    @SerialName("issue_date") val issueDate: String,
    @SerialName("contact_id") val contactId: Int? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    val subtotal: Double = 0.0,
    @SerialName("tax_amount") val taxAmount: Double = 0.0,
    val total: Double = 0.0,
    val currency: String = "PEN",
    val status: String = "received",
    val notes: String? = null,
    val items: List<PurchaseItemDto> = emptyList(),
)

@Serializable
data class PurchaseDetailResponseDto(
    val data: PurchaseDetailDataDto,
)

@Serializable
data class CreatePurchaseRequestDto(
    @SerialName("branch_id") val branchId: Int? = null,
    @SerialName("contact_id") val contactId: Int,
    @SerialName("doc_type") val docType: String,
    val series: String? = null,
    val number: String,
    @SerialName("issue_date") val issueDate: String,
    val currency: String = "PEN",
    @SerialName("payment_method") val paymentMethod: String? = null,
    val notes: String? = null,
    val items: List<PurchaseItemDto>,
)

@Serializable
data class CreatePurchaseResponseDto(
    val success: Boolean = true,
    val data: PurchaseDto? = null,
)
