package com.bendey.restaurant.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
) {
    if (sidebarCategories) {
        Row(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(min = 108.dp, max = 140.dp)
                    .fillMaxHeight()
                    .background(BendeyColors.SurfaceVariant.copy(alpha = 0.45f))
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
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
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = BendeyTabletTokens.productGridColumns(maxWidth)
        Column(modifier = Modifier.fillMaxSize()) {
            PosCompactSearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            if (categories.isNotEmpty() && onCategorySelect != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 2.dp),
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
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 72.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(products, key = { it.id }) { product ->
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

@Composable
private fun PosCompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        placeholder = { Text("Buscar producto…", style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = BendeyColors.OnSurfaceVariant)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {}),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BendeyColors.Primary,
            unfocusedBorderColor = BendeyColors.Outline,
        ),
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
