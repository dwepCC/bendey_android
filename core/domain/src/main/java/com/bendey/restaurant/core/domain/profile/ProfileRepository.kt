package com.bendey.restaurant.core.domain.profile

import com.bendey.restaurant.core.domain.model.AppResult

interface ProfileRepository {
    suspend fun getMyProfile(): AppResult<UserProfile>
    suspend fun updateMyProfile(input: ProfileFormInput): AppResult<UserProfile>
    suspend fun changeMyPassword(currentPassword: String, newPassword: String): AppResult<Unit>
}
