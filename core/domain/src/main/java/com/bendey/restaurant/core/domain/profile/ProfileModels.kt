package com.bendey.restaurant.core.domain.profile

/** Perfil del usuario autenticado — solo accesible con login completo (email/contraseña). */
data class UserProfile(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String,
    val roleName: String,
    val branchName: String?,
)

data class ProfileFormInput(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
)

fun UserProfile.toFormInput() = ProfileFormInput(
    name = name,
    email = email,
    phone = phone,
)
