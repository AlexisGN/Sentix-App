package com.example.sentix

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt

class SuccessActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private var usuarioActual: UsuarioCompleto? = null

    private lateinit var sidebarWrapper: View
    private lateinit var overlayView: View
    private lateinit var btnSidebarToggle: ImageButton
    private lateinit var mainContentContainer: ScrollView

    private lateinit var txtNombreMenu: TextView
    private lateinit var txtCorreoMenu: TextView
    private lateinit var txtNombrePrincipal: TextView

    private var menuAbierto = false
    private var hiddenTranslationX = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)

        dbHelper = DBHelper(this)

        sidebarWrapper = findViewById(R.id.sidebarWrapper)
        overlayView = findViewById(R.id.viewOverlay)
        btnSidebarToggle = findViewById(R.id.btnSidebarToggle)
        mainContentContainer = findViewById(R.id.mainContentContainer)

        txtNombreMenu = findViewById(R.id.txtNombreMenu)
        txtCorreoMenu = findViewById(R.id.txtCorreoMenu)
        txtNombrePrincipal = findViewById(R.id.txtNombrePrincipal)

        val login = intent.getStringExtra("login") ?: ""
        usuarioActual = dbHelper.obtenerDatosUsuarioPorLogin(login)

        cargarDatosUsuario()
        configurarSidebar()
        configurarEventos()
    }

    private fun cargarDatosUsuario() {
        usuarioActual?.let { usuario ->
            val nombreCompleto = listOf(
                usuario.nombre,
                usuario.apellidoPaterno,
                usuario.apellidoMaterno
            ).filter { it.isNotBlank() }.joinToString(" ")

            txtNombreMenu.text = if (nombreCompleto.isNotBlank()) nombreCompleto else "Usuario"
            txtCorreoMenu.text = usuario.email
            txtNombrePrincipal.text = if (usuario.nombre.isNotBlank()) usuario.nombre else "Usuario"
        } ?: run {
            txtNombreMenu.text = "Usuario"
            txtCorreoMenu.text = "correo@gmail.com"
            txtNombrePrincipal.text = "Usuario"
        }
    }

    private fun configurarSidebar() {
        sidebarWrapper.post {
            val anchoVisibleToggle = dpToPx(28f)
            hiddenTranslationX = -(sidebarWrapper.width - anchoVisibleToggle).toFloat()

            sidebarWrapper.translationX = hiddenTranslationX
            overlayView.visibility = View.GONE
            overlayView.alpha = 0f
            menuAbierto = false

            actualizarIconoToggle()
        }

        btnSidebarToggle.setOnClickListener {
            toggleMenu()
        }

        overlayView.setOnClickListener {
            ocultarMenu()
        }

        mainContentContainer.setOnClickListener {
            if (menuAbierto) {
                ocultarMenu()
            }
        }
    }

    private fun configurarEventos() {
        findViewById<Button>(R.id.btnComenzarEvaluacion).setOnClickListener {
            Toast.makeText(
                this,
                "Aquí comenzará la evaluación emocional multimodal de Sentix",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<LinearLayout>(R.id.itemCuenta).setOnClickListener {
            Toast.makeText(this, "Próximamente: Tu cuenta", Toast.LENGTH_SHORT).show()
            ocultarMenu()
        }

        findViewById<LinearLayout>(R.id.itemEvaluacion).setOnClickListener {
            Toast.makeText(this, "Próximamente: Evaluación emocional", Toast.LENGTH_SHORT).show()
            ocultarMenu()
        }

        findViewById<LinearLayout>(R.id.itemHistorial).setOnClickListener {
            usuarioActual?.let {
                val historial = dbHelper.listarHistorialPorUsuario(it.id)
                Toast.makeText(this, "Registros emocionales: ${historial.size}", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "Sin usuario activo", Toast.LENGTH_SHORT).show()

            ocultarMenu()
        }

        findViewById<LinearLayout>(R.id.itemRecomendaciones).setOnClickListener {
            Toast.makeText(this, "Próximamente: Recomendaciones", Toast.LENGTH_SHORT).show()
            ocultarMenu()
        }

        findViewById<LinearLayout>(R.id.itemSeguimiento).setOnClickListener {
            Toast.makeText(this, "Próximamente: Seguimiento emocional", Toast.LENGTH_SHORT).show()
            ocultarMenu()
        }

        findViewById<LinearLayout>(R.id.itemAlertas).setOnClickListener {
            Toast.makeText(this, "Próximamente: Alertas de riesgo", Toast.LENGTH_SHORT).show()
            ocultarMenu()
        }

        findViewById<LinearLayout>(R.id.itemAcerca).setOnClickListener {
            Toast.makeText(
                this,
                "Sentix brinda apoyo preventivo y no reemplaza atención profesional.",
                Toast.LENGTH_LONG
            ).show()
            ocultarMenu()
        }

        findViewById<LinearLayout>(R.id.itemCerrarSesion).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun toggleMenu() {
        if (menuAbierto) {
            ocultarMenu()
        } else {
            mostrarMenu()
        }
    }

    private fun mostrarMenu() {
        sidebarWrapper.animate()
            .translationX(0f)
            .setDuration(260)
            .start()

        overlayView.visibility = View.VISIBLE
        overlayView.animate()
            .alpha(1f)
            .setDuration(220)
            .start()

        menuAbierto = true
        actualizarIconoToggle()
    }

    private fun ocultarMenu() {
        sidebarWrapper.animate()
            .translationX(hiddenTranslationX)
            .setDuration(260)
            .start()

        overlayView.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                overlayView.visibility = View.GONE
            }
            .start()

        menuAbierto = false
        actualizarIconoToggle()
    }

    private fun actualizarIconoToggle() {
        btnSidebarToggle.setImageResource(
            if (menuAbierto) android.R.drawable.ic_media_previous
            else android.R.drawable.ic_media_next
        )
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }
}