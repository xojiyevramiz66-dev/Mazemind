package com.seed.mazemind

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build

class SoundManager {
    private var soundPool: SoundPool? = null

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}
