package com.bendey.restaurant.feature.productos

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import com.bendey.restaurant.core.ui.components.BendeyAlertDialog
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextButton
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.catalog.ModifierGroup
import com.bendey.restaurant.core.domain.catalog.ProductPresentation
import com.bendey.restaurant.core.domain.catalog.resolvePublicAssetUrl
import com.bendey.restaurant.core.domain.products.CatalogSection
import com.bendey.restaurant.core.ui.components.CatalogSectionNav
import com.bendey.restaurant.core.domain.products.ProductFormInput
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField
import java.io.ByteArrayOutputStream

@Composable
fun ProductCatalogSectionNav(
    onOpenModificadores: () -> Unit,
    onOpenAreasPreparacion: () -> Unit = {},
    onOpenCombos: () -> Unit,
) {
    CatalogSectionNav(
        current = CatalogSection.PRODUCTOS,
        onOpenProductos = {},
        onOpenModificadores = onOpenModificadores,
        onOpenAreasPreparacion = onOpenAreasPreparacion,
        onOpenCombos = onOpenCombos,
    )
}

@Composable
fun ProductImageSection(
    form: ProductFormInput,
    tenantBaseUrl: String?,
    onImagePicked: (ByteArray, String) -> Unit,
) {
    val context = LocalContext.current
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bytes = readBytes(context, it) ?: return@let
            val mime = context.contentResolver.getType(it) ?: "image/jpeg"
            onImagePicked(bytes, mime)
        }
    }
    val displayUrl = when {
        form.pendingImageBytes != null -> null
        !form.imageUrl.isNullOrBlank() -> resolvePublicAssetUrl(tenantBaseUrl, form.imageUrl)
        else -> null
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
    ) {
        if (form.pendingImageBytes != null) {
            Text("Imagen lista para subir", style = MaterialTheme.typography.bodySmall, color = BendeyColors.Primary)
        } else if (displayUrl != null) {
            AsyncImage(
                model = displayUrl,
                contentDescription = null,
                modifier = Modifier.size(72.dp).clip(BendeyShapeTokens.xs),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(Icons.Default.Image, contentDescription = null, tint = BendeyColors.OnSurfaceVariant, modifier = Modifier.size(48.dp))
        }
        BendeyPrimaryButton("Elegir imagen", { imageLauncher.launch("image/*") })
    }
}

@Composable
fun ProductPresentationsSection(
    presentations: List<ProductPresentation>,
    onChange: (List<ProductPresentation>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
        presentations.forEachIndexed { index, presentation ->
            Row(horizontalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
                BendeyTextField(
                    presentation.name,
                    { value -> onChange(presentations.mapIndexed { i, p -> if (i == index) p.copy(name = value) else p }) },
                    "Presentación",
                    modifier = Modifier.weight(1f),
                )
                BendeyTextField(
                    presentation.salePrice.toString(),
                    { value -> onChange(presentations.mapIndexed { i, p -> if (i == index) p.copy(salePrice = value.replace(",", ".").toDoubleOrNull() ?: 0.0) else p }) },
                    "Precio",
                    modifier = Modifier.weight(0.7f),
                )
            }
        }
        BendeyTextButton(
            text = "Agregar presentación",
            onClick = { onChange(presentations + ProductPresentation(name = "", salePrice = 0.0)) },
        )
    }
}

@Composable
fun ProductModifiersSection(
    groups: List<ModifierGroup>,
    selectedIds: List<Int>,
    onToggle: (Int) -> Unit,
) {
    if (groups.isEmpty()) {
        Text("No hay grupos de modificadores", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(BendeySpacing.xs)) {
        groups.forEach { group ->
            BendeyFilterChip(
                selected = selectedIds.contains(group.id),
                onClick = { onToggle(group.id) },
                label = { Text(group.name) },
            )
        }
    }
}

private fun readBytes(context: Context, uri: Uri): ByteArray? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { input ->
        ByteArrayOutputStream().apply { input.copyTo(this) }.toByteArray()
    }
}.getOrNull()

private const val EXCEL_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
