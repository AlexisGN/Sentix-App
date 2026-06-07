package com.example.sentix

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class ResultadoEvaluacionActivity : BaseMenuActivity() {

    private lateinit var cardResultadoPrincipal: LinearLayout
    private lateinit var cardRecomendacion: LinearLayout

    private lateinit var imgResultadoIlustracion: ImageView
    private lateinit var txtIconoResultado: TextView
    private lateinit var txtNivelFinal: TextView
    private lateinit var txtMensajeResultado: TextView

    private lateinit var indicadorFavorable: View
    private lateinit var indicadorLeve: View
    private lateinit var indicadorSeguimiento: View
    private lateinit var indicadorAtencion: View

    private lateinit var txtLabelFavorable: TextView
    private lateinit var txtLabelLeve: TextView
    private lateinit var txtLabelSeguimiento: TextView
    private lateinit var txtLabelAtencion: TextView

    private lateinit var imgIconoRecomendacion: ImageView
    private lateinit var txtTituloRecomendacion: TextView
    private lateinit var txtMensajeRecomendacion: TextView

    private lateinit var btnVerHistorial: Button
    private lateinit var btnVolverInicio: Button

    private var puntajeFinal = 0f
    private var nivelFinal = ""

    private var emocionFacial = ""
    private var emocionFacialTraducida = ""
    private var confianzaFacial = 0f

    private var puntajeTest = 0
    private var puntajeMaximoTest = 0
    private var nivelTest = ""
    private var nivelTestVisible = ""

    private var etiquetaNlp = ""
    private var etiquetaNlpTraducida = ""
    private var confianzaNlp = 0f
    private var textoUsuario = ""

    override fun getContenidoLayoutId(): Int {
        return R.layout.activity_resultado_evaluacion
    }

    override fun onContenidoCreado() {
        enlazarVistas()
        cargarDatosUsuarioLocal()
        obtenerDatosResultado()
        mostrarResultado()
        configurarEventos()
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
        cardResultadoPrincipal = findViewById(R.id.cardResultadoPrincipal)
        cardRecomendacion = findViewById(R.id.cardRecomendacion)

        imgResultadoIlustracion = findViewById(R.id.imgResultadoIlustracion)
        txtIconoResultado = findViewById(R.id.txtIconoResultado)
        txtNivelFinal = findViewById(R.id.txtNivelFinal)
        txtMensajeResultado = findViewById(R.id.txtMensajeResultado)

        indicadorFavorable = findViewById(R.id.indicadorFavorable)
        indicadorLeve = findViewById(R.id.indicadorLeve)
        indicadorSeguimiento = findViewById(R.id.indicadorSeguimiento)
        indicadorAtencion = findViewById(R.id.indicadorAtencion)

        txtLabelFavorable = findViewById(R.id.txtLabelFavorable)
        txtLabelLeve = findViewById(R.id.txtLabelLeve)
        txtLabelSeguimiento = findViewById(R.id.txtLabelSeguimiento)
        txtLabelAtencion = findViewById(R.id.txtLabelAtencion)

        imgIconoRecomendacion = findViewById(R.id.imgIconoRecomendacion)
        txtTituloRecomendacion = findViewById(R.id.txtTituloRecomendacion)
        txtMensajeRecomendacion = findViewById(R.id.txtMensajeRecomendacion)

        btnVerHistorial = findViewById(R.id.btnVerHistorial)
        btnVolverInicio = findViewById(R.id.btnVolverInicio)
    }

    private fun cargarDatosUsuarioLocal() {
        val cache = UsuarioCacheManager.obtener(this)

        if (uidActual.isBlank() && cache.uid.isNotBlank()) {
            uidActual = cache.uid
        }

        if (emailActual.isBlank() && cache.email.isNotBlank()) {
            emailActual = cache.email
        }
    }

    private fun obtenerDatosResultado() {
        puntajeFinal = intent.getFloatExtra("puntajeFinal", 0f)
        nivelFinal = intent.getStringExtra("nivelFinal") ?: "Resultado orientativo"

        emocionFacial = intent.getStringExtra("emocionFacial") ?: ""
        emocionFacialTraducida = intent.getStringExtra("emocionFacialTraducida") ?: ""
        confianzaFacial = intent.getFloatExtra("confianzaFacial", 0f)

        puntajeTest = intent.getIntExtra("puntajeTest", 0)
        puntajeMaximoTest = intent.getIntExtra("puntajeMaximoTest", 0)
        nivelTest = intent.getStringExtra("nivelTest") ?: ""
        nivelTestVisible = intent.getStringExtra("nivelTestVisible") ?: ""

        etiquetaNlp = intent.getStringExtra("etiquetaNlp") ?: ""
        etiquetaNlpTraducida = intent.getStringExtra("etiquetaNlpTraducida") ?: ""
        confianzaNlp = intent.getFloatExtra("confianzaNlp", 0f)
        textoUsuario = intent.getStringExtra("textoUsuario") ?: ""

        Log.d("RESULTADO_SENTIX", "UID: $uidActual")
        Log.d("RESULTADO_SENTIX", "Nivel final: $nivelFinal")
        Log.d("RESULTADO_SENTIX", "Puntaje interno: $puntajeFinal")
        Log.d("RESULTADO_SENTIX", "Facial: $emocionFacial / $confianzaFacial")
        Log.d("RESULTADO_SENTIX", "Test: $nivelTest / $puntajeTest/$puntajeMaximoTest")
        Log.d("RESULTADO_SENTIX", "NLP: $etiquetaNlp / $confianzaNlp")
    }

    private fun mostrarResultado() {
        val estilo = obtenerEstiloResultado(nivelFinal)
        val recomendacion = obtenerRecomendacionPersonalizada()

        imgResultadoIlustracion.setImageResource(estilo.imagenResultado)

        txtIconoResultado.text = estilo.icono
        txtNivelFinal.text = nivelFinal
        txtNivelFinal.setTextColor(estilo.colorPrincipal)

        txtMensajeResultado.text = obtenerMensajePrincipal(nivelFinal)

        imgIconoRecomendacion.setImageResource(estilo.iconoRecomendacion)
        aplicarFondoIconoRecomendacion(estilo)
        txtTituloRecomendacion.text = "Recomendación"
        txtTituloRecomendacion.setTextColor(estilo.colorPrincipal)
        txtMensajeRecomendacion.text = recomendacion

        aplicarFondoTarjeta(
            cardResultadoPrincipal,
            Color.TRANSPARENT,
            Color.TRANSPARENT
        )

        aplicarFondoTarjeta(
            cardRecomendacion,
            estilo.colorFondoRecomendacion,
            estilo.colorBorde
        )

        aplicarIndicadores(estilo)
    }
    private fun aplicarFondoIconoRecomendacion(estilo: EstiloResultado) {
        val fondoIcono = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(16).toFloat()
            setColor(estilo.colorFondoRecomendacion)
            setStroke(dpToPx(1), estilo.colorBorde)
        }

        imgIconoRecomendacion.background = fondoIcono
        imgIconoRecomendacion.setPadding(
            dpToPx(10),
            dpToPx(10),
            dpToPx(10),
            dpToPx(10)
        )
    }
    private fun obtenerMensajePrincipal(nivel: String): String {
        return when (nivel) {
            "Estado favorable" -> {
                "Tu evaluación muestra señales favorables en este momento."
            }

            "Señales leves" -> {
                "Se identificaron algunas señales leves. Puedes seguir observando cómo te sientes."
            }

            "Seguimiento recomendado" -> {
                "Se encontraron señales que podrían requerir seguimiento durante los próximos días."
            }

            "Atención recomendada" -> {
                "Se identificaron señales importantes. Considera buscar apoyo o hablar con alguien de confianza."
            }

            else -> {
                "Este resultado es una orientación preventiva basada en tu evaluación."
            }
        }
    }

    private fun obtenerRecomendacionPersonalizada(): String {
        val testAlto = nivelTest == "alto" || nivelTest == "moderado"
        val finalAlto = nivelFinal == "Atención recomendada" || nivelFinal == "Seguimiento recomendado"

        return when (etiquetaNlp) {
            "estres_academico" -> {
                if (testAlto || finalAlto) {
                    "Organiza tus pendientes por prioridad, realiza pausas breves y registra cómo te sientes durante los próximos días. Si la carga continúa afectándote, conversa con alguien de confianza."
                } else {
                    "Haz una pausa breve, ordena tus tareas principales y evita acumular pendientes para reducir la carga académica."
                }
            }

            "preocupacion" -> {
                if (testAlto || finalAlto) {
                    "Intenta identificar qué situación te preocupa más y compártela con una persona de confianza. Si la preocupación continúa, considera buscar orientación profesional."
                } else {
                    "Tómate unos minutos para respirar, ordenar tus ideas y enfócate en una acción pequeña que puedas realizar hoy."
                }
            }

            "desmotivacion" -> {
                if (testAlto || finalAlto) {
                    "Empieza con una meta pequeña y alcanzable. Si la falta de motivación persiste varios días, busca apoyo de alguien cercano o de un profesional."
                } else {
                    "Divide tus actividades en pasos pequeños y reconoce cada avance, aunque sea mínimo."
                }
            }

            "aislamiento" -> {
                if (testAlto || finalAlto) {
                    "Evita aislarte por completo. Intenta contactar a una persona de confianza y expresa cómo te has sentido, aunque sea con una frase breve."
                } else {
                    "Procura mantener contacto con alguien cercano y no guardar todo lo que sientes para ti."
                }
            }

            "bienestar" -> {
                when (nivelFinal) {
                    "Estado favorable" -> {
                        "Mantén tus hábitos de autocuidado y equilibra estudio, descanso y actividades que te hagan bien."
                    }

                    "Señales leves" -> {
                        "Aunque hay señales favorables, sigue observando tu descanso, energía y organización durante los próximos días."
                    }

                    else -> {
                        "Aunque tu texto muestra una señal positiva, el resultado general sugiere hacer seguimiento y cuidar tu descanso."
                    }
                }
            }

            "neutral" -> {
                when (nivelFinal) {
                    "Estado favorable" -> {
                        "Mantén una rutina estable y realiza una nueva evaluación si notas cambios en tu estado de ánimo."
                    }

                    "Señales leves" -> {
                        "Observa cómo evoluciona tu estado durante los próximos días y procura mantener horarios de descanso."
                    }

                    else -> {
                        "Aunque tu texto fue neutral, el resultado general sugiere observar tu bienestar y buscar apoyo si las señales se mantienen."
                    }
                }
            }

            else -> {
                when (nivelFinal) {
                    "Estado favorable" -> {
                        "Mantén tus hábitos de autocuidado y realiza seguimiento si notas cambios importantes."
                    }

                    "Señales leves" -> {
                        "Observa cómo te sientes durante los próximos días y procura descansar adecuadamente."
                    }

                    "Seguimiento recomendado" -> {
                        "Registra cómo te sientes en los próximos días y conversa con alguien de confianza si notas que las señales continúan."
                    }

                    "Atención recomendada" -> {
                        "Busca apoyo en una persona de confianza, tutor u orientación profesional si sientes que la situación te está afectando."
                    }

                    else -> {
                        "Este resultado es preventivo. Continúa observando tu bienestar y busca apoyo si lo necesitas."
                    }
                }
            }
        }
    }

    private fun aplicarIndicadores(estilo: EstiloResultado) {
        val gris = Color.parseColor("#D1D5DB")
        val azulTexto = Color.parseColor("#1F2937")

        val indicadores = listOf(
            indicadorFavorable,
            indicadorLeve,
            indicadorSeguimiento,
            indicadorAtencion
        )

        val labels = listOf(
            txtLabelFavorable,
            txtLabelLeve,
            txtLabelSeguimiento,
            txtLabelAtencion
        )

        indicadores.forEach { indicador ->
            indicador.background = crearCirculo(gris)
        }

        labels.forEach { label ->
            label.setTextColor(azulTexto)
            label.setTypeface(null, Typeface.NORMAL)
        }

        val indexActivo = when (nivelFinal) {
            "Estado favorable" -> 0
            "Señales leves" -> 1
            "Seguimiento recomendado" -> 2
            "Atención recomendada" -> 3
            else -> 0
        }

        indicadores[indexActivo].background = crearCirculo(estilo.colorPrincipal)
        labels[indexActivo].setTextColor(estilo.colorPrincipal)
        labels[indexActivo].setTypeface(null, Typeface.BOLD)
    }

    private fun aplicarFondoTarjeta(view: View, colorFondo: Int, colorBorde: Int) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(20).toFloat()
            setColor(colorFondo)
            setStroke(dpToPx(1), colorBorde)
        }

        view.background = drawable
    }

    private fun crearCirculo(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dpToPx(3), Color.WHITE)
        }
    }

    private fun obtenerEstiloResultado(nivel: String): EstiloResultado {
        return when (nivel) {
            "Estado favorable" -> EstiloResultado(
                colorPrincipal = Color.parseColor("#10B981"),
                colorFondo = Color.parseColor("#ECFDF5"),
                colorFondoRecomendacion = Color.parseColor("#F0FDF4"),
                colorBorde = Color.parseColor("#A7F3D0"),
                icono = "♡",
                iconoRecomendacion = R.drawable.ic_recomendacion_favorable,
                imagenResultado = R.drawable.img_resultado_favorable
            )

            "Señales leves" -> EstiloResultado(
                colorPrincipal = Color.parseColor("#2563EB"),
                colorFondo = Color.parseColor("#EFF6FF"),
                colorFondoRecomendacion = Color.parseColor("#EEF5FF"),
                colorBorde = Color.parseColor("#BFDBFE"),
                icono = "•",
                iconoRecomendacion = R.drawable.ic_recomendacion_leve,
                imagenResultado = R.drawable.img_resultado_leve
            )

            "Seguimiento recomendado" -> EstiloResultado(
                colorPrincipal = Color.parseColor("#F97316"),
                colorFondo = Color.parseColor("#FFF7ED"),
                colorFondoRecomendacion = Color.parseColor("#FFFBEB"),
                colorBorde = Color.parseColor("#FED7AA"),
                icono = "✓",
                iconoRecomendacion = R.drawable.ic_recomendacion_seguimiento,
                imagenResultado = R.drawable.img_resultado_seguimiento
            )

            "Atención recomendada" -> EstiloResultado(
                colorPrincipal = Color.parseColor("#F43F5E"),
                colorFondo = Color.parseColor("#FFF1F2"),
                colorFondoRecomendacion = Color.parseColor("#FFF1F2"),
                colorBorde = Color.parseColor("#FECDD3"),
                icono = "♡",
                iconoRecomendacion = R.drawable.ic_recomendacion_atencion,
                imagenResultado = R.drawable.img_resultado_atencion
            )

            else -> EstiloResultado(
                colorPrincipal = Color.parseColor("#2563EB"),
                colorFondo = Color.parseColor("#EFF6FF"),
                colorFondoRecomendacion = Color.parseColor("#EEF5FF"),
                colorBorde = Color.parseColor("#BFDBFE"),
                icono = "•",
                iconoRecomendacion = R.drawable.ic_recomendacion_default,
                imagenResultado = R.drawable.img_resultado_leve
            )
        }
    }

    private fun configurarEventos() {
        btnVolverInicio.setOnClickListener {
            val intent = Intent(this, SuccessActivity::class.java)
            intent.putExtra("uid", uidActual)
            intent.putExtra("email", emailActual)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        btnVerHistorial.setOnClickListener {
            Toast.makeText(
                this,
                "Próximamente podrás revisar tu historial emocional.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onMenuEvaluacionSeleccionada() {
        ocultarMenu()
    }

    data class EstiloResultado(
        val colorPrincipal: Int,
        val colorFondo: Int,
        val colorFondoRecomendacion: Int,
        val colorBorde: Int,
        val icono: String,
        val iconoRecomendacion: Int,
        val imagenResultado: Int
    )
}