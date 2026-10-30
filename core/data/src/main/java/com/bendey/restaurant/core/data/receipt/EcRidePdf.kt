package com.bendey.restaurant.core.data.receipt

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.bendey.restaurant.core.domain.billing.SalePrintData
import com.bendey.restaurant.core.domain.locale.TenantLocale
import com.bendey.restaurant.core.domain.locale.TenantLocaleHolder
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.oned.Code128Writer
import java.text.NumberFormat
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * PDF de Ecuador dibujado EN EL DISPOSITIVO con los datos de la venta (no depende del facturador ni de que el
 * SRI responda): A4 con el diseño del RIDE y ticket de 80 mm con el formato de la referencia. Es la misma
 * maqueta (en mm) que ya dibujan el ERP y Resto Tauri en `receiptPdfEc.ts`.
 *
 * Mientras el SRI no autorice, «NÚMERO AUTORIZACIÓN» muestra la clave de acceso (el número con que se envió);
 * cuando llega la autorización, el código y su fecha y hora.
 */
internal object EcRidePdf {
    private const val MM = 72f / 25.4f

    // ── A4 ────────────────────────────────────────────────────────────────────────────────────────
    private const val PAGE_W = 210f
    private const val PAGE_H = 297f
    private const val M = 5f
    private const val CONTENT_W = PAGE_W - 2 * M
    private const val GAP = 4f
    private const val COL_W = (CONTENT_W - GAP) / 2
    private const val HEADER_MIN_H = 77f
    private const val PT = 9f
    private const val PT_H = 10.5f
    private const val PT_SM = 8f
    private const val LINE = 4.1f
    private const val PAD = 2f
    private const val BOTTOM = PAGE_H - 10

    // ── Ticket ────────────────────────────────────────────────────────────────────────────────────
    const val TICKET_WIDTH_MM = 80f
    private const val T_M = 4f
    private const val T_LINE = 3.9f
    private const val T_BODY = 8f
    private const val T_TITLE = 10f

    /** Barras del Code128 de la clave de acceso: ancho total en módulos y tramos negros (inicio, largo). */
    internal fun code128Runs(key: String): Pair<Int, List<Pair<Int, Int>>>? = runCatching {
        val m = Code128Writer().encode(key, BarcodeFormat.CODE_128, 0, 1, mapOf(EncodeHintType.MARGIN to 0))
        val runs = mutableListOf<Pair<Int, Int>>()
        var x = 0
        while (x < m.width) {
            if (m[x, 0]) {
                val start = x
                while (x < m.width && m[x, 0]) x++
                runs += start to (x - start)
            } else {
                x++
            }
        }
        m.width to runs
    }.getOrNull()

