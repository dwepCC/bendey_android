package com.bendey.restaurant.core.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Miniatura clicable: al tocarla abre el selector de imágenes y sube de inmediato,
 * sin pasar por ningún formulario. Espejo del `QuickImageThumb` de Tauri.
 */
@Composable
fun BendeyQuickImageThumb(
    imageUrl: String?,
    contentDescription: String,
    onImagePicked: suspend (bytes: ByteArray, mimeType: String) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bytes = readQuickThumbBytes(context, it) ?: return@let
            val mime = context.contentResolver.getType(it) ?: "image/jpeg"
            scope.launch {
                uploading = true
                onImagePicked(bytes, mime)
                uploading = false
            }
        }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(BendeyShapeTokens.xs)
            .background(BendeyColors.SurfaceVariant)
            .clickable(enabled = !uploading) { launcher.launch("image/*") },
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                tint = BendeyColors.OnSurfaceVariant,
                modifier = Modifier.size(size / 2),
            )
        }
        if (uploading) {
            CircularProgressIndicator(modifier = Modifier.size(size / 2.5f))
        } else {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = BendeyColors.Primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size / 3.5f),
            )
        }
    }
}

private fun readQuickThumbBytes(context: Context, uri: Uri): ByteArray? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { input ->
        ByteArrayOutputStream().apply { input.copyTo(this) }.toByteArray()
    }
}.getOrNull()
