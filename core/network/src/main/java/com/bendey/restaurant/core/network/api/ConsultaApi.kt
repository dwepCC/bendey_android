package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.ConsultaDniRequestDto
import com.bendey.restaurant.core.network.dto.ConsultaDniResponseDto
import com.bendey.restaurant.core.network.dto.ConsultaRucRequestDto
import com.bendey.restaurant.core.network.dto.ConsultaRucResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface ConsultaApi {
    @POST("/api/consulta/dni")
    suspend fun consultDni(@Body body: ConsultaDniRequestDto): ConsultaDniResponseDto

    @POST("/api/consulta/ruc")
    suspend fun consultRuc(@Body body: ConsultaRucRequestDto): ConsultaRucResponseDto
}
