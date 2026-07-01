package com.bendey.restaurant.core.ui.pos.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChip
import com.bendey.restaurant.core.designsystem.components.BendeyFilterChipVariant
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing
import com.bendey.restaurant.core.domain.restaurant.ProductCategory
import com.bendey.restaurant.core.ui.components.BendeyHorizontalLazyScrollRow
import com.bendey.restaurant.core.ui.layout.adaptive.AdaptivePos
import com.bendey.restaurant.core.ui.layout.adaptive.BendeyAdaptiveProfile

/** Categorías horizontales — mismo estilo que móvil ([BendeyFilterChipVariant.Pos]). */
@Composable
fun BendeyPosCategoryChipRow(
    categories: List<ProductCategory>,
    selectedCategoryId: Int?,
    onCategorySelect: (Int?) -> Unit,
    profile: BendeyAdaptiveProfile,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val selectedIndex = remember(selectedCategoryId, categories) {
        if (selectedCategoryId == null) 0 else categories.indexOfFirst { it.id == selectedCategoryId } + 1
    }
    val chipGap = if (profile.isCompact) {
        BendeySpacing.xxs
    } else {
        AdaptivePos.portraitMobileCategoryChipGap()
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            listState.scrollToItem(selectedIndex.coerceAtMost(categories.size))
        }
    }

    BendeyHorizontalLazyScrollRow(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        state = listState,
        contentPadding = PaddingValues(horizontal = BendeySpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(chipGap),
        showEdgeFade = true,
    ) {
        item(key = "all") {
            PosCategoryChip(
                selected = selectedCategoryId == null,
                label = "Todos",
                onClick = { onCategorySelect(null) },
                profile = profile,
            )
        }
        items(categories, key = { it.id }) { category ->
            PosCategoryChip(
                selected = selectedCategoryId == category.id,
                label = category.name,
                onClick = { onCategorySelect(category.id) },
                profile = profile,
            )
        }
    }
}

@Composable
private fun PosCategoryChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    profile: BendeyAdaptiveProfile,
) {
    val labelStyle = if (profile.isCompact) {
        MaterialTheme.typography.labelMedium
    } else {
        MaterialTheme.typography.labelLarge
    }
    BendeyFilterChip(
        selected = selected,
        onClick = onClick,
        variant = BendeyFilterChipVariant.Pos,
        label = {
            Text(label, maxLines = 1, style = labelStyle)
        },
    )
}
