package com.bendey.restaurant.core.realtime

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewOrderSoundPlayer @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val soundPool: SoundPool
    private val soundId: Int

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attrs)
            .build()
        soundId = soundPool.load(context, R.raw.new_order, 1)
    }

    fun play() {
        soundPool.play(soundId, 0.9f, 0.9f, 1, 0, 1f)
    }
}

object NewOrderEventTypes {
    val ALERT_TYPES = setOf(
        "restaurant.order.created",
        "menu.order.created",
    )
}
