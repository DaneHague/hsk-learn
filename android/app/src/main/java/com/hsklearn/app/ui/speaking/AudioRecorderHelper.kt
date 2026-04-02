package com.hsklearn.app.ui.speaking

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class AudioRecorderHelper(private val cacheDir: File) {

    companion object {
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BITS_PER_SAMPLE = 16
        private const val NUM_CHANNELS = 1
    }

    private var audioRecord: AudioRecord? = null
    private var outputFile: File? = null

    @Volatile
    private var isRecording = false

    fun startRecording(): File {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(4096)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize,
        )

        val file = File(cacheDir, "speech_${System.currentTimeMillis()}.wav")
        outputFile = file

        // Write a placeholder WAV header (44 bytes), filled in on stop
        FileOutputStream(file).use { fos ->
            fos.write(ByteArray(44))
        }

        recorder.startRecording()
        audioRecord = recorder
        isRecording = true

        return file
    }

    suspend fun recordLoop() = withContext(Dispatchers.IO) {
        val recorder = audioRecord ?: return@withContext
        val file = outputFile ?: return@withContext
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(4096)
        val buffer = ByteArray(bufferSize)

        FileOutputStream(file, true).use { fos ->
            while (isRecording && isActive) {
                val bytesRead = recorder.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    fos.write(buffer, 0, bytesRead)
                }
            }
        }
    }

    fun stopRecording(): File? {
        isRecording = false

        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null

        val file = outputFile ?: return null
        outputFile = null

        writeWavHeader(file)
        return file
    }

    fun release() {
        isRecording = false
        audioRecord?.release()
        audioRecord = null
        outputFile = null
    }

    private fun writeWavHeader(file: File) {
        val fileSize = file.length()
        val dataSize = fileSize - 44
        val byteRate = SAMPLE_RATE * NUM_CHANNELS * BITS_PER_SAMPLE / 8
        val blockAlign = NUM_CHANNELS * BITS_PER_SAMPLE / 8

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            // RIFF header
            raf.writeBytes("RIFF")
            raf.writeIntLE((fileSize - 8).toInt())
            raf.writeBytes("WAVE")
            // fmt sub-chunk
            raf.writeBytes("fmt ")
            raf.writeIntLE(16) // sub-chunk size for PCM
            raf.writeShortLE(1) // audio format: PCM
            raf.writeShortLE(NUM_CHANNELS)
            raf.writeIntLE(SAMPLE_RATE)
            raf.writeIntLE(byteRate)
            raf.writeShortLE(blockAlign)
            raf.writeShortLE(BITS_PER_SAMPLE)
            // data sub-chunk
            raf.writeBytes("data")
            raf.writeIntLE(dataSize.toInt())
        }
    }

    private fun RandomAccessFile.writeIntLE(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }

    private fun RandomAccessFile.writeShortLE(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }
}
