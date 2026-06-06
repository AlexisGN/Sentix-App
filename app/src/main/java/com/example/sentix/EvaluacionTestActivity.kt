package com.example.sentix

import android.content.Intent
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast

class EvaluacionTestActivity : BaseMenuActivity() {

    private lateinit var txtProgresoTest: TextView
    private lateinit var txtPreguntaTest: TextView
    private lateinit var radioGroupOpciones: RadioGroup
    private lateinit var rbNunca: RadioButton
    private lateinit var rbAveces: RadioButton
    private lateinit var rbFrecuente: RadioButton
    private lateinit var rbCasiSiempre: RadioButton
    private lateinit var btnAnteriorPregunta: Button
    private lateinit var btnSiguientePregunta: Button
    private lateinit var txtResultadoDevTest: TextView

    private val modoDesarrollador = true

    private var indicePreguntaActual = 0

    private val preguntas = listOf(
        "¿Te has sentido agotado por tus actividades académicas?",
        "¿Has tenido dificultad para concentrarte en clases, tareas o estudios?",
        "¿Has sentido presión por cumplir con trabajos, exámenes o responsabilidades?",
        "¿Has tenido problemas para dormir o descansar bien?",
        "¿Te has sentido preocupado por tu rendimiento académico?",
        "¿Has sentido que la presión social afecta tu estado de ánimo?",
        "¿Has perdido motivación para realizar actividades que antes disfrutabas?",
        "¿Te has sentido aislado o con pocas ganas de hablar con otras personas?",
        "¿Has sentido que tus emociones te sobrepasan durante el día?",
        "¿Sientes que necesitas apoyo o acompañamiento para manejar cómo te sientes?"
    )
    /*
     * Puntaje:
     * Nunca = 0
     * A veces = 1
     * Frecuentemente = 2
     * Casi siempre = 3
     *
     * Máximo: 10 preguntas x 3 = 30 puntos
     */
    private val respuestas = MutableList(preguntas.size) { -1 }

    private var emocionFacial = ""
    private var emocionFacialTraducida = ""
    private var confianzaFacial = 0f

    override fun getContenidoLayoutId(): Int {
        return R.layout.activity_evaluacion_test
    }

    override fun onContenidoCreado() {
        enlazarVistas()
        cargarDatosUsuarioLocal()
        obtenerDatosFaciales()
        configurarEventos()
        mostrarPregunta()
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
        txtProgresoTest = findViewById(R.id.txtProgresoTest)
        txtPreguntaTest = findViewById(R.id.txtPreguntaTest)
        radioGroupOpciones = findViewById(R.id.radioGroupOpciones)

        rbNunca = findViewById(R.id.rbNunca)
        rbAveces = findViewById(R.id.rbAveces)
        rbFrecuente = findViewById(R.id.rbFrecuente)
        rbCasiSiempre = findViewById(R.id.rbCasiSiempre)

        btnAnteriorPregunta = findViewById(R.id.btnAnteriorPregunta)
        btnSiguientePregunta = findViewById(R.id.btnSiguientePregunta)
        txtResultadoDevTest = findViewById(R.id.txtResultadoDevTest)

        rbNunca.text = "Nunca"
        rbAveces.text = "A veces"
        rbFrecuente.text = "Frecuentemente"
        rbCasiSiempre.text = "Casi siempre"
    }

    private fun cargarDatosUsuarioLocal() {
        val cache = UsuarioCacheManager.obtener(this)

        if (uidActual.isBlank() && cache.uid.isNotBlank()) {
            uidActual = cache.uid
        }

        if (emailActual.isBlank() && cache.email.isNotBlank()) {
            emailActual = cache.email
        }

        Log.d("TEST_CACHE", "UID actual: $uidActual")
        Log.d("TEST_CACHE", "Email actual: $emailActual")
    }

    private fun obtenerDatosFaciales() {
        emocionFacial = intent.getStringExtra("emocionFacial") ?: ""
        emocionFacialTraducida = intent.getStringExtra("emocionFacialTraducida") ?: ""
        confianzaFacial = intent.getFloatExtra("confianzaFacial", 0f)

        if (modoDesarrollador) {
            txtResultadoDevTest.text =
                "DEV\n" +
                        "Resultado facial recibido:\n" +
                        "Clase interna: ${emocionFacial.ifBlank { "sin dato" }}\n" +
                        "Clase visible: ${emocionFacialTraducida.ifBlank { "sin dato" }}\n" +
                        "Confianza: ${"%.2f".format(confianzaFacial)}%"
        }
    }

