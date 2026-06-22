package com.bendey.restaurant.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bendey.restaurant.core.designsystem.R
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.designsystem.theme.BendeyShapeTokens
import com.bendey.restaurant.core.designsystem.theme.BendeySpacing

@Composable
fun BendeyBrandLogo(
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    contentDescription: String = "Bendey Resto",
    showBackground: Boolean = false,
) {
    val imageModifier = Modifier
        .height(height)
        .widthIn(min = 96.dp, max = 240.dp)

    val image: @Composable () -> Unit = {
        Image(
            painter = painterResource(R.drawable.logo_bendey),
            contentDescription = contentDescription,
            modifier = imageModifier,
            contentScale = ContentScale.Fit,
        )
    }
    if (showBackground) {
        Box(
            modifier = modifier
                .clip(BendeyShapeTokens.md)
                .background(BendeyColors.Surface)
                .padding(horizontal = BendeySpacing.sm, vertical = BendeySpacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            image()
        }
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            image()
        }
    }
}
