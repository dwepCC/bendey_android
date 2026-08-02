package com.bendey.restaurant.core.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

@Composable
fun BendeyMenuQrPreview(
    menuUrl: String,
    title: String,
    qrPngBase64: String? = null,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: (() -> Unit)? = null,
    regenerating: Boolean = false,
    canManage: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val qrBitmap = remember(qrPngBase64) {
        qrPngBase64?.let { raw ->
            val payload = if (raw.startsWith("data:")) raw.substringAfter(',') else raw
            runCatching {
                val bytes = Base64.decode(payload, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    BendeyManagementCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
        ) {
            qrBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "QR menú $title",
                    modifier = Modifier.size(192.dp),
                )
            }
            Text(
                menuUrl,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = BendeyColors.OnSurfaceVariant,
            )
            BendeyTextButton(text = "Copiar enlace", onClick = onCopy, modifier = Modifier.fillMaxWidth())
            BendeyTextButton(text = "Compartir", onClick = onShare, modifier = Modifier.fillMaxWidth())
            // Solo si hay imagen: sin QR no hay nada que descargar y el botón sería
            // una promesa vacía.
            if (qrPngBase64 != null) {
                BendeyTextButton(
                    text = "Descargar QR",
                    onClick = { saveMenuQrToDownloads(context, qrPngBase64, title) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (onRegenerate != null && canManage) {
                BendeyTextButton(
                    text = if (regenerating) "Regenerando…" else "Regenerar token",
                    onClick = onRegenerate,
                    enabled = !regenerating,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Guarda la imagen del QR en la galería del dispositivo.
 *
 * Va a MediaStore (Imágenes → Pictures) y no a un archivo de la app: así el dueño lo
 * encuentra desde su galería para imprimirlo o mandarlo por donde quiera, sin permisos
 * de almacenamiento en Android 10+.
 */
fun saveMenuQrToDownloads(context: Context, qrPngBase64: String, title: String) {
    val payload = if (qrPngBase64.startsWith("data:")) qrPngBase64.substringAfter(',') else qrPngBase64
    val name = "qr-menu-" + title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-') + ".png"
    val result = runCatching {
        val bytes = Base64.decode(payload, Base64.DEFAULT)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            values,
        ) ?: error("sin destino")
        resolver.openOutputStream(uri).use { out ->
            requireNotNull(out).write(bytes)
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
    val msg = if (result.isSuccess) "QR guardado en tus imágenes" else "No se pudo guardar el QR"
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
}

fun copyMenuUrl(context: Context, url: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Menú digital", url))
    android.widget.Toast.makeText(context, "Enlace copiado", android.widget.Toast.LENGTH_SHORT).show()
}

fun shareMenuUrl(context: Context, url: String, title: String) {
    val text = "Menú digital — $title\n$url"
    val whatsapp = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        setPackage("com.whatsapp")
    }
    val generic = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        context.startActivity(whatsapp)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent.createChooser(generic, "Compartir menú"))
        } catch (_: ActivityNotFoundException) {
            copyMenuUrl(context, url)
        }
    }
}
