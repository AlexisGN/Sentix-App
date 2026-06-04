package com.example.sentix

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.sentix.data.FirebaseAuthHelper
import com.example.sentix.data.FirebaseUserHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey

abstract class BaseMenuActivity : AppCompatActivity() {

    /*
     * Se mantienen por compatibilidad con tus pantallas hijas.
     * Aunque CuentaActivity ya esté adaptada, EditarPerfilActivity u otra clase
     * todavía puede usar auth/firestore heredados desde BaseMenuActivity.
     *
     * Además, BaseMenuActivity todavía usa firestore para historial_emocional.
     */
    protected lateinit var auth: FirebaseAuth
    protected lateinit var firestore: FirebaseFirestore

    protected var uidActual: String = ""
    protected var emailActual: String = ""

    private lateinit var rootBaseMenu: View
    private lateinit var mainContentContainer: FrameLayout
    private lateinit var sidebarWrapper: View
    private lateinit var sidebarPanel: LinearLayout
    private lateinit var overlayView: View
    private lateinit var btnSidebarToggle: ImageButton

    private lateinit var txtNombreMenu: TextView
    private lateinit var txtCorreoMenu: TextView
    private lateinit var imgPerfilMenu: ImageView

    private var menuAbierto = false
    private var hiddenTranslationX = 0f

    abstract fun getContenidoLayoutId(): Int

    open fun onContenidoCreado() {}

    open fun onUsuarioActualizado(cache: UsuarioCache) {}

    open fun onMenuEvaluacionSeleccionada() {
        if (this is EvaluacionCamaraActivity) {
            ocultarMenu()
            return
        }

        val intent = Intent(this, EvaluacionCamaraActivity::class.java)
        intent.putExtra("uid", uidActual)
        intent.putExtra("email", emailActual)
        startActivity(intent)
    }

    open fun onMenuHistorialSeleccionado() {
        contarHistorialEmocional()
    }

    open fun onMenuRecomendacionesSeleccionada() {
        Toast.makeText(this, "Próximamente: Recomendaciones", Toast.LENGTH_SHORT).show()
    }

    open fun onMenuSeguimientoSeleccionado() {
        Toast.makeText(this, "Próximamente: Seguimiento emocional", Toast.LENGTH_SHORT).show()
    }

    open fun onMenuAlertasSeleccionado() {
        Toast.makeText(this, "Próximamente: Alertas de riesgo", Toast.LENGTH_SHORT).show()
    }

    open fun onMenuAcercaSeleccionado() {
        Toast.makeText(
            this,
            "Sentix brinda apoyo preventivo y no reemplaza atención profesional.",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_menu)

        /*
         * Se siguen inicializando para no romper Activities hijas
         * y porque firestore aún se usa para historial_emocional.
         */
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        enlazarMenuVistas()

        SystemBarsHelper.aplicarInsetsPersonalizado(
            activity = this,
            view = sidebarPanel,
            paddingStartDp = 18f,
            paddingTopExtraDp = 24f,
            paddingEndDp = 18f,
            paddingBottomExtraDp = 24f
        )

        SystemBarsHelper.aplicarInsets(
            activity = this,
            rootView = mainContentContainer,
            aplicarArriba = true,
            aplicarAbajo = true
        )

        cargarContenido()
        prepararSesion()
        cargarDatosDesdeCache()
        cargarDatosUsuarioDesdeFirebase()
        configurarSidebar()
        configurarEventosMenu()
    }

    override fun onResume() {
        super.onResume()

        if (uidActual.isNotEmpty()) {
            cargarDatosDesdeCache()
            cargarDatosUsuarioDesdeFirebase()
        }
    }

    private fun enlazarMenuVistas() {
        rootBaseMenu = findViewById(R.id.rootBaseMenu)
        mainContentContainer = findViewById(R.id.mainContentContainer)
        sidebarWrapper = findViewById(R.id.sidebarWrapper)
        sidebarPanel = findViewById(R.id.sidebarPanel)
        overlayView = findViewById(R.id.viewOverlay)
        btnSidebarToggle = findViewById(R.id.btnSidebarToggle)

        txtNombreMenu = findViewById(R.id.txtNombreMenu)
        txtCorreoMenu = findViewById(R.id.txtCorreoMenu)
        imgPerfilMenu = findViewById(R.id.imgPerfilMenu)
    }

    private fun cargarContenido() {
        layoutInflater.inflate(getContenidoLayoutId(), mainContentContainer, true)
        onContenidoCreado()
    }

