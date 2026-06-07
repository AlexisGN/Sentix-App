package com.example.sentix

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class RecomendacionesActivity : BaseMenuActivity() {

    private lateinit var imgFotoUsuarioRecomendaciones: ImageView
    private lateinit var txtInicialesUsuarioRecomendaciones: TextView
    private lateinit var txtNombreUsuarioRecomendaciones: TextView
    private lateinit var txtCorreoUsuarioRecomendaciones: TextView

    private lateinit var txtResumenBase: TextView
    private lateinit var imgRecomendacionPrincipal: ImageView
    private lateinit var txtTituloRecomendacionPrincipal: TextView
    private lateinit var txtMensajeRecomendacionPrincipal: TextView
    private lateinit var txtEtiquetaContexto: TextView

    private lateinit var contenedorAccionesRapidas: LinearLayout

    private lateinit var txtPlanHoy: TextView
    private lateinit var txtPlanSemana: TextView
    private lateinit var txtPlanContinua: TextView

    private lateinit var btnNuevaEvaluacion: Button
    private lateinit var btnVerHistorial: Button

    private val evaluaciones = mutableListOf<HistorialEmocionalActivity.EvaluacionHistorial>()

    override fun getContenidoLayoutId(): Int {
        return R.layout.activity_recomendaciones
    }

    override fun onContenidoCreado() {
        enlazarVistas()
        cargarDatosUsuarioLocal()
        configurarEventos()
        cargarDatosParaRecomendaciones()
    }

    override fun onUsuarioActualizado(cache: UsuarioCache) {
        if (uidActual.isBlank() && cache.uid.isNotBlank()) {
            uidActual = cache.uid
        }

        if (emailActual.isBlank() && cache.email.isNotBlank()) {
            emailActual = cache.email
        }

        mostrarUsuarioEnCabecera(cache)
    }

    private fun enlazarVistas() {
        imgFotoUsuarioRecomendaciones = findViewById(R.id.imgFotoUsuarioRecomendaciones)
        txtInicialesUsuarioRecomendaciones = findViewById(R.id.txtInicialesUsuarioRecomendaciones)
        txtNombreUsuarioRecomendaciones = findViewById(R.id.txtNombreUsuarioRecomendaciones)
        txtCorreoUsuarioRecomendaciones = findViewById(R.id.txtCorreoUsuarioRecomendaciones)

        txtResumenBase = findViewById(R.id.txtResumenBase)
        imgRecomendacionPrincipal = findViewById(R.id.imgRecomendacionPrincipal)
        txtTituloRecomendacionPrincipal = findViewById(R.id.txtTituloRecomendacionPrincipal)
        txtMensajeRecomendacionPrincipal = findViewById(R.id.txtMensajeRecomendacionPrincipal)
        txtEtiquetaContexto = findViewById(R.id.txtEtiquetaContexto)

        contenedorAccionesRapidas = findViewById(R.id.contenedorAccionesRapidas)

        txtPlanHoy = findViewById(R.id.txtPlanHoy)
        txtPlanSemana = findViewById(R.id.txtPlanSemana)
        txtPlanContinua = findViewById(R.id.txtPlanContinua)

        btnNuevaEvaluacion = findViewById(R.id.btnNuevaEvaluacion)
        btnVerHistorial = findViewById(R.id.btnVerHistorial)
    }

    private fun cargarDatosUsuarioLocal() {
        val cache = UsuarioCacheManager.obtener(this)

        if (uidActual.isBlank() && cache.uid.isNotBlank()) {
            uidActual = cache.uid
        }

        if (emailActual.isBlank() && cache.email.isNotBlank()) {
            emailActual = cache.email
        }

        mostrarUsuarioEnCabecera(cache)

        Log.d("RECOMENDACIONES_CACHE", "UID: $uidActual")
        Log.d("RECOMENDACIONES_CACHE", "Email: $emailActual")
    }

    private fun mostrarUsuarioEnCabecera(cache: UsuarioCache) {
        val nombreMostrado = when {
            cache.nombreCompleto.isNotBlank() -> cache.nombreCompleto
            cache.nombre.isNotBlank() -> cache.nombre
            emailActual.isNotBlank() -> emailActual.substringBefore("@")
            else -> "Usuario Sentix"
        }

        val correoMostrado = when {
            cache.email.isNotBlank() -> cache.email
            emailActual.isNotBlank() -> emailActual
            else -> "Cuenta activa"
        }

        txtNombreUsuarioRecomendaciones.text = nombreMostrado
        txtCorreoUsuarioRecomendaciones.text = correoMostrado
        txtInicialesUsuarioRecomendaciones.text = obtenerIniciales(nombreMostrado)

        if (cache.fotoPerfilUrl.isNotBlank()) {
            txtInicialesUsuarioRecomendaciones.visibility = View.GONE
            imgFotoUsuarioRecomendaciones.visibility = View.VISIBLE

            Glide.with(this)
                .load(cache.fotoPerfilUrl)
                .placeholder(R.drawable.ic_usermenu)
                .error(R.drawable.ic_usermenu)
                .circleCrop()
                .into(imgFotoUsuarioRecomendaciones)
        } else {
            imgFotoUsuarioRecomendaciones.visibility = View.GONE
            txtInicialesUsuarioRecomendaciones.visibility = View.VISIBLE
        }
    }

    private fun obtenerIniciales(nombreCompleto: String): String {
        val partes = nombreCompleto.trim()
            .split(" ")
            .filter { it.isNotBlank() }

        return when {
            partes.size >= 2 -> "${partes[0].first()}${partes[1].first()}".uppercase()
            partes.size == 1 -> partes[0].take(2).uppercase()
            else -> "SX"
        }
    }

    private fun configurarEventos() {
        btnNuevaEvaluacion.setOnClickListener {
            val intent = Intent(this, EvaluacionCamaraActivity::class.java)
            intent.putExtra("uid", uidActual)
            intent.putExtra("email", emailActual)
            startActivity(intent)
        }

        btnVerHistorial.setOnClickListener {
            val intent = Intent(this, HistorialEmocionalActivity::class.java)
            intent.putExtra("uid", uidActual)
            intent.putExtra("email", emailActual)
            startActivity(intent)
        }
    }

    private fun cargarDatosParaRecomendaciones() {
        if (uidActual.isBlank()) {
            mostrarSinDatos()
            return
        }

        val historialCache = HistorialCacheManager.obtener(this, uidActual)

        if (historialCache.isNotEmpty()) {
            evaluaciones.clear()
            evaluaciones.addAll(historialCache.sortedByDescending { it.timestamp })
            generarRecomendaciones()
        } else {
            mostrarCargando()
        }

        cargarDesdeFirebase()
    }

    private fun cargarDesdeFirebase() {
        if (uidActual.isBlank()) {
            mostrarSinDatos()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(uidActual)
            .collection("historial_emocional")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(40)
            .get()
            .addOnSuccessListener { snapshot ->
                evaluaciones.clear()

                for (documento in snapshot.documents) {
                    evaluaciones.add(
                        HistorialEmocionalActivity.EvaluacionHistorial(
                            id = documento.id,
                            fecha = documento.getString("fecha") ?: "",
                            hora = documento.getString("hora") ?: "",
                            fechaHora = documento.getString("fechaHora") ?: "",
                            timestamp = documento.getLong("timestamp") ?: 0L,
                            nivelFinal = documento.getString("nivelFinal") ?: "Resultado orientativo",
                            puntajeFinalInterno = (documento.getDouble("puntajeFinalInterno") ?: 0.0).toFloat(),
                            nivelTestVisible = documento.getString("nivelTestVisible") ?: "",
                            etiquetaNlp = documento.getString("etiquetaNlp") ?: "",
                            etiquetaNlpTraducida = documento.getString("etiquetaNlpTraducida") ?: "",
                            textoUsuario = documento.getString("textoUsuario") ?: "",
                            emocionFacialTraducida = documento.getString("emocionFacialTraducida") ?: ""
                        )
                    )
                }

                HistorialCacheManager.guardar(this, uidActual, evaluaciones)

                if (evaluaciones.isEmpty()) {
                    mostrarSinDatos()
                } else {
                    generarRecomendaciones()
                }
            }
            .addOnFailureListener { e ->
                Log.e("RECOMENDACIONES_FIREBASE", "Error cargando historial", e)

                if (evaluaciones.isEmpty()) {
                    mostrarSinDatos()
                }

                Toast.makeText(
                    this,
                    "No se pudo actualizar recomendaciones.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun mostrarCargando() {
        txtResumenBase.text = "Cargando tus recomendaciones..."
        imgRecomendacionPrincipal.setImageResource(R.drawable.img_reco_bienestar)
        txtTituloRecomendacionPrincipal.text = "Preparando orientación"
        txtTituloRecomendacionPrincipal.setTextColor(Color.parseColor("#2563EB"))
        txtMensajeRecomendacionPrincipal.text =
            "Estamos revisando tus evaluaciones recientes para generar una sugerencia personalizada."
        txtEtiquetaContexto.text = "Basado en tu historial emocional"

        contenedorAccionesRapidas.removeAllViews()

        txtPlanHoy.text = "Hoy: espera unos segundos mientras se actualizan tus datos."
        txtPlanSemana.text = "Esta semana: tus recomendaciones se mostrarán al cargar tu historial."
        txtPlanContinua.text = "Si continúa: realiza una nueva evaluación para mejorar la orientación."
    }

    private fun mostrarSinDatos() {
        txtResumenBase.text = "Aún no hay suficientes evaluaciones para personalizar tus recomendaciones."
        imgRecomendacionPrincipal.setImageResource(R.drawable.img_reco_bienestar)
        txtTituloRecomendacionPrincipal.text = "Realiza una evaluación"
        txtTituloRecomendacionPrincipal.setTextColor(Color.parseColor("#2563EB"))
        txtMensajeRecomendacionPrincipal.text =
            "Completa una evaluación emocional para que Sentix pueda generar recomendaciones según tu estado reciente."
        txtEtiquetaContexto.text = "Sin historial disponible"

        contenedorAccionesRapidas.removeAllViews()
        agregarAccionRapida(
            icono = R.drawable.ic_reco_evaluacion,
            titulo = "Iniciar evaluación",
            mensaje = "Registra tu primera evaluación emocional.",
            color = Color.parseColor("#2563EB")
        )

        agregarAccionRapida(
            icono = R.drawable.ic_reco_rutina,
            titulo = "Mantén una rutina",
            mensaje = "Procura equilibrar estudio, descanso y actividades personales.",
            color = Color.parseColor("#10B981")
        )

        txtPlanHoy.text = "Hoy: realiza una evaluación para iniciar tu historial."
        txtPlanSemana.text = "Esta semana: registra cómo te sientes si notas cambios."
        txtPlanContinua.text = "Si continúa: busca apoyo si sientes malestar persistente."
    }

    private fun generarRecomendaciones() {
        if (evaluaciones.isEmpty()) {
            mostrarSinDatos()
            return
        }

        val recientes = evaluaciones
            .sortedByDescending { it.timestamp }
            .take(10)

        val ultima = recientes.first()

        val etiquetaDominante = recientes
            .groupingBy { it.etiquetaNlp.ifBlank { "neutral" } }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "neutral"

        val nivelDominante = recientes
            .groupingBy { it.nivelFinal }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: ultima.nivelFinal

        val cantidadAtencion = recientes.count { it.nivelFinal == "Atención recomendada" }
        val cantidadSeguimiento = recientes.count { it.nivelFinal == "Seguimiento recomendado" }
        val cantidadEvaluaciones = recientes.size

        val recomendacion = construirRecomendacion(
            etiquetaDominante = etiquetaDominante,
            nivelDominante = nivelDominante,
            ultima = ultima,
            cantidadAtencion = cantidadAtencion,
            cantidadSeguimiento = cantidadSeguimiento,
            cantidadEvaluaciones = cantidadEvaluaciones
        )

        aplicarRecomendacion(recomendacion)
    }

    private fun construirRecomendacion(
        etiquetaDominante: String,
        nivelDominante: String,
        ultima: HistorialEmocionalActivity.EvaluacionHistorial,
        cantidadAtencion: Int,
        cantidadSeguimiento: Int,
        cantidadEvaluaciones: Int
    ): RecomendacionSentix {
        val contexto = when {
            cantidadAtencion >= 2 -> "Se observaron señales importantes en más de una evaluación reciente."
            cantidadSeguimiento >= 2 -> "Se observaron señales que conviene seguir durante los próximos días."
            else -> "Basado en tus últimas $cantidadEvaluaciones evaluaciones guardadas."
        }

        return when (etiquetaDominante) {
            "estres_academico" -> RecomendacionSentix(
                color = Color.parseColor("#F97316"),
                imagen = R.drawable.img_reco_estres_academico,
                titulo = "Organiza tu carga académica",
                mensaje = "En tus evaluaciones recientes aparecen señales relacionadas con pendientes, presión o carga académica. Prioriza tus tareas principales y evita intentar resolver todo al mismo tiempo.",
                contexto = contexto,
                acciones = listOf(
                    AccionRapida(R.drawable.ic_reco_priorizar, "Prioriza tareas", "Elige 2 o 3 actividades importantes para hoy."),
                    AccionRapida(R.drawable.ic_reco_pausas, "Haz pausas", "Descansa unos minutos entre bloques de estudio."),
                    AccionRapida(R.drawable.ic_reco_dividir_tareas, "Divide pendientes", "Convierte tareas grandes en pasos pequeños.")
                ),
                planHoy = "Hoy: escribe tus pendientes y marca cuáles son realmente urgentes.",
                planSemana = "Esta semana: revisa si la carga académica se repite en tus evaluaciones.",
                planContinua = "Si continúa: conversa con un tutor, docente o persona de confianza para organizar mejor tus responsabilidades."
            )

            "preocupacion" -> RecomendacionSentix(
                color = Color.parseColor("#2563EB"),
                imagen = R.drawable.img_reco_preocupacion,
                titulo = "Reduce la preocupación principal",
                mensaje = "Tus evaluaciones muestran señales de preocupación o inquietud. Intenta identificar qué situación te preocupa más y enfócate en una acción pequeña que puedas realizar hoy.",
                contexto = contexto,
                acciones = listOf(
                    AccionRapida(R.drawable.ic_reco_respirar, "Ordena tu mente", "Respira y separa lo urgente de lo que puede esperar."),
                    AccionRapida(R.drawable.ic_reco_hablar, "Habla con alguien", "Comparte lo que sientes con una persona de confianza."),
                    AccionRapida(R.drawable.ic_reco_priorizar, "Acción pequeña", "Elige una acción concreta que puedas hacer hoy.")
                ),
                planHoy = "Hoy: identifica una preocupación principal y una acción posible.",
                planSemana = "Esta semana: observa si la preocupación disminuye o se mantiene.",
                planContinua = "Si continúa: considera pedir orientación profesional o apoyo académico."
            )

            "desmotivacion" -> RecomendacionSentix(
                color = Color.parseColor("#8B5CF6"),
                imagen = R.drawable.img_reco_desmotivacion,
                titulo = "Empieza con pasos pequeños",
                mensaje = "Se observan señales asociadas a baja motivación. No necesitas resolver todo de golpe; empezar con una meta pequeña puede ayudarte a recuperar ritmo.",
                contexto = contexto,
                acciones = listOf(
                    AccionRapida(R.drawable.ic_reco_meta_pequena, "Meta pequeña", "Elige una actividad simple para empezar."),
                    AccionRapida(R.drawable.ic_reco_rutina, "Crea rutina", "Repite una acción breve para recuperar continuidad."),
                    AccionRapida(R.drawable.ic_reco_pausas, "Reduce presión", "Empieza de forma gradual sin exigirte demasiado.")
                ),
                planHoy = "Hoy: realiza una tarea pequeña que puedas terminar en poco tiempo.",
                planSemana = "Esta semana: registra si tu motivación mejora con metas más simples.",
                planContinua = "Si continúa: busca apoyo de alguien cercano o de un profesional."
            )

            "aislamiento" -> RecomendacionSentix(
                color = Color.parseColor("#06B6D4"),
                imagen = R.drawable.img_reco_aislamiento,
                titulo = "Mantén contacto gradual",
                mensaje = "Tus registros muestran señales relacionadas con distancia social o aislamiento. Intenta no guardar todo lo que sientes y mantén contacto con alguien cercano.",
                contexto = contexto,
                acciones = listOf(
                    AccionRapida(R.drawable.ic_reco_contacto, "Mensaje breve", "Escribe a alguien de confianza."),
                    AccionRapida(R.drawable.ic_reco_hablar, "No guardes todo", "Comparte una parte de lo que sientes."),
                    AccionRapida(R.drawable.ic_reco_respirar, "Ve con calma", "Empieza con una conversación corta.")
                ),
                planHoy = "Hoy: envía un mensaje simple a una persona cercana.",
                planSemana = "Esta semana: observa si el aislamiento aumenta o disminuye.",
                planContinua = "Si continúa: considera buscar acompañamiento o apoyo profesional."
            )

            "bienestar" -> RecomendacionSentix(
                color = Color.parseColor("#10B981"),
                imagen = R.drawable.img_reco_bienestar,
                titulo = "Mantén tus hábitos positivos",
                mensaje = "Tus evaluaciones muestran señales favorables. Continúa cuidando tu descanso, organización y actividades que te ayudan a sentirte bien.",
                contexto = contexto,
                acciones = listOf(
                    AccionRapida(R.drawable.ic_reco_rutina, "Mantén rutina", "Conserva hábitos que te funcionan."),
                    AccionRapida(R.drawable.ic_reco_descanso, "Descansa bien", "Cuida tus horarios de sueño."),
                    AccionRapida(R.drawable.ic_reco_evaluacion, "Registra cambios", "Evalúate si notas variaciones importantes.")
                ),
                planHoy = "Hoy: mantén una actividad que te haga sentir bien.",
                planSemana = "Esta semana: conserva equilibrio entre estudio, descanso y vida personal.",
                planContinua = "Si notas cambios: realiza una nueva evaluación para comparar tu evolución."
            )

            else -> RecomendacionSentix(
                color = obtenerColorNivel(nivelDominante.ifBlank { ultima.nivelFinal }),
                imagen = R.drawable.img_reco_bienestar,
                titulo = "Observa tu bienestar",
                mensaje = "Tus evaluaciones recientes no muestran un solo patrón dominante. Mantén seguimiento de cómo te sientes y registra una nueva evaluación si notas cambios.",
                contexto = contexto,
                acciones = listOf(
                    AccionRapida(R.drawable.ic_reco_evaluacion, "Observa cambios", "Presta atención a energía, sueño y ánimo."),
                    AccionRapida(R.drawable.ic_reco_rutina, "Mantén estabilidad", "Cuida tus rutinas diarias."),
                    AccionRapida(R.drawable.ic_reco_descanso, "Descansa", "Procura recuperar energía física y mental.")
                ),
                planHoy = "Hoy: observa cómo te sientes sin exigirte demasiado.",
                planSemana = "Esta semana: revisa si aparece algún patrón repetido.",
                planContinua = "Si se repite el malestar: conversa con alguien de confianza."
            )
        }
    }

    private fun aplicarRecomendacion(recomendacion: RecomendacionSentix) {
        txtResumenBase.text = recomendacion.contexto
        imgRecomendacionPrincipal.setImageResource(recomendacion.imagen)

        txtTituloRecomendacionPrincipal.text = recomendacion.titulo
        txtTituloRecomendacionPrincipal.setTextColor(recomendacion.color)

        txtMensajeRecomendacionPrincipal.text = recomendacion.mensaje
        txtEtiquetaContexto.text = "Orientación personalizada según tu historial emocional"

        contenedorAccionesRapidas.removeAllViews()

        recomendacion.acciones.forEach { accion ->
            agregarAccionRapida(
                icono = accion.icono,
                titulo = accion.titulo,
                mensaje = accion.mensaje,
                color = recomendacion.color
            )
        }

        txtPlanHoy.text = "Hoy: ${recomendacion.planHoy.removePrefix("Hoy:").trim()}"
        txtPlanSemana.text = "Esta semana: ${recomendacion.planSemana.removePrefix("Esta semana:").trim()}"
        txtPlanContinua.text = "Si continúa: ${recomendacion.planContinua.removePrefix("Si continúa:").trim()}"
    }

    private fun agregarAccionRapida(
        icono: Int,
        titulo: String,
        mensaje: String,
        color: Int
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = crearFondoRedondeado(
                colorFondo = Color.WHITE,
                colorBorde = ajustarAlpha(color, 90),
                radio = 18
            )
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
        }

        val contenedorIcono = FrameLayout(this).apply {
            background = crearFondoRedondeado(
                colorFondo = ajustarAlpha(color, 35),
                colorBorde = ajustarAlpha(color, 80),
                radio = 16
            )
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(48),
                dpToPx(48)
            ).apply {
                marginEnd = dpToPx(12)
            }
        }

        val imgIcono = ImageView(this).apply {
            setImageResource(icono)
            setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        contenedorIcono.addView(imgIcono)

        val textos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val txtTitulo = TextView(this).apply {
            text = titulo
            setTextColor(color)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
        }

        val txtMensaje = TextView(this).apply {
            text = mensaje
            setTextColor(Color.parseColor("#1F2937"))
            textSize = 13f
            setPadding(0, dpToPx(4), 0, 0)
        }

        textos.addView(txtTitulo)
        textos.addView(txtMensaje)

        card.addView(contenedorIcono)
        card.addView(textos)

        contenedorAccionesRapidas.addView(card)
    }

    private fun obtenerColorNivel(nivel: String): Int {
        return when (nivel) {
            "Estado favorable" -> Color.parseColor("#10B981")
            "Señales leves" -> Color.parseColor("#2563EB")
            "Seguimiento recomendado" -> Color.parseColor("#F97316")
            "Atención recomendada" -> Color.parseColor("#F43F5E")
            else -> Color.parseColor("#2563EB")
        }
    }

    private fun crearFondoRedondeado(
        colorFondo: Int,
        colorBorde: Int,
        radio: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(radio).toFloat()
            setColor(colorFondo)
            setStroke(dpToPx(1), colorBorde)
        }
    }

    private fun ajustarAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onMenuRecomendacionesSeleccionada() {
        ocultarMenu()
    }

    data class AccionRapida(
        val icono: Int,
        val titulo: String,
        val mensaje: String
    )

    data class RecomendacionSentix(
        val color: Int,
        val imagen: Int,
        val titulo: String,
        val mensaje: String,
        val contexto: String,
        val acciones: List<AccionRapida>,
        val planHoy: String,
        val planSemana: String,
        val planContinua: String
    )
}