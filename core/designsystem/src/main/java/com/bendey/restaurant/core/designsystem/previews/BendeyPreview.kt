package com.bendey.restaurant.core.designsystem.previews

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.bendey.restaurant.core.designsystem.theme.BendeyTheme

@Preview(
    name = "Phone",
    showBackground = true,
    widthDp = 411,
    heightDp = 891,
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class BendeyPhonePreview

@Preview(
    name = "Tablet",
    showBackground = true,
    widthDp = 1280,
    heightDp = 800,
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class BendeyTabletPreview

@Preview(
    name = "Landscape",
    showBackground = true,
    widthDp = 891,
    heightDp = 411,
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class BendeyLandscapePreview

@Preview(
    name = "Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class BendeyLightPreview

/** Contenedor estándar para previews del Design System Bendey. */
@Composable
fun BendeyPreviewSurface(content: @Composable () -> Unit) {
    BendeyTheme {
        Surface {
            content()
        }
    }
}
