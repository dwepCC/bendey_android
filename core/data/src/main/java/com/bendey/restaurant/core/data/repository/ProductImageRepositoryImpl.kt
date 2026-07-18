package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.catalog.ProductImageRepository
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.network.dto.ApiErrorDto
import com.bendey.restaurant.core.network.dto.ProductImageUploadResponseDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import com.bendey.restaurant.core.network.BuildConfig
import com.bendey.restaurant.core.network.serialization.ApiJson
import com.bendey.restaurant.core.network.session.NetworkSessionProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductImageRepositoryImpl @Inject constructor(
    private val client: OkHttpClient,
    private val sessionProvider: NetworkSessionProvider,
) : ProductImageRepository {

    /**
     * Origen público para /uploads y /storage — siempre API central (como React `getPublicAssetsBaseUrl`).
     * No usar la URL del tenant: los archivos los sirve el backend Go en api.bendey.cloud.
     */
    override fun tenantAssetsBaseUrl(): String? =
        BuildConfig.CENTRAL_API_URL.trim().trimEnd('/').removeSuffix("/api")

    override suspend fun uploadProductImage(
        productId: Int,
        bytes: ByteArray,
        mimeType: String,
    ): AppResult<String> = uploadImage("products", productId, "product", bytes, mimeType)

    override suspend fun uploadComboImage(
        comboId: Int,
        bytes: ByteArray,
        mimeType: String,
    ): AppResult<String> = uploadImage("combos", comboId, "combo", bytes, mimeType)

    private suspend fun uploadImage(
        resource: String,
        id: Int,
        filePrefix: String,
        bytes: ByteArray,
        mimeType: String,
    ): AppResult<String> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sessionProvider.tenantApiBaseUrl()?.trim()?.trimEnd('/')
                ?: return@withContext AppResult.Error("Tenant API URL no configurada")
            val normalizedBase = baseUrl.removeSuffix("/api")
            val url = "$normalizedBase/api/$resource/$id/image"
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                else -> "jpg"
            }
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    "$filePrefix.$extension",
                    bytes.toRequestBody(mimeType.toMediaType()),
                )
                .build()
            val requestBuilder = Request.Builder()
                .url(url)
                .post(body)
            sessionProvider.token()?.let { requestBuilder.header("Authorization", "Bearer $it") }
            // execute() es bloqueante — por eso toda la función corre en Dispatchers.IO
            // (si no, Android lanza NetworkOnMainThreadException, que no trae mensaje).
            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                // Respuesta cruda de OkHttp (no pasa por Retrofit/HttpException), así que
                // NetworkErrorMapper.map() no sabe parsear el JSON de error — se hace aquí.
                val parsedMessage = runCatching {
                    ApiJson.decodeFromString(ApiErrorDto.serializer(), responseBody).error
                }.getOrNull()?.takeIf { it.isNotBlank() }
                val message = parsedMessage ?: responseBody.ifBlank { "Error al subir imagen (${response.code})" }
                return@withContext AppResult.Error(message, IllegalStateException(message))
            }
            val parsed = ApiJson.decodeFromString(ProductImageUploadResponseDto.serializer(), responseBody)
            AppResult.Success(parsed.imageUrl)
        } catch (e: Exception) {
            val mapped = NetworkErrorMapper.map(e)
            AppResult.Error(mapped.message ?: "Error al subir imagen", mapped)
        }
    }
}
