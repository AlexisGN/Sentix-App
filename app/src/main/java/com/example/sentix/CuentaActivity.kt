package com.example.sentix

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.sentix.data.FirebaseAuthHelper
import com.example.sentix.data.FirebaseStorageHelper
import com.example.sentix.data.FirebaseUserHelper
import com.yalantis.ucrop.UCrop
import java.io.File

class CuentaActivity : BaseMenuActivity() {

    private lateinit var imgPerfil: ImageView
    private lateinit var btnCambiarFoto: View

    private lateinit var txtNombrePerfil: TextView
    private lateinit var txtCorreoPerfil: TextView
    private lateinit var txtUsuarioPerfil: TextView

    private lateinit var itemEditarPerfil: LinearLayout
    private lateinit var itemCambiarPassword: LinearLayout
    private lateinit var itemNotificaciones: LinearLayout
    private lateinit var itemEliminarCuenta: LinearLayout

    private val canalSentix = "sentix_bienestar_channel"
    private val prefsNotificaciones = "sentix_notificaciones"

    private val pedirPermisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permitido ->
            if (permitido) {
                activarNotificaciones()
            } else {
                Toast.makeText(
                    this,
                    "No se activaron las notificaciones porque no se concedió el permiso.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val seleccionarImagen =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                abrirRecorteImagen(uri)
            } else {
                Toast.makeText(this, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show()
            }
        }

    private val recortarImagen =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uriRecortada = result.data?.let { UCrop.getOutput(it) }

                if (uriRecortada != null) {
                    subirFotoPerfil(uriRecortada)
                } else {
                    Toast.makeText(
                        this,
                        "No se pudo obtener la imagen recortada",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val error = result.data?.let { UCrop.getError(it) }

                Log.e("CUENTA_CROP", "Error al recortar imagen", error)

                Toast.makeText(
                    this,
                    "No se pudo recortar la imagen",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun getContenidoLayoutId(): Int {
        return R.layout.activity_cuenta
    }

    override fun onContenidoCreado() {
        enlazarVistas()
        configurarEventos()
        cargarPerfilDesdeCache()
    }

    override fun onResume() {
        super.onResume()

        if (::txtNombrePerfil.isInitialized) {
            cargarPerfilDesdeCache()
            cargarPerfilDesdeFirebase()
        }
    }

    override fun onUsuarioActualizado(cache: UsuarioCache) {
        if (!::txtNombrePerfil.isInitialized) return
        cargarPerfilDesdeCache()
    }

    private fun enlazarVistas() {
        imgPerfil = findViewById(R.id.imgFotoPerfil)
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto)

        txtNombrePerfil = findViewById(R.id.txtNombrePerfil)
        txtCorreoPerfil = findViewById(R.id.txtCorreoPerfil)
        txtUsuarioPerfil = findViewById(R.id.txtUsuarioPerfil)

        itemEditarPerfil = findViewById(R.id.itemEditarPerfil)
        itemCambiarPassword = findViewById(R.id.itemCambiarPassword)
        itemNotificaciones = findViewById(R.id.itemNotificaciones)
        itemEliminarCuenta = findViewById(R.id.itemEliminarCuenta)
    }

    private fun configurarEventos() {
        btnCambiarFoto.setOnClickListener {
            seleccionarImagen.launch("image/*")
        }

        itemEditarPerfil.setOnClickListener {
            val intent = Intent(this, EditarPerfilActivity::class.java)
            intent.putExtra("uid", uidActual)
            intent.putExtra("email", emailActual)
            startActivity(intent)
        }

        itemCambiarPassword.setOnClickListener {
            val email = FirebaseAuthHelper.obtenerEmailActual().ifBlank {
                emailActual.ifBlank {
                    UsuarioCacheManager.obtener(this).email
                }
            }

            if (email.isBlank()) {
                Toast.makeText(
                    this,
                    "No se encontró el correo de la cuenta",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            FirebaseAuthHelper.enviarRecuperacionPassword(
                email = email,
                onSuccess = {
                    Toast.makeText(
                        this,
                        "Enlace enviado.\nRevisa tu correo, Bandeja o Spam.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onError = { mensaje ->
                    Log.e("CUENTA_PASSWORD", "Error al enviar recuperación: $mensaje")

                    Toast.makeText(
                        this,
                        "No se pudo enviar el enlace de cambio de contraseña",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        itemNotificaciones.setOnClickListener {
            mostrarDialogoNotificaciones()
        }

        itemEliminarCuenta.setOnClickListener {
            mostrarDialogoEliminarCuenta()
        }
    }

    private fun cargarPerfilDesdeCache() {
        val cache = UsuarioCacheManager.obtener(this)

        if (cache.uid.isNotBlank() && uidActual.isBlank()) {
            uidActual = cache.uid
        }

        if (cache.email.isNotBlank()) {
            emailActual = cache.email
            txtCorreoPerfil.text = cache.email
        } else if (emailActual.isNotBlank()) {
            txtCorreoPerfil.text = emailActual
        } else {
            txtCorreoPerfil.text = "correo@gmail.com"
        }

        txtNombrePerfil.text =
            if (cache.nombreCompleto.isNotBlank()) {
                cache.nombreCompleto
            } else {
                "Usuario"
            }

        txtUsuarioPerfil.text =
            if (cache.usuario.isNotBlank()) {
                "@${cache.usuario}"
            } else {
                "@usuario"
            }

        if (cache.fotoPerfilUrl.isNotBlank()) {
            cargarImagenPerfil(cache.fotoPerfilUrl)
        } else {
            imgPerfil.setImageResource(R.drawable.ic_user_big)
        }
    }

    private fun cargarPerfilDesdeFirebase() {
        val usuarioAuth = FirebaseAuthHelper.obtenerUsuarioActual()

        if (usuarioAuth == null) {
            Log.w(
                "CUENTA_FIREBASE",
                "FirebaseAuth.currentUser es null. Se muestran datos desde caché."
            )
            return
        }

        if (uidActual.isBlank()) {
            uidActual = usuarioAuth.uid
        }

        if (usuarioAuth.uid != uidActual) {
            Log.e(
                "CUENTA_FIREBASE",
                "UID Auth no coincide. Auth=${usuarioAuth.uid}, recibido=$uidActual"
            )

            Toast.makeText(
                this,
                "La sesión activa no coincide con este perfil.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        FirebaseUserHelper.obtenerDocumentoUsuario(
            uid = uidActual,
            onSuccess = { documento ->
                if (!documento.exists()) {
                    Toast.makeText(
                        this,
                        "No se encontró el perfil del usuario",
                        Toast.LENGTH_LONG
                    ).show()
                    return@obtenerDocumentoUsuario
                }

                UsuarioCacheManager.guardarDesdeFirestore(
                    context = this,
                    uid = uidActual,
                    documento = documento
                )

                val cacheActualizado = UsuarioCacheManager.obtener(this)

                emailActual = cacheActualizado.email

                txtNombrePerfil.text =
                    if (cacheActualizado.nombreCompleto.isNotBlank()) {
                        cacheActualizado.nombreCompleto
                    } else {
                        "Usuario"
                    }

                txtCorreoPerfil.text =
                    if (cacheActualizado.email.isNotBlank()) {
                        cacheActualizado.email
                    } else {
                        emailActual.ifBlank { "correo@gmail.com" }
                    }

                txtUsuarioPerfil.text =
                    if (cacheActualizado.usuario.isNotBlank()) {
                        "@${cacheActualizado.usuario}"
                    } else {
                        "@usuario"
                    }

                if (cacheActualizado.fotoPerfilUrl.isNotBlank()) {
                    cargarImagenPerfil(cacheActualizado.fotoPerfilUrl)
                } else {
                    imgPerfil.setImageResource(R.drawable.ic_user_big)
                }

                Log.d("CUENTA_FIREBASE", "Datos cargados correctamente")
                Log.d(
                    "CUENTA_IMAGEN",
                    "fotoPerfilUrl Firestore: ${cacheActualizado.fotoPerfilUrl}"
                )
            },
            onError = { mensaje ->
                Log.e("CUENTA_FIREBASE", "Error al cargar perfil: $mensaje")

                Toast.makeText(
                    this,
                    "No se pudo actualizar la información del perfil. Se muestran datos guardados.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun abrirRecorteImagen(uriOrigen: Uri) {
        try {
            val archivoDestino = File(
                cacheDir,
                "perfil_recortado_${System.currentTimeMillis()}.jpg"
            )

            val uriDestino = Uri.fromFile(archivoDestino)

            val opciones = UCrop.Options().apply {
                setCompressionQuality(85)
                setHideBottomControls(false)
                setFreeStyleCropEnabled(false)

                setToolbarTitle("Ajustar foto")
                setToolbarColor(Color.parseColor("#2563EB"))
                setStatusBarColor(Color.parseColor("#1D4ED8"))
                setActiveControlsWidgetColor(Color.parseColor("#7C3AED"))
                setToolbarWidgetColor(Color.WHITE)
                setRootViewBackgroundColor(Color.BLACK)
            }

            val intentCrop = UCrop.of(uriOrigen, uriDestino)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(900, 900)
                .withOptions(opciones)
                .getIntent(this)

            recortarImagen.launch(intentCrop)

        } catch (e: Exception) {
            Log.e("CUENTA_CROP", "Error abriendo recorte", e)

            Toast.makeText(
                this,
                "No se pudo abrir el editor de imagen",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun cargarImagenPerfil(url: String) {
        Log.d("CUENTA_IMAGEN", "URL recibida para cargar imagen: $url")

        if (url.isNotBlank()) {
            val drawableActual = imgPerfil.drawable

            val request = Glide.with(this)
                .load(url)
                .circleCrop()
                .dontAnimate()

            if (drawableActual != null) {
                request
                    .placeholder(drawableActual)
                    .error(drawableActual)
                    .into(imgPerfil)
            } else {
                request
                    .placeholder(R.drawable.ic_user_big)
                    .error(R.drawable.ic_user_big)
                    .into(imgPerfil)
            }

        } else {
            if (imgPerfil.drawable == null) {
                imgPerfil.setImageResource(R.drawable.ic_user_big)
            }
        }
    }

    private fun subirFotoPerfil(uri: Uri) {
        val user = FirebaseAuthHelper.obtenerUsuarioActual()

        if (user == null) {
            Toast.makeText(
                this,
                "La sesión no está activa.\nInicie sesión con contraseña para cambiar la foto.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (uidActual.isBlank()) {
            uidActual = user.uid
        }

        if (user.uid != uidActual) {
            Toast.makeText(
                this,
                "La sesión activa no coincide con este perfil.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        btnCambiarFoto.isEnabled = false
        Toast.makeText(this, "Subiendo foto...", Toast.LENGTH_SHORT).show()

        FirebaseStorageHelper.subirFotoPerfil(
            uid = uidActual,
            imagenUri = uri,
            onSuccess = { url ->
                FirebaseUserHelper.actualizarFotoPerfilUrl(
                    uid = uidActual,
                    fotoPerfilUrl = url,
                    onSuccess = {
                        btnCambiarFoto.isEnabled = true

                        cargarImagenPerfil(url)

                        UsuarioCacheManager.actualizarFoto(
                            context = this,
                            fotoPerfilUrl = url
                        )

                        /*
                         * Actualiza el menú lateral al instante.
                         * Primero refresca los datos desde caché y luego fuerza la recarga visual de la foto.
                         */
                        refrescarMenuDesdeCache()
                        refrescarFotoMenuAlInstante(url)

                        Toast.makeText(
                            this,
                            "Foto actualizada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        Log.d("CUENTA_UPLOAD", "Foto guardada correctamente: $url")
                    },
                    onError = { mensaje ->
                        btnCambiarFoto.isEnabled = true
                        Log.e("CUENTA_UPLOAD", "La foto subió, pero falló Firestore: $mensaje")

                        Toast.makeText(
                            this,
                            "La foto subió, pero no se guardó en el perfil.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            },
            onError = { mensaje ->
                btnCambiarFoto.isEnabled = true
                Log.e("CUENTA_UPLOAD", "Error al subir imagen: $mensaje")

                Toast.makeText(
                    this,
                    "No se pudo subir la imagen. Revise Storage y Logcat.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun mostrarDialogoNotificaciones() {
        val prefs = getSharedPreferences(prefsNotificaciones, MODE_PRIVATE)
        val notificacionesActivas = prefs.getBoolean("notificacionesActivas", false)

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_notificaciones)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val txtEstado = dialog.findViewById<TextView>(R.id.txtEstadoNotificaciones)
        val txtDescripcion = dialog.findViewById<TextView>(R.id.txtDescripcionNotificaciones)
        val btnAccion = dialog.findViewById<Button>(R.id.btnAccionNotificaciones)
        val btnPrueba = dialog.findViewById<Button>(R.id.btnPruebaNotificaciones)
        val btnCancelar = dialog.findViewById<TextView>(R.id.btnCancelarNotificaciones)

        if (notificacionesActivas) {
            txtEstado.text = "Activadas"
            txtEstado.setTextColor(Color.parseColor("#10B981"))
            txtEstado.setBackgroundResource(R.drawable.bg_notification_chip_on)

            txtDescripcion.text =
                "Tus recordatorios preventivos están activos. Puedes enviar una prueba o desactivarlos cuando lo necesites."

            btnAccion.text = "Desactivar notificaciones"
            btnPrueba.visibility = View.VISIBLE
        } else {
            txtEstado.text = "Desactivadas"
            txtEstado.setTextColor(Color.parseColor("#6B7280"))
            txtEstado.setBackgroundResource(R.drawable.bg_notification_chip_off)

            txtDescripcion.text =
                "Activa recordatorios preventivos para mantener una rutina de cuidado personal dentro de Sentix."

            btnAccion.text = "Activar notificaciones"
            btnPrueba.visibility = View.GONE
        }

        btnAccion.setOnClickListener {
            dialog.dismiss()

            if (notificacionesActivas) {
                desactivarNotificaciones()
            } else {
                verificarPermisoYActivarNotificaciones()
            }
        }

        btnPrueba.setOnClickListener {
            dialog.dismiss()
            mostrarNotificacionPrueba()
        }

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        val anchoPantalla = resources.displayMetrics.widthPixels
        val anchoDialogo = (anchoPantalla * 0.90).toInt()

        dialog.window?.setLayout(
            anchoDialogo,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun verificarPermisoYActivarNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permiso = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permiso != PackageManager.PERMISSION_GRANTED) {
                pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        activarNotificaciones()
    }

    private fun activarNotificaciones() {
        crearCanalNotificaciones()

        getSharedPreferences(prefsNotificaciones, MODE_PRIVATE)
            .edit()
            .putBoolean("notificacionesActivas", true)
            .apply()

        if (uidActual.isNotBlank()) {
            FirebaseUserHelper.actualizarNotificaciones(
                uid = uidActual,
                notificacionesEnabled = true,
                onSuccess = {},
                onError = { mensaje ->
                    Log.e("NOTIFICACIONES", "No se pudo sincronizar activación: $mensaje")
                }
            )
        }

        Toast.makeText(
            this,
            "Notificaciones activadas correctamente",
            Toast.LENGTH_SHORT
        ).show()

        mostrarNotificacionPrueba()
    }

    private fun desactivarNotificaciones() {
        getSharedPreferences(prefsNotificaciones, MODE_PRIVATE)
            .edit()
            .putBoolean("notificacionesActivas", false)
            .apply()

        NotificationManagerCompat.from(this).cancelAll()

        if (uidActual.isNotBlank()) {
            FirebaseUserHelper.actualizarNotificaciones(
                uid = uidActual,
                notificacionesEnabled = false,
                onSuccess = {},
                onError = { mensaje ->
                    Log.e("NOTIFICACIONES", "No se pudo sincronizar desactivación: $mensaje")
                }
            )
        }

        Toast.makeText(
            this,
            "Notificaciones desactivadas",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                canalSentix,
                "Bienestar Sentix",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recordatorios y alertas preventivas de Sentix"
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }
    }

    @SuppressLint("MissingPermission")
    private fun mostrarNotificacionPrueba() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permiso = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permiso != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Primero concede el permiso de notificaciones.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        crearCanalNotificaciones()

        val intent = Intent(this, SuccessActivity::class.java).apply {
            putExtra("uid", uidActual)
            putExtra("email", emailActual)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(this, canalSentix)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sentix")
            .setContentText("Sentix te acompañará con recordatorios preventivos.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Sentix te enviará recordatorios y alertas preventivas relacionadas con tu bienestar emocional.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(2001, notificacion)
    }

    private fun mostrarDialogoEliminarCuenta() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_eliminar_cuenta)
        dialog.setCanceledOnTouchOutside(true)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnCancelar = dialog.findViewById<Button>(R.id.btnCancelarEliminarCuenta)
        val btnContinuar = dialog.findViewById<Button>(R.id.btnContinuarEliminarCuenta)

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnContinuar.setOnClickListener {
            dialog.dismiss()
            mostrarDialogoConfirmarPasswordEliminar()
        }

        dialog.show()

        val anchoPantalla = resources.displayMetrics.widthPixels
        val anchoDialogo = (anchoPantalla * 0.88).toInt()

        dialog.window?.setLayout(
            anchoDialogo,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun mostrarDialogoConfirmarPasswordEliminar() {
        val email = FirebaseAuthHelper.obtenerEmailActual().ifBlank {
            emailActual.ifBlank {
                UsuarioCacheManager.obtener(this).email
            }
        }

        if (email.isBlank()) {
            Toast.makeText(
                this,
                "No se encontró el correo de la cuenta.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_confirmar_password_eliminar)
        dialog.setCanceledOnTouchOutside(true)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val txtCorreoEliminar = dialog.findViewById<TextView>(R.id.txtCorreoEliminarPassword)
        val edtPasswordEliminar = dialog.findViewById<EditText>(R.id.edtPasswordEliminarCuenta)
        val btnCancelar = dialog.findViewById<Button>(R.id.btnCancelarPasswordEliminar)
        val btnEliminar = dialog.findViewById<Button>(R.id.btnConfirmarPasswordEliminar)

        txtCorreoEliminar.text = email

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnEliminar.setOnClickListener {
            val password = edtPasswordEliminar.text.toString().trim()

            if (password.isBlank()) {
                edtPasswordEliminar.error = "Ingrese su contraseña"
                return@setOnClickListener
            }

            btnEliminar.isEnabled = false
            btnEliminar.text = "Verificando..."

            dialog.dismiss()
            reautenticarYEliminarCuenta(email, password)
        }

        dialog.show()

        val anchoPantalla = resources.displayMetrics.widthPixels
        val anchoDialogo = (anchoPantalla * 0.88).toInt()

        dialog.window?.setLayout(
            anchoDialogo,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun reautenticarYEliminarCuenta(email: String, password: String) {
        Toast.makeText(this, "Verificando contraseña...", Toast.LENGTH_SHORT).show()

        FirebaseAuthHelper.reautenticarConPassword(
            email = email,
            password = password,
            onSuccess = {
                eliminarCuentaCompleta()
            },
            onError = { mensaje ->
                Log.e("ELIMINAR_CUENTA", "Error de reautenticación: $mensaje")

                Toast.makeText(
                    this,
                    "La contraseña ingresada no es correcta.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun eliminarCuentaCompleta() {
        val user = FirebaseAuthHelper.obtenerUsuarioActual()

        if (user == null) {
            Toast.makeText(
                this,
                "La sesión no está activa.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val uid = user.uid

        if (uidActual.isBlank()) {
            uidActual = uid
        }

        if (uid != uidActual) {
            Toast.makeText(
                this,
                "La sesión activa no coincide con este perfil.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        itemEliminarCuenta.isEnabled = false

        Toast.makeText(
            this,
            "Eliminando cuenta...",
            Toast.LENGTH_SHORT
        ).show()

        eliminarFotoPerfil {
            eliminarHistorialUsuario(uid) {
                FirebaseUserHelper.eliminarDocumentoUsuario(
                    uid = uid,
                    onSuccess = {
                        FirebaseAuthHelper.eliminarCuentaAuth(
                            onSuccess = {
                                limpiarDatosLocalesTrasEliminar()

                                Toast.makeText(
                                    this,
                                    "Cuenta eliminada correctamente",
                                    Toast.LENGTH_LONG
                                ).show()

                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            },
                            onError = { mensaje ->
                                itemEliminarCuenta.isEnabled = true
                                Log.e("ELIMINAR_CUENTA", "Error eliminando Auth: $mensaje")

                                Toast.makeText(
                                    this,
                                    "No se pudo eliminar la cuenta de autenticación.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    },
                    onError = { mensaje ->
                        itemEliminarCuenta.isEnabled = true
                        Log.e("ELIMINAR_CUENTA", "Error eliminando documento Firestore: $mensaje")

                        Toast.makeText(
                            this,
                            "No se pudo eliminar el perfil de Firestore.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    private fun eliminarFotoPerfil(onComplete: () -> Unit) {
        val cache = UsuarioCacheManager.obtener(this)
        val urlFoto = cache.fotoPerfilUrl

        if (urlFoto.isBlank()) {
            onComplete()
            return
        }

        FirebaseStorageHelper.eliminarFotoPerfilPorUrl(
            fotoPerfilUrl = urlFoto,
            onSuccess = {
                Log.d("ELIMINAR_CUENTA", "Foto de perfil eliminada")
                onComplete()
            },
            onError = { mensaje ->
                Log.w("ELIMINAR_CUENTA", "No se eliminó foto o no existía: $mensaje")
                onComplete()
            }
        )
    }

    private fun eliminarHistorialUsuario(uid: String, onComplete: () -> Unit) {
        /*
         * Se mantiene con Firestore directo porque es una subcolección.
         * Más adelante se puede mover a un HistorialHelper si deseas.
         */
        firestore.collection("usuarios")
            .document(uid)
            .collection("historial_emocional")
            .get()
            .addOnSuccessListener { resultado ->
                if (resultado.isEmpty) {
                    onComplete()
                    return@addOnSuccessListener
                }

                val batch = firestore.batch()

                for (documento in resultado.documents) {
                    batch.delete(documento.reference)
                }

                batch.commit()
                    .addOnSuccessListener {
                        Log.d("ELIMINAR_CUENTA", "Historial emocional eliminado")
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e("ELIMINAR_CUENTA", "Error eliminando historial", e)

                        Toast.makeText(
                            this,
                            "No se pudo eliminar el historial emocional.",
                            Toast.LENGTH_LONG
                        ).show()

                        itemEliminarCuenta.isEnabled = true
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ELIMINAR_CUENTA", "Error consultando historial", e)

                Toast.makeText(
                    this,
                    "No se pudo consultar el historial emocional.",
                    Toast.LENGTH_LONG
                ).show()

                itemEliminarCuenta.isEnabled = true
            }
    }

    private fun limpiarDatosLocalesTrasEliminar() {
        UsuarioCacheManager.limpiar(this)

        getSharedPreferences("login", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        getSharedPreferences(prefsNotificaciones, MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}