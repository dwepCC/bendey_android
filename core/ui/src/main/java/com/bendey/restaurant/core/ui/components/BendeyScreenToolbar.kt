package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChipVariant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.domain.restaurant.ProductCategory
import com.bendey.restaurant.core.ui.layout.BendeyFlexibleContentSlot
import com.bendey.restaurant.core.ui.layout.BendeyTabletTokens
import com.bendey.restaurant.core.ui.layout.adaptive.AdaptivePos
import com.bendey.restaurant.core.ui.pos.PosPolishTokens
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.rememberBendeyAdaptiveProfile
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
            .padding(horizontal = BendeySpacing.xxs, vertical = BendeySpacing.xxs),
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
    hasMoreProducts: Boolean = false,
    productsLoadingMore: Boolean = false,
    onLoadMoreProducts: (() -> Unit)? = null,
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
            val categoryListState = rememberLazyListState()
            BendeyLazyColumn(
                modifier = Modifier
                    .widthIn(min = 96.dp, max = 120.dp)
                    .fillMaxHeight()
                    .background(BendeyColors.SurfaceVariant.copy(alpha = 0.45f))
                    .padding(4.dp),
                state = categoryListState,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item {
                    PosCategoryChip(selectedCategoryId == null, "Todos") { onCategorySelect(null) }
                }
                items(categories, key = { it.id }) { category ->
                    PosCategoryChip(selectedCategoryId == category.id, category.name) {
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                hasMoreProducts = hasMoreProducts,
                productsLoadingMore = productsLoadingMore,
                onLoadMoreProducts = onLoadMoreProducts,
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
            hasMoreProducts = hasMoreProducts,
            productsLoadingMore = productsLoadingMore,
            onLoadMoreProducts = onLoadMoreProducts,
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
    hasMoreProducts: Boolean = false,
    productsLoadingMore: Boolean = false,
    onLoadMoreProducts: (() -> Unit)? = null,
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
    val profile = rememberBendeyAdaptiveProfile()
    val isTabletMobileCatalog = PosPolishTokens.isTabletProfile(profile)
    val horizontalPad = if (isTabletMobileCatalog) {
        AdaptivePos.portraitMobileCatalogHorizontalPadding()
    } else {
        12.dp
    }
    val categoryGap = if (isTabletMobileCatalog) AdaptivePos.portraitMobileCategoryChipGap() else 4.dp
    val gridGap = if (isTabletMobileCatalog) AdaptivePos.portraitMobileGridGap() else 6.dp
    val searchHeight = if (isTabletMobileCatalog) AdaptivePos.portraitMobileSearchHeight() else 38.dp

    Column(modifier = modifier.fillMaxSize()) {
        PosCompactSearchField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = searchPlaceholder,
            barcodeScanEnabled = barcodeScanEnabled,
            onBarcodeScanChange = onBarcodeScanChange,
            fieldHeight = searchHeight,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPad, vertical = 2.dp),
        )
        if (categories.isNotEmpty() && onCategorySelect != null) {
            BendeyHorizontalScrollRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentPadding = PaddingValues(horizontal = if (isTabletMobileCatalog) BendeySpacing.sm else 8.dp),
                horizontalArrangement = Arrangement.spacedBy(categoryGap),
                showEdgeFade = true,
            ) {
                PosCategoryChip(selectedCategoryId == null, "Todos") { onCategorySelect(null) }
                categories.forEach { category ->
                    PosCategoryChip(selectedCategoryId == category.id, category.name) {
                        onCategorySelect(category.id)
                    }
                }
            }
        }
        val gridState = rememberLazyGridState()
        if (onLoadMoreProducts != null) {
            BendeyLazyGridLoadMoreEffect(
                gridState = gridState,
                hasMore = hasMoreProducts,
                loadingMore = productsLoadingMore,
                onLoadMore = onLoadMoreProducts,
            )
        }
        BendeyFlexibleContentSlot {
            BoxWithConstraints(modifier = it) {
                val columns = when {
                    profile.isCompact && posCatalogStyle ->
                        AdaptivePos.compactPosProductGridColumns(profile)
                    isTabletMobileCatalog && posCatalogStyle ->
                        AdaptivePos.portraitMobileProductGridColumns(maxWidth.value.toInt())
                    posCatalogStyle ->
                        BendeyTabletTokens.posProductGridColumns(profile, maxWidth)
                    else ->
                        BendeyTabletTokens.productGridColumns(profile, maxWidth)
                }
                BendeyLazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    showScrollHints = false,
                    contentPadding = PaddingValues(
                        start = horizontalPad,
                        end = horizontalPad,
                        top = 2.dp,
                        bottom = gridBottomPadding,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(gridGap),
                    verticalArrangement = Arrangement.spacedBy(gridGap),
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
                                profile = profile,
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
                    if (productsLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                            }
                        }
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
    fieldHeight: Dp = 38.dp,
    barcodeScanEnabled: Boolean = false,
    onBarcodeScanChange: ((Boolean) -> Unit)? = null,
) {
    Row(
        modifier = modifier.height(fieldHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BendeyCompactSearchInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            fieldHeight = fieldHeight,
            modifier = Modifier.weight(1f),
        )
        if (onBarcodeScanChange != null) {
            Box(
                modifier = Modifier
                    .size(fieldHeight)
                    .clip(BendeyShapeTokens.sm)
                    .background(BendeyColors.Primary)
                    .clickable { onBarcodeScanChange(true) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = "Escanear código",
                    tint = BendeyColors.OnPrimary,
                    modifier = Modifier.size(if (fieldHeight >= 44.dp) 22.dp else 20.dp),
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
    fieldHeight: Dp = 38.dp,
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
private fun PosCategoryChip(selected: Boolean, label: String, onClick: () -> Unit) {
    BendeyFilterChip(
        selected = selected,
        onClick = onClick,
        variant = BendeyFilterChipVariant.Pos,
        label = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) },
    )
}
