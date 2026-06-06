package com.example.sentix

import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.sentix.ml.EmotionTextClassifier

class EvaluacionTextoActivity : BaseMenuActivity() {

    private lateinit var txtTituloTexto: TextView
    private lateinit var txtSubtituloTexto: TextView
    private lateinit var edtTextoUsuario: EditText
    private lateinit var btnAnalizarTexto: Button
    private lateinit var txtResultadoDevTexto: TextView

    private lateinit var textClassifier: EmotionTextClassifier

    private val modoDesarrollador = true

    private var emocionFacial = ""
    private var emocionFacialTraducida = ""
    private var confianzaFacial = 0f

    private var puntajeTest = 0
    private var puntajeMaximoTest = 0
    private var nivelTest = ""
    private var nivelTestVisible = ""
    private var respuestasTest = ""

    private var resultadoTextoInterno: EmotionTextClassifier.ResultadoTexto? = null

    override fun getContenidoLayoutId(): Int {
        return R.layout.activity_evaluacion_texto
    }

    override fun onContenidoCreado() {
        enlazarVistas()
        cargarDatosUsuarioLocal()
        obtenerDatosPrevios()
        textClassifier = EmotionTextClassifier(this)
        configurarEventos()
        mostrarDevInicial()
    }

    override fun onUsuarioActualizado(cache: UsuarioCache) {
        if (uidActual.isBlank() && cache.uid.isNotBlank()) {
            uidActual = cache.uid
        }

        if (emailActual.isBlank() && cache.email.isNotBlank()) {
            emailActual = cache.email
        }
    }

    private fun enlazarVistas() {
        txtTituloTexto = findViewById(R.id.txtTituloTexto)
        txtSubtituloTexto = findViewById(R.id.txtSubtituloTexto)
        edtTextoUsuario = findViewById(R.id.edtTextoUsuario)
        btnAnalizarTexto = findViewById(R.id.btnAnalizarTexto)
        txtResultadoDevTexto = findViewById(R.id.txtResultadoDevTexto)
    }

    private fun cargarDatosUsuarioLocal() {
        val cache = UsuarioCacheManager.obtener(this)

        if (uidActual.isBlank() && cache.uid.isNotBlank()) {
            uidActual = cache.uid
        }

        if (emailActual.isBlank() && cache.email.isNotBlank()) {
            emailActual = cache.email
        }

        Log.d("TEXTO_CACHE", "UID actual: $uidActual")
        Log.d("TEXTO_CACHE", "Email actual: $emailActual")
    }

    private fun obtenerDatosPrevios() {
        emocionFacial = intent.getStringExtra("emocionFacial") ?: ""
        emocionFacialTraducida = intent.getStringExtra("emocionFacialTraducida") ?: ""
        confianzaFacial = intent.getFloatExtra("confianzaFacial", 0f)

        puntajeTest = intent.getIntExtra("puntajeTest", 0)
        puntajeMaximoTest = intent.getIntExtra("puntajeMaximoTest", 0)
        nivelTest = intent.getStringExtra("nivelTest") ?: ""
        nivelTestVisible = intent.getStringExtra("nivelTestVisible") ?: ""
        respuestasTest = intent.getStringExtra("respuestasTest") ?: ""
    }

    private fun configurarEventos() {
        btnAnalizarTexto.setOnClickListener {
            analizarTexto()
        }
    }

    private fun analizarTexto() {
        val texto = edtTextoUsuario.text.toString().trim()

        if (texto.isBlank()) {
            edtTextoUsuario.error = "Escribe cómo te has sentido"
            return
        }

        if (texto.length < 10) {
            edtTextoUsuario.error = "Agrega un poco más de información"
            return
        }

        if (texto.length > 500) {
            edtTextoUsuario.error = "Resume tu respuesta en máximo 500 caracteres"
            return
        }

        try {
            btnAnalizarTexto.isEnabled = false
            btnAnalizarTexto.text = "Analizando..."

            val resultado = textClassifier.clasificar(texto)
            resultadoTextoInterno = resultado

            Log.d("NLP_SENTIX", "Texto normalizado: ${resultado.textoNormalizado}")
            Log.d("NLP_SENTIX", "Clase NLP: ${resultado.etiqueta}")
            Log.d("NLP_SENTIX", "Confianza NLP: ${resultado.confianza}")
            Log.d("NLP_SENTIX", "Probabilidades NLP: ${resultado.probabilidades}")

            if (modoDesarrollador) {
                val combinado = calcularResultadoCombinado(resultado)

                txtResultadoDevTexto.text =
                    "DEV - Validación de evaluación completa\n\n" +

                            "1) CNN facial\n" +
                            "Clase interna: ${emocionFacial.ifBlank { "sin dato" }}\n" +
                            "Clase visible: ${emocionFacialTraducida.ifBlank { "sin dato" }}\n" +
                            "Confianza: ${"%.2f".format(confianzaFacial)}%\n" +
                            "Puntaje facial 20%: ${"%.2f".format(combinado.puntajeFacial)} / 100\n\n" +

                            "2) Test emocional\n" +
                            "Puntaje: $puntajeTest/$puntajeMaximoTest\n" +
                            "Nivel interno: ${nivelTest.ifBlank { "sin dato" }}\n" +
                            "Nivel visible: ${nivelTestVisible.ifBlank { "sin dato" }}\n" +
                            "Puntaje test 40%: ${"%.2f".format(combinado.puntajeTest)} / 100\n" +
                            "Respuestas: ${respuestasTest.ifBlank { "sin dato" }}\n\n" +

                            "3) NLP texto\n" +
                            "Texto normalizado:\n${resultado.textoNormalizado}\n\n" +
                            "Clase interna: ${resultado.etiqueta}\n" +
                            "Clase visible: ${resultado.etiquetaTraducida}\n" +
                            "Confianza: ${"%.2f".format(resultado.confianza)}%\n" +
                            "Puntaje NLP 40%: ${"%.2f".format(combinado.puntajeNlp)} / 100\n\n" +

                            "Probabilidades NLP:\n" +
                            resultado.probabilidades.entries
                                .sortedByDescending { it.value }
                                .joinToString("\n") {
                                    "${traducirEtiqueta(it.key)}: ${"%.2f".format(it.value)}%"
                                } +

                            "\n\nResultado combinado temporal\n" +
                            "Peso CNN: 20%\n" +
                            "Peso Test: 40%\n" +
                            "Peso NLP: 40%\n" +
                            "Puntaje final: ${"%.2f".format(combinado.puntajeFinal)} / 100\n" +
                            "Nivel final: ${combinado.nivelFinal}"
            }

            Toast.makeText(
                this,
                "Texto analizado correctamente",
                Toast.LENGTH_SHORT
            ).show()

            /*
             * Luego aquí abriremos ResultadoEvaluacionActivity.
             * Por ahora dejamos la predicción visible en modo DEV.
             */

        } catch (e: Exception) {
            Log.e("NLP_SENTIX", "Error al analizar texto", e)

            Toast.makeText(
                this,
                "No se pudo analizar el texto.",
                Toast.LENGTH_LONG
            ).show()

        } finally {
            btnAnalizarTexto.isEnabled = true
            btnAnalizarTexto.text = "Analizar texto"
        }
    }

