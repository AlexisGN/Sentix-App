package com.example.sentix.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class EmotionImageClassifier(
    private val context: Context
) {

    data class ResultadoEmocion(
        val etiqueta: String,
        val etiquetaTraducida: String,
        val confianza: Float,
        val probabilidades: Map<String, Float>
    )

    private val modelName = "modelo_sentix_emociones.tflite"
    private val labelsName = "labels.txt"

    private val imageSize = 224
    private val numChannels = 3

    private val interpreter: Interpreter
    private val labels: List<String>

    init {
        interpreter = Interpreter(loadModelFile())
        labels = loadLabels()

        Log.d("EMOTION_MODEL", "Modelo cargado correctamente")
        Log.d("EMOTION_MODEL", "Labels cargados: $labels")
    }

    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel

        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )
    }

    private fun loadLabels(): List<String> {
        val labelsList = mutableListOf<String>()

        context.assets.open(labelsName).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line = reader.readLine()

                while (line != null) {
                    if (line.trim().isNotEmpty()) {
                        labelsList.add(line.trim())
                    }

                    line = reader.readLine()
                }
            }
        }

        return labelsList
    }

    fun clasificar(bitmapOriginal: Bitmap): ResultadoEmocion {
        val bitmapRedimensionado = Bitmap.createScaledBitmap(
            bitmapOriginal,
            imageSize,
            imageSize,
            true
        )

        val inputBuffer = convertirBitmapAByteBuffer(bitmapRedimensionado)

        val output = Array(1) { FloatArray(labels.size) }

        interpreter.run(inputBuffer, output)

        val probabilidades = output[0]

        val indiceMayor = probabilidades.indices.maxByOrNull {
            probabilidades[it]
        } ?: 0

        val etiqueta = labels[indiceMayor]
        val confianza = probabilidades[indiceMayor] * 100f

        val mapaProbabilidades = labels.mapIndexed { index, label ->
            label to (probabilidades[index] * 100f)
        }.toMap()

        Log.d("EMOTION_MODEL", "Resultado: $etiqueta - $confianza%")
        Log.d("EMOTION_MODEL", "Probabilidades: $mapaProbabilidades")

        return ResultadoEmocion(
            etiqueta = etiqueta,
            etiquetaTraducida = traducirEtiqueta(etiqueta),
            confianza = confianza,
            probabilidades = mapaProbabilidades
        )
    }

    private fun convertirBitmapAByteBuffer(bitmap: Bitmap): ByteBuffer {
        /*
         * Tu modelo tiene Rescaling(1.0 / 255) dentro.
         * Por eso aquí enviamos valores RGB en rango 0 a 255 como Float32.
         * No dividimos entre 255 otra vez.
         */
        val byteBuffer = ByteBuffer.allocateDirect(
            4 * imageSize * imageSize * numChannels
        )

        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(imageSize * imageSize)
        bitmap.getPixels(
            pixels,
            0,
            imageSize,
            0,
            0,
            imageSize,
            imageSize
        )

        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF).toFloat()
            val g = (pixel shr 8 and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    private fun traducirEtiqueta(etiqueta: String): String {
        return when (etiqueta.lowercase()) {
            "angry" -> "Enojo"
            "disgust" -> "Disgusto"
            "fear" -> "Miedo"
            "happy" -> "Felicidad"
            "neutral" -> "Neutral"
            "sad" -> "Tristeza"
            "surprise" -> "Sorpresa"
            else -> etiqueta
        }
    }

    fun cerrar() {
        interpreter.close()
    }
}