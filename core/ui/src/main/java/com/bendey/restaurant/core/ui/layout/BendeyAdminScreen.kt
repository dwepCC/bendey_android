package com.bendey.restaurant.core.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

/** Contenedor estándar para pantallas administrativas / gestión. */
@Composable
fun BendeyAdminScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BendeyColors.Background)
            .bendeyImePadding()
            .padding(horizontal = BendeySpacing.screenHorizontal),
        content = content,
    )
}
