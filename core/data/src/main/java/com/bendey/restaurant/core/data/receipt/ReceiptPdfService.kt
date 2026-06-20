package com.bendey.restaurant.core.data.receipt

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.bendey.restaurant.core.domain.billing.SalePrintData
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
) {
    private val cacheDir: File
        get() = File(context.cacheDir, "receipts").also { it.mkdirs() }

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

    fun uriFor(file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

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
        val contentWidth = pageWidthPt - margin * 2

        val lines = buildLines(data, money, format)
        val wrapped = lines.flatMap { (text, bold) ->
            val paint = if (bold) titlePaint else bodyPaint
            wrapText(text, paint, contentWidth).map { it to bold }
        }
        val contentHeight = margin * 2 + wrapped.size * lineHeight
        val pageHeight = when (format) {
            ReceiptPdfFormat.TICKET -> contentHeight.toInt().coerceAtLeast(320)
            ReceiptPdfFormat.A4 -> 842
        }

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        var y = margin + titleSize
        wrapped.forEach { (text, bold) ->
            val paint = when {
                bold && text.length <= 24 -> titlePaint
                bold -> boldBodyPaint
                else -> bodyPaint
            }
            val x = if (format == ReceiptPdfFormat.A4 && bold && text == data.companyName.ifBlank { "EMPRESA" }) {
                (pageWidthPt - paint.measureText(text)) / 2f
            } else {
                margin
            }
            page.canvas.drawText(text, x, y, paint)
            y += lineHeight
        }
        document.finishPage(page)
        return document
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

    private fun buildLines(
        data: SalePrintData,
        money: NumberFormat,
        format: ReceiptPdfFormat,
    ): List<Pair<String, Boolean>> {
        val lines = mutableListOf<Pair<String, Boolean>>()
        fun add(text: String, bold: Boolean = false) {
            lines += text to bold
        }
        val divider = if (format == ReceiptPdfFormat.TICKET) "------------------------" else "— — — — — — — — — — — —"
        add(data.companyName.ifBlank { "EMPRESA" }, bold = true)
        if (data.companyRuc.isNotBlank()) add("RUC: ${data.companyRuc}")
        data.companyAddress?.takeIf { it.isNotBlank() }?.let { add(it) }
        data.branchName?.takeIf { it.isNotBlank() }?.let { add(it) }
        add(divider)
        add(data.docType.ifBlank { "Comprobante" }, bold = true)
        add(data.number, bold = true)
        add(divider)
        add("Fecha: ${data.issueDate}")
        data.clientName?.takeIf { it.isNotBlank() }?.let { add("Cliente: $it") }
        data.clientDocNumber?.takeIf { it.isNotBlank() }?.let { add("Doc: $it") }
        add(divider)
        if (format == ReceiptPdfFormat.A4) {
            add("Cant.  Descripción                    Importe")
            add(divider)
        }
        data.items.forEach { item ->
            val qty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else "%.2f".format(item.quantity)
            if (format == ReceiptPdfFormat.A4) {
                add(String.format("%-5s  %-28s  %s", qty, item.description.take(28), money.format(item.total)))
            } else {
                add("$qty x ${item.description}")
                add("   ${money.format(item.total)}")
            }
        }
        add(divider)
        if (data.taxAmount > 0) {
            add("Subtotal: ${money.format(data.subtotal)}")
            add("IGV: ${money.format(data.taxAmount)}")
        }
        add("TOTAL: ${money.format(data.total)}", bold = true)
        if (data.payments.isNotEmpty()) {
            add(divider)
            data.payments.forEach { p ->
                add("${p.method}: ${money.format(p.amount)}")
            }
        }
        data.legendText?.takeIf { it.isNotBlank() }?.let {
            add(divider)
            add(it)
        }
        return lines
    }
}
