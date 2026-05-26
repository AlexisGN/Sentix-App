package com.example.sentix

import android.widget.TextView
import android.widget.Toast

class SuccessActivity : BaseMenuActivity() {

    private lateinit var txtNombrePrincipal: TextView

    override fun getContenidoLayoutId(): Int {
        return R.layout.activity_success
    }

    override fun onContenidoCreado() {
        txtNombrePrincipal = findViewById(R.id.txtNombrePrincipal)
    }

    override fun onUsuarioActualizado(cache: UsuarioCache) {
        if (!::txtNombrePrincipal.isInitialized) return

        val nombreMostrar = obtenerNombreParaSaludo(cache)

        txtNombrePrincipal.text = "Hola, $nombreMostrar"
    }

    override fun onMenuEvaluacionSeleccionada() {
        Toast.makeText(
            this,
            "Próximamente: evaluación emocional multimodal",
            Toast.LENGTH_SHORT
        ).show()
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