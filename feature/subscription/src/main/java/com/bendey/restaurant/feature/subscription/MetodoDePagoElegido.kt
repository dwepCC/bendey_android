package com.bendey.restaurant.feature.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.catalog.resolvePublicAssetUrl
import com.bendey.restaurant.core.domain.subscription.PaymentConfig

/**
 * LOS DATOS DEL METODO QUE EL CLIENTE ACABA DE ELEGIR, y solo de ese.
 *
 * En el celular esto pesa más que en el escritorio: mostrar los dos QR y además las cuentas
 * bancarias llenaría la pantalla de datos que no va a usar, justo cuando necesita uno solo. Los
 * métodos salen del Panel Central ya filtrados a los activos.
 */
@Composable
fun MetodoDePagoElegido(
    metodo: String,
    cfg: PaymentConfig,
    assetsBaseUrl: String?,
    modifier: Modifier = Modifier,
) {
    var ampliado by remember { mutableStateOf<QrDelMetodo?>(null) }
    val qr = qrDelMetodo(metodo, cfg)
    val info = infoDelMetodo(metodo, cfg)
    val cuentas = cfg.bankAccounts

    // Sin QR y sin cuentas no hay nada útil que mostrar: el bloque desaparece en vez de dejar un
    // recuadro vacío que parece un error de carga.
    if (qr == null && cuentas.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BendeyColors.SurfaceVariant)
            .padding(BendeySpacing.sm),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs),
    ) {
        if (qr != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // SIN MARCO NI RELLENO ALREDEDOR DEL CODIGO: el QR ya trae su propio margen, y cada
                // borde extra es superficie que la cámara no aprovecha. Al tocarlo se abre a pantalla
                // completa, que es como se escanea desde otro teléfono.
                AsyncImage(
                    model = resolvePublicAssetUrl(assetsBaseUrl, qr.url),
                    contentDescription = "Código QR de " + qr.marca,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(132.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable { ampliado = qr },
                )
                if (info.isNotBlank()) {
                    Text(
                        conNegritas(info),
                        style = MaterialTheme.typography.bodyMedium,
                        color = BendeyColors.OnSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = BendeyColors.OnSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "  Cuentas para " + (if (metodo == "deposit") "depósito" else "transferencia"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = BendeyColors.OnSurface,
                )
            }
            cuentas.forEach { c ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BendeyColors.Surface)
                        .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xs),
                ) {
                    Text(
                        if (c.currency.isNotBlank()) c.bank + " · " + c.currency else c.bank,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    if (c.accountNumber.isNotBlank()) {
                        Text(
                            c.accountNumber,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                    if (c.cci.isNotBlank()) {
                        Text(
                            "CCI " + c.cci,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                    if (c.holder.isNotBlank()) {
                        Text(
                            c.holder,
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    ampliado?.let { qrAmpliado ->
        VisorDeImagen(
            url = resolvePublicAssetUrl(assetsBaseUrl, qrAmpliado.url),
            titulo = qrAmpliado.marca,
            cuadrada = true,
            onClose = { ampliado = null },
        )
    }
}

/**
 * Una imagen a pantalla completa sobre fondo oscuro.
 *
 * Sirve para las dos cosas que el cliente necesita mirar de cerca: el QR —que se abre para que OTRO
 * teléfono lo escanee, así que lo único que importa es el tamaño del código— y el comprobante que
 * él mismo subió.
 */
@Composable
fun VisorDeImagen(
    url: String,
    titulo: String,
    onClose: () -> Unit,
    cuadrada: Boolean = false,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6000000))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = url,
                contentDescription = titulo,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(BendeySpacing.lg)
                    .fillMaxWidth()
                    .then(if (cuadrada) Modifier.aspectRatio(1f) else Modifier)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(BendeySpacing.xs),
            )
            Text(
                titulo,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp),
            )
            Text(
                "Toca para cerrar",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
            )
        }
    }
}

/** Un método que se cobra escaneando. El resto se paga contra una cuenta bancaria. */
data class QrDelMetodo(val marca: String, val url: String)

private fun qrDelMetodo(metodo: String, cfg: PaymentConfig): QrDelMetodo? = when {
    metodo == "yape" && cfg.yapeQrUrl.isNotBlank() -> QrDelMetodo("Yape", cfg.yapeQrUrl)
    metodo == "plin" && cfg.plinQrUrl.isNotBlank() -> QrDelMetodo("Plin", cfg.plinQrUrl)
    else -> null
}

/** El número y el titular, tal como se cargaron en el Panel Central. */
private fun infoDelMetodo(metodo: String, cfg: PaymentConfig): String = when (metodo) {
    "yape" -> cfg.yapeInfo.trim()
    "plin" -> cfg.plinInfo.trim()
    else -> ""
}

/**
 * Texto con *asteriscos* para negrita, como en WhatsApp.
 *
 * Es el formato que quien carga estos datos ya escribe todos los días, así que no hay nada nuevo que
 * aprender para resaltar un número. Se construye un `AnnotatedString`, no HTML: el texto sale de un
 * campo editable del Panel Central y nunca se interpreta como marcado.
 */
internal fun conNegritas(texto: String): AnnotatedString = buildAnnotatedString {
    // Se recorre con `findAll` y no con `split`: a diferencia de JavaScript, el `split` de Kotlin
    // descarta los grupos capturados, así que partir por esta expresión borraría justo el texto que
    // había que resaltar.
    var cursor = 0
    NEGRITA.findAll(texto).forEach { coincidencia ->
        append(texto.substring(cursor, coincidencia.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(coincidencia.groupValues[1]) }
        cursor = coincidencia.range.last + 1
    }
    append(texto.substring(cursor))
}

private val NEGRITA = Regex("\\*([^*\\n]+)\\*")
