package com.bendey.restaurant.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.domain.restaurant.ProductCategory
import com.bendey.restaurant.core.ui.layout.BendeyTabletTokens
import com.bendey.restaurant.core.ui.motion.BendeyMotion
import java.text.NumberFormat

@Composable
fun BendeyScreenToolbar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
            }
        }
        actions()
    }
}

@Composable
fun BendeyPosCatalogPane(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    categories: List<ProductCategory>,
    selectedCategoryId: Int?,
    onCategorySelect: (Int?) -> Unit,
    products: List<PosProduct>,
    currency: NumberFormat,
    assetsBaseUrl: String?,
    onProductClick: (PosProduct) -> Unit,
    modifier: Modifier = Modifier,
    sidebarCategories: Boolean = false,
    compactCards: Boolean = true,
    posCatalogStyle: Boolean = false,
    barcodeScanEnabled: Boolean = false,
    onBarcodeScanChange: ((Boolean) -> Unit)? = null,
    gridBottomPadding: Dp = 12.dp,
    searchPlaceholder: String = "Buscar producto…",
) {
    if (sidebarCategories) {
        Row(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(min = 96.dp, max = 120.dp)
                    .fillMaxHeight()
                    .background(BendeyColors.SurfaceVariant.copy(alpha = 0.45f))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item {
                    CategoryChip(selectedCategoryId == null, "Todos") { onCategorySelect(null) }
                }
                items(categories, key = { it.id }) { category ->
                    CategoryChip(selectedCategoryId == category.id, category.name) {
                        onCategorySelect(category.id)
                    }
                }
            }
            CatalogGridBody(
                searchQuery = searchQuery,
                onSearchChange = onSearchChange,
                products = products,
                currency = currency,
                assetsBaseUrl = assetsBaseUrl,
                onProductClick = onProductClick,
                modifier = Modifier.weight(1f),
                compactCards = compactCards,
                posCatalogStyle = posCatalogStyle,
                barcodeScanEnabled = barcodeScanEnabled,
                onBarcodeScanChange = onBarcodeScanChange,
                gridBottomPadding = gridBottomPadding,
                searchPlaceholder = searchPlaceholder,
            )
        }
    } else {
        CatalogGridBody(
            searchQuery = searchQuery,
            onSearchChange = onSearchChange,
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelect = onCategorySelect,
            products = products,
            currency = currency,
            assetsBaseUrl = assetsBaseUrl,
            onProductClick = onProductClick,
            modifier = modifier,
            compactCards = compactCards,
            posCatalogStyle = posCatalogStyle,
            barcodeScanEnabled = barcodeScanEnabled,
            onBarcodeScanChange = onBarcodeScanChange,
            gridBottomPadding = gridBottomPadding,
            searchPlaceholder = searchPlaceholder,
        )
    }
}

@Composable
private fun CatalogGridBody(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    products: List<PosProduct>,
    currency: NumberFormat,
    assetsBaseUrl: String?,
    onProductClick: (PosProduct) -> Unit,
    modifier: Modifier = Modifier,
    categories: List<ProductCategory> = emptyList(),
    selectedCategoryId: Int? = null,
    onCategorySelect: ((Int?) -> Unit)? = null,
    compactCards: Boolean = true,
    posCatalogStyle: Boolean = false,
    barcodeScanEnabled: Boolean = false,
    onBarcodeScanChange: ((Boolean) -> Unit)? = null,
    gridBottomPadding: Dp = 12.dp,
    searchPlaceholder: String = "Buscar producto…",
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (posCatalogStyle) {
            BendeyTabletTokens.posProductGridColumns(maxWidth)
        } else {
            BendeyTabletTokens.productGridColumns(maxWidth)
        }
        Column(modifier = Modifier.fillMaxSize()) {
            PosCompactSearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = searchPlaceholder,
                barcodeScanEnabled = barcodeScanEnabled,
                onBarcodeScanChange = onBarcodeScanChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            )
            if (categories.isNotEmpty() && onCategorySelect != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CategoryChip(selectedCategoryId == null, "Todos") { onCategorySelect(null) }
                    categories.forEach { category ->
                        CategoryChip(selectedCategoryId == category.id, category.name) {
                            onCategorySelect(category.id)
                        }
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 2.dp, bottom = gridBottomPadding),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(products, key = { it.id }) { product ->
                    if (posCatalogStyle) {
                        BendeyPosProductCard(
                            name = product.name,
                            price = product.salePrice,
                            currency = currency,
                            imageUrl = product.imageUrl,
                            assetsBaseUrl = assetsBaseUrl,
                            onClick = { onProductClick(product) },
                            enabled = product.availableForSale,
                        )
                    } else {
                        BendeyProductCard(
                            name = product.name,
                            price = product.salePrice,
                            currency = currency,
                            imageUrl = product.imageUrl,
                            assetsBaseUrl = assetsBaseUrl,
                            onClick = { onProductClick(product) },
                            badges = product.productBadges(),
                            compact = compactCards,
                            enabled = product.availableForSale,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PosCompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar producto…",
    barcodeScanEnabled: Boolean = false,
    onBarcodeScanChange: ((Boolean) -> Unit)? = null,
) {
    Row(
        modifier = modifier.height(38.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BendeyCompactSearchInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier.weight(1f),
        )
        if (onBarcodeScanChange != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = if (barcodeScanEnabled) BendeyColors.Primary else BendeyColors.OnSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Switch(
                    checked = barcodeScanEnabled,
                    onCheckedChange = onBarcodeScanChange,
                    modifier = Modifier.height(28.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BendeyColors.OnPrimary,
                        checkedTrackColor = BendeyColors.Primary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun BendeyCompactSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = BendeyColors.OnSurface),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {}),
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, BendeyColors.Outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = BendeyColors.OnSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BendeyColors.OnSurfaceVariant.copy(alpha = 0.75f),
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun CategoryChip(selected: Boolean, label: String, onClick: () -> Unit) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) BendeyColors.Primary else BendeyColors.SurfaceVariant,
        animationSpec = BendeyMotion.ChipColorSpring,
        label = "chipBg",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) BendeyColors.OnPrimary else BendeyColors.NavInactive,
        animationSpec = BendeyMotion.ChipColorSpring,
        label = "chipLabel",
    )
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = containerColor,
            selectedLabelColor = labelColor,
            containerColor = containerColor,
            labelColor = labelColor,
        ),
        shape = RoundedCornerShape(8.dp),
    )
}