    /** Parte un texto sin espacios (un código de producto) en líneas que caben en `w` mm. */
    internal fun breakLong(text: String, w: Float, measure: (String) -> Float): List<String> {
        if (text.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        var cur = StringBuilder()
        for (ch in text) {
            if (cur.isNotEmpty() && measure(cur.toString() + ch) > w) {
                out += cur.toString()
                cur = StringBuilder().append(ch)
            } else {
                cur.append(ch)
            }
        }
        if (cur.isNotEmpty()) out += cur.toString()
        return out
    }

    /** Parte por palabras en líneas de a lo sumo `w` mm; una palabra más larga que `w` se corta. */
    internal fun wrap(text: String, w: Float, measure: (String) -> Float): List<String> {
        val out = mutableListOf<String>()
        for (para in text.split('\n')) {
            var cur = ""
            for (word in para.split(' ').filter { it.isNotEmpty() }) {
                val cand = if (cur.isEmpty()) word else "$cur $word"
                if (measure(cand) <= w) {
                    cur = cand
                    continue
                }
                if (cur.isNotEmpty()) out += cur
                if (measure(word) <= w) {
                    cur = word
                } else {
                    val parts = breakLong(word, w, measure)
                    out += parts.dropLast(1)
                    cur = parts.lastOrNull().orEmpty()
                }
            }
            if (cur.isNotEmpty() || para.isBlank()) out += cur
        }
        return out
    }

    // ── Entrada pública ──────────────────────────────────────────────────────────────────────────

    fun renderA4(data: SalePrintData, money: NumberFormat, logo: Bitmap?, locale: TenantLocale = TenantLocaleHolder.current): PdfDocument {
        val doc = PdfDocument()
        val sheet = Sheet(doc, PAGE_W, PAGE_H)
        sheet.start()
        A4Painter(data, money, logo, locale, sheet).draw()
        sheet.finish()
        return doc
    }

    /** Ticket: se mide en una primera pasada y se dibuja en un rollo del alto exacto. */
    fun renderTicket(
        data: SalePrintData,
        money: NumberFormat,
        logo: Bitmap?,
        widthMm: Float = TICKET_WIDTH_MM,
        locale: TenantLocale = TenantLocaleHolder.current,
    ): PdfDocument {
        val probeDoc = PdfDocument()
        val probe = Sheet(probeDoc, widthMm, 800f)
        probe.start()
        val measured = TicketPainter(data, money, logo, locale, probe, widthMm).draw()
        probe.finish()
        probeDoc.close()

        val height = max(60f, kotlin.math.ceil(measured))
        val doc = PdfDocument()
        val sheet = Sheet(doc, widthMm, height)
        sheet.start()
        TicketPainter(data, money, logo, locale, sheet, widthMm).draw()
        sheet.finish()
        return doc
    }

    // ── Soporte de dibujo (todo en mm) ───────────────────────────────────────────────────────────

    private class Sheet(val doc: PdfDocument, val wMm: Float, val hMm: Float) {
        private var page: PdfDocument.Page? = null
        private var number = 0
        lateinit var canvas: Canvas

        fun start() {
            number++
            val info = PdfDocument.PageInfo.Builder((wMm * MM).roundToInt(), (hMm * MM).roundToInt(), number).create()
            val p = doc.startPage(info)
            page = p
            canvas = p.canvas
        }

        fun finish() {
            page?.let { doc.finishPage(it) }
            page = null
        }

        fun next() {
            finish()
            start()
        }
    }

    private enum class Align { LEFT, CENTER, RIGHT }

    private abstract class Ink(val sheet: Sheet) {
        private val normals = HashMap<Float, Paint>()
        private val bolds = HashMap<Float, Paint>()

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.25f * MM
            color = Color.BLACK
        }
        val gray = Paint().apply {
            style = Paint.Style.FILL
            color = Color.rgb(242, 242, 242)
        }
        val black = Paint().apply {
            style = Paint.Style.FILL
            color = Color.BLACK
        }

        fun font(pt: Float, bold: Boolean): Paint {
            val cache = if (bold) bolds else normals
            return cache.getOrPut(pt) {
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = pt
                    color = Color.BLACK
                    typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
                }
            }
        }

        fun width(s: String, pt: Float, bold: Boolean): Float = font(pt, bold).measureText(s) / MM

        fun text(s: String, x: Float, y: Float, pt: Float, bold: Boolean = false, align: Align = Align.LEFT) {
            val p = font(pt, bold)
            val w = p.measureText(s) / MM
            val xx = when (align) {
                Align.LEFT -> x
                Align.CENTER -> x - w / 2
                Align.RIGHT -> x - w
            }
            sheet.canvas.drawText(s, xx * MM, y * MM, p)
        }

