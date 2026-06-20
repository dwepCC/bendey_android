package com.bendey.restaurant.core.data.feedback

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartFeedback @Inject constructor() {
    @Volatile
    private var toneGenerator: ToneGenerator? = null

    fun playAddToCart() {
        try {
            val tone = toneGenerator ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75).also {
                toneGenerator = it
            }
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 90)
        } catch (_: Exception) {
            // Sin audio en emulador o permisos restringidos — ignorar.
        }
    }
}
