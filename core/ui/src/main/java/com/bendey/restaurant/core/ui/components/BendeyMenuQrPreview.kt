package com.bendey.restaurant.core.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
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
