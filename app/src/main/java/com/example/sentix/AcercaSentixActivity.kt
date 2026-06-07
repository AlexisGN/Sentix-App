package com.example.sentix

class AcercaSentixActivity : BaseMenuActivity() {

    override fun getContenidoLayoutId(): Int {
        return R.layout.activity_acerca_sentix
    }

    override fun onContenidoCreado() {
        // Pantalla informativa.
        // Mantiene BaseMenuActivity, UsuarioCacheManager y SystemBarsHelper.
    }

    override fun onMenuAcercaSentixSeleccionado() {
        ocultarMenu()
    }
}