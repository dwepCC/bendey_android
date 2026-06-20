package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactDataResponseDto(
    val data: ContactDto,
)

@Serializable
data class CreateContactRequestDto(
    val type: String = "customer",
    @SerialName("doc_type") val docType: String,
    @SerialName("doc_number") val docNumber: String,
    @SerialName("business_name") val businessName: String,
    @SerialName("trade_name") val tradeName: String = "",
    val address: String = "",
    val ubigeo: String? = null,
    val phone: String = "",
    val email: String = "",
)

@Serializable
data class UpdateContactRequestDto(
    val type: String? = null,
    @SerialName("doc_type") val docType: String? = null,
    @SerialName("doc_number") val docNumber: String? = null,
    @SerialName("business_name") val businessName: String? = null,
    @SerialName("trade_name") val tradeName: String? = null,
    val address: String? = null,
    val ubigeo: String? = null,
    val phone: String? = null,
    val email: String? = null,
)

@Serializable
data class ConsultaDniRequestDto(
    val dni: String,
    @SerialName("tenant_ruc") val tenantRuc: String,
)

@Serializable
data class ConsultaRucRequestDto(
    val ruc: String,
    @SerialName("tenant_ruc") val tenantRuc: String,
)

@Serializable
data class ConsultaDniResponseDto(
    val success: Boolean = false,
    @SerialName("nombre_completo") val nombreCompleto: String? = null,
)

@Serializable
data class ConsultaRucResponseDto(
    val success: Boolean = false,
    @SerialName("razon_social") val razonSocial: String? = null,
    val direccion: String? = null,
    val ubigeo: String? = null,
)
