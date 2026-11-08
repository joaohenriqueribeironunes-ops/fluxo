package com.equilibrio.core.data.ml

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modelo TFLite opcional. O arquivo não é distribuído por padrão: coloque
 * app/src/main/assets/crisis_model.tflite (gerado por ml/train_crisis_model.py)
 * e o app passa a usá-lo quando houver >= 20 crises rotuladas.
 */
@Singleton
class TfliteCrisisModel @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val interpreter: Interpreter? by lazy {
        runCatching { Interpreter(loadModel()) }.getOrNull()
    }

    fun isAvailable(): Boolean = runCatching {
        context.assets.open(MODEL_NAME).close()
        true
    }.getOrDefault(false) && interpreter != null

    fun predict(features: FloatArray): Float? {
        val engine = interpreter ?: return null
        if (features.size != EXPECTED_FEATURES) return null
        return runCatching {
            val input = arrayOf(features.copyOf())
            val output = Array(1) { FloatArray(1) }
            engine.run(input, output)
            output[0][0].coerceIn(0f, 1f)
        }.getOrNull()
    }

    fun close() {
        runCatching { interpreter?.close() }
    }

    private fun loadModel(): MappedByteBuffer {
        context.assets.openFd(MODEL_NAME).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { stream ->
                return stream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.declaredLength
                )
            }
        }
    }

    private companion object {
        const val MODEL_NAME = "crisis_model.tflite"
        const val EXPECTED_FEATURES = 24
    }
}
