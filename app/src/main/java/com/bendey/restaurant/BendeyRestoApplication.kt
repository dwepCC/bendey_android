package com.bendey.restaurant

import android.app.Application
import com.bendey.restaurant.BuildConfig
import com.bendey.restaurant.core.data.locale.LocaleRepository
import com.bendey.restaurant.core.ui.diagnostics.BendeyUiDiagnostics
import com.bendey.restaurant.push.Push
import coil.Coil
import coil.ImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BendeyRestoApplication : Application() {

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var localeRepository: LocaleRepository

    override fun onCreate() {
        super.onCreate()
        BendeyUiDiagnostics.configure(debugBuild = BuildConfig.DEBUG, enabled = false)
        Coil.setImageLoader(imageLoader)
        Push.createChannels(this)
        // Paquete de país: moneda y zona del tenant (lo guardado al instante; el servidor tras el login).
        localeRepository.start()
    }
}
