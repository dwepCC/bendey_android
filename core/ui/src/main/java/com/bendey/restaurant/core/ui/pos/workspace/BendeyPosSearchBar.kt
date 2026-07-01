package com.bendey.restaurant.core.ui.pos.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.ui.layout.adaptive.AdaptivePos
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile

/**
 * Buscador POS tablet — misma apariencia que [com.bendey.restaurant.core.ui.components.BendeyPosCatalogPane]
 * (fondo blanco, borde suave, iconografía móvil). Solo escala ligera de altura en tablet.
 */
@Composable
fun BendeyPosSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    profile: BendeyAdaptiveProfile,
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar producto…",
    onBarcodeScan: (() -> Unit)? = null,
) {
    val fieldHeight = AdaptivePos.searchBarMinHeight(profile)
    val qrSize = AdaptivePos.searchBarQrSize(profile)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(fieldHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = BendeyColors.OnSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {}),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .height(fieldHeight),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(BendeyShapeTokens.sm)
                        .border(1.dp, BendeyColors.Outline.copy(alpha = 0.45f), BendeyShapeTokens.sm)
                        .background(BendeyColors.Surface)
                        .padding(horizontal = BendeySpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BendeySpacing.sm),
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = BendeyColors.OnSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = BendeyColors.OnSurfaceVariant.copy(alpha = 0.75f),
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Limpiar búsqueda",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            },
        )
        if (query.isEmpty() && onBarcodeScan != null) {
            Box(
                modifier = Modifier
                    .size(qrSize)
                    .clip(BendeyShapeTokens.sm)
                    .background(BendeyColors.Primary)
                    .clickable(onClick = onBarcodeScan),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = "Escanear código",
                    tint = BendeyColors.OnPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