    private fun mostrarDevInicial() {
        if (!modoDesarrollador) {
            txtResultadoDevTexto.text = ""
            return
        }

        txtResultadoDevTexto.text =
            "DEV\n" +
                    "Datos recibidos:\n" +
                    "UID: ${uidActual.ifBlank { "sin uid" }}\n" +
                    "Email: ${emailActual.ifBlank { "sin email" }}\n\n" +
                    "Facial: ${emocionFacialTraducida.ifBlank { "sin dato" }} " +
                    "(${"%.2f".format(confianzaFacial)}%)\n" +
                    "Test: ${nivelTestVisible.ifBlank { "sin dato" }} " +
                    "($puntajeTest/$puntajeMaximoTest)\n" +
                    "Respuestas: ${respuestasTest.ifBlank { "sin dato" }}"
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
    private fun obtenerPuntajeFacial(etiqueta: String, confianza: Float): Float {
        /*
         * CNN tiene poco peso. Esta función convierte la emoción facial
         * en un puntaje preventivo de 0 a 100.
         */
        val base = when (etiqueta) {
            "bienestar", "happy" -> 10f
            "neutral" -> 25f
            "sad", "fear", "angry", "disgust" -> 70f
            "surprise" -> 40f
            else -> 30f
        }

        val factorConfianza = (confianza / 100f).coerceIn(0f, 1f)
        return base * factorConfianza
    }

    private fun obtenerPuntajeTest(): Float {
        if (puntajeMaximoTest <= 0) return 0f

        return ((puntajeTest.toFloat() / puntajeMaximoTest.toFloat()) * 100f)
            .coerceIn(0f, 100f)
    }

    private fun obtenerPuntajeNlp(etiqueta: String, confianza: Float): Float {
        /*
         * NLP convierte la clase textual en puntaje preventivo de 0 a 100.
         */
        val base = when (etiqueta) {
            "bienestar" -> 10f
            "neutral" -> 25f
            "estres_academico" -> 65f
            "preocupacion" -> 70f
            "desmotivacion" -> 75f
            "aislamiento" -> 80f
            else -> 30f
        }

        val factorConfianza = (confianza / 100f).coerceIn(0f, 1f)
        return base * factorConfianza
    }

    private fun calcularResultadoCombinado(resultadoTexto: EmotionTextClassifier.ResultadoTexto): ResultadoCombinado {
        val puntajeFacial = obtenerPuntajeFacial(emocionFacial, confianzaFacial)
        val puntajeTestNormalizado = obtenerPuntajeTest()
        val puntajeNlp = obtenerPuntajeNlp(resultadoTexto.etiqueta, resultadoTexto.confianza)

        /*
         * Pesos definidos:
         * CNN imagen = 20%
         * Test emocional = 40%
         * NLP texto = 40%
         */
        val resultadoFinal = (
                puntajeFacial * 0.10f +
                        puntajeTestNormalizado * 0.40f +
                        puntajeNlp * 0.40f
                ).coerceIn(0f, 100f)

        val nivelFinal = when {
            resultadoFinal <= 25f -> "Estado favorable"
            resultadoFinal <= 45f -> "Señales leves"
            resultadoFinal <= 70f -> "Seguimiento recomendado"
            else -> "Atención recomendada"
        }

        return ResultadoCombinado(
            puntajeFacial = puntajeFacial,
            puntajeTest = puntajeTestNormalizado,
            puntajeNlp = puntajeNlp,
            puntajeFinal = resultadoFinal,
            nivelFinal = nivelFinal
        )
    }

    data class ResultadoCombinado(
        val puntajeFacial: Float,
        val puntajeTest: Float,
        val puntajeNlp: Float,
        val puntajeFinal: Float,
        val nivelFinal: String
    )

    override fun onDestroy() {
        super.onDestroy()

        if (::textClassifier.isInitialized) {
            textClassifier.cerrar()
        }
    }
}