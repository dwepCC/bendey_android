package com.bendey.restaurant.feature.pos

import android.Manifest
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun PosBarcodeScannerSheet(
    open: Boolean,
    onDismiss: () -> Unit,
    onBarcodeDetected: (String) -> Unit,
) {
    if (!open) return
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    DisposableEffect(open) {
        if (open && !hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        onDispose { }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = BendeyShapeTokens.pill,
            color = BendeyColors.Surface,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, end = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BendeyColors.PrimaryContainer, BendeyShapeTokens.md),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = BendeyColors.Primary,
                            )
                        }
                        Text(
                            text = "Escanear código",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            text = "Centra el código dentro del marco",
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                if (!hasPermission) {
                    Text(
                        text = "Permite el acceso a la cámara para escanear productos.",
                        color = BendeyColors.OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 32.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .height(300.dp)
                            .clip(BendeyShapeTokens.xl)
                            .background(Color.Black)
                            .border(1.dp, BendeyColors.Outline.copy(alpha = 0.35f), BendeyShapeTokens.xl),
                        contentAlignment = Alignment.Center,
                    ) {
                        BarcodeCameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            onBarcode = { code ->
                                onBarcodeDetected(code)
                                onDismiss()
                            },
                        )
                        BarcodeScanOverlay(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun BarcodeScanOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scanLine")
    val scanProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanLineProgress",
    )
    val frameColor = BendeyColors.OnPrimary
    val accentColor = BendeyColors.Primary

    Canvas(modifier = modifier) {
        val frameWidth = size.width * 0.82f
        val frameHeight = size.height * 0.42f
        val left = (size.width - frameWidth) / 2f
        val top = (size.height - frameHeight) / 2f
        val corner = 22f
        val stroke = 4f

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset.Zero,
            size = GeoSize(size.width, top),
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(0f, top + frameHeight),
            size = GeoSize(size.width, size.height - top - frameHeight),
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(0f, top),
            size = GeoSize(left, frameHeight),
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(left + frameWidth, top),
            size = GeoSize(size.width - left - frameWidth, frameHeight),
        )

        drawRoundRect(
            color = frameColor.copy(alpha = 0.85f),
            topLeft = Offset(left, top),
            size = GeoSize(frameWidth, frameHeight),
            cornerRadius = CornerRadius(14f, 14f),
            style = Stroke(width = 2f),
        )

        val corners = listOf(
            Offset(left, top) to Offset(left + corner, top),
            Offset(left, top) to Offset(left, top + corner),
            Offset(left + frameWidth, top) to Offset(left + frameWidth - corner, top),
            Offset(left + frameWidth, top) to Offset(left + frameWidth, top + corner),
            Offset(left, top + frameHeight) to Offset(left + corner, top + frameHeight),
            Offset(left, top + frameHeight) to Offset(left, top + frameHeight - corner),
            Offset(left + frameWidth, top + frameHeight) to Offset(left + frameWidth - corner, top + frameHeight),
            Offset(left + frameWidth, top + frameHeight) to Offset(left + frameWidth, top + frameHeight - corner),
        )
        corners.forEach { (start, end) ->
            drawLine(
                color = accentColor,
                start = start,
                end = end,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }

        val lineY = top + (frameHeight * scanProgress)
        drawLine(
            color = accentColor.copy(alpha = 0.9f),
            start = Offset(left + 12f, lineY),
            end = Offset(left + frameWidth - 12f, lineY),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun BarcodeCameraPreview(
    modifier: Modifier = Modifier,
    onBarcode: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    var handled by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                ),
                            )
                            .build(),
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    if (handled) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val raw = barcodes.firstValidRaw()
                            if (raw != null && !handled) {
                                handled = true
                                onBarcode(raw)
                            }
                        }
                        .addOnCompleteListener { imageProxy.close() }
                }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(context))
        },
    )
}

private fun List<Barcode>.firstValidRaw(): String? {
    for (barcode in this) {
        val value = barcode.rawValue?.trim().orEmpty()
        if (value.isNotEmpty()) return value
    }
    return null
}
