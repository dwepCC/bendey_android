package com.bendey.restaurant

import android.app.Application
import com.bendey.restaurant.BuildConfig
import com.bendey.restaurant.core.ui.diagnostics.BendeyUiDiagnostics
import coil.Coil
import coil.ImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BendeyRestoApplication : Application() {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()
        BendeyUiDiagnostics.configure(debugBuild = BuildConfig.DEBUG, enabled = false)
        Coil.setImageLoader(imageLoader)
    }
}
