package com.bendey.restaurant.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun BendeySnackMessage(
    message: String?,
    onShow: (String) -> Unit,
    onConsume: () -> Unit,
) {
    LaunchedEffect(message) {
        message?.let {
            onShow(it)
            onConsume()
        }
    }
}
