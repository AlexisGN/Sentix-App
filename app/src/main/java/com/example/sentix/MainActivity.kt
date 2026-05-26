package com.example.sentix

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.sentix.data.FirebaseAuthHelper
import com.example.sentix.data.FirebaseUserHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class MainActivity : AppCompatActivity() {

    private lateinit var btnBiometrico: ImageButton
    private lateinit var btnLogin: Button

    private val handler = Handler(Looper.getMainLooper())
    private var mostrandoHuella = true

    private var emailPendiente = ""
    private var uidPendiente = ""
    private var recordarPendiente = false
    private var correoRegistradoPendiente = ""

    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)

                val gmailSeleccionado = account.email ?: ""

                Log.d("GMAIL", "Gmail seleccionado: $gmailSeleccionado")
                Log.d("GMAIL", "Gmail registrado: $correoRegistradoPendiente")

                if (gmailSeleccionado.equals(correoRegistradoPendiente, ignoreCase = true)) {
                    guardarPreferencias(emailPendiente, uidPendiente, recordarPendiente)

                    Toast.makeText(
                        this,
                        "Gmail verificado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    irAPantallaExito(emailPendiente, uidPendiente)

                } else {
                    FirebaseAuthHelper.cerrarSesion(this)
                    restaurarBotonLogin()

                    Toast.makeText(
                        this,
                        "Debe seleccionar el Gmail registrado en su cuenta: $correoRegistradoPendiente",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: ApiException) {
                FirebaseAuthHelper.cerrarSesion(this)
                restaurarBotonLogin()

                Log.e("GMAIL", "Error ApiException al verificar Gmail", e)

                val mensaje = when (e.statusCode) {
                    12501 -> "Verificación cancelada. Debe seleccionar su cuenta Gmail para continuar"
                    7 -> "No hay conexión a internet para verificar Gmail"
                    10 -> "La configuración de Google Sign-In no es válida. Revise el Web Client ID"
                    else -> "No se pudo verificar Gmail. Inténtelo nuevamente"
                }

                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                FirebaseAuthHelper.cerrarSesion(this)
                restaurarBotonLogin()

                Log.e("GMAIL", "Error general al verificar Gmail", e)

                Toast.makeText(
                    this,
                    "No se pudo verificar Gmail. Inténtelo nuevamente",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootMain = findViewById<View>(R.id.rootMain)

        SystemBarsHelper.aplicarInsets(
            activity = this,
            rootView = rootMain,
            aplicarArriba = true,
            aplicarAbajo = true
        )

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val chkMostrarPassword = findViewById<CheckBox>(R.id.chkMostrarPassword)
        val chkRecordar = findViewById<CheckBox>(R.id.chkRecordar)
        val txtRegistro = findViewById<TextView>(R.id.txtRegistro)
        val txtRecuperarPassword = findViewById<TextView>(R.id.txtRecuperarPassword)

        btnLogin = findViewById(R.id.btnLogin)
        btnBiometrico = findViewById(R.id.btnBiometrico)

        val preferencias = getSharedPreferences("login", MODE_PRIVATE)
        val recordar = preferencias.getBoolean("recordar", false)
        val emailGuardado = preferencias.getString("email", "") ?: ""

        var cargandoPreferencias = true

        if (recordar && emailGuardado.isNotEmpty()) {
            edtEmail.setText(emailGuardado)
            edtPassword.setText("")
            chkRecordar.isChecked = true
            btnBiometrico.visibility = View.VISIBLE
            iniciarAnimacionBiometrica()
        } else {
            btnBiometrico.visibility = View.GONE
        }

        cargandoPreferencias = false

        chkRecordar.setOnCheckedChangeListener { _, isChecked ->
            if (!cargandoPreferencias && !isChecked) {
                getSharedPreferences("login", MODE_PRIVATE).edit().clear().apply()

                edtEmail.setText("")
                edtPassword.setText("")
                btnBiometrico.visibility = View.GONE

                Toast.makeText(this, "Cuenta recordada desactivada", Toast.LENGTH_SHORT).show()
            }
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!cargandoPreferencias && chkRecordar.isChecked) {
                    val correoActual = edtEmail.text.toString().trim().lowercase()

                    if (correoActual != emailGuardado.lowercase()) {
                        getSharedPreferences("login", MODE_PRIVATE).edit().clear().apply()

                        chkRecordar.isChecked = false
                        btnBiometrico.visibility = View.GONE

                        Toast.makeText(
                            this@MainActivity,
                            "Cuenta recordada desactivada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        edtEmail.addTextChangedListener(textWatcher)

        chkMostrarPassword.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                edtPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                chkMostrarPassword.text = "Ocultar contraseña"
            } else {
                edtPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                chkMostrarPassword.text = "Ver contraseña"
            }

            edtPassword.setSelection(edtPassword.text.length)
        }

        btnLogin.setOnClickListener {
            Log.d("LOGIN_FIREBASE", "Click en INGRESAR")

            val login = edtEmail.text.toString().trim().lowercase()
            val password = edtPassword.text.toString().trim()

            if (login.isEmpty()) {
                edtEmail.error = "Ingrese su correo o usuario"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                edtPassword.error = "Ingrese su contraseña"
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "VALIDANDO..."

            iniciarSesionFirebase(login, password, chkRecordar.isChecked)
        }

        btnBiometrico.setOnClickListener {
            val preferenciasActuales = getSharedPreferences("login", MODE_PRIVATE)

            val emailBiometrico = preferenciasActuales.getString("email", "") ?: ""
            val uidBiometrico = preferenciasActuales.getString("uid", "") ?: ""
            val biometriaHabilitada = preferenciasActuales.getBoolean("biometriaHabilitada", false)

            if (emailBiometrico.isEmpty() || uidBiometrico.isEmpty()) {
                Toast.makeText(
                    this,
                    "Primero inicie sesión con contraseña y active recordar cuenta",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (biometriaHabilitada) {
                autenticarBiometricamente(emailBiometrico, uidBiometrico)
            } else {
                pedirPasswordAntesDeBiometria(emailBiometrico, uidBiometrico)
            }
        }

        txtRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        txtRecuperarPassword.setOnClickListener {
            mostrarDialogoRecuperarPassword()
        }
    }

    private fun iniciarSesionFirebase(login: String, password: String, recordar: Boolean) {
        if (login.contains("@")) {
            if (!Patterns.EMAIL_ADDRESS.matcher(login).matches()) {
                restaurarBotonLogin()
                Toast.makeText(this, "Ingrese un correo electrónico válido", Toast.LENGTH_SHORT).show()
                return
            }

            verificarCorreoRegistradoAntesDeLogin(login, password, recordar)
        } else {
            buscarEmailPorUsuario(login, password, recordar)
        }
    }

    private fun verificarCorreoRegistradoAntesDeLogin(
        email: String,
        password: String,
        recordar: Boolean
    ) {
        FirebaseUserHelper.buscarUsuarioPorEmailOUsuario(
            login = email,
            onSuccess = { documento ->
                if (documento == null || !documento.exists()) {
                    restaurarBotonLogin()

                    Toast.makeText(
                        this,
                        "El correo ingresado no está registrado",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@buscarUsuarioPorEmailOUsuario
                }

                iniciarSesionConEmail(email, password, recordar)
            },
            onError = { mensaje ->
                restaurarBotonLogin()

                Log.e("LOGIN_FIREBASE", "Error al verificar correo: $mensaje")

                Toast.makeText(
                    this,
                    obtenerMensajeFirestoreDesdeTexto(mensaje, "No se pudo verificar el correo"),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun buscarEmailPorUsuario(usuario: String, password: String, recordar: Boolean) {
        FirebaseUserHelper.buscarUsuarioPorEmailOUsuario(
            login = usuario,
            onSuccess = { documento ->
                if (documento == null || !documento.exists()) {
                    restaurarBotonLogin()

                    Toast.makeText(
                        this,
                        "El usuario ingresado no está registrado",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@buscarUsuarioPorEmailOUsuario
                }

                val email = documento.getString("email") ?: ""

                if (email.isEmpty()) {
                    restaurarBotonLogin()

                    Toast.makeText(
                        this,
                        "La cuenta existe, pero no tiene un correo asociado",
                        Toast.LENGTH_LONG
                    ).show()

                    return@buscarUsuarioPorEmailOUsuario
                }

                iniciarSesionConEmail(email.lowercase(), password, recordar)
            },
            onError = { mensaje ->
                restaurarBotonLogin()

                Log.e("LOGIN_FIREBASE", "Error al buscar usuario: $mensaje")

                Toast.makeText(
                    this,
                    obtenerMensajeFirestoreDesdeTexto(mensaje, "No se pudo buscar el usuario"),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun iniciarSesionConEmail(email: String, password: String, recordar: Boolean) {
        FirebaseAuthHelper.iniciarSesion(
            email = email,
            password = password,
            onSuccess = { firebaseUser ->
                val uid = firebaseUser.uid

                FirebaseUserHelper.obtenerDocumentoUsuario(
                    uid = uid,
                    onSuccess = { documento ->
                        if (!documento.exists()) {
                            FirebaseAuthHelper.cerrarSesion(this)
                            restaurarBotonLogin()

                            Toast.makeText(
                                this,
                                "La cuenta existe, pero el perfil no está completo",
                                Toast.LENGTH_LONG
                            ).show()

                            return@obtenerDocumentoUsuario
                        }

                        UsuarioCacheManager.guardarDesdeFirestore(
                            context = this,
                            uid = uid,
                            documento = documento
                        )

                        val correoRegistrado = documento.getString("email") ?: email

                        emailPendiente = correoRegistrado.lowercase()
                        uidPendiente = uid
                        recordarPendiente = recordar
                        correoRegistradoPendiente = correoRegistrado.lowercase()

                        Toast.makeText(
                            this,
                            "Credenciales correctas. Verificando Gmail...",
                            Toast.LENGTH_SHORT
                        ).show()

                        verificarGmailSegundoFactor()
                    },
                    onError = { mensaje ->
                        FirebaseAuthHelper.cerrarSesion(this)
                        restaurarBotonLogin()

                        Log.e("LOGIN_FIREBASE", "Error al cargar perfil: $mensaje")

                        Toast.makeText(
                            this,
                            obtenerMensajeFirestoreDesdeTexto(mensaje, "No se pudo cargar el perfil"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            },
            onError = { mensaje ->
                restaurarBotonLogin()

                Log.e("LOGIN_FIREBASE", "Error al iniciar sesión: $mensaje")

                Toast.makeText(
                    this,
                    obtenerMensajeLoginFirebaseDesdeTexto(mensaje),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun ajustarDialogResponsivo(dialog: Dialog) {
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.setOnShowListener {
            val ancho = (resources.displayMetrics.widthPixels * 0.90).toInt()
            dialog.window?.setLayout(ancho, WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun verificarGmailSegundoFactor() {
        btnLogin.isEnabled = false
        btnLogin.text = "VERIFICANDO GMAIL..."

        Log.d("GMAIL", "Correo registrado en Firestore: $correoRegistradoPendiente")

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.web_client_id))
            .build()

        val googleClient = GoogleSignIn.getClient(this, googleSignInOptions)

        googleClient.signOut().addOnCompleteListener {
            googleLauncher.launch(googleClient.signInIntent)
        }
    }

    private fun guardarPreferencias(email: String, uid: String, recordar: Boolean) {
        val preferencias = getSharedPreferences("login", MODE_PRIVATE)
        val biometriaYaHabilitada = preferencias.getBoolean("biometriaHabilitada", false)
        val passwordGuardada = preferencias.getString("password", "") ?: ""

        val editor = preferencias.edit()

        if (recordar) {
            editor.putBoolean("recordar", true)
            editor.putString("email", email)
            editor.putString("uid", uid)
            editor.putBoolean("biometriaHabilitada", biometriaYaHabilitada)

            if (passwordGuardada.isNotBlank()) {
                editor.putString("password", passwordGuardada)
            }
        } else {
            editor.clear()
        }

        editor.apply()
    }

    private fun restaurarBotonLogin() {
        btnLogin.isEnabled = true
        btnLogin.text = "INGRESAR"
    }

    private fun autenticarBiometricamente(email: String, uid: String) {
        val biometricManager = BiometricManager.from(this)

        val puedeAutenticar = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (puedeAutenticar != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(
                this,
                "La biometría no está disponible en este dispositivo",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    Toast.makeText(
                        this@MainActivity,
                        "Ingreso biométrico correcto",
                        Toast.LENGTH_SHORT
                    ).show()

                    entrarConBiometria(email, uid)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)

                    val mensaje = when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> {
                            "Ingreso biométrico cancelado"
                        }

                        BiometricPrompt.ERROR_LOCKOUT -> {
                            "Demasiados intentos fallidos. Espere un momento e inténtelo nuevamente"
                        }

                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            "La biometría fue bloqueada. Use el bloqueo del dispositivo para continuar"
                        }

                        else -> {
                            "No se pudo completar la autenticación biométrica"
                        }
                    }

                    Toast.makeText(this@MainActivity, mensaje, Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()

                    Toast.makeText(
                        this@MainActivity,
                        "Biometría no reconocida. Inténtelo nuevamente",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Ingreso biométrico")
            .setSubtitle("Use su huella o bloqueo del dispositivo")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun entrarConBiometria(email: String, uid: String) {
        val preferencias = getSharedPreferences("login", MODE_PRIVATE)

        val biometriaHabilitada = preferencias.getBoolean("biometriaHabilitada", false)
        val passwordGuardada = preferencias.getString("password", "") ?: ""

        if (!biometriaHabilitada) {
            pedirPasswordAntesDeBiometria(email, uid)
            return
        }

        if (passwordGuardada.isBlank()) {
            Toast.makeText(
                this,
                "Ingrese su contraseña una vez para activar el ingreso biométrico.",
                Toast.LENGTH_LONG
            ).show()

            pedirPasswordAntesDeBiometria(email, uid)
            return
        }

        btnBiometrico.isEnabled = false

        FirebaseAuthHelper.iniciarSesion(
            email = email,
            password = passwordGuardada,
            onSuccess = { user ->
                if (user.uid != uid) {
                    btnBiometrico.isEnabled = true

                    Toast.makeText(
                        this,
                        "La cuenta biométrica no coincide con la cuenta recordada.",
                        Toast.LENGTH_LONG
                    ).show()

                    FirebaseAuthHelper.cerrarSesion(this)
                    return@iniciarSesion
                }

                actualizarCacheYEntrar(email, uid)
            },
            onError = { mensaje ->
                btnBiometrico.isEnabled = true

                Log.e("BIOMETRIA_LOGIN", "Error al ingresar con biometría: $mensaje")

                Toast.makeText(
                    this,
                    "No se pudo ingresar con biometría. Active nuevamente el ingreso biométrico.",
                    Toast.LENGTH_LONG
                ).show()

                getSharedPreferences("login", MODE_PRIVATE)
                    .edit()
                    .putBoolean("biometriaHabilitada", false)
                    .remove("password")
                    .apply()

                pedirPasswordAntesDeBiometria(email, uid)
            }
        )
    }

    private fun actualizarCacheYEntrar(email: String, uid: String) {
        FirebaseUserHelper.obtenerDocumentoUsuario(
            uid = uid,
            onSuccess = { documento ->
                if (documento.exists()) {
                    UsuarioCacheManager.guardarDesdeFirestore(
                        context = this,
                        uid = uid,
                        documento = documento
                    )
                }

                btnBiometrico.isEnabled = true
                irAPantallaExito(email, uid)
            },
            onError = { mensaje ->
                Log.e("BIOMETRIA_LOGIN", "No se pudo actualizar caché antes de entrar: $mensaje")

                btnBiometrico.isEnabled = true
                irAPantallaExito(email, uid)
            }
        )
    }

    private fun pedirPasswordAntesDeBiometria(email: String, uid: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_activar_biometria)
        dialog.setCanceledOnTouchOutside(true)
        ajustarDialogResponsivo(dialog)

        val edtPassword = dialog.findViewById<EditText>(R.id.edtPasswordBiometria)
        val chkMostrar = dialog.findViewById<CheckBox>(R.id.chkMostrarPasswordBiometria)
        val txtMensaje = dialog.findViewById<TextView>(R.id.txtMensajeBiometria)
        val btnActivar = dialog.findViewById<Button>(R.id.btnActivarBiometria)
        val btnCancelar = dialog.findViewById<TextView>(R.id.btnCancelarBiometria)

        chkMostrar.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                edtPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                chkMostrar.text = "Ocultar contraseña"
            } else {
                edtPassword.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                chkMostrar.text = "Ver contraseña"
            }

            edtPassword.setSelection(edtPassword.text.length)
        }

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnActivar.setOnClickListener {
            val password = edtPassword.text.toString().trim()

            txtMensaje.visibility = View.GONE
            txtMensaje.text = ""

            if (password.isEmpty()) {
                txtMensaje.text = "Ingrese su contraseña para activar el ingreso biométrico."
                txtMensaje.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnActivar.isEnabled = false
            btnActivar.text = "Validando..."

            FirebaseAuthHelper.iniciarSesion(
                email = email,
                password = password,
                onSuccess = { user ->
                    if (user.uid != uid) {
                        btnActivar.isEnabled = true
                        btnActivar.text = "Activar biometría"

                        txtMensaje.text = "La cuenta autenticada no coincide con la cuenta recordada."
                        txtMensaje.visibility = View.VISIBLE

                        FirebaseAuthHelper.cerrarSesion(this)
                        return@iniciarSesion
                    }

                    getSharedPreferences("login", MODE_PRIVATE)
                        .edit()
                        .putBoolean("recordar", true)
                        .putString("email", email)
                        .putString("uid", uid)
                        .putString("password", password)
                        .putBoolean("biometriaHabilitada", true)
                        .apply()

                    FirebaseUserHelper.actualizarBiometria(
                        uid = uid,
                        biometricEnabled = true,
                        onSuccess = {
                            Toast.makeText(
                                this,
                                "Sesión biométrica reactivada correctamente",
                                Toast.LENGTH_SHORT
                            ).show()

                            dialog.dismiss()

                            Toast.makeText(
                                this,
                                "Biometría activada. Confirme su huella para ingresar.",
                                Toast.LENGTH_SHORT
                            ).show()

                            autenticarBiometricamente(email, uid)
                        },
                        onError = { mensaje ->
                            Log.e("BIOMETRIA", "No se pudo actualizar biometría: $mensaje")

                            Toast.makeText(
                                this,
                                "Biometría activada localmente, pero no se pudo sincronizar con Firebase.",
                                Toast.LENGTH_LONG
                            ).show()

                            dialog.dismiss()
                            autenticarBiometricamente(email, uid)
                        }
                    )
                },
                onError = { mensaje ->
                    btnActivar.isEnabled = true
                    btnActivar.text = "Activar biometría"

                    Log.e("BIOMETRIA", "Error al validar contraseña para biometría: $mensaje")

                    txtMensaje.text = obtenerMensajeLoginFirebaseDesdeTexto(mensaje)
                    txtMensaje.visibility = View.VISIBLE
                }
            )
        }

        dialog.show()
    }

    private fun mostrarDialogoRecuperarPassword() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_recuperar_password)
        dialog.setCanceledOnTouchOutside(true)
        ajustarDialogResponsivo(dialog)

        val edtCorreo = dialog.findViewById<EditText>(R.id.edtCorreoRecuperacion)
        val txtMensaje = dialog.findViewById<TextView>(R.id.txtMensajeRecuperacion)
        val btnEnviar = dialog.findViewById<Button>(R.id.btnEnviarRecuperacion)
        val btnCancelar = dialog.findViewById<TextView>(R.id.btnCancelarRecuperacion)

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnEnviar.setOnClickListener {
            val email = edtCorreo.text.toString().trim().lowercase()

            txtMensaje.visibility = View.GONE
            txtMensaje.text = ""

            if (email.isEmpty()) {
                txtMensaje.text = "Ingrese su correo electrónico."
                txtMensaje.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                txtMensaje.text = "Ingrese un correo electrónico válido."
                txtMensaje.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnEnviar.isEnabled = false
            btnEnviar.text = "Verificando..."

            FirebaseUserHelper.buscarUsuarioPorEmailOUsuario(
                login = email,
                onSuccess = { documento ->
                    if (documento == null || !documento.exists()) {
                        btnEnviar.isEnabled = true
                        btnEnviar.text = "Enviar enlace"

                        txtMensaje.text = "No encontramos una cuenta registrada con ese correo."
                        txtMensaje.visibility = View.VISIBLE
                        return@buscarUsuarioPorEmailOUsuario
                    }

                    btnEnviar.text = "Enviando..."

                    FirebaseAuthHelper.enviarRecuperacionPassword(
                        email = email,
                        onSuccess = {
                            Log.d(
                                "RECUPERAR_PASSWORD",
                                "Correo de recuperación solicitado correctamente para: $email"
                            )

                            Toast.makeText(
                                this,
                                "Enlace enviado.\nRevisa tu correo, Bandeja o Spam.",
                                Toast.LENGTH_LONG
                            ).show()

                            dialog.dismiss()
                        },
                        onError = { mensaje ->
                            btnEnviar.isEnabled = true
                            btnEnviar.text = "Enviar enlace"

                            Log.e("RECUPERAR_PASSWORD", "Error al enviar recuperación: $mensaje")

                            txtMensaje.text = obtenerMensajeRecuperacionDesdeTexto(mensaje)
                            txtMensaje.visibility = View.VISIBLE
                        }
                    )
                },
                onError = { mensaje ->
                    btnEnviar.isEnabled = true
                    btnEnviar.text = "Enviar enlace"

                    Log.e("RECUPERAR_PASSWORD", "Error al verificar correo: $mensaje")

                    txtMensaje.text =
                        obtenerMensajeFirestoreDesdeTexto(mensaje, "No se pudo verificar el correo")
                    txtMensaje.visibility = View.VISIBLE
                }
            )
        }

        dialog.show()
    }

    private fun obtenerMensajeLoginFirebaseDesdeTexto(mensajeOriginal: String): String {
        val mensaje = mensajeOriginal.lowercase()

        return when {
            mensaje.contains("network") ||
                    mensaje.contains("timeout") ||
                    mensaje.contains("unable to resolve host") ||
                    mensaje.contains("internet") -> {
                "No hay conexión a internet. Revise su red e inténtelo nuevamente"
            }

            mensaje.contains("too many") ||
                    mensaje.contains("blocked") ||
                    mensaje.contains("attempts") -> {
                "Se realizaron demasiados intentos. Espere unos minutos e inténtelo otra vez"
            }

            mensaje.contains("invalid_login_credentials") ||
                    mensaje.contains("invalid credential") ||
                    mensaje.contains("auth credential") ||
                    mensaje.contains("credential") ||
                    mensaje.contains("incorrect") ||
                    mensaje.contains("malformed") ||
                    mensaje.contains("expired") ||
                    mensaje.contains("password") ||
                    mensaje.contains("contraseña") -> {
                "La contraseña ingresada es incorrecta"
            }

            mensaje.contains("no user record") ||
                    mensaje.contains("user-not-found") ||
                    mensaje.contains("user not found") ||
                    mensaje.contains("there is no user") -> {
                "El correo ingresado no está registrado"
            }

            mensaje.contains("email badly formatted") ||
                    mensaje.contains("badly formatted") ||
                    mensaje.contains("invalid email") -> {
                "El correo ingresado no es válido"
            }

            else -> {
                "No se pudo iniciar sesión. Verifique sus datos e inténtelo nuevamente"
            }
        }
    }

    private fun obtenerMensajeFirestoreDesdeTexto(mensajeOriginal: String, accion: String): String {
        val mensaje = mensajeOriginal.lowercase()

        return when {
            mensaje.contains("permission") || mensaje.contains("denied") -> {
                "$accion porque no tiene permisos suficientes"
            }

            mensaje.contains("network") ||
                    mensaje.contains("timeout") ||
                    mensaje.contains("unable to resolve host") ||
                    mensaje.contains("internet") -> {
                "$accion porque no hay conexión a internet"
            }

            mensaje.contains("unavailable") -> {
                "$accion porque el servicio no está disponible temporalmente"
            }

            else -> {
                "$accion. Inténtelo nuevamente"
            }
        }
    }

    private fun obtenerMensajeRecuperacionDesdeTexto(mensajeOriginal: String): String {
        val mensaje = mensajeOriginal.lowercase()

        return when {
            mensaje.contains("network") ||
                    mensaje.contains("timeout") ||
                    mensaje.contains("unable to resolve host") ||
                    mensaje.contains("internet") -> {
                "No hay conexión a internet. Revise su red e inténtelo nuevamente"
            }

            mensaje.contains("too many") ||
                    mensaje.contains("blocked") -> {
                "Se realizaron demasiados intentos. Espere unos minutos e inténtelo otra vez"
            }

            else -> {
                "No se pudo enviar el correo de recuperación. Inténtelo nuevamente"
            }
        }
    }

    private fun iniciarAnimacionBiometrica() {
        handler.post(object : Runnable {
            override fun run() {
                if (::btnBiometrico.isInitialized && btnBiometrico.visibility == View.VISIBLE) {
                    if (mostrandoHuella) {
                        btnBiometrico.setImageResource(R.drawable.ic_face)
                    } else {
                        btnBiometrico.setImageResource(R.drawable.ic_fingerprint)
                    }

                    mostrandoHuella = !mostrandoHuella
                    handler.postDelayed(this, 1200)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun irAPantallaExito(email: String, uid: String) {
        val intent = Intent(this, SuccessActivity::class.java)
        intent.putExtra("login", email)
        intent.putExtra("email", email)
        intent.putExtra("uid", uid)
        startActivity(intent)
        finish()
    }
}