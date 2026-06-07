package com.example.sentix

import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.sentix.ml.EmotionTextClassifier
import android.content.Intent
import android.view.View

class EvaluacionTextoActivity : BaseMenuActivity() {

    private lateinit var txtTituloTexto: TextView
    private lateinit var txtSubtituloTexto: TextView
    private lateinit var edtTextoUsuario: EditText
    private lateinit var btnAnalizarTexto: Button
    private lateinit var txtResultadoDevTexto: TextView

    private lateinit var textClassifier: EmotionTextClassifier

    private val modoDesarrollador = false

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
        txtResultadoDevTexto.visibility = View.GONE
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
        txtResultadoDevTexto.visibility = View.GONE
        btnAnalizarTexto.text = "Finalizar evaluación"
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
            edtTextoUsuario.error = "Escribe una frase breve"
            return
        }

        if (texto.length < 8) {
            edtTextoUsuario.error = "Agrega un poco más de información"
            return
        }

        if (texto.length > 160) {
            edtTextoUsuario.error = "Escribe una frase más corta"
            return
        }

        try {
            btnAnalizarTexto.isEnabled = false
            btnAnalizarTexto.text = "Finalizando..."

            val resultado = textClassifier.clasificar(texto)
            resultadoTextoInterno = resultado

            val combinado = calcularResultadoCombinado(resultado)

            Log.d("NLP_SENTIX", "Texto original: $texto")
            Log.d("NLP_SENTIX", "Texto normalizado: ${resultado.textoNormalizado}")
            Log.d("NLP_SENTIX", "Clase NLP: ${resultado.etiqueta}")
            Log.d("NLP_SENTIX", "Confianza NLP: ${resultado.confianza}")
            Log.d("RESULTADO_SENTIX", "Puntaje final: ${combinado.puntajeFinal}")
            Log.d("RESULTADO_SENTIX", "Nivel final: ${combinado.nivelFinal}")

            irAResultadoFinal(
                textoUsuario = texto,
                resultadoTexto = resultado,
                combinado = combinado
            )

        } catch (e: Exception) {
            Log.e("NLP_SENTIX", "Error al analizar texto", e)

            Toast.makeText(
                this,
                "No se pudo finalizar la evaluación. Intenta nuevamente.",
                Toast.LENGTH_LONG
            ).show()

            btnAnalizarTexto.isEnabled = true
            btnAnalizarTexto.text = "Finalizar evaluación"
        }
    }
    private fun irAResultadoFinal(
        textoUsuario: String,
        resultadoTexto: EmotionTextClassifier.ResultadoTexto,
        combinado: ResultadoCombinado
    ) {
        val intent = Intent(this, ResultadoEvaluacionActivity::class.java)

        intent.putExtra("uid", uidActual)
        intent.putExtra("email", emailActual)

        intent.putExtra("emocionFacial", emocionFacial)
        intent.putExtra("emocionFacialTraducida", emocionFacialTraducida)
        intent.putExtra("confianzaFacial", confianzaFacial)

        intent.putExtra("puntajeTest", puntajeTest)
        intent.putExtra("puntajeMaximoTest", puntajeMaximoTest)
        intent.putExtra("nivelTest", nivelTest)
        intent.putExtra("nivelTestVisible", nivelTestVisible)
        intent.putExtra("respuestasTest", respuestasTest)

        intent.putExtra("textoUsuario", textoUsuario)
        intent.putExtra("textoNormalizado", resultadoTexto.textoNormalizado)
        intent.putExtra("etiquetaNlp", resultadoTexto.etiqueta)
        intent.putExtra("etiquetaNlpTraducida", resultadoTexto.etiquetaTraducida)
        intent.putExtra("confianzaNlp", resultadoTexto.confianza)

        intent.putExtra("puntajeFacial", combinado.puntajeFacial)
        intent.putExtra("puntajeTestNormalizado", combinado.puntajeTest)
        intent.putExtra("puntajeNlp", combinado.puntajeNlp)
        intent.putExtra("puntajeFinal", combinado.puntajeFinal)
        intent.putExtra("nivelFinal", combinado.nivelFinal)

        startActivity(intent)
    }

    private fun mostrarDevInicial() {
        txtResultadoDevTexto.text = ""
        txtResultadoDevTexto.visibility = View.GONE
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
         * CNN imagen = 10%
         * Test emocional = 45%
         * NLP texto = 45%
         */
        val resultadoFinal = (
                puntajeFacial * 0.10f +
                        puntajeTestNormalizado * 0.45f +
                        puntajeNlp * 0.45f
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