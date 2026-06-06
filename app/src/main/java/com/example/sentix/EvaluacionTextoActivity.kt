package com.example.sentix

import android.widget.TextView

class EvaluacionTextoActivity : BaseMenuActivity() {

    private lateinit var txtTituloTexto: TextView

    override fun getContenidoLayoutId(): Int {
        return R.layout.activity_evaluacion_texto
    }

    override fun onContenidoCreado() {
        txtTituloTexto = findViewById(R.id.txtTituloTexto)
        txtTituloTexto.text = "Análisis de texto"
    }
}