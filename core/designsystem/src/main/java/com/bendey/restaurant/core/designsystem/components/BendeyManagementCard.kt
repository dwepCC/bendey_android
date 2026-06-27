package com.bendey.restaurant.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

@Composable
fun BendeyManagementCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(BendeySpacing.cardPadding),
    content: @Composable () -> Unit,
) {
    BendeyCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = contentPadding,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}
