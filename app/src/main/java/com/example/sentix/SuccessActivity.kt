package com.example.sentix

import android.content.Intent
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class SuccessActivity : BaseMenuActivity() {

    private lateinit var txtNombrePrincipal: TextView
    private lateinit var txtSubtituloUltimoResultado: TextView

    private lateinit var cardIniciarEvaluacion: LinearLayout
    private lateinit var cardUltimoResultado: LinearLayout
    private lateinit var cardRecomendacionesInicio: LinearLayout

    override fun getContenidoLayoutId(): Int {
        return R.layout.activity_success
    }

    override fun onContenidoCreado() {
        txtNombrePrincipal = findViewById(R.id.txtNombrePrincipal)
        txtSubtituloUltimoResultado = findViewById(R.id.txtSubtituloUltimoResultado)

        cardIniciarEvaluacion = findViewById(R.id.cardIniciarEvaluacion)
        cardUltimoResultado = findViewById(R.id.cardUltimoResultado)
        cardRecomendacionesInicio = findViewById(R.id.cardRecomendacionesInicio)

        configurarAccesosRapidos()
    }

    override fun onUsuarioActualizado(cache: UsuarioCache) {
        if (!::txtNombrePrincipal.isInitialized) return

        val nombreMostrar = obtenerNombreParaSaludo(cache)
        txtNombrePrincipal.text = "Hola, $nombreMostrar"

        actualizarResumenUltimoResultado()
    }

    private fun configurarAccesosRapidos() {
        cardIniciarEvaluacion.setOnClickListener {
            abrirEvaluacionEmocional()
        }

        cardUltimoResultado.setOnClickListener {
            abrirUltimoResultado()
        }

        cardRecomendacionesInicio.setOnClickListener {
            abrirRecomendaciones()
        }
    }

    private fun abrirEvaluacionEmocional() {
        val intent = Intent(this, EvaluacionCamaraActivity::class.java)
        intent.putExtra("uid", uidActual)
        intent.putExtra("email", emailActual)
        startActivity(intent)
    }

    private fun abrirUltimoResultado() {
        val historial = HistorialCacheManager.obtener(this, uidActual)

        if (historial.isEmpty()) {
            Toast.makeText(
                this,
                "Aún no tienes resultados guardados. Realiza una evaluación para iniciar tu historial.",
                Toast.LENGTH_LONG
            ).show()

            abrirEvaluacionEmocional()
            return
        }

        val intent = Intent(this, HistorialEmocionalActivity::class.java)
        intent.putExtra("uid", uidActual)
        intent.putExtra("email", emailActual)
        startActivity(intent)
    }

    private fun abrirRecomendaciones() {
        val intent = Intent(this, RecomendacionesActivity::class.java)
        intent.putExtra("uid", uidActual)
        intent.putExtra("email", emailActual)
        startActivity(intent)
    }

    private fun actualizarResumenUltimoResultado() {
        if (!::txtSubtituloUltimoResultado.isInitialized) return

        val historial = HistorialCacheManager.obtener(this, uidActual)

        if (historial.isEmpty()) {
            txtSubtituloUltimoResultado.text = "Aún no tienes registros."
            return
        }

        val ultimaEvaluacion = historial.maxByOrNull { it.timestamp }

        txtSubtituloUltimoResultado.text =
            if (ultimaEvaluacion != null) {
                "Último: ${ultimaEvaluacion.nivelFinal}."
            } else {
                "Revisa tu registro reciente."
            }
    }

    private fun obtenerNombreParaSaludo(cache: UsuarioCache): String {
        if (cache.nombre.isNotBlank()) {
            val partesNombre = cache.nombre.trim().split("\\s+".toRegex())

            return if (partesNombre.size >= 2) {
                "${partesNombre[0]} ${partesNombre[1]}"
            } else {
                partesNombre.firstOrNull() ?: "Usuario"
            }
        }

        if (cache.nombreCompleto.isNotBlank()) {
            val partesNombreCompleto = cache.nombreCompleto.trim().split("\\s+".toRegex())

            return if (partesNombreCompleto.size >= 2) {
                "${partesNombreCompleto[0]} ${partesNombreCompleto[1]}"
            } else {
                partesNombreCompleto.firstOrNull() ?: "Usuario"
            }
        }

        return "Usuario"
    }
}