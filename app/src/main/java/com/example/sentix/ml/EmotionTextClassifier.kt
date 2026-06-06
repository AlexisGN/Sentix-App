package com.example.sentix.ml

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.Normalizer
import java.util.Locale

class EmotionTextClassifier(private val context: Context) {

    data class ResultadoTexto(
        val etiqueta: String,
        val etiquetaTraducida: String,
        val confianza: Float,
        val probabilidades: Map<String, Float>,
        val textoNormalizado: String
    )

    private var interpreter: Interpreter? = null

    private val labels: List<String>
    private val vocabulario: List<String>
    private val wordIndex: Map<String, Int>

    private val maxLen: Int
    private val numClasses: Int

    init {
        labels = cargarLineasAsset("labels_nlp.txt")
        vocabulario = cargarVocabularioAsset("vocabulario_nlp.txt")
        wordIndex = vocabulario.mapIndexed { index, palabra -> palabra to index }.toMap()

        val config = cargarJsonAsset("config_nlp.json")
        maxLen = config.optInt("max_len", 50)
        numClasses = config.optInt("num_classes", labels.size)

        val modelo = cargarModelo("modelo_nlp_sentix.tflite")

        val opciones = Interpreter.Options().apply {
            setNumThreads(4)
        }

        interpreter = Interpreter(modelo, opciones)

        Log.d("NLP_SENTIX", "Modelo NLP cargado correctamente")
        Log.d("NLP_SENTIX", "Labels: $labels")
        Log.d("NLP_SENTIX", "Vocabulario: ${vocabulario.size}")
        Log.d("NLP_SENTIX", "MaxLen: $maxLen")
        Log.d("NLP_SENTIX", "NumClasses: $numClasses")
    }

    fun clasificar(textoOriginal: String): ResultadoTexto {
        val textoNormalizado = normalizarTexto(textoOriginal)
        val vector = textoAVector(textoNormalizado)

        val inputBuffer = ByteBuffer.allocateDirect(4 * maxLen)
        inputBuffer.order(ByteOrder.nativeOrder())

        for (valor in vector) {
            inputBuffer.putInt(valor)
        }

        inputBuffer.rewind()

        val output = Array(1) { FloatArray(numClasses) }

        val modelo = interpreter
            ?: throw IllegalStateException("El modelo NLP no está inicializado")

        modelo.run(inputBuffer, output)

        val probabilidadesArray = output[0]

        val indiceMayor = probabilidadesArray.indices.maxByOrNull {
            probabilidadesArray[it]
        } ?: 0

        val etiqueta = labels.getOrElse(indiceMayor) { "desconocido" }
        val confianza = probabilidadesArray[indiceMayor] * 100f

        val probabilidades = labels.mapIndexed { index, label ->
            label to (probabilidadesArray.getOrElse(index) { 0f } * 100f)
        }.toMap()

        return ResultadoTexto(
            etiqueta = etiqueta,
            etiquetaTraducida = traducirEtiqueta(etiqueta),
            confianza = confianza,
            probabilidades = probabilidades,
            textoNormalizado = textoNormalizado
        )
    }

    private fun textoAVector(textoNormalizado: String): IntArray {
        val palabras = textoNormalizado
            .split(" ")
            .filter { it.isNotBlank() }

        val vector = IntArray(maxLen) { 0 }

        val limite = minOf(palabras.size, maxLen)

        for (i in 0 until limite) {
            val palabra = palabras[i]
            val indice = wordIndex[palabra] ?: 1
            vector[i] = indice
        }

        return vector
    }

    private fun normalizarTexto(texto: String): String {
        var resultado = texto
            .lowercase(Locale.ROOT)
            .trim()

        resultado = quitarTildes(resultado)

        val reemplazos = linkedMapOf(
            Regex("\\bxq\\b") to "porque",
            Regex("\\bpq\\b") to "porque",
            Regex("\\bx\\b") to "por",
            Regex("\\btoy\\b") to "estoy",
            Regex("\\bstoy\\b") to "estoy",
            Regex("\\besoty\\b") to "estoy",
            Regex("\\bestoi\\b") to "estoy",
            Regex("\\buni\\b") to "universidad",
            Regex("\\bu\\b") to "universidad",
            Regex("\\bprofe\\b") to "profesor",
            Regex("\\bexpo\\b") to "exposicion",
            Regex("\\bexpos\\b") to "exposiciones",
            Regex("\\bparcial\\b") to "examen",
            Regex("\\bparciales\\b") to "examenes",
            Regex("\\bfinal\\b") to "examen final",
            Regex("\\bfinales\\b") to "examenes finales",
            Regex("\\bchamba\\b") to "trabajo",
            Regex("\\bcansao\\b") to "cansado",
            Regex("\\bagotao\\b") to "agotado",
            Regex("\\bestresao\\b") to "estresado",
            Regex("\\bpreocupao\\b") to "preocupado",
            Regex("\\bnomas\\b") to "solamente",
            Regex("\\bdesanimao\\b") to "desanimado",
            Regex("\\bbajoneao\\b") to "bajoneado",
            Regex("\\bxd\\b") to "",
            Regex("\\bjaja\\b") to "",
            Regex("\\bps\\b") to "",
            Regex("\\basu\\b") to "",
            Regex("\\bpucha\\b") to "",
            Regex("\\bbro\\b") to ""
        )

        for ((regex, reemplazo) in reemplazos) {
            resultado = resultado.replace(regex, reemplazo)
        }

        resultado = resultado.replace(Regex("[^a-zñ0-9\\s]"), " ")
        resultado = resultado.replace(Regex("\\s+"), " ").trim()

        return resultado
    }

    private fun quitarTildes(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return normalizado.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    private fun traducirEtiqueta(etiqueta: String): String {
        return when (etiqueta) {
            "bienestar" -> "Bienestar"
            "neutral" -> "Neutral"
            "estres_academico" -> "Estrés académico"
            "preocupacion" -> "Preocupación"
            "desmotivacion" -> "Desmotivación"
            "aislamiento" -> "Aislamiento"
            else -> etiqueta
        }
    }

    private fun cargarModelo(nombreArchivo: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(nombreArchivo)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel

        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    private fun cargarLineasAsset(nombreArchivo: String): List<String> {
        return context.assets.open(nombreArchivo)
            .bufferedReader(Charsets.UTF_8)
            .useLines { lineas ->
                lineas
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toList()
            }
    }
    private fun cargarVocabularioAsset(nombreArchivo: String): List<String> {
        return context.assets.open(nombreArchivo)
            .bufferedReader(Charsets.UTF_8)
            .readLines()
            .map { it.trim() }
    }

    private fun cargarJsonAsset(nombreArchivo: String): JSONObject {
        val json = context.assets.open(nombreArchivo)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        return JSONObject(json)
    }

    fun cerrar() {
        interpreter?.close()
        interpreter = null
    }
}