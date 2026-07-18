package com.bendey.restaurant.feature.configuracion

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bendey.restaurant.core.designsystem.components.BendeyManagementCard
import com.bendey.restaurant.core.designsystem.components.BendeySectionTitle
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.digitalmenu.MenuStyleVariant
import com.bendey.restaurant.core.domain.digitalmenu.MenuThemeMode
import com.bendey.restaurant.core.ui.components.BendeyHexColorPicker
import com.bendey.restaurant.core.ui.components.BendeyLazyColumn
import com.bendey.restaurant.core.ui.components.BendeyMenuQrPreview
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeySwitchRow
import com.bendey.restaurant.core.ui.components.BendeyTextField
import com.bendey.restaurant.core.ui.components.copyMenuUrl
import com.bendey.restaurant.core.ui.components.parseHexColorOrNull
import com.bendey.restaurant.core.ui.components.shareMenuUrl
import com.bendey.restaurant.core.ui.layout.rememberBendeyLazyListContentPadding
import java.io.ByteArrayOutputStream

private const val MAX_BACKGROUND_IMAGE_BYTES = 1_500_000

private fun readBytes(context: android.content.Context, uri: Uri): ByteArray? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { input ->
        ByteArrayOutputStream().apply { input.copyTo(this) }.toByteArray()
    }
}.getOrNull()

private fun toDataUrl(bytes: ByteArray, mime: String): String {
    val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    return "data:$mime;base64,$b64"
}

private fun dataUrlToBitmap(dataUrl: String): android.graphics.Bitmap? {
    val comma = dataUrl.indexOf(',')
    if (comma == -1) return null
    return runCatching {
        val bytes = android.util.Base64.decode(dataUrl.substring(comma + 1), android.util.Base64.DEFAULT)
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

@Composable
fun MenuDigitalTab(
    modifier: Modifier = Modifier,
    viewModel: MenuDigitalViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.snackMessage) {
        state.snackMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSnack()
        }
    }

    if (state.loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val backgroundImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bytes = readBytes(context, it) ?: return@let
            if (bytes.size > MAX_BACKGROUND_IMAGE_BYTES) {
                Toast.makeText(context, "La imagen supera el tamaño máximo (1.5 MB)", Toast.LENGTH_SHORT).show()
                return@let
            }
            val mime = context.contentResolver.getType(it) ?: "image/jpeg"
            viewModel.setBackgroundImageBase64(toDataUrl(bytes, mime))
        }
    }

    BendeyLazyColumn(
        state = rememberLazyListState(),
        modifier = modifier.fillMaxSize(),
        contentPadding = rememberBendeyLazyListContentPadding(horizontal = BendeySpacing.md, top = BendeySpacing.md),
        verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
    ) {
        item {
            BendeyManagementCard {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
                    BendeySectionTitle(text = "Menú digital")
                    Text(
                        "Catálogo web, pedidos desde QR de mesa y pedido público sin mesa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )
                    BendeySwitchRow(
                        label = "Menú habilitado",
                        checked = state.menuEnabled,
                        onCheckedChange = viewModel::setMenuEnabled,
                        enabled = state.canManage,
                    )
                    BendeyTextField(
                        value = state.welcomeTitle,
                        onValueChange = viewModel::setWelcomeTitle,
                        label = "Título de bienvenida",
                        enabled = state.canManage,
                    )
                    BendeyTextField(
                        value = state.welcomeDescription,
                        onValueChange = viewModel::setWelcomeDescription,
                        label = "Descripción",
                        enabled = state.canManage,
                    )
                    BendeySwitchRow(
                        label = "Mostrar precios",
                        checked = state.showPrices,
                        onCheckedChange = viewModel::setShowPrices,
                        enabled = state.canManage,
                    )
                    BendeySwitchRow(
                        label = "Pedido para recoger (sin mesa)",
                        checked = state.publicTakeawayEnabled,
                        onCheckedChange = viewModel::setPublicTakeawayEnabled,
                        enabled = state.canManage,
                    )
                    BendeySwitchRow(
                        label = "Delivery (sin mesa)",
                        checked = state.publicDeliveryEnabled,
                        onCheckedChange = viewModel::setPublicDeliveryEnabled,
                        enabled = state.canManage,
                    )
                    BendeyTextField(
                        value = state.whatsapp,
                        onValueChange = viewModel::setWhatsapp,
                        label = "WhatsApp (enlace o número)",
                        enabled = state.canManage,
                    )
                    BendeyPrimaryButton(
                        text = if (state.saving) "Guardando…" else "Guardar configuración",
                        onClick = viewModel::save,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canManage && !state.saving,
                    )
                }
            }
        }
        item {
            BendeyManagementCard {
                Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = BendeyColors.Primary)
                        BendeySectionTitle(text = "Apariencia de la carta")
                    }
                    Text(
                        "Por defecto se usa el tema oficial Bendey Resto. Personalízalo si lo necesitas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BendeyColors.OnSurfaceVariant,
                    )

                    ThemeModeSelector(
                        mode = state.themeMode,
                        enabled = state.canManage,
                        onSelect = viewModel::setThemeMode,
                    )

                    if (state.isCustomTheme) {
                        BendeyHexColorPicker(
                            label = "Color principal",
                            value = state.primaryColorHex,
                            onValueChange = viewModel::setPrimaryColorHex,
                            enabled = state.canManage,
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                            Text("Imagen de fondo (opcional)", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm)) {
                                val backgroundBitmap = remember(state.backgroundImageBase64) {
                                    state.backgroundImageBase64.takeIf { it.isNotBlank() }?.let(::dataUrlToBitmap)
                                }
                                if (backgroundBitmap != null) {
                                    Image(
                                        bitmap = backgroundBitmap.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                                    )
                                }
                                BendeyPrimaryButton(
                                    text = if (state.backgroundImageBase64.isNotBlank()) "Cambiar imagen" else "Elegir imagen",
                                    onClick = { backgroundImageLauncher.launch("image/*") },
                                    enabled = state.canManage,
                                )
                            }
                            if (state.backgroundImageBase64.isNotBlank()) {
                                Text(
                                    "Quitar imagen de fondo",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BendeyColors.Primary,
                                    modifier = Modifier.clickable(enabled = state.canManage) { viewModel.clearBackgroundImage() },
                                )
                            }
                        }

                        StyleVariantSelector(
                            variant = state.styleVariant,
                            enabled = state.canManage,
                            onSelect = viewModel::setStyleVariant,
                        )
                    }

                    ThemePreview(
                        colorHex = state.previewColorHex,
                        backgroundDataUrl = if (state.isCustomTheme) state.backgroundImageBase64.ifBlank { null } else null,
                        isGlass = state.isGlassStyle,
                    )

                    BendeyPrimaryButton(
                        text = if (state.saving) "Guardando…" else "Guardar apariencia",
                        onClick = viewModel::save,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canManage && !state.saving,
                    )
                }
            }
        }
        if (state.menuUrl.isNotBlank()) {
            item {
                BendeyMenuQrPreview(
                    menuUrl = state.menuUrl,
                    title = "principal",
                    onCopy = { copyMenuUrl(context, state.menuUrl) },
                    onShare = { shareMenuUrl(context, state.menuUrl, "principal") },
                    onRegenerate = viewModel::regenerateToken,
                    regenerating = state.regenerating,
                    canManage = state.canManage,
                )
            }
        }
        state.error?.let { error ->
            item {
                Text(error, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    mode: MenuThemeMode,
    enabled: Boolean,
    onSelect: (MenuThemeMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
        SelectableChip(
            label = "Bendey oficial",
            selected = mode == MenuThemeMode.BENDEY_DEFAULT,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(MenuThemeMode.BENDEY_DEFAULT) },
        )
        SelectableChip(
            label = "Personalizado",
            selected = mode == MenuThemeMode.CUSTOM,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(MenuThemeMode.CUSTOM) },
        )
    }
}

