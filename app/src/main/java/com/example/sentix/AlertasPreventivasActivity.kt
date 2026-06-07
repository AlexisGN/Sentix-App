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

class AlertasPreventivasActivity : BaseMenuActivity() {

    private lateinit var imgFotoUsuarioAlertas: ImageView
    private lateinit var txtInicialesUsuarioAlertas: TextView
    private lateinit var txtNombreUsuarioAlertas: TextView
    private lateinit var txtCorreoUsuarioAlertas: TextView

    private lateinit var imgIconoAlertaPrincipal: ImageView
    private lateinit var txtEtiquetaAlerta: TextView
    private lateinit var txtTituloAlertaPrincipal: TextView
    private lateinit var txtMensajeAlertaPrincipal: TextView
    private lateinit var txtBaseAlerta: TextView
    private lateinit var contenedorSenales: LinearLayout
    private lateinit var txtAccionSugerida: TextView

    private lateinit var btnVerRecomendaciones: Button
    private lateinit var btnNuevaEvaluacion: Button

    private val evaluaciones = mutableListOf<HistorialEmocionalActivity.EvaluacionHistorial>()

    override fun getContenidoLayoutId(): Int {
        return R.layout.activity_alertas_preventivas
    }

    override fun onContenidoCreado() {
        enlazarVistas()
        cargarDatosUsuarioLocal()
        configurarEventos()
        cargarDatosParaAlertas()
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
        imgFotoUsuarioAlertas = findViewById(R.id.imgFotoUsuarioAlertas)
        txtInicialesUsuarioAlertas = findViewById(R.id.txtInicialesUsuarioAlertas)
        txtNombreUsuarioAlertas = findViewById(R.id.txtNombreUsuarioAlertas)
        txtCorreoUsuarioAlertas = findViewById(R.id.txtCorreoUsuarioAlertas)

        imgIconoAlertaPrincipal = findViewById(R.id.imgIconoAlertaPrincipal)
        txtEtiquetaAlerta = findViewById(R.id.txtEtiquetaAlerta)
        txtTituloAlertaPrincipal = findViewById(R.id.txtTituloAlertaPrincipal)
        txtMensajeAlertaPrincipal = findViewById(R.id.txtMensajeAlertaPrincipal)
        txtBaseAlerta = findViewById(R.id.txtBaseAlerta)
        contenedorSenales = findViewById(R.id.contenedorSenales)
        txtAccionSugerida = findViewById(R.id.txtAccionSugerida)

        btnVerRecomendaciones = findViewById(R.id.btnVerRecomendaciones)
        btnNuevaEvaluacion = findViewById(R.id.btnNuevaEvaluacion)
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

        Log.d("ALERTAS_CACHE", "UID: $uidActual")
        Log.d("ALERTAS_CACHE", "Email: $emailActual")
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

        txtNombreUsuarioAlertas.text = nombreMostrado
        txtCorreoUsuarioAlertas.text = correoMostrado
        txtInicialesUsuarioAlertas.text = obtenerIniciales(nombreMostrado)

        if (cache.fotoPerfilUrl.isNotBlank()) {
            txtInicialesUsuarioAlertas.visibility = View.GONE
            imgFotoUsuarioAlertas.visibility = View.VISIBLE

            Glide.with(this)
                .load(cache.fotoPerfilUrl)
                .placeholder(R.drawable.ic_usermenu)
                .error(R.drawable.ic_usermenu)
                .circleCrop()
                .into(imgFotoUsuarioAlertas)
        } else {
            imgFotoUsuarioAlertas.visibility = View.GONE
            txtInicialesUsuarioAlertas.visibility = View.VISIBLE
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

        btnVerRecomendaciones.setOnClickListener {
            val intent = Intent(this, RecomendacionesActivity::class.java)
            intent.putExtra("uid", uidActual)
            intent.putExtra("email", emailActual)
            startActivity(intent)
        }
    }

    private fun cargarDatosParaAlertas() {
        if (uidActual.isBlank()) {
            mostrarSinDatos()
            return
        }

        val cache = HistorialCacheManager.obtener(this, uidActual)

        if (cache.isNotEmpty()) {
            evaluaciones.clear()
            evaluaciones.addAll(cache.sortedByDescending { it.timestamp })
            analizarAlertas()
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
                    analizarAlertas()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ALERTAS_FIREBASE", "Error cargando historial", e)

                if (evaluaciones.isEmpty()) {
                    mostrarSinDatos()
                }

                Toast.makeText(
                    this,
                    "No se pudo actualizar alertas.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun mostrarCargando() {
        aplicarAlerta(
            AlertaPreventiva(
                titulo = "Revisando señales",
                mensaje = "Estamos revisando tus evaluaciones recientes para detectar señales preventivas.",
                base = "Cargando información del historial emocional.",
                accion = "Espera unos segundos mientras Sentix actualiza tus datos.",
                color = Color.parseColor("#2563EB"),
                icono = R.drawable.ic_alerta_historial,
                senales = listOf(
                    SenalRevisada(R.drawable.ic_alerta_historial, "Historial", "Buscando evaluaciones recientes.")
                )
            )
        )
    }

    private fun mostrarSinDatos() {
        aplicarAlerta(
            AlertaPreventiva(
                titulo = "Sin alertas activas",
                mensaje = "Aún no hay evaluaciones suficientes para generar alertas preventivas.",
                base = "Completa una evaluación emocional para iniciar el seguimiento preventivo.",
                accion = "Realiza una evaluación para que Sentix pueda analizar señales recientes.",
                color = Color.parseColor("#2563EB"),
                icono = R.drawable.ic_alerta_historial,
                senales = listOf(
                    SenalRevisada(R.drawable.ic_reco_evaluacion, "Evaluación pendiente", "Todavía no hay registros emocionales suficientes."),
                    SenalRevisada(R.drawable.ic_alerta_estable, "Estado", "Sin señales preventivas activas por ahora.")
                )
            )
        )
    }

    private fun analizarAlertas() {
        if (evaluaciones.isEmpty()) {
            mostrarSinDatos()
            return
        }

        val recientes = evaluaciones
            .sortedByDescending { it.timestamp }
            .take(10)

        val ultimas5 = recientes.take(5)
        val cantidadAtencion = ultimas5.count { it.nivelFinal == "Atención recomendada" }
        val cantidadSeguimiento = recientes.count { it.nivelFinal == "Seguimiento recomendado" }
        val cantidadLeves = recientes.count { it.nivelFinal == "Señales leves" }
        val cantidadFavorables = recientes.count { it.nivelFinal == "Estado favorable" }

        val cantidadAislamiento = recientes.count { it.etiquetaNlp == "aislamiento" }
        val cantidadDesmotivacion = recientes.count { it.etiquetaNlp == "desmotivacion" }
        val cantidadEstres = recientes.count { it.etiquetaNlp == "estres_academico" }
        val cantidadPreocupacion = recientes.count { it.etiquetaNlp == "preocupacion" }

        val alerta = when {
            cantidadAtencion >= 2 -> crearAlertaAtencion(cantidadAtencion, recientes.size)

            cantidadAislamiento >= 2 || cantidadDesmotivacion >= 2 -> {
                crearAlertaAcompanamiento(
                    aislamiento = cantidadAislamiento,
                    desmotivacion = cantidadDesmotivacion,
                    total = recientes.size
                )
            }

            cantidadSeguimiento >= 3 -> crearAlertaSeguimiento(cantidadSeguimiento, recientes.size)

            cantidadEstres >= 3 || cantidadPreocupacion >= 3 -> {
                crearAlertaCargaEmocional(
                    estres = cantidadEstres,
                    preocupacion = cantidadPreocupacion,
                    total = recientes.size
                )
            }

            else -> crearAlertaEstable(
                favorables = cantidadFavorables,
                leves = cantidadLeves,
                seguimiento = cantidadSeguimiento,
                atencion = cantidadAtencion,
                total = recientes.size
            )
        }

        aplicarAlerta(alerta)
    }

    private fun crearAlertaAtencion(cantidad: Int, total: Int): AlertaPreventiva {
        return AlertaPreventiva(
            titulo = "Atención preventiva",
            mensaje = "Se repiten señales importantes en tus evaluaciones recientes. Puede ser útil conversar con alguien de confianza o buscar orientación profesional.",
            base = "Se identificaron $cantidad evaluaciones con atención recomendada dentro de los últimos registros revisados.",
            accion = "Busca apoyo de una persona de confianza y revisa tus recomendaciones personalizadas.",
            color = Color.parseColor("#F43F5E"),
            icono = R.drawable.ic_alerta_atencion,
            senales = listOf(
                SenalRevisada(R.drawable.ic_alerta_atencion, "Señales importantes", "$cantidad de $total evaluaciones recientes muestran atención recomendada."),
                SenalRevisada(R.drawable.ic_alerta_recomendacion, "Siguiente paso", "Revisar recomendaciones puede ayudarte a ordenar acciones de cuidado.")
            )
        )
    }

    private fun crearAlertaAcompanamiento(
        aislamiento: Int,
        desmotivacion: Int,
        total: Int
    ): AlertaPreventiva {
        return AlertaPreventiva(
            titulo = "Acompañamiento sugerido",
            mensaje = "Se observan señales repetidas relacionadas con aislamiento o desmotivación. Mantener contacto con alguien cercano puede ayudarte a no cargar todo solo.",
            base = "En los últimos registros revisados aparecen $aislamiento señales de aislamiento y $desmotivacion de desmotivación.",
            accion = "Intenta contactar a una persona de confianza y expresa cómo te has sentido de forma breve.",
            color = Color.parseColor("#8B5CF6"),
            icono = R.drawable.ic_alerta_apoyo,
            senales = listOf(
                SenalRevisada(R.drawable.ic_reco_contacto, "Contacto social", "Se revisaron patrones de aislamiento en tus evaluaciones."),
                SenalRevisada(R.drawable.ic_reco_meta_pequena, "Motivación", "Se revisaron señales de baja motivación reciente.")
            )
        )
    }

    private fun crearAlertaSeguimiento(cantidad: Int, total: Int): AlertaPreventiva {
        return AlertaPreventiva(
            titulo = "Seguimiento sugerido",
            mensaje = "Se repiten señales que conviene observar durante los próximos días. No significa un diagnóstico, pero sí puede ayudarte mantener registro.",
            base = "Se encontraron $cantidad evaluaciones con seguimiento recomendado en los últimos $total registros revisados.",
            accion = "Realiza una nueva evaluación en los próximos días y revisa si las señales disminuyen o se mantienen.",
            color = Color.parseColor("#F97316"),
            icono = R.drawable.ic_alerta_seguimiento,
            senales = listOf(
                SenalRevisada(R.drawable.ic_alerta_seguimiento, "Seguimiento", "$cantidad registros recientes sugieren observación preventiva."),
                SenalRevisada(R.drawable.ic_alerta_historial, "Historial", "Comparar tus registros puede ayudarte a notar cambios.")
            )
        )
    }

    private fun crearAlertaCargaEmocional(
        estres: Int,
        preocupacion: Int,
        total: Int
    ): AlertaPreventiva {
        return AlertaPreventiva(
            titulo = "Carga emocional repetida",
            mensaje = "Tus evaluaciones muestran señales repetidas de estrés académico o preocupación. Conviene revisar qué situaciones se están acumulando.",
            base = "Se detectaron $estres señales de estrés académico y $preocupacion de preocupación en los últimos registros.",
            accion = "Ordena tus pendientes, prioriza una acción pequeña y conversa con alguien si la preocupación continúa.",
            color = Color.parseColor("#2563EB"),
            icono = R.drawable.ic_alerta_recomendacion,
            senales = listOf(
                SenalRevisada(R.drawable.ic_reco_priorizar, "Carga académica", "Se revisaron señales relacionadas con presión o pendientes."),
                SenalRevisada(R.drawable.ic_reco_respirar, "Preocupación", "Se revisaron señales de inquietud o incertidumbre.")
            )
        )
    }

    private fun crearAlertaEstable(
        favorables: Int,
        leves: Int,
        seguimiento: Int,
        atencion: Int,
        total: Int
    ): AlertaPreventiva {
        return AlertaPreventiva(
            titulo = "Sin alertas activas",
            mensaje = "No se detectan señales repetidas que requieran una alerta preventiva en este momento.",
            base = "Se revisaron $total evaluaciones recientes. Favorables: $favorables, señales leves: $leves, seguimiento: $seguimiento, atención: $atencion.",
            accion = "Continúa observando tu bienestar y realiza una evaluación si notas cambios importantes.",
            color = Color.parseColor("#10B981"),
            icono = R.drawable.ic_alerta_estable,
            senales = listOf(
                SenalRevisada(
                    R.drawable.ic_alerta_estable,
                    "Estado actual",
                    "No hay señales repetidas que activen una alerta preventiva."
                ),
                SenalRevisada(
                    R.drawable.ic_alerta_historial,
                    "Historial revisado",
                    "Se analizaron tus evaluaciones recientes para detectar patrones repetidos."
                )
            )
        )
    }

    private fun aplicarAlerta(alerta: AlertaPreventiva) {
        imgIconoAlertaPrincipal.setImageResource(alerta.icono)
        imgIconoAlertaPrincipal.background = crearFondoRedondeado(
            colorFondo = ajustarAlpha(alerta.color, 35),
            colorBorde = ajustarAlpha(alerta.color, 90),
            radio = 24
        )

        txtEtiquetaAlerta.text = "Alerta preventiva"
        txtTituloAlertaPrincipal.text = alerta.titulo
        txtTituloAlertaPrincipal.setTextColor(alerta.color)
        txtMensajeAlertaPrincipal.text = alerta.mensaje
        txtBaseAlerta.text = alerta.base
        txtAccionSugerida.text = alerta.accion

        contenedorSenales.removeAllViews()

        alerta.senales.forEach { senal ->
            agregarSenalRevisada(
                icono = senal.icono,
                titulo = senal.titulo,
                mensaje = senal.mensaje,
                color = alerta.color
            )
        }
    }

    private fun agregarSenalRevisada(
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

        contenedorSenales.addView(card)
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

    override fun onMenuAlertasSeleccionado() {
        ocultarMenu()
    }

    data class AlertaPreventiva(
        val titulo: String,
        val mensaje: String,
        val base: String,
        val accion: String,
        val color: Int,
        val icono: Int,
        val senales: List<SenalRevisada>
    )

    data class SenalRevisada(
        val icono: Int,
        val titulo: String,
        val mensaje: String
    )
}