package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.ChangePasswordRequestDto
import com.bendey.restaurant.core.network.dto.SuccessResponseDto
import com.bendey.restaurant.core.network.dto.UpdateProfileRequestDto
import com.bendey.restaurant.core.network.dto.UserProfileResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

/** Perfil del usuario autenticado — solo el login completo (email/contraseña), nunca PIN. */
interface ProfileApi {
    @GET("/api/profile/me")
    suspend fun getMyProfile(): UserProfileResponseDto

    @PUT("/api/profile/me")
    suspend fun updateMyProfile(@Body body: UpdateProfileRequestDto): UserProfileResponseDto

    @POST("/api/profile/me/password")
    suspend fun changeMyPassword(@Body body: ChangePasswordRequestDto): SuccessResponseDto
}
