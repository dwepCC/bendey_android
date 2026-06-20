package com.bendey.restaurant.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** @deprecated Usar [BendeyScreenToolbar] */
@Composable
fun BendeySecondaryToolbar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
) {
    BendeyScreenToolbar(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        onBack = onBack,
    )
}
