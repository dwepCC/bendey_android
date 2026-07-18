package com.bendey.restaurant.feature.mesas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyMenuQrPreview
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.copyMenuUrl
import com.bendey.restaurant.core.ui.components.shareMenuUrl

@Composable
fun TableMenuQrDialog(
    tableName: String?,
    menuUrl: String,
    qrPngBase64: String?,
    loading: Boolean,
    rotating: Boolean,
    canManage: Boolean,
    onDismiss: () -> Unit,
    onRotate: () -> Unit,
) {
    if (tableName == null) return
    val context = LocalContext.current

    BendeyAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QR menú — $tableName") },
        text = {
            if (loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (menuUrl.isNotBlank()) {
                BendeyMenuQrPreview(
                    menuUrl = menuUrl,
                    title = tableName,
                    qrPngBase64 = qrPngBase64,
                    onCopy = { copyMenuUrl(context, menuUrl) },
                    onShare = { shareMenuUrl(context, menuUrl, tableName) },
                    onRegenerate = onRotate,
                    regenerating = rotating,
                    canManage = canManage,
                )
            }
        },
        confirmButton = {
            BendeyPrimaryButton(text = "Cerrar", onClick = onDismiss, fillWidth = false)
        },
    )
}
