package com.bendey.restaurant.core.ui.diagnostics

import android.util.Log

private const val LOG_TAG = "BendeyUiDiag"

/**
 * Infraestructura de diagnóstico de renderizado Compose (solo desarrollo).
 *
 * **Política:** desactivado por defecto. Para reactivar temporalmente:
 * `BendeyUiDiagnostics.configure(debugBuild = BuildConfig.DEBUG, enabled = true)`
 * en [com.bendey.restaurant.BendeyRestoApplication].
 *
 * En RELEASE [isActive] siempre es false.
 */
object BendeyUiDiagnostics {
    /** Flag central — activar/desactivar diagnósticos de renderizado. */
    var renderDiagnosticsEnabled: Boolean = false

    var showProbeBounds: Boolean = false

    var lastLazyListSnapshot: BendeyLazyListRenderSnapshot? = null
    var lastLazyGridSnapshot: BendeyLazyGridRenderSnapshot? = null

    val isActive: Boolean
        get() = renderDiagnosticsEnabled

    /**
     * @param debugBuild `BuildConfig.DEBUG` de la app.
     * @param enabled Activar diagnósticos (default: **false**).
     */
    fun configure(debugBuild: Boolean, enabled: Boolean = false) {
        renderDiagnosticsEnabled = debugBuild && enabled
        showProbeBounds = debugBuild && enabled
        if (!renderDiagnosticsEnabled) {
            lastLazyListSnapshot = null
            lastLazyGridSnapshot = null
        }
    }

    fun log(screen: String, message: String) {
        if (isActive) {
            Log.d(LOG_TAG, "[$screen] $message")
        }
    }
}
