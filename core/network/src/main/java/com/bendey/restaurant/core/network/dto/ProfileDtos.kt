package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: Int,
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    @SerialName("role_id") val roleId: Int? = null,
    @SerialName("role_name") val roleName: String? = null,
    @SerialName("branch_id") val branchId: Int? = null,
    @SerialName("branch_name") val branchName: String? = null,
    val active: Boolean = true,
)

@Serializable
data class UserProfileResponseDto(
    val data: UserProfileDto,
)

@Serializable
data class UpdateProfileRequestDto(
    val name: String,
    val email: String,
    val phone: String = "",
)

@Serializable
data class ChangePasswordRequestDto(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)
