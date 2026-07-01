package com.bendey.restaurant.core.ui.pos.workspace

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.pos.PosCatalogTab
import com.bendey.restaurant.core.domain.restaurant.PosProduct
import com.bendey.restaurant.core.domain.restaurant.ProductCategory
import com.bendey.restaurant.core.ui.components.BendeyLazyGridLoadMoreEffect
import com.bendey.restaurant.core.ui.components.BendeyLazyVerticalGrid
import com.bendey.restaurant.core.ui.components.BendeyPosProductCard
import com.bendey.restaurant.core.ui.components.BendeyPosProductCardVariant
import com.bendey.restaurant.core.ui.layout.adaptive.AdaptivePos
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyPosWorkspaceMode
import com.bendey.restaurant.core.ui.pos.PosCatalogTabRow
import java.text.NumberFormat

@Composable
fun BendeyPosCatalogWorkspace(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    categories: List<ProductCategory>,
    selectedCategoryId: Int?,
    onCategorySelect: (Int?) -> Unit,
    products: List<PosProduct>,
    currency: NumberFormat,
    assetsBaseUrl: String?,
    onProductClick: (PosProduct) -> Unit,
    profile: BendeyAdaptiveProfile,
    workspaceMode: BendeyPosWorkspaceMode,
    modifier: Modifier = Modifier,
    catalogTab: PosCatalogTab = PosCatalogTab.PRODUCTS,
    onCatalogTab: ((PosCatalogTab) -> Unit)? = null,
    orderTypeStrip: @Composable (() -> Unit)? = null,
    onBarcodeScan: (() -> Unit)? = null,
    gridBottomPadding: Dp = BendeySpacing.sm,
    hasMoreProducts: Boolean = false,
    productsLoadingMore: Boolean = false,
    onLoadMoreProducts: (() -> Unit)? = null,
    catalogBody: (@Composable (Modifier) -> Unit)? = null,
) {
    val horizontalPad = AdaptivePos.catalogHorizontalPadding(profile)
    val gridGap = AdaptivePos.catalogGridGap(profile)
    val sectionGap = AdaptivePos.catalogSectionGap(profile)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPad),
        verticalArrangement = Arrangement.spacedBy(sectionGap),
    ) {
        orderTypeStrip?.invoke()
        BendeyPosSearchBar(
            query = searchQuery,
            onQueryChange = onSearchChange,
            profile = profile,
            onBarcodeScan = onBarcodeScan,
        )
        if (onCatalogTab != null) {
            PosCatalogTabRow(
                selected = catalogTab,
                onSelect = onCatalogTab,
                modifier = Modifier.fillMaxWidth(),
                horizontalPadding = 0.dp,
            )
        }
        if (catalogTab == PosCatalogTab.PRODUCTS && categories.isNotEmpty()) {
            BendeyPosCategoryChipRow(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelect = onCategorySelect,
                profile = profile,
            )
        }
        val gridModifier = Modifier.weight(1f).fillMaxWidth()
        if (catalogBody != null) {
            catalogBody(gridModifier)
        } else {
            AnimatedContent(
                targetState = selectedCategoryId to catalogTab,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(160)) },
                label = "product_grid",
                modifier = gridModifier,
            ) { _ ->
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columns = remember(maxWidth, profile) {
                        AdaptivePos.productGridColumns(profile, maxWidth.value.toInt())
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
                    BendeyLazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        state = gridState,
                        showScrollHints = false,
                        contentPadding = PaddingValues(top = 0.dp, bottom = gridBottomPadding),
                        horizontalArrangement = Arrangement.spacedBy(gridGap),
                        verticalArrangement = Arrangement.spacedBy(gridGap),
                    ) {
                        items(products, key = { it.id }) { product ->
                            BendeyPosProductCard(
                                name = product.name,
                                price = product.salePrice,
                                currency = currency,
                                imageUrl = product.imageUrl,
                                assetsBaseUrl = assetsBaseUrl,
                                onClick = { onProductClick(product) },
                                enabled = product.availableForSale,
                                variant = BendeyPosProductCardVariant.Workspace,
                                profile = profile,
                            )
                        }
                        if (productsLoadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = BendeySpacing.md),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                        if (products.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = "Sin productos — prueba otra categoría o busca por nombre",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BendeyColors.OnSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = BendeySpacing.lg),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
