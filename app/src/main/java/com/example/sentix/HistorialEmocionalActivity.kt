package com.example.sentix

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistorialEmocionalActivity : BaseMenuActivity() {

    private lateinit var imgFotoUsuarioHistorial: ImageView
    private lateinit var txtInicialesUsuarioHistorial: TextView
    private lateinit var txtNombreUsuarioHistorial: TextView
    private lateinit var txtCorreoUsuarioHistorial: TextView

    private lateinit var contenedorGrafico: LinearLayout
    private lateinit var txtMesSemana: TextView
    private lateinit var btnSemanaAnterior: TextView
    private lateinit var btnSemanaSiguiente: TextView

    private lateinit var panelListaDia: LinearLayout
    private lateinit var panelDetalleEvaluacion: LinearLayout
    private lateinit var btnVolverLista: TextView

    private lateinit var txtTituloDiaSeleccionado: TextView
    private lateinit var contenedorEvaluacionesDia: LinearLayout

    private lateinit var txtFechaDetalle: TextView
    private lateinit var txtNivelDetalle: TextView
    private lateinit var txtTextoUsuarioDetalle: TextView
    private lateinit var txtTestDetalle: TextView
    private lateinit var txtTextoDetalle: TextView
    private lateinit var txtImagenDetalle: TextView

    private lateinit var btnNuevaEvaluacion: Button

    private val evaluaciones = mutableListOf<EvaluacionHistorial>()

    private var fechaInicioSemana: LocalDate = LocalDate.now().minusDays(6)
    private var fechaSeleccionada: LocalDate = LocalDate.now()
    private var evaluacionSeleccionada: EvaluacionHistorial? = null

    private val localeEs = Locale.forLanguageTag("es-ES")

    override fun getContenidoLayoutId(): Int {
        return R.layout.activity_historial_emocional
    }

    override fun onContenidoCreado() {
        enlazarVistas()
        cargarDatosUsuarioLocal()
        configurarEventos()
        cargarDesdeCache()
        cargarHistorialFirebase()
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
        imgFotoUsuarioHistorial = findViewById(R.id.imgFotoUsuarioHistorial)
        txtInicialesUsuarioHistorial = findViewById(R.id.txtInicialesUsuarioHistorial)
        txtNombreUsuarioHistorial = findViewById(R.id.txtNombreUsuarioHistorial)
        txtCorreoUsuarioHistorial = findViewById(R.id.txtCorreoUsuarioHistorial)

        contenedorGrafico = findViewById(R.id.contenedorGrafico)
        txtMesSemana = findViewById(R.id.txtMesSemana)
        btnSemanaAnterior = findViewById(R.id.btnSemanaAnterior)
        btnSemanaSiguiente = findViewById(R.id.btnSemanaSiguiente)

        panelListaDia = findViewById(R.id.panelListaDia)
        panelDetalleEvaluacion = findViewById(R.id.panelDetalleEvaluacion)
        btnVolverLista = findViewById(R.id.btnVolverLista)

        txtTituloDiaSeleccionado = findViewById(R.id.txtTituloDiaSeleccionado)
        contenedorEvaluacionesDia = findViewById(R.id.contenedorEvaluacionesDia)

        txtFechaDetalle = findViewById(R.id.txtFechaDetalle)
        txtNivelDetalle = findViewById(R.id.txtNivelDetalle)
        txtTextoUsuarioDetalle = findViewById(R.id.txtTextoUsuarioDetalle)
        txtTestDetalle = findViewById(R.id.txtTestDetalle)
        txtTextoDetalle = findViewById(R.id.txtTextoDetalle)
        txtImagenDetalle = findViewById(R.id.txtImagenDetalle)

        btnNuevaEvaluacion = findViewById(R.id.btnNuevaEvaluacion)

        mostrarListaDia()
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

        Log.d("HISTORIAL_CACHE", "UID: $uidActual")
        Log.d("HISTORIAL_CACHE", "Email: $emailActual")
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

        txtNombreUsuarioHistorial.text = nombreMostrado
        txtCorreoUsuarioHistorial.text = correoMostrado
        txtInicialesUsuarioHistorial.text = obtenerIniciales(nombreMostrado)

        if (cache.fotoPerfilUrl.isNotBlank()) {
            txtInicialesUsuarioHistorial.visibility = View.GONE
            imgFotoUsuarioHistorial.visibility = View.VISIBLE

            Glide.with(this)
                .load(cache.fotoPerfilUrl)
                .placeholder(R.drawable.ic_usermenu)
                .error(R.drawable.ic_usermenu)
                .circleCrop()
                .into(imgFotoUsuarioHistorial)
        } else {
            imgFotoUsuarioHistorial.visibility = View.GONE
            txtInicialesUsuarioHistorial.visibility = View.VISIBLE
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

        btnSemanaAnterior.setOnClickListener {
            fechaInicioSemana = fechaInicioSemana.minusDays(7)
            fechaSeleccionada = fechaInicioSemana.plusDays(6)
            evaluacionSeleccionada = null
            mostrarListaDia()
            refrescarPantalla()
        }

        btnSemanaSiguiente.setOnClickListener {
            fechaInicioSemana = fechaInicioSemana.plusDays(7)
            fechaSeleccionada = fechaInicioSemana.plusDays(6)
            evaluacionSeleccionada = null
            mostrarListaDia()
            refrescarPantalla()
        }

        txtMesSemana.setOnClickListener {
            mostrarSelectorMesAnio()
        }

        btnVolverLista.setOnClickListener {
            mostrarListaDia()
        }
    }

    private fun mostrarSelectorMesAnio() {
        val meses = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril",
            "Mayo", "Junio", "Julio", "Agosto",
            "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )

        val anioActual = LocalDate.now().year
        val anioMinimo = anioActual - 3
        val anioMaximo = anioActual + 1

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(18), dpToPx(12), dpToPx(18), dpToPx(4))
        }

        val pickerMes = NumberPicker(this).apply {
            minValue = 0
            maxValue = 11
            displayedValues = meses
            value = fechaSeleccionada.monthValue - 1
            wrapSelectorWheel = true
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val pickerAnio = NumberPicker(this).apply {
            minValue = anioMinimo
            maxValue = anioMaximo
            value = fechaSeleccionada.year
            wrapSelectorWheel = false
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        contenedor.addView(pickerMes)
        contenedor.addView(pickerAnio)

        AlertDialog.Builder(this)
            .setTitle("Seleccionar mes y año")
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ ->
                val mesSeleccionado = pickerMes.value + 1
                val anioSeleccionado = pickerAnio.value

                val nuevaFecha = LocalDate.of(anioSeleccionado, mesSeleccionado, 1)

                fechaInicioSemana = nuevaFecha
                fechaSeleccionada = nuevaFecha
                evaluacionSeleccionada = null
                mostrarListaDia()

                refrescarPantalla()
            }
            .show()
    }

    private fun cargarDesdeCache() {
        if (uidActual.isBlank()) {
            mostrarEstadoVacio()
            return
        }

        val cache = HistorialCacheManager.obtener(this, uidActual)

        if (cache.isNotEmpty()) {
            evaluaciones.clear()
            evaluaciones.addAll(cache.sortedByDescending { it.timestamp })
            refrescarPantalla()
        } else {
            mostrarEstadoVacio()
        }
    }

    private fun cargarHistorialFirebase() {
        if (uidActual.isBlank()) {
            mostrarEstadoVacio()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(uidActual)
            .collection("historial_emocional")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(120)
            .get()
            .addOnSuccessListener { snapshot ->
                evaluaciones.clear()

                for (documento in snapshot.documents) {
                    evaluaciones.add(
                        EvaluacionHistorial(
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
                refrescarPantalla()
            }
            .addOnFailureListener { e ->
                Log.e("HISTORIAL_FIREBASE", "Error al cargar historial", e)

                if (evaluaciones.isEmpty()) {
                    mostrarEstadoVacio()
                }

                Toast.makeText(
                    this,
                    "No se pudo actualizar el historial.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun refrescarPantalla() {
        construirGraficoSemanal()
        mostrarEvaluacionesDelDia(fechaSeleccionada)
    }

    private fun construirGraficoSemanal() {
        contenedorGrafico.removeAllViews()

        val dias = (0..6).map { fechaInicioSemana.plusDays(it.toLong()) }

        val formatoMes = DateTimeFormatter.ofPattern("MMMM yyyy", localeEs)
        txtMesSemana.text = fechaInicioSemana.plusDays(3)
            .format(formatoMes)
            .replaceFirstChar { it.uppercase() }

        val formatoFechaFirebase = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
        val formatoDiaTexto = DateTimeFormatter.ofPattern("EEE", localeEs)
        val formatoNumero = DateTimeFormatter.ofPattern("dd", Locale.getDefault())

        dias.forEach { dia ->
            val fechaTexto = dia.format(formatoFechaFirebase)
            val evaluacionesDia = evaluaciones.filter { it.fecha == fechaTexto }
            val evaluacionPrincipal = evaluacionesDia.maxByOrNull { it.timestamp }

            val columna = crearColumnaGrafico(
                dia = dia,
                diaLabel = dia.format(formatoDiaTexto).replace(".", "").take(3),
                diaNumero = dia.format(formatoNumero),
                evaluacion = evaluacionPrincipal,
                evaluacionesDia = evaluacionesDia
            )

            contenedorGrafico.addView(columna)
        }
    }

    private fun crearColumnaGrafico(
        dia: LocalDate,
        diaLabel: String,
        diaNumero: String,
        evaluacion: EvaluacionHistorial?,
        evaluacionesDia: List<EvaluacionHistorial>
    ): LinearLayout {
        val esSeleccionado = dia == fechaSeleccionada
        val nivelesDistintos = evaluacionesDia.map { it.nivelFinal }.distinct()
        val todosMismoResultado = evaluacionesDia.isNotEmpty() && nivelesDistintos.size == 1

        val columna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setOnClickListener {
                fechaSeleccionada = dia
                evaluacionSeleccionada = null
                mostrarListaDia()
                mostrarEvaluacionesDelDia(dia)
                construirGraficoSemanal()
            }
        }

        val barraContenedor = LinearLayout(this).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            orientation = LinearLayout.VERTICAL
            background = crearFondoRedondeado(
                colorFondo = Color.parseColor("#F1ECFF"),
                colorBorde = Color.TRANSPARENT,
                radio = 9
            )
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(24),
                dpToPx(116)
            )
        }

        when {
            evaluacionesDia.isEmpty() -> {
                val barraVacia = View(this).apply {
                    background = crearFondoRedondeado(
                        colorFondo = Color.parseColor("#E5E7EB"),
                        colorBorde = Color.TRANSPARENT,
                        radio = 9
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(18)
                    )
                }

                barraContenedor.addView(barraVacia)
            }

            todosMismoResultado -> {
                val barra = View(this).apply {
                    background = crearFondoRedondeado(
                        colorFondo = obtenerColorNivel(evaluacionesDia.first().nivelFinal),
                        colorBorde = Color.TRANSPARENT,
                        radio = 9
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        calcularAlturaBarra(evaluacionesDia.first())
                    )
                }

                barraContenedor.addView(barra)
            }

            else -> {
                val espacio = Space(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                }

                barraContenedor.addView(espacio)

                evaluacionesDia
                    .sortedBy { it.timestamp }
                    .takeLast(6)
                    .forEach { eval ->
                        val punto = View(this).apply {
                            background = crearCirculo(obtenerColorNivel(eval.nivelFinal))
                            layoutParams = LinearLayout.LayoutParams(
                                dpToPx(13),
                                dpToPx(13)
                            ).apply {
                                bottomMargin = dpToPx(4)
                            }
                        }

                        barraContenedor.addView(punto)
                    }
            }
        }

        val indicadorPerfil = crearIndicadorPerfilParaGrafico(
            visible = evaluacionesDia.isNotEmpty(),
            color = obtenerColorNivel(evaluacion?.nivelFinal ?: ""),
            texto = obtenerIniciales(txtNombreUsuarioHistorial.text.toString())
        )

        val txtDia = TextView(this).apply {
            text = diaLabel.replaceFirstChar { it.uppercase() }
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#9CA3AF"))
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
        }

        val txtNumero = TextView(this).apply {
            text = diaNumero
            gravity = Gravity.CENTER
            setTextColor(
                if (esSeleccionado) {
                    Color.WHITE
                } else {
                    Color.parseColor("#9CA3AF")
                }
            )
            textSize = 11f
            setTypeface(null, if (esSeleccionado) Typeface.BOLD else Typeface.NORMAL)

            if (esSeleccionado) {
                background = crearCirculo(obtenerColorNivel(evaluacion?.nivelFinal ?: ""))
                setPadding(0, dpToPx(3), 0, dpToPx(3))
            }
        }

        val indicadorCantidad = TextView(this).apply {
            text = if (evaluacionesDia.size > 1) "${evaluacionesDia.size}" else ""
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 9f
            background = crearCirculo(obtenerColorNivel(evaluacion?.nivelFinal ?: ""))
            visibility = if (evaluacionesDia.size > 1) View.VISIBLE else View.INVISIBLE
            layoutParams = LinearLayout.LayoutParams(dpToPx(20), dpToPx(20)).apply {
                topMargin = dpToPx(3)
            }
        }

        columna.addView(barraContenedor)
        columna.addView(indicadorPerfil)
        columna.addView(txtDia)
        columna.addView(txtNumero)
        columna.addView(indicadorCantidad)

        return columna
    }

    private fun crearIndicadorPerfilParaGrafico(
        visible: Boolean,
        color: Int,
        texto: String
    ): TextView {
        return TextView(this).apply {
            this.text = if (visible) texto.take(2) else ""
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 8f
            setTypeface(null, Typeface.BOLD)
            background = crearCirculo(
                if (color == Color.parseColor("#9CA3AF")) {
                    Color.parseColor("#2563EB")
                } else {
                    color
                }
            )
            visibility = if (visible) View.VISIBLE else View.INVISIBLE
            layoutParams = LinearLayout.LayoutParams(dpToPx(22), dpToPx(22)).apply {
                topMargin = dpToPx(-10)
            }
        }
    }

    private fun mostrarEvaluacionesDelDia(dia: LocalDate) {
        contenedorEvaluacionesDia.removeAllViews()

        val formatoFechaFirebase = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
        val formatoTitulo = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", localeEs)

        val fechaTexto = dia.format(formatoFechaFirebase)
        val evaluacionesDia = evaluaciones
            .filter { it.fecha == fechaTexto }
            .sortedByDescending { it.timestamp }

        txtTituloDiaSeleccionado.text = dia.format(formatoTitulo)
            .replaceFirstChar { it.uppercase() }

        evaluacionSeleccionada = null
        mostrarListaDia()

        if (evaluacionesDia.isEmpty()) {
            val txtVacio = TextView(this).apply {
                text = "No hay evaluaciones registradas en este día."
                setTextColor(Color.parseColor("#6B7280"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(16), 0, dpToPx(16))
            }

            contenedorEvaluacionesDia.addView(txtVacio)
            limpiarDetalle()
            return
        }

        evaluacionesDia.forEach { evaluacion ->
            contenedorEvaluacionesDia.addView(crearItemEvaluacionDia(evaluacion))
        }
    }

    private fun crearItemEvaluacionDia(evaluacion: EvaluacionHistorial): View {
        val colorNivel = obtenerColorNivel(evaluacion.nivelFinal)

        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = crearFondoRedondeado(
                colorFondo = Color.WHITE,
                colorBorde = Color.parseColor("#BFDBFE"),
                radio = 18
            )
            setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(9)
            }

            setOnClickListener {
                evaluacionSeleccionada = evaluacion
                mostrarDetalleEvaluacion(evaluacion)
            }
        }

        val indicador = View(this).apply {
            background = crearCirculo(colorNivel)
            layoutParams = LinearLayout.LayoutParams(dpToPx(13), dpToPx(13)).apply {
                marginEnd = dpToPx(12)
            }
        }

        val texto = TextView(this).apply {
            text = "${evaluacion.hora}  •  ${evaluacion.nivelFinal}"
            setTextColor(Color.parseColor("#1F2937"))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        item.addView(indicador)
        item.addView(texto)

        return item
    }

    private fun mostrarDetalleEvaluacion(evaluacion: EvaluacionHistorial) {
        panelListaDia.visibility = View.GONE
        panelDetalleEvaluacion.visibility = View.VISIBLE

        val colorNivel = obtenerColorNivel(evaluacion.nivelFinal)

        txtFechaDetalle.text = evaluacion.fechaHora.ifBlank {
            "${evaluacion.fecha} ${evaluacion.hora}"
        }

        txtNivelDetalle.text = evaluacion.nivelFinal
        txtNivelDetalle.setTextColor(colorNivel)

        txtTextoUsuarioDetalle.text =
            if (evaluacion.textoUsuario.isBlank()) {
                "Texto registrado: sin texto disponible."
            } else {
                "Texto registrado:\n“${evaluacion.textoUsuario}”"
            }

        txtTestDetalle.text =
            "Test emocional: ${evaluacion.nivelTestVisible.ifBlank { "Sin dato" }}"

        txtTextoDetalle.text =
            "Análisis de texto: ${evaluacion.etiquetaNlpTraducida.ifBlank { "Sin dato" }}"

        txtImagenDetalle.text =
            "Imagen facial: ${evaluacion.emocionFacialTraducida.ifBlank { "Sin dato" }}"
    }

    private fun mostrarListaDia() {
        panelListaDia.visibility = View.VISIBLE
        panelDetalleEvaluacion.visibility = View.GONE
    }

    private fun limpiarDetalle() {
        txtFechaDetalle.text = ""
        txtNivelDetalle.text = ""
        txtTextoUsuarioDetalle.text = ""
        txtTestDetalle.text = ""
        txtTextoDetalle.text = ""
        txtImagenDetalle.text = ""
        mostrarListaDia()
    }

    private fun mostrarEstadoVacio() {
        contenedorGrafico.removeAllViews()
        contenedorEvaluacionesDia.removeAllViews()

        txtMesSemana.text = "Sin historial"
        txtTituloDiaSeleccionado.text = "Aún no hay evaluaciones"

        val txtVacio = TextView(this).apply {
            text = "Cuando completes una evaluación, aparecerá aquí organizada por fecha."
            setTextColor(Color.parseColor("#6B7280"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(18), 0, dpToPx(18))
        }

        contenedorEvaluacionesDia.addView(txtVacio)
        limpiarDetalle()
    }

    private fun calcularAlturaBarra(evaluacion: EvaluacionHistorial?): Int {
        if (evaluacion == null) return dpToPx(18)

        return when (evaluacion.nivelFinal) {
            "Estado favorable" -> dpToPx(48)
            "Señales leves" -> dpToPx(68)
            "Seguimiento recomendado" -> dpToPx(92)
            "Atención recomendada" -> dpToPx(112)
            else -> dpToPx(45)
        }
    }

    private fun obtenerColorNivel(nivel: String): Int {
        return when (nivel) {
            "Estado favorable" -> Color.parseColor("#10B981")
            "Señales leves" -> Color.parseColor("#2563EB")
            "Seguimiento recomendado" -> Color.parseColor("#F97316")
            "Atención recomendada" -> Color.parseColor("#F43F5E")
            else -> Color.parseColor("#9CA3AF")
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

            if (colorBorde != Color.TRANSPARENT) {
                setStroke(dpToPx(1), colorBorde)
            }
        }
    }

    private fun crearCirculo(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dpToPx(2), Color.WHITE)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onMenuHistorialSeleccionado() {
        ocultarMenu()
    }

    data class EvaluacionHistorial(
        val id: String,
        val fecha: String,
        val hora: String,
        val fechaHora: String,
        val timestamp: Long,
        val nivelFinal: String,
        val puntajeFinalInterno: Float,
        val nivelTestVisible: String,
        val etiquetaNlp: String,
        val etiquetaNlpTraducida: String,
        val textoUsuario: String,
        val emocionFacialTraducida: String
    )
}