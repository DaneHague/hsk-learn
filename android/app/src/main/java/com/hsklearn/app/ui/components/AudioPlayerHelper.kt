package com.hsklearn.app.ui.components

import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AudioPlayerHelper(private val cacheDir: File) {

    private var mediaPlayer: MediaPlayer? = null

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    suspend fun play(audioBytes: ByteArray) {
        stop()
        val tempFile = withContext(Dispatchers.IO) {
            File(cacheDir, "tts_${System.currentTimeMillis()}.wav").also {
                it.writeBytes(audioBytes)
            }
        }
        val player = MediaPlayer()
        player.setDataSource(tempFile.absolutePath)
        player.setOnCompletionListener {
            it.release()
            mediaPlayer = null
            tempFile.delete()
        }
        player.prepare()
        player.start()
        mediaPlayer = player
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    fun release() {
        stop()
    }
}