        fun wrap(s: String, w: Float, pt: Float, bold: Boolean): List<String> =
            EcRidePdf.wrap(s, w) { width(it, pt, bold) }

        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            sheet.canvas.drawLine(x1 * MM, y1 * MM, x2 * MM, y2 * MM, stroke)
        }

        fun rect(x: Float, y: Float, w: Float, h: Float, fillGray: Boolean = false) {
            val r = RectF(x * MM, y * MM, (x + w) * MM, (y + h) * MM)
            if (fillGray) sheet.canvas.drawRect(r, gray)
            sheet.canvas.drawRect(r, stroke)
        }

        fun roundRect(x: Float, y: Float, w: Float, h: Float) {
            sheet.canvas.drawRoundRect(RectF(x * MM, y * MM, (x + w) * MM, (y + h) * MM), 1.5f * MM, 1.5f * MM, stroke)
        }

        fun image(bmp: Bitmap, x: Float, y: Float, w: Float, h: Float) {
            sheet.canvas.drawBitmap(bmp, null, RectF(x * MM, y * MM, (x + w) * MM, (y + h) * MM), Paint(Paint.FILTER_BITMAP_FLAG))
        }

        /** Dibuja las barras del Code128 como rectángulos (vectorial, nítido a cualquier zoom). */
        fun barcode(key: String, x: Float, y: Float, w: Float, h: Float): Boolean {
            val (modules, runs) = code128Runs(key) ?: return false
            val unit = w / modules
            for ((start, len) in runs) {
                sheet.canvas.drawRect(
                    (x + start * unit) * MM, y * MM, (x + (start + len) * unit) * MM, (y + h) * MM, black,
                )
            }
            return true
        }
    }

    private fun fit(bmp: Bitmap, maxW: Float, maxH: Float): Pair<Float, Float> {
        val ratio = min(maxW / bmp.width, maxH / bmp.height)
        return bmp.width * ratio to bmp.height * ratio
    }

    // ── A4 (RIDE) ─────────────────────────────────────────────────────────────────────────────────

    private class A4Painter(
        val data: SalePrintData,
        val money: NumberFormat,
        val logo: Bitmap?,
        val locale: TenantLocale,
        sheet: Sheet,
    ) : Ink(sheet) {
        var y = M

        /** Escribe «Etiqueta: valor» (etiqueta en negrita) con salto de línea; devuelve la y siguiente. */
        fun labelValue(x: Float, yy: Float, w: Float, label: String, value: String, size: Float = PT): Float {
            text(label, x, yy, size, bold = true)
            val lw = width("$label ", size, true) + 1
            val first = wrap(value, max(10f, w - lw), size, false)
            if (first.isEmpty()) return yy + LINE
            text(first[0], x + lw, yy, size)
            var y2 = yy + LINE
            val rest = first.drop(1).joinToString(" ")
            if (rest.isNotEmpty()) {
                for (l in wrap(rest, w, size, false)) {
                    text(l, x, y2, size)
                    y2 += LINE
                }
            }
            return y2
        }

        fun draw() {
            val ec = data.ec
            val key = data.accessKey?.trim().orEmpty()
            val authorized = EcRideModel.isAuthorized(data)

            // Cabecera: logo y recuadro de la empresa (izquierda)
            var leftY = M
            if (logo != null) {
                val (w, h) = fit(logo, 53f, 21f)
                image(logo, M + (COL_W - w) / 2, M, w, h)
                leftY = M + 24
            }
            val legal = data.companyLegalName?.trim().orEmpty()
            val razon = legal.ifEmpty { data.companyName.trim() }
            val comercial = if (legal.isNotEmpty()) data.companyName.trim() else ""
            val matriz = (ec?.matrizAddress ?: data.companyAddress).orEmpty().trim()
            val estab = data.branchAddress.orEmpty().trim()
            val boxTop = leftY
            var ly = boxTop + PAD + 3
            val innerW = COL_W - 2 * PAD
            fun heading(txt: String) {
                for (l in wrap(txt, innerW, PT_H, true)) {
                    text(l, M + PAD, ly, PT_H, bold = true)
                    ly += LINE + 0.6f
                }
            }
            if (comercial.isNotEmpty() && !comercial.equals(razon, ignoreCase = true)) heading(comercial)
            heading(razon)
            ly = labelValue(M + PAD, ly, innerW, "Dirección Matriz:", matriz)
            if (estab.isNotEmpty() && estab != matriz) ly = labelValue(M + PAD, ly, innerW, "Dirección Establecimiento:", estab)
            data.companyPhone?.let { ly = labelValue(M + PAD, ly, innerW, "Teléfono:", it) }
            data.companyEmail?.let { ly = labelValue(M + PAD, ly, innerW, "Correo:", it) }
            ec?.specialContributor?.let { ly = labelValue(M + PAD, ly, innerW, "Contribuyente Especial Nro.", it) }
            ly = labelValue(M + PAD, ly, innerW, "OBLIGADO A LLEVAR CONTABILIDAD:", if (ec?.accountingRequired == true) "SÍ" else "NO")
            ec?.withholdingAgent?.let { ly = labelValue(M + PAD, ly, innerW, "Agente de Retención Resolución No.", it) }
            ec?.rimpeLegend?.let {
                for (l in wrap(it, innerW, PT, true)) {
                    text(l, M + PAD, ly, PT, bold = true)
                    ly += LINE
                }
            }
            val leftBoxH = ly - boxTop + PAD
            val headerH = max(HEADER_MIN_H, leftBoxH + (boxTop - M))
            roundRect(M, boxTop, COL_W, max(leftBoxH, headerH - (boxTop - M)))

            // Recuadro del comprobante (derecha)
            val rx = M + COL_W + GAP
            roundRect(rx, M, COL_W, headerH)
            var ry = M + 6
            text("RUC: ${data.companyRuc}", rx + PAD + 1, ry, PT_H, bold = true)
            ry += LINE + 2.5f
            text(EcRideModel.RIDE_TITLES[data.sunatCode] ?: "COMPROBANTE", rx + COL_W / 2, ry, PT_H, bold = true, align = Align.CENTER)
            ry += LINE + 2.5f
            text("N°: ${data.number}", rx + PAD + 1, ry, PT_H, bold = true)
            ry += LINE + 2
            text("NÚMERO AUTORIZACIÓN", rx + PAD + 1, ry, PT, bold = true)
            ry += LINE
            val authText = EcRideModel.authText(data)
            val authW = COL_W - 2 * PAD - 2
            // La clave (49 dígitos) no cabe en una línea a 8 pt de todos modos: se encoge hasta que quepa.
            var authPt = PT_SM
            while (authPt > 5f && width(authText, authPt, false) > authW) authPt -= 0.25f
            text(authText, rx + PAD + 1, ry, authPt)
            ry += LINE + 1.5f
            text("FECHA Y HORA DE AUTORIZACIÓN", rx + PAD + 1, ry, PT, bold = true)
            text(if (authorized) ec?.authorizedAt.orEmpty() else "", rx + COL_W - PAD - 1, ry, PT, align = Align.RIGHT)
            ry += LINE + 1
            ry = labelValue(rx + PAD + 1, ry, authW, "AMBIENTE:", EcRideModel.environmentLabel(data))
            ry = labelValue(rx + PAD + 1, ry, authW, "EMISIÓN:", "NORMAL")
            ry += 0.5f
            text("CLAVE DE ACCESO", rx + COL_W / 2, ry, PT_H, bold = true, align = Align.CENTER)
            ry += 2.5f
            if (key.isNotEmpty()) {
                barcode(key, rx + PAD + 1, ry, COL_W - 2 * PAD - 2, 11f)
                ry += 14.5f
                var keyPt = PT_SM
                while (keyPt > 5f && width(key, keyPt, false) > authW) keyPt -= 0.25f
                text(key, rx + COL_W / 2, ry, keyPt, align = Align.CENTER)
            }

            y = M + headerH + 5

            drawBuyer()
            drawAffected()
            drawDetail()
            drawFooter()
        }

        fun drawBuyer() {
            val cons = EcRideModel.isFinalConsumer(data)
            val rows: List<List<Pair<String, String>>> = listOf(
                listOf("Razón Social / Nombres y Apellidos:" to (data.clientName?.takeIf { it.isNotBlank() } ?: "CONSUMIDOR FINAL")),
                listOf(
                    "Identificación:" to (if (cons) "" else data.clientDocNumber.orEmpty()),
                    "Dirección:" to (if (cons) "" else data.clientAddress.orEmpty()),
                ),
                listOf(
                    "Fecha Emisión:" to data.issueDate,
                    "Teléfono:" to (if (cons) "" else data.clientPhone.orEmpty()),
                ),
            )
            val top = y
            y += PAD + 3
            val half = (CONTENT_W - 2 * PAD) / 2
            for (row in rows) {
                if (row.size == 1) {
                    y = labelValue(M + PAD, y, CONTENT_W - 2 * PAD, row[0].first, row[0].second)
                } else {
                    val y1 = labelValue(M + PAD, y, half - 2, row[0].first, row[0].second)
                    val y2 = labelValue(M + PAD + half, y, half - 2, row[1].first, row[1].second)
                    y = max(y1, y2)
                }
            }
            roundRect(M, top, CONTENT_W, y - top)
            y += 3
        }

        /** NC/ND: comprobante que se modifica y razón. */
        fun drawAffected() {
            if (!EcRideModel.hasAffectedDoc(data)) return
            val top = y
            y += PAD + 3
            y = labelValue(M + PAD, y, CONTENT_W - 2 * PAD, "Comprobante que se modifica:", "FACTURA ${data.affectedDocNumber}")
            data.affectedIssueDate?.let {
                y = labelValue(M + PAD, y, CONTENT_W - 2 * PAD, "Fecha Emisión (Comprobante a modificar):", it)
            }
            data.noteReason?.let { y = labelValue(M + PAD, y, CONTENT_W - 2 * PAD, "Razón de modificación:", it) }
            roundRect(M, top, CONTENT_W, y - top)
            y += 3
        }

        private class Col(val h: String, var w: Float, val align: Align)

        fun drawDetail() {
            val cols = listOf(
                Col("Código", 22f, Align.LEFT),
                Col("Descripción", 0f, Align.LEFT),
                Col("Cant.", 16f, Align.LEFT),
                Col("P. U.", 22f, Align.RIGHT),
                Col("Descuento", 22f, Align.RIGHT),
                Col("Total", 22f, Align.RIGHT),
            )
            cols[1].w = CONTENT_W - cols.sumOf { it.w.toDouble() }.toFloat()
            val xs = FloatArray(cols.size)
            var acc = M
            cols.forEachIndexed { i, c ->
                xs[i] = acc
                acc += c.w
            }

            fun header() {
                rect(M, y, CONTENT_W, 7f, fillGray = true)
                cols.forEachIndexed { i, c ->
                    val tx = if (c.align == Align.RIGHT) xs[i] + c.w - PAD else xs[i] + PAD
                    text(c.h, tx, y + 4.8f, PT, bold = true, align = c.align)
                    if (i > 0) line(xs[i], y, xs[i], y + 7f)
                }
                y += 7f
            }
            header()
            for (it in data.items) {
                val descLines = wrap(it.description.trim().ifEmpty { "—" }, cols[1].w - 2 * PAD, PT, false)
                val codeLines = breakLong(it.code.trim(), cols[0].w - 2 * PAD) { s -> width(s, PT, false) }
                val rowH = max(7f, max(descLines.size, codeLines.size) * LINE + 3)
                if (y + rowH > BOTTOM) {
                    sheet.next()
                    y = M
                    header()
                }
                val a = EcRideModel.lineAmounts(it)
                rect(M, y, CONTENT_W, rowH)
                for (i in 1 until cols.size) line(xs[i], y, xs[i], y + rowH)
                codeLines.forEachIndexed { i, l -> text(l, xs[0] + PAD, y + 4.8f + i * LINE, PT) }
                descLines.forEachIndexed { i, l -> text(l, xs[1] + PAD, y + 4.8f + i * LINE, PT) }
                text(EcRideModel.qty(it.quantity), xs[2] + PAD, y + 4.8f, PT)
                text(EcRideModel.fixed2(a.unitPrice), xs[3] + cols[3].w - PAD, y + 4.8f, PT, align = Align.RIGHT)
                text(EcRideModel.fixed2(a.discount), xs[4] + cols[4].w - PAD, y + 4.8f, PT, align = Align.RIGHT)
                text(EcRideModel.fixed2(a.base), xs[5] + cols[5].w - PAD, y + 4.8f, PT, align = Align.RIGHT)
                y += rowH
            }
            y += 4
        }

        /** Información adicional + forma de pago (izquierda) y totales por tarifa (derecha). */
        fun drawFooter() {
            val adicional = EcRideModel.additionalInfo(data)
            val pagos = data.ec?.payments.orEmpty()
            val totals = EcRideModel.ridTotals(data, locale)

            val totalsH = totals.size * 6 + 2
            var leftH = 0f
            if (adicional.isNotEmpty()) leftH += 7 + adicional.size * LINE + 4
            if (pagos.isNotEmpty()) leftH += 7 + pagos.size * 6 + 2
            if (y + max(totalsH.toFloat(), leftH) > BOTTOM) {
                sheet.next()
                y = M
            }
            val top = y
            val tx = M + (CONTENT_W + GAP) / 2
            val tw = CONTENT_W - (CONTENT_W + GAP) / 2
            var ty = top
            for (t in totals) {
                rect(tx, ty, tw, 6f)
                text(t.label, tx + PAD, ty + 4.3f, if (t.strong) 11f else PT, bold = true)
                text(money.format(t.amount), tx + tw - PAD, ty + 4.3f, if (t.strong) 11f else PT, bold = t.strong, align = Align.RIGHT)
                ty += 6f
            }
            val lw = (CONTENT_W - GAP) / 2
            var ly = top
            if (adicional.isNotEmpty()) {
                rect(M, ly, lw, 7f, fillGray = true)
                text("Información Adicional", M + PAD, ly + 4.8f, PT, bold = true)
                ly += 7f
                val bodyTop = ly
                ly += 1.5f
                for ((label, value) in adicional) {
                    ly = labelValue(M + PAD, ly + 2.5f, lw - 2 * PAD, "$label:", value) - 2.5f
                }
                ly += 2f
                rect(M, bodyTop, lw, ly - bodyTop)
                ly += 4f
            }
            if (pagos.isNotEmpty()) {
                rect(M, ly, lw, 7f, fillGray = true)
                text("Forma de pago SRI", M + PAD, ly + 4.8f, PT, bold = true)
                text("Valor", M + lw - PAD, ly + 4.8f, PT, bold = true, align = Align.RIGHT)
                ly += 7f
                for (p in pagos) {
                    rect(M, ly, lw, 6f)
                    val lab = wrap("${p.code} - ${p.label}", lw - 30, PT, false).firstOrNull().orEmpty()
                    text(lab, M + PAD, ly + 4.3f, PT)
                    text(money.format(p.amount), M + lw - PAD, ly + 4.3f, PT, align = Align.RIGHT)
                    ly += 6f
                }
            }
            y = max(ty, ly) + 6

            data.companyAdditionalNotes?.let { notes ->
                for (l in wrap(notes, CONTENT_W, PT_SM, false)) {
                    if (y > PAGE_H - 6) {
                        sheet.next()
                        y = M
                    }
                    text(l, PAGE_W / 2, y, PT_SM, align = Align.CENTER)
                    y += LINE - 0.6f
                }
            }
        }
    }

    // ── Ticket de 80 mm (formato de la referencia: ticket-sri) ────────────────────────────────────

    private class TicketPainter(
        val data: SalePrintData,
        val money: NumberFormat,
        val logo: Bitmap?,
        val locale: TenantLocale,
        sheet: Sheet,
        val tw: Float,
    ) : Ink(sheet) {
        var y = T_M
        val innerW = tw - 2 * T_M

        fun center(txt: String, size: Float = T_BODY, bold: Boolean = false) {
            for (l in wrap(txt, innerW, size, bold)) {
                text(l, tw / 2, y, size, bold, Align.CENTER)
                y += T_LINE + if (size > T_BODY) 0.6f else 0f
            }
        }

        fun left(label: String, value: String) {
            text(label, T_M, y, T_BODY, bold = true)
            val lw = width("$label ", T_BODY, true) + 0.8f
            val lines = wrap(value, max(8f, innerW - lw), T_BODY, false)
            if (lines.isNotEmpty()) text(lines[0], T_M + lw, y, T_BODY)
            y += T_LINE
            val rest = lines.drop(1).joinToString(" ")
            if (rest.isNotEmpty()) {
                for (l in wrap(rest, innerW, T_BODY, false)) {
                    text(l, T_M, y, T_BODY)
                    y += T_LINE
                }
            }
        }

        fun rule(ch: Char) {
            val w = width(ch.toString(), T_BODY, false)
            text(ch.toString().repeat((innerW / w).toInt()), tw / 2, y, T_BODY, align = Align.CENTER)
            y += T_LINE
        }

        /** Dibuja el ticket y devuelve la y final (mm). */
        fun draw(): Float {
            val ec = data.ec
            val key = data.accessKey?.trim().orEmpty()
            val authNumber = data.authorizationNumber?.trim().orEmpty()

            if (logo != null) {
                val (w, h) = fit(logo, innerW, 16f)
                image(logo, (tw - w) / 2, y, w, h)
                y += h + 2
            }
            val legal = data.companyLegalName?.trim().orEmpty()
            val razon = legal.ifEmpty { data.companyName.trim() }
            if (legal.isNotEmpty() && !data.companyName.trim().equals(razon, ignoreCase = true)) center(data.companyName.trim(), T_TITLE, true)
            center(razon, T_TITLE, true)
            center("RUC: ${data.companyRuc}")
            val matriz = (ec?.matrizAddress ?: data.companyAddress).orEmpty().trim()
            val estab = data.branchAddress.orEmpty().trim()
            if (matriz.isNotEmpty()) center("Matriz: $matriz")
            if (estab.isNotEmpty() && estab != matriz) center("Sucursal: $estab")
            data.companyPhone?.let { center("Tel: $it") }
            y += 1.5f

            center(EcRideModel.TICKET_TITLES[data.sunatCode] ?: "COMPROBANTE ELECTRÓNICO N°:", T_BODY, true)
            center(data.number, T_TITLE, true)
            center("Ambiente: ${EcRideModel.environmentLabel(data)}")
            center("Emisión: NORMAL")
            if (key.isNotEmpty() || authNumber.isNotEmpty()) {
                center("Autorización / Clave:", T_BODY, true)
                for (l in breakLong(authNumber.ifEmpty { key }, innerW) { width(it, T_BODY - 0.5f, false) }) {
                    text(l, tw / 2, y, T_BODY - 0.5f, align = Align.CENTER)
                    y += T_LINE - 0.3f
                }
            }
            if (authNumber.isNotEmpty() && !ec?.authorizedAt.isNullOrBlank()) center("Fecha y hora: ${ec?.authorizedAt}")
            y += 1.5f

            val cons = EcRideModel.isFinalConsumer(data)
            if (!cons && !data.clientDocNumber.isNullOrBlank()) left("RUC/CI:", data.clientDocNumber.orEmpty())
            left("Cliente:", data.clientName?.takeIf { it.isNotBlank() } ?: "CONSUMIDOR FINAL")
            left("Fecha:", data.issueDate)
            if (!cons) data.clientAddress?.let { left("Dir:", it) }
            if (!cons) data.clientPhone?.let { left("Tel:", it) }
            if (EcRideModel.hasAffectedDoc(data)) {
                left("Doc. que modifica:", data.affectedDocNumber.orEmpty())
                data.noteReason?.let { left("Motivo:", it) }
            }
            rule('=')

            // Detalle: Descripción | Cant. | P. U. | Total (sin IVA)
            val xQty = tw - T_M - 36
            val xPu = tw - T_M - 17
            val xTot = tw - T_M
            val descW = xQty - T_M - 8
            text("Descripción", T_M, y, T_BODY, bold = true)
            text("Cant.", xQty, y, T_BODY, bold = true, align = Align.RIGHT)
            text("P. U.", xPu, y, T_BODY, bold = true, align = Align.RIGHT)
            text("Total", xTot, y, T_BODY, bold = true, align = Align.RIGHT)
            y += T_LINE
            rule('-')
            for (it in data.items) {
                val a = EcRideModel.lineAmounts(it)
                val lines = wrap(it.description.trim().ifEmpty { "—" }, descW, T_BODY, false)
                text(EcRideModel.qty(it.quantity), xQty, y, T_BODY, align = Align.RIGHT)
                text(EcRideModel.fixed2(a.unitPrice), xPu, y, T_BODY, align = Align.RIGHT)
                text(EcRideModel.fixed2(a.base), xTot, y, T_BODY, align = Align.RIGHT)
                lines.forEachIndexed { i, l ->
                    text(l, T_M, y, T_BODY)
                    if (i < lines.size - 1) y += T_LINE
                }
                y += T_LINE + 0.6f
            }
            rule('-')

            fun amountRow(label: String, amount: Double, bold: Boolean = false) {
                val pt = if (bold) T_TITLE else T_BODY
                text(label, xPu + 6, y, pt, bold, Align.RIGHT)
                text(money.format(amount), xTot, y, pt, bold, Align.RIGHT)
                y += T_LINE + if (bold) 0.6f else 0f
            }
            val totals = EcRideModel.ticketTotals(data, locale)
            totals.forEach { amountRow(it.label, it.amount) }
            rule('-')
            amountRow("Total", data.total, bold = true)
            y += 1.5f

            val pagos = ec?.payments.orEmpty()
            if (pagos.isNotEmpty()) {
                center("Forma de pago SRI:", T_BODY, true)
                for (p in pagos) center("${p.label}: ${money.format(p.amount)}")
            }
            data.legendText?.takeIf { it.isNotBlank() }?.let { center("Son: $it") }
            data.notes?.let {
                y += 1
                left("Observaciones:", it)
            }
            data.companyAdditionalNotes?.let {
                y += 1
                center(it, T_BODY - 1)
            }
            return y + T_M
        }
    }
}
