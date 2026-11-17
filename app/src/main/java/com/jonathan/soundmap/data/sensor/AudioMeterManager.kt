package com.jonathan.soundmap.data.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.jonathan.soundmap.domain.acoustics.NoiseMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lee el micrófono con [AudioRecord] y emite el nivel en dB de cada bloque (~10 por segundo).
 *
 * Privacidad: las muestras solo existen en un buffer de memoria que se sobrescribe en cada
 * lectura. No se graba, no se guarda en disco y no se envía nada.
 */
@Singleton
class AudioMeterManager @Inject constructor() {

    /**
     * Flujo frío: el micrófono se abre al empezar a recolectar y se libera al cancelar.
     * Requiere el permiso RECORD_AUDIO concedido.
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun decibels(): Flow<Double> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        check(minBuffer > 0) { "El dispositivo no admite captura PCM a $SAMPLE_RATE Hz" }
        val blockSize = SAMPLE_RATE / BLOCKS_PER_SECOND
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL, ENCODING,
            maxOf(minBuffer, blockSize * 2 * 2),
        )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "No se pudo abrir el micrófono" }
        val buffer = ShortArray(blockSize)
        try {
            recorder.startRecording()
            while (currentCoroutineContext().isActive) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) emit(NoiseMath.blockDb(buffer, read))
            }
        } finally {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
            recorder.release()
            buffer.fill(0)
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        const val SAMPLE_RATE = 44_100
        const val BLOCKS_PER_SECOND = 10
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }
}