    private fun configurarEventos() {
        radioGroupOpciones.setOnCheckedChangeListener { _, checkedId ->
            respuestas[indicePreguntaActual] = obtenerPuntajeSeleccionado(checkedId)
            actualizarDev()
        }

        btnAnteriorPregunta.setOnClickListener {
            guardarRespuestaActual()

            if (indicePreguntaActual > 0) {
                indicePreguntaActual--
                mostrarPregunta()
            }
        }

        btnSiguientePregunta.setOnClickListener {
            guardarRespuestaActual()

            if (respuestas[indicePreguntaActual] == -1) {
                Toast.makeText(
                    this,
                    "Selecciona una respuesta para continuar.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (indicePreguntaActual < preguntas.lastIndex) {
                indicePreguntaActual++
                mostrarPregunta()
            } else {
                finalizarTest()
            }
        }
    }

    private fun mostrarPregunta() {
        txtProgresoTest.text = "Pregunta ${indicePreguntaActual + 1} de ${preguntas.size}"
        txtPreguntaTest.text = preguntas[indicePreguntaActual]

        btnAnteriorPregunta.isEnabled = indicePreguntaActual > 0
        btnSiguientePregunta.text =
            if (indicePreguntaActual == preguntas.lastIndex) {
                "Finalizar"
            } else {
                "Siguiente"
            }

        radioGroupOpciones.setOnCheckedChangeListener(null)
        radioGroupOpciones.clearCheck()

        when (respuestas[indicePreguntaActual]) {
            0 -> rbNunca.isChecked = true
            1 -> rbAveces.isChecked = true
            2 -> rbFrecuente.isChecked = true
            3 -> rbCasiSiempre.isChecked = true
        }

        radioGroupOpciones.setOnCheckedChangeListener { _, checkedId ->
            respuestas[indicePreguntaActual] = obtenerPuntajeSeleccionado(checkedId)
            actualizarDev()
        }

        actualizarDev()
    }

    private fun obtenerPuntajeSeleccionado(checkedId: Int): Int {
        return when (checkedId) {
            R.id.rbNunca -> 0
            R.id.rbAveces -> 1
            R.id.rbFrecuente -> 2
            R.id.rbCasiSiempre -> 3
            else -> -1
        }
    }

    private fun guardarRespuestaActual() {
        val puntajeSeleccionado = obtenerPuntajeSeleccionado(radioGroupOpciones.checkedRadioButtonId)

        if (puntajeSeleccionado != -1) {
            respuestas[indicePreguntaActual] = puntajeSeleccionado
        }
    }

    private fun finalizarTest() {
        if (respuestas.any { it == -1 }) {
            Toast.makeText(
                this,
                "Responde todas las preguntas antes de continuar.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val puntaje = respuestas.sum()
        val puntajeMaximo = preguntas.size * 3
        val nivel = calcularNivelTest(puntaje)
        val nivelVisible = obtenerNivelVisible(nivel)

        Log.d("TEST_SENTIX", "Puntaje: $puntaje/$puntajeMaximo")
        Log.d("TEST_SENTIX", "Nivel interno: $nivel")
        Log.d("TEST_SENTIX", "Nivel visible: $nivelVisible")
        Log.d("TEST_SENTIX", "Respuestas: $respuestas")

        Toast.makeText(
            this,
            "Test completado. Siguiente paso: análisis de texto.",
            Toast.LENGTH_LONG
        ).show()

        val intent = Intent(this, EvaluacionTextoActivity::class.java)
        intent.putExtra("uid", uidActual)
        intent.putExtra("email", emailActual)

        intent.putExtra("emocionFacial", emocionFacial)
        intent.putExtra("emocionFacialTraducida", emocionFacialTraducida)
        intent.putExtra("confianzaFacial", confianzaFacial)

        intent.putExtra("puntajeTest", puntaje)
        intent.putExtra("puntajeMaximoTest", puntajeMaximo)
        intent.putExtra("nivelTest", nivel)
        intent.putExtra("nivelTestVisible", nivelVisible)
        intent.putExtra("respuestasTest", respuestas.joinToString(","))

        startActivity(intent)
    }

    private fun calcularNivelTest(puntaje: Int): String {
        return when (puntaje) {
            in 0..7 -> "bajo"
            in 8..15 -> "leve"
            in 16..23 -> "moderado"
            else -> "alto"
        }
    }

    private fun obtenerNivelVisible(nivel: String): String {
        return when (nivel) {
            "bajo" -> "Estado favorable"
            "leve" -> "Señales leves"
            "moderado" -> "Seguimiento recomendado"
            "alto" -> "Atención recomendada"
            else -> "Resultado orientativo"
        }
    }

    private fun actualizarDev() {
        if (!modoDesarrollador) {
            txtResultadoDevTest.text = ""
            return
        }

        val puntajeParcial = respuestas.filter { it >= 0 }.sum()
        val respondidas = respuestas.count { it >= 0 }
        val puntajeMaximo = preguntas.size * 3
        val nivelParcial = calcularNivelTest(puntajeParcial)

        txtResultadoDevTest.text =
            "DEV\n" +
                    "Facial: ${emocionFacialTraducida.ifBlank { "sin dato" }} " +
                    "(${"%.2f".format(confianzaFacial)}%)\n" +
                    "Respondidas: $respondidas/${preguntas.size}\n" +
                    "Puntaje parcial: $puntajeParcial/$puntajeMaximo\n" +
                    "Nivel parcial: $nivelParcial\n" +
                    "Respuestas: $respuestas"
    }
}