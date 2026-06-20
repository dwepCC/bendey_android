package com.bendey.restaurant.core.data.receipt

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object ReceiptShareHelper {
    fun sharePdfWhatsApp(context: Context, uri: Uri, message: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val whatsapp = Intent(send).apply { setPackage("com.whatsapp") }
        try {
            context.startActivity(whatsapp)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent.createChooser(send, "Compartir comprobante"))
        }
    }

    fun openPdfExternal(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Ver comprobante"))
    }
}
