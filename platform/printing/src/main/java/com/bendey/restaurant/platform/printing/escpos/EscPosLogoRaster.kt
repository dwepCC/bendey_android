package com.bendey.restaurant.platform.printing.escpos

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Ancho imprimible en píxeles (58 mm ≈ 384, 80 mm ≈ 576). */
fun escposPrintWidthPx(paperWidth: PaperWidthMm): Int = when (paperWidth) {
    PaperWidthMm.W58 -> 384
    PaperWidthMm.W80 -> 576
}

private fun escposLogoMaxWidthPx(paperWidth: PaperWidthMm): Int = when (paperWidth) {
    PaperWidthMm.W58 -> 360
    PaperWidthMm.W80 -> 520
}

private fun escposLogoMaxHeightPx(paperWidth: PaperWidthMm): Int = when (paperWidth) {
    PaperWidthMm.W58 -> 120
    PaperWidthMm.W80 -> 156
}

/**
 * Convierte un bitmap a raster ESC/POS (GS v 0) centrado en el ancho del rollo.
 * Port de la lógica web en escposRasterImage.ts.
 */
object EscPosLogoRaster {

    fun encode(source: Bitmap, paperWidth: PaperWidthMm): ByteArray? {
        if (source.width < 1 || source.height < 1) return null
        val maxW = escposLogoMaxWidthPx(paperWidth)
        val maxH = escposLogoMaxHeightPx(paperWidth)
        val printW = escposPrintWidthPx(paperWidth)

        val flattened = flattenOnWhite(source)
        try {
            val bounds = findContentBounds(flattened) ?: return null
            val cropW = bounds.right - bounds.left + 1
            val cropH = bounds.bottom - bounds.top + 1
            if (cropW < 1 || cropH < 1) return null

            val scale = min(
                min(maxW.toFloat() / cropW, maxH.toFloat() / cropH),
                printW.toFloat() / cropW,
            ).coerceAtMost(1f)

            var w = max(8, (cropW * scale).roundToInt())
            var h = max(8, (cropH * scale).roundToInt())
            w = ((w + 7) / 8) * 8

            val cropped = Bitmap.createBitmap(flattened, bounds.left, bounds.top, cropW, cropH)
            val scaled = scaleOnWhite(cropped, w, h)
            if (cropped !== flattened && cropped !== scaled) cropped.recycle()

            var gray = toGrayscaleWithDither(scaled, w, h)
            if (scaled !== flattened) scaled.recycle()

            val trimmed = trimEmptyRows(gray, w, h)
            gray = trimmed.first
            h = trimmed.second
            if (h < 1) return null

            val paddedW = printW
            if (w < paddedW) {
                val padded = IntArray(paddedW * h) { 255 }
                val leftPad = (paddedW - w) / 2
                for (row in 0 until h) {
                    for (col in 0 until w) {
                        padded[row * paddedW + leftPad + col] = gray[row * w + col]
                    }
                }
                return grayscaleToEscPosRaster(padded, paddedW, h)
            }
            return grayscaleToEscPosRaster(gray, w, h)
        } finally {
            if (flattened !== source && !flattened.isRecycled) flattened.recycle()
        }
    }

    /** PNG/WebP con transparencia: los píxeles alpha=0 suelen traer RGB negro; hay que aplanar sobre blanco. */
    private fun flattenOnWhite(source: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        return out
    }

