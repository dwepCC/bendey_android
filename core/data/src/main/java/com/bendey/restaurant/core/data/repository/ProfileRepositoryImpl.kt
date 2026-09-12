package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.profile.ProfileFormInput
import com.bendey.restaurant.core.domain.profile.ProfileRepository
import com.bendey.restaurant.core.domain.profile.UserProfile
import com.bendey.restaurant.core.network.api.ProfileApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.ChangePasswordRequestDto
import com.bendey.restaurant.core.network.dto.UpdateProfileRequestDto
import com.bendey.restaurant.core.network.dto.UserProfileDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : ProfileRepository {

    private val profileApi: ProfileApi
        get() = tenantRetrofitProvider.create()

    override suspend fun getMyProfile(): AppResult<UserProfile> = apiCall {
        profileApi.getMyProfile().data.toDomain()
    }

    override suspend fun updateMyProfile(input: ProfileFormInput): AppResult<UserProfile> = apiCall {
        profileApi.updateMyProfile(
            UpdateProfileRequestDto(
                name = input.name.trim(),
                email = input.email.trim(),
                phone = input.phone.trim(),
            ),
        ).data.toDomain()
    }

    override suspend fun changeMyPassword(currentPassword: String, newPassword: String): AppResult<Unit> = apiCall {
        profileApi.changeMyPassword(
            ChangePasswordRequestDto(
                currentPassword = currentPassword,
                newPassword = newPassword,
            ),
        )
        Unit
    }
}

private inline fun <T> apiCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: Exception) {
    val mapped = NetworkErrorMapper.map(e)
    AppResult.Error(mapped.message ?: "Error de conexión", mapped)
}

private fun UserProfileDto.toDomain() = UserProfile(
    id = id,
    name = name,
    email = email,
    phone = phone.orEmpty(),
    roleName = roleName.orEmpty(),
    branchName = branchName?.takeIf { it.isNotBlank() },
)