    private fun prepararSesion() {
        uidActual = intent.getStringExtra("uid") ?: ""
        emailActual = intent.getStringExtra("email") ?: ""

        val firebaseUser = FirebaseAuthHelper.obtenerUsuarioActual()

        if (uidActual.isEmpty()) {
            uidActual = firebaseUser?.uid ?: ""
        }

        if (emailActual.isEmpty()) {
            emailActual = firebaseUser?.email ?: ""
        }

        if (uidActual.isEmpty()) {
            val cache = UsuarioCacheManager.obtener(this)

            if (cache.uid.isNotBlank()) {
                uidActual = cache.uid
            }

            if (emailActual.isEmpty() && cache.email.isNotBlank()) {
                emailActual = cache.email
            }
        }

        if (uidActual.isEmpty()) {
            Toast.makeText(
                this,
                "No se pudo identificar la sesión del usuario",
                Toast.LENGTH_LONG
            ).show()

            volverAlLogin()
        }
    }

    protected fun cargarDatosDesdeCache() {
        val cache = UsuarioCacheManager.obtener(this)

        if (cache.uid.isNotBlank() && uidActual.isBlank()) {
            uidActual = cache.uid
        }

        if (cache.email.isNotBlank()) {
            emailActual = cache.email
            txtCorreoMenu.text = cache.email
        } else {
            txtCorreoMenu.text = emailActual.ifEmpty { "correo@gmail.com" }
        }

        txtNombreMenu.text =
            if (cache.nombreCompleto.isNotBlank()) {
                cache.nombreCompleto
            } else {
                "Cargando..."
            }

        if (cache.fotoPerfilUrl.isNotBlank()) {
            cargarFotoMenu(cache.fotoPerfilUrl)
        } else {
            imgPerfilMenu.setImageResource(R.drawable.ic_usermenu)
        }

        onUsuarioActualizado(cache)
    }

    protected fun refrescarMenuDesdeCache() {
        if (
            !::txtNombreMenu.isInitialized ||
            !::txtCorreoMenu.isInitialized ||
            !::imgPerfilMenu.isInitialized
        ) {
            return
        }

        cargarDatosDesdeCache()
    }