@Composable
private fun StyleVariantSelector(
    variant: MenuStyleVariant,
    enabled: Boolean,
    onSelect: (MenuStyleVariant) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
        Text("Estilo visual", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
            SelectableChip(
                label = "Vidrio esmerilado",
                selected = variant == MenuStyleVariant.GLASS,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(MenuStyleVariant.GLASS) },
            )
            SelectableChip(
                label = "Sólido",
                selected = variant == MenuStyleVariant.SOLID,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(MenuStyleVariant.SOLID) },
            )
        }
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) BendeyColors.PrimaryContainer else BendeyColors.Surface)
            .border(
                width = 1.dp,
                color = if (selected) BendeyColors.Primary else BendeyColors.Outline,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) BendeyColors.OnPrimaryContainer else BendeyColors.OnSurfaceVariant,
        )
    }
}

@Composable
private fun ThemePreview(colorHex: String, backgroundDataUrl: String?, isGlass: Boolean) {
    val accent = parseHexColorOrNull(colorHex) ?: BendeyColors.Primary

    Column {
        Text("Vista previa", style = MaterialTheme.typography.labelMedium, color = BendeyColors.OnSurfaceVariant)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, BendeyColors.Outline, RoundedCornerShape(16.dp)),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (backgroundDataUrl == null) accent.copy(alpha = 0.08f) else Color(0xFF1A1A1A),
                        ),
                ) {
                    val previewBitmap = remember(backgroundDataUrl) { backgroundDataUrl?.let(::dataUrlToBitmap) }
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(72.dp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(BendeySpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("M", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Tu Restaurante",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (backgroundDataUrl != null) Color.White else BendeyColors.OnSurface,
                            )
                            Text(
                                "Menú digital",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (backgroundDataUrl != null) Color.White.copy(alpha = 0.8f) else BendeyColors.OnSurfaceVariant,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(50))
                                .background(accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accent.copy(alpha = 0.04f))
                        .padding(BendeySpacing.sm),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isGlass && backgroundDataUrl != null) Color.White.copy(alpha = 0.7f) else Color.White,
                            )
                            .border(1.dp, BendeyColors.Outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(BendeySpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent.copy(alpha = 0.18f)),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(width = 96.dp, height = 8.dp).clip(RoundedCornerShape(4.dp)).background(BendeyColors.OnSurfaceVariant.copy(alpha = 0.25f)))
                            Box(modifier = Modifier.size(width = 48.dp, height = 8.dp).clip(RoundedCornerShape(4.dp)).background(accent))
                        }
                    }
                }
            }
        }
    }
}
