package com.bendey.restaurant.core.ui.layout

import android.app.Activity
import android.content.pm.ActivityInfo

/**
 * Politica de orientacion de la app. Telefono: solo vertical. Tablet: libre, salvo en modo KDS
 * (pantalla de cocina), donde queda en horizontal (R7). El modo KDS lo activa SOLO `CocinaScreen`
 * y lo limpia al salir.
 */
object BendeyOrientationPolicy {
    @Volatile
    var kdsMode: Boolean = false

    fun requestedOrientation(smallestWidthDp: Int, kdsMode: Boolean): Int = when {
        !BendeyDeviceFormFactor.isTablet(smallestWidthDp) -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        kdsMode -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        else -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
    }

    fun apply(activity: Activity) {
        activity.requestedOrientation =
            requestedOrientation(activity.resources.configuration.smallestScreenWidthDp, kdsMode)
    }

    fun setKdsMode(activity: Activity, active: Boolean) {
        kdsMode = active
        apply(activity)
    }
}