    protected fun refrescarFotoMenuAlInstante(fotoPerfilUrl: String) {
        if (!::imgPerfilMenu.isInitialized) return

        if (fotoPerfilUrl.isNotBlank()) {
            cargarFotoMenu(fotoPerfilUrl, forzarRecarga = true)
        } else {
            imgPerfilMenu.setImageResource(R.drawable.ic_usermenu)
        }
    }
    protected fun cargarDatosUsuarioDesdeFirebase() {
        if (uidActual.isEmpty()) {
            val uidAuth = FirebaseAuthHelper.obtenerUidActual()

            if (uidAuth.isNotEmpty()) {
                uidActual = uidAuth
            } else {
                Log.w("BASE_MENU", "uidActual vacío y FirebaseAuth.currentUser es null")
                return
            }
        }

        val usuarioAuth = FirebaseAuthHelper.obtenerUsuarioActual()

        if (usuarioAuth == null) {
            Log.w("BASE_MENU", "FirebaseAuth.currentUser es null. Se mantiene caché.")
            return
        }

        if (usuarioAuth.uid != uidActual) {
            Log.e(
                "BASE_MENU",
                "UID no coincide. Auth=${usuarioAuth.uid}, uidActual=$uidActual"
            )

            Toast.makeText(
                this,
                "La sesión activa no coincide con este usuario.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        FirebaseUserHelper.obtenerDocumentoUsuario(
            uid = uidActual,
            onSuccess = { documento ->
                if (!documento.exists()) {
                    Log.e("BASE_MENU", "No existe el documento usuarios/$uidActual")
                    cargarDatosDesdeCache()
                    return@obtenerDocumentoUsuario
                }

                UsuarioCacheManager.guardarDesdeFirestore(
                    context = this,
                    uid = uidActual,
                    documento = documento
                )

                val cacheActualizado = UsuarioCacheManager.obtener(this)

                txtNombreMenu.text =
                    if (cacheActualizado.nombreCompleto.isNotBlank()) {
                        cacheActualizado.nombreCompleto
                    } else {
                        "Usuario"
                    }

                txtCorreoMenu.text =
                    if (cacheActualizado.email.isNotBlank()) {
                        cacheActualizado.email
                    } else {
                        emailActual.ifEmpty { "correo@gmail.com" }
                    }

                emailActual = txtCorreoMenu.text.toString()

                if (cacheActualizado.fotoPerfilUrl.isNotBlank()) {
                    cargarFotoMenu(cacheActualizado.fotoPerfilUrl)
                } else {
                    imgPerfilMenu.setImageResource(R.drawable.ic_usermenu)
                }

                onUsuarioActualizado(cacheActualizado)

                Log.d(
                    "BASE_MENU",
                    "Perfil cargado correctamente: ${cacheActualizado.nombreCompleto}"
                )
            },
            onError = { mensaje ->
                Log.e("BASE_MENU", "Error al cargar perfil: $mensaje")

                cargarDatosDesdeCache()

                Toast.makeText(
                    this,
                    "No se pudo actualizar el perfil. Se muestran datos guardados.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun cargarFotoMenu(url: String, forzarRecarga: Boolean = false) {
        if (url.isNotBlank()) {
            val drawableActual = imgPerfilMenu.drawable

            val request = Glide.with(this)
                .load(url)
                .circleCrop()
                .dontAnimate()

            if (forzarRecarga) {
                request
                    .signature(ObjectKey(System.currentTimeMillis().toString()))
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
            }

            if (drawableActual != null) {
                request
                    .placeholder(drawableActual)
                    .error(drawableActual)
                    .into(imgPerfilMenu)
            } else {
                request
                    .placeholder(R.drawable.ic_usermenu)
                    .error(R.drawable.ic_usermenu)
                    .into(imgPerfilMenu)
            }

        } else {
            imgPerfilMenu.setImageResource(R.drawable.ic_usermenu)
        }
    }

    private fun configurarSidebar() {
        sidebarWrapper.post {
            val anchoVisibleToggle = dpToPx(38f)
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
    }

    private fun configurarEventosMenu() {
        findViewById<LinearLayout>(R.id.itemCuenta).setOnClickListener {
            val cache = UsuarioCacheManager.obtener(this)

            if (this is CuentaActivity) {
                ocultarMenu()
                return@setOnClickListener
            }

            val intent = Intent(this, CuentaActivity::class.java)
            intent.putExtra("uid", uidActual)
            intent.putExtra("email", cache.email.ifBlank { emailActual })
            intent.putExtra("nombreCompleto", cache.nombreCompleto)
            intent.putExtra("usuario", cache.usuario)
            intent.putExtra("fotoPerfilUrl", cache.fotoPerfilUrl)

            startActivity(intent)
            ocultarMenu()
        }

        findViewById<LinearLayout>(R.id.itemEvaluacion).setOnClickListener {
            onMenuEvaluacionSeleccionada()

        }

        findViewById<LinearLayout>(R.id.itemHistorial).setOnClickListener {
            onMenuHistorialSeleccionado()
            ocultarMenu()
        }

        findViewById<LinearLayout>(R.id.itemRecomendaciones).setOnClickListener {
            onMenuRecomendacionesSeleccionada()
            ocultarMenu()
        }

        findViewById<LinearLayout>(R.id.itemSeguimiento).setOnClickListener {
            onMenuSeguimientoSeleccionado()
            ocultarMenu()
        }

        findViewById<LinearLayout>(R.id.itemAlertas).setOnClickListener {
            onMenuAlertasSeleccionado()
            ocultarMenu()
        }



        findViewById<LinearLayout>(R.id.itemCerrarSesion).setOnClickListener {
            cerrarSesion()
        }
    }

    private fun contarHistorialEmocional() {
        if (uidActual.isEmpty()) {
            Toast.makeText(this, "No se encontró el usuario activo", Toast.LENGTH_SHORT).show()
            return
        }

        /*
         * Esta parte se mantiene con Firestore directo porque todavía no hemos creado
         * un helper específico para historial emocional.
         */
        firestore.collection("usuarios")
            .document(uidActual)
            .collection("historial_emocional")
            .get()
            .addOnSuccessListener { resultado ->
                Toast.makeText(
                    this,
                    "Registros emocionales: ${resultado.size()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Log.e("BASE_MENU", "Error al leer historial emocional", e)

                Toast.makeText(
                    this,
                    "No se pudo cargar el historial emocional",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun cerrarSesion() {
        FirebaseAuthHelper.cerrarSesion(this)

        /*
         * No borrar getSharedPreferences("login").
         * Ahí está recordar cuenta, password guardada, uid, email y biometriaHabilitada.
         *
         * No limpiar UsuarioCacheManager para mantener carga rápida al volver.
         */

        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()

        volverAlLogin()
    }

    private fun volverAlLogin() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun toggleMenu() {
        if (menuAbierto) {
            ocultarMenu()
        } else {
            mostrarMenu()
        }
    }

    protected fun mostrarMenu() {
        sidebarWrapper.animate()
            .translationX(0f)
            .setDuration(180)
            .start()

        overlayView.visibility = View.VISIBLE
        overlayView.animate()
            .alpha(1f)
            .setDuration(160)
            .start()

        menuAbierto = true
        actualizarIconoToggle()
    }

    protected fun ocultarMenu() {
        sidebarWrapper.animate()
            .translationX(hiddenTranslationX)
            .setDuration(180)
            .start()

        overlayView.animate()
            .alpha(0f)
            .setDuration(140)
            .withEndAction {
                overlayView.visibility = View.GONE
            }
            .start()

        menuAbierto = false
        actualizarIconoToggle()
    }

    private fun actualizarIconoToggle() {
        btnSidebarToggle.setImageResource(R.drawable.ic_menu_handle_minimal)

        btnSidebarToggle.animate()
            .rotation(if (menuAbierto) 180f else 0f)
            .setDuration(120)
            .start()
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).roundToInt()
    }
}