package com.bendey.restaurant.core.data.receipt

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.bendey.restaurant.core.data.export.BendeyExportPaths
import com.bendey.restaurant.core.data.export.BendeyFileShareService
import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.platform.printing.escpos.SunatPrintUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

enum class ReceiptPdfFormat {
    TICKET,
    A4,
}

@Singleton
class ReceiptPdfService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileShareService: BendeyFileShareService,
    private val logoLoader: ReceiptLogoLoader,
) {
    private val cacheDir: File
        get() = BendeyExportPaths.receiptsDir(context)

    fun generate(data: SalePrintData, format: ReceiptPdfFormat): File {
        val suffix = if (format == ReceiptPdfFormat.TICKET) "ticket" else "a4"
        val safeNumber = data.number.replace(Regex("\\s+"), "")
        val file = File(cacheDir, "comprobante-$safeNumber-$suffix.pdf")
        file.outputStream().use { out ->
            val doc = buildPdf(data, format)
            doc.writeTo(out)
            doc.close()
        }
        return file
    }

    fun uriFor(file: File) = fileShareService.uriForFile(file)

    private fun buildPdf(data: SalePrintData, format: ReceiptPdfFormat): PdfDocument {
        val pageWidthPt = when (format) {
            ReceiptPdfFormat.TICKET -> (80f / 25.4f * 72f).toInt()
            ReceiptPdfFormat.A4 -> 595
        }
        val margin = when (format) {
            ReceiptPdfFormat.TICKET -> 12f
            ReceiptPdfFormat.A4 -> 40f
        }
        val titleSize = if (format == ReceiptPdfFormat.TICKET) 11f else 14f
        val bodySize = if (format == ReceiptPdfFormat.TICKET) 9f else 10f
        val lineHeight = if (format == ReceiptPdfFormat.TICKET) 13f else 15f
        val contentWidth = pageWidthPt - margin * 2

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = titleSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = bodySize }
        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = bodySize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val money = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

        val logoMaxWidthPt = contentWidth * 0.42f
        val logoMaxHeightPt = if (format == ReceiptPdfFormat.TICKET) 32f else 48f
        val logoMaxWidthPx = (logoMaxWidthPt * (160f / 72f)).toInt().coerceAtLeast(48)
        val logoBitmap = logoLoader.load(data.companyLogoUrl, logoMaxWidthPx)
        val logoHeightPt = logoBitmap?.let { bmp ->
            val scaledHeight = logoMaxWidthPt * (bmp.height.toFloat() / bmp.width.toFloat())
            scaledHeight.coerceAtMost(logoMaxHeightPt)
        } ?: 0f
        val logoWidthPt = logoBitmap?.let { bmp ->
            if (logoHeightPt <= 0f) 0f
            else logoHeightPt * (bmp.width.toFloat() / bmp.height.toFloat())
        } ?: 0f

        val qrSizePt = if (format == ReceiptPdfFormat.TICKET) 96f else 120f
        val qrBitmap = if (SunatPrintUtils.isElectronicSunatCode(data.sunatCode) && !data.qrData.isNullOrBlank()) {
            ReceiptQrBitmap.encode(data.qrData!!, qrSizePt.toInt().coerceAtLeast(64))
        } else {
            null
        }

        val layoutLines = if (format == ReceiptPdfFormat.TICKET) {
            ReceiptTicketLayout.build(data, money)
        } else {
            ReceiptTicketLayout.build(data, money)
        }

        val wrappedBlocks = layoutLines.map { line ->
            when {
                line.text == "__QR__" -> ReceiptDrawBlock.Qr(qrBitmap)
                else -> {
                    val paint = when {
                        line.bold && line.text.length <= 24 -> titlePaint
                        line.bold -> boldBodyPaint
                        else -> bodyPaint
                    }
                    val wrapped = wrapText(line.text, paint, contentWidth)
                    ReceiptDrawBlock.Text(wrapped, line.align, paint)
                }
            }
        }

        val textLineCount = wrappedBlocks.sumOf { block ->
            when (block) {
                is ReceiptDrawBlock.Text -> block.lines.size
                is ReceiptDrawBlock.Qr -> if (block.bitmap != null) 1 else 0
            }
        }
        val qrExtra = wrappedBlocks.filterIsInstance<ReceiptDrawBlock.Qr>()
            .count { it.bitmap != null } * (qrSizePt + lineHeight)
        val contentHeight = margin * 2 + logoHeightPt + (textLineCount * lineHeight) + qrExtra + lineHeight
        val pageHeight = when (format) {
            ReceiptPdfFormat.TICKET -> contentHeight.toInt().coerceAtLeast(320)
            ReceiptPdfFormat.A4 -> 842.coerceAtLeast(contentHeight.toInt())
        }

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        var y = margin

        logoBitmap?.let { bmp ->
            val drawWidth = logoWidthPt
            val drawHeight = logoHeightPt
            val left = margin + (contentWidth - drawWidth) / 2f
            canvas.drawBitmap(bmp, null, android.graphics.RectF(left, y, left + drawWidth, y + drawHeight), null)
            y += drawHeight + lineHeight * 0.5f
        }

        wrappedBlocks.forEach { block ->
            when (block) {
                is ReceiptDrawBlock.Text -> {
                    block.lines.forEach { text ->
                        val paint = block.paint
                        val x = when (block.align) {
                            ReceiptTextAlign.LEFT -> margin
                            ReceiptTextAlign.CENTER -> (pageWidthPt - paint.measureText(text)) / 2f
                            ReceiptTextAlign.RIGHT -> pageWidthPt - margin - paint.measureText(text)
                        }
                        canvas.drawText(text, x, y + bodySize, paint)
                        y += lineHeight
                    }
                }
                is ReceiptDrawBlock.Qr -> {
                    val bmp = block.bitmap ?: return@forEach
                    y += lineHeight * 0.25f
                    val left = margin + (contentWidth - qrSizePt) / 2f
                    canvas.drawBitmap(
                        bmp,
                        null,
                        android.graphics.RectF(left, y, left + qrSizePt, y + qrSizePt),
                        null,
                    )
                    y += qrSizePt + lineHeight * 0.5f
                }
            }
        }

        document.finishPage(page)
        return document
    }

    private sealed class ReceiptDrawBlock {
        data class Text(
            val lines: List<String>,
            val align: ReceiptTextAlign,
            val paint: Paint,
        ) : ReceiptDrawBlock()

        data class Qr(val bitmap: android.graphics.Bitmap?) : ReceiptDrawBlock()
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        if (paint.measureText(text) <= maxWidth) return listOf(text)
        val words = text.split(' ')
        val result = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "${current} $word"
            if (paint.measureText(candidate) <= maxWidth) {
                if (current.isNotEmpty()) current.append(' ')
                current.append(word)
            } else {
                if (current.isNotEmpty()) {
                    result += current.toString()
                    current = StringBuilder(word)
                } else {
                    result += word.take(ceil(maxWidth / paint.measureText("M")).toInt().coerceAtLeast(1))
                    current = StringBuilder()
                }
            }
        }
        if (current.isNotEmpty()) result += current.toString()
        return result.ifEmpty { listOf(text) }
    }
}
