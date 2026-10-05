package com.bendey.restaurant.core.ui.layout

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class BendeyOrientationPolicyTest {
    @Test fun telefono_siempre_vertical() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, BendeyOrientationPolicy.requestedOrientation(411, false))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, BendeyOrientationPolicy.requestedOrientation(411, true))
    }

    @Test fun tablet_libre_salvo_en_modo_kds() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER, BendeyOrientationPolicy.requestedOrientation(800, false))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE, BendeyOrientationPolicy.requestedOrientation(800, true))
    }
}
