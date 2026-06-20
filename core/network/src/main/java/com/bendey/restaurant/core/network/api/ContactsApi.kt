package com.bendey.restaurant.core.network.api

import com.bendey.restaurant.core.network.dto.ContactDataResponseDto
import com.bendey.restaurant.core.network.dto.ContactDto
import com.bendey.restaurant.core.network.dto.CreateContactRequestDto
import com.bendey.restaurant.core.network.dto.ListResponseDto
import com.bendey.restaurant.core.network.dto.SuccessResponseDto
import com.bendey.restaurant.core.network.dto.UpdateContactRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ContactsApi {
    @GET("/api/contacts")
    suspend fun listContacts(
        @Query("q") query: String = "",
        @Query("type") type: String = "customer",
        @Query("status") status: String? = null,
    ): ListResponseDto<ContactDto>

    @GET("/api/contacts/{id}")
    suspend fun getContact(@Path("id") id: Int): ContactDataResponseDto

    @POST("/api/contacts")
    suspend fun createContact(@Body body: CreateContactRequestDto): ContactDataResponseDto

    @PUT("/api/contacts/{id}")
    suspend fun updateContact(
        @Path("id") id: Int,
        @Body body: UpdateContactRequestDto,
    ): ContactDataResponseDto

    @DELETE("/api/contacts/{id}")
    suspend fun deleteContact(@Path("id") id: Int): SuccessResponseDto

    @PATCH("/api/contacts/{id}/toggle")
    suspend fun toggleContact(@Path("id") id: Int): SuccessResponseDto
}
