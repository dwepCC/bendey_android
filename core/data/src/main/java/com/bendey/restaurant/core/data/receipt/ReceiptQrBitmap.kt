package com.bendey.restaurant.core.data.receipt

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

internal object ReceiptQrBitmap {
    fun encode(data: String, sizePx: Int): Bitmap? {
        if (data.isBlank()) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 0,
            )
            val matrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = matrix.width
            val height = matrix.height
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
            }
        }.getOrNull()
    }
}