    private fun scaleOnWhite(source: Bitmap, width: Int, height: Int): Bitmap {
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        Canvas(out).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, null, Rect(0, 0, width, height), paint)
        }
        return out
    }

    private data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

    private fun findContentBounds(bitmap: Bitmap): Bounds? {
        val w = bitmap.width
        val h = bitmap.height
        var top = h
        var bottom = -1
        var left = w
        var right = -1
        val threshold = 245
        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = bitmap.getPixel(x, y)
                val a = Color.alpha(pixel)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                if (a < 20 || (r >= threshold && g >= threshold && b >= threshold)) continue
                if (y < top) top = y
                if (y > bottom) bottom = y
                if (x < left) left = x
                if (x > right) right = x
            }
        }
        return if (bottom < top || right < left) null else Bounds(left, top, right, bottom)
    }

    private fun toGrayscaleWithDither(pixels: Bitmap, width: Int, height: Int): IntArray {
        val threshold = 105
        val gray = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val c = pixels.getPixel(x, y)
                val alpha = Color.alpha(c) / 255f
                val lum = (
                    Color.red(c) * 0.299 +
                        Color.green(c) * 0.587 +
                        Color.blue(c) * 0.114
                    ).roundToInt()
                gray[y * width + x] = (255 * (1f - alpha) + lum * alpha).roundToInt()
            }
        }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val oldPixel = gray[idx]
                val newPixel = if (oldPixel < threshold) 0 else 255
                gray[idx] = newPixel
                val quantError = oldPixel - newPixel
                val factor = 0.5
                if (x + 1 < width) gray[idx + 1] += ((quantError * 7) / 16 * factor).roundToInt()
                if (x - 1 >= 0 && y + 1 < height) gray[idx + width - 1] += ((quantError * 3) / 16 * factor).roundToInt()
                if (y + 1 < height) gray[idx + width] += ((quantError * 5) / 16 * factor).roundToInt()
                if (x + 1 < width && y + 1 < height) gray[idx + width + 1] += ((quantError * 1) / 16 * factor).roundToInt()
            }
        }
        return gray
    }

    private fun trimEmptyRows(gray: IntArray, width: Int, height: Int): Pair<IntArray, Int> {
        fun rowHasInk(y: Int) = (0 until width).any { gray[y * width + it] == 0 }
        var top = 0
        var bottom = height - 1
        while (top < height && !rowHasInk(top)) top++
        while (bottom > top && !rowHasInk(bottom)) bottom--
        val h = bottom - top + 1
        if (h <= 0) return gray to height
        val trimmed = IntArray(width * h)
        for (y in 0 until h) {
            System.arraycopy(gray, (top + y) * width, trimmed, y * width, width)
        }
        return trimmed to h
    }

    /**
     * Alto máximo de cada banda raster. Varias ticketeras clon abortan un `GS v 0` con imágenes
     * altas (buffer raster limitado por comando / timeout de transporte) y vuelcan el resto de
     * los bytes como TEXTO (los caracteres basura tras el logo). Enviar la imagen en tiras — cada
     * una con su propio `GS v 0` — evita ese límite sin cambiar el resultado visual: las bandas se
     * imprimen contiguas (el comando avanza el papel exactamente su alto, sin costuras ni LF entre
     * bandas). Un logo más bajo que esto genera una sola banda = comportamiento idéntico al previo.
     */
    private const val RASTER_BAND_ROWS = 64

    private fun grayscaleToEscPosRaster(gray: IntArray, width: Int, height: Int): ByteArray {
        val bitmapWidthBytes = width / 8
        val xL = (bitmapWidthBytes and 0xFF).toByte()
        val xH = ((bitmapWidthBytes shr 8) and 0xFF).toByte()

        // Empaqueta toda la imagen (row-major, 8 px por byte). Con ancho múltiplo de 8, cada fila
        // ocupa exactamente `bitmapWidthBytes`, así que las bandas son cortes contiguos del array.
        val data = ByteArray(bitmapWidthBytes * height)
        var offset = 0
        var i = 0
        while (i < gray.size) {
            var byte = 0
            for (j in 0 until 8) {
                if (i + j < gray.size && gray[i + j] == 0) {
                    byte = byte or (1 shl (7 - j))
                }
            }
            data[offset++] = byte.toByte()
            i += 8
        }

        val bandCount = (height + RASTER_BAND_ROWS - 1) / RASTER_BAND_ROWS
        val out = ByteArray(data.size + bandCount * 8) // 8 bytes de encabezado por banda
        var o = 0
        var top = 0
        while (top < height) {
            val bandH = min(RASTER_BAND_ROWS, height - top)
            out[o++] = 0x1D
            out[o++] = 0x76
            out[o++] = 0x30
            out[o++] = 0
            out[o++] = xL
            out[o++] = xH
            out[o++] = (bandH and 0xFF).toByte()
            out[o++] = ((bandH shr 8) and 0xFF).toByte()
            val start = top * bitmapWidthBytes
            val len = bandH * bitmapWidthBytes
            System.arraycopy(data, start, out, o, len)
            o += len
            top += RASTER_BAND_ROWS
        }
        return out
    }
}
