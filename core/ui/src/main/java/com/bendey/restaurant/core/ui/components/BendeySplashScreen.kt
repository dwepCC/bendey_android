package com.bendey.restaurant.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.components.BendeyBrandLogo
import com.bendey.restaurant.core.designsystem.motion.BendeyExpressiveFadeSlideIn
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyExpressiveScope
import kotlinx.coroutines.delay

@Composable
fun BendeySplashScreen(
    modifier: Modifier = Modifier,
) {
    var logoVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60)
        logoVisible = true
    }

    BendeyExpressiveScope {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BendeyColors.Rest900),
            contentAlignment = Alignment.Center,
        ) {
            BendeyExpressiveFadeSlideIn(visible = logoVisible) {
                BendeyBrandLogo(height = 72.dp, showBackground = true)
            }
        }
    }
}
