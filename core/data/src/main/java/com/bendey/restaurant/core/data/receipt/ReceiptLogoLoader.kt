package com.bendey.restaurant.core.data.receipt

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class ReceiptLogoLoader @Inject constructor(
    @Named("image") private val okHttpClient: OkHttpClient,
) {
    fun load(logoUrl: String?, maxWidthPx: Int): Bitmap? {
        if (logoUrl.isNullOrBlank()) return null
        val bytes = loadBytes(logoUrl.trim()) ?: return null
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        if (decoded.width <= maxWidthPx) return decoded
        val scale = maxWidthPx.toFloat() / decoded.width.toFloat()
        val height = (decoded.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(decoded, maxWidthPx, height, true)
    }

    private fun loadBytes(url: String): ByteArray? {
        return when {
            url.startsWith("data:", ignoreCase = true) -> decodeDataUrl(url)
            url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true) -> {
                okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) return null
                    response.body?.bytes()
                }
            }
            else -> null
        }
    }

    private fun decodeDataUrl(dataUrl: String): ByteArray? {
        val commaIndex = dataUrl.indexOf(',')
        if (commaIndex < 0) return null
        return runCatching {
            Base64.decode(dataUrl.substring(commaIndex + 1), Base64.DEFAULT)
        }.getOrNull()
    }
}
