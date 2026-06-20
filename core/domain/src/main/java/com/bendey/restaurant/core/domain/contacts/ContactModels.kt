package com.bendey.restaurant.core.domain.contacts

enum class ContactDocType(val code: String, val label: String) {
    SIN_RUC("0", "Sin RUC"),
    DNI("1", "DNI"),
    RUC("6", "RUC"),
    CE("4", "Carnet extranjería"),
    PASAPORTE("7", "Pasaporte"),
    ;

    companion object {
        fun fromCode(code: String?): ContactDocType =
            entries.firstOrNull { it.code == code?.trim() } ?: RUC

        fun supportsConsulta(code: String): Boolean {
            val c = fromCode(code)
            return c == DNI || c == RUC
        }
    }
}

data class CustomerContact(
    val id: Int,
    val docType: String,
    val docNumber: String,
    val businessName: String,
    val tradeName: String?,
    val address: String?,
    val ubigeo: String?,
    val phone: String?,
    val email: String?,
    val active: Boolean,
) {
    val displayName: String get() = businessName.ifBlank { tradeName.orEmpty() }.ifBlank { docNumber }
    val docLabel: String get() = ContactDocType.fromCode(docType).label
}

data class ContactFormInput(
    val docType: ContactDocType = ContactDocType.RUC,
    val docNumber: String = "",
    val businessName: String = "",
    val tradeName: String = "",
    val address: String = "",
    val ubigeo: String = "",
    val phone: String = "",
    val email: String = "",
)

data class ConsultaRucResult(
    val success: Boolean,
    val razonSocial: String? = null,
    val direccion: String? = null,
    val ubigeo: String? = null,
)

data class ConsultaDniResult(
    val success: Boolean,
    val nombreCompleto: String? = null,
)

fun CustomerContact.toFormInput() = ContactFormInput(
    docType = ContactDocType.fromCode(docType),
    docNumber = docNumber,
    businessName = businessName,
    tradeName = tradeName.orEmpty(),
    address = address.orEmpty(),
    ubigeo = ubigeo.orEmpty(),
    phone = phone.orEmpty(),
    email = email.orEmpty(),
)

fun sanitizeDocNumber(docType: ContactDocType, raw: String): String {
    val trimmed = raw.trim()
    return when (docType) {
        ContactDocType.RUC -> trimmed.filter { it.isDigit() }.take(11)
        ContactDocType.DNI -> trimmed.filter { it.isDigit() }.take(8)
        else -> trimmed.take(20)
    }
}
