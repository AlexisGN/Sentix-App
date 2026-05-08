package com.example.sentix

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var btnBiometrico: ImageButton
    private lateinit var btnLogin: Button

    private val handler = Handler(Looper.getMainLooper())
    private var mostrandoHuella = true

    private var loginPendiente = ""
    private var passwordPendiente = ""
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
                    guardarPreferencias(loginPendiente, passwordPendiente, recordarPendiente)
                    Toast.makeText(this, "Gmail verificado correctamente", Toast.LENGTH_SHORT).show()
                    irAPantallaExito(loginPendiente)
                } else {
                    restaurarBotonLogin()
                    Toast.makeText(
                        this,
                        "Debes seleccionar el Gmail registrado: $correoRegistradoPendiente",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                restaurarBotonLogin()
                Log.e("GMAIL", "Error al verificar Gmail", e)
                Toast.makeText(this, "No se pudo verificar Gmail", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DBHelper(this)

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val chkMostrarPassword = findViewById<CheckBox>(R.id.chkMostrarPassword)
        val chkRecordar = findViewById<CheckBox>(R.id.chkRecordar)
        val txtRegistro = findViewById<TextView>(R.id.txtRegistro)

        btnLogin = findViewById(R.id.btnLogin)
        btnBiometrico = findViewById(R.id.btnBiometrico)

        val preferencias = getSharedPreferences("login", MODE_PRIVATE)
        val recordar = preferencias.getBoolean("recordar", false)
        val loginGuardado = preferencias.getString("login", "") ?: ""

        var cargandoPreferencias = true

        if (recordar && loginGuardado.isNotEmpty()) {
            edtEmail.setText(loginGuardado)
            edtPassword.setText(preferencias.getString("password", ""))
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
                    val correoVacio = edtEmail.text.toString().trim().isEmpty()
                    val passwordVacio = edtPassword.text.toString().trim().isEmpty()

                    if (correoVacio || passwordVacio) {
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
        edtPassword.addTextChangedListener(textWatcher)

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
            Log.d("GMAIL", "Click en INGRESAR")

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

            val existeLogin = dbHelper.existeUsuarioOCorreo(login)
            val passwordCorrecta = dbHelper.validarPassword(login, password)

            if (!existeLogin) {
                Toast.makeText(this, "Usuario/correo incorrecto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!passwordCorrecta) {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verificarGmailSegundoFactor(login, password, chkRecordar.isChecked)
        }

        btnBiometrico.setOnClickListener {
            val loginBiometrico = preferencias.getString("login", "") ?: ""
            val biometriaHabilitada = preferencias.getBoolean("biometriaHabilitada", false)

            if (loginBiometrico.isEmpty()) {
                Toast.makeText(
                    this,
                    "Primero inicia sesión con contraseña y activa recordar cuenta",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (biometriaHabilitada) {
                autenticarBiometricamente(loginBiometrico)
            } else {
                pedirPasswordAntesDeBiometria(loginBiometrico)
            }
        }

        txtRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun verificarGmailSegundoFactor(login: String, password: String, recordar: Boolean) {
        val usuario = dbHelper.obtenerDatosUsuarioPorLogin(login)

        if (usuario == null) {
            Toast.makeText(this, "No se pudo obtener el correo del usuario", Toast.LENGTH_SHORT).show()
            return
        }

        loginPendiente = login
        passwordPendiente = password
        recordarPendiente = recordar
        correoRegistradoPendiente = usuario.email

        btnLogin.isEnabled = false
        btnLogin.text = "VERIFICANDO GMAIL..."

        Log.d("GMAIL", "Correo registrado en BD: ${usuario.email}")

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.web_client_id))
            .build()

        val googleClient = GoogleSignIn.getClient(this, googleSignInOptions)

        googleClient.signOut().addOnCompleteListener {
            googleLauncher.launch(googleClient.signInIntent)
        }
    }

    private fun guardarPreferencias(login: String, password: String, recordar: Boolean) {
        val editor = getSharedPreferences("login", MODE_PRIVATE).edit()

        if (recordar) {
            editor.putBoolean("recordar", true)
            editor.putString("login", login)
            editor.putString("password", password)
        } else {
            editor.clear()
        }

        editor.apply()
    }

    private fun restaurarBotonLogin() {
        btnLogin.isEnabled = true
        btnLogin.text = "INGRESAR"
    }

    private fun autenticarBiometricamente(login: String) {
        val biometricManager = BiometricManager.from(this)

        val puedeAutenticar = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (puedeAutenticar != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometría no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    irAPantallaExito(login)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@MainActivity, errString, Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@MainActivity, "Biometría no reconocida", Toast.LENGTH_SHORT).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Ingreso biométrico")
            .setSubtitle("Usa tu huella o bloqueo del dispositivo")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun pedirPasswordAntesDeBiometria(login: String) {
        val input = EditText(this)
        input.hint = "Ingrese su contraseña"
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Habilitar huella digital")
            .setMessage("Ingrese su contraseña una sola vez para activar el ingreso con huella.")
            .setView(input)
            .setPositiveButton("Continuar") { _, _ ->
                val password = input.text.toString().trim()

                if (password.isEmpty()) {
                    Toast.makeText(this, "Ingrese su contraseña", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val passwordCorrecta = dbHelper.validarPassword(login, password)

                if (passwordCorrecta) {
                    getSharedPreferences("login", MODE_PRIVATE)
                        .edit()
                        .putBoolean("biometriaHabilitada", true)
                        .apply()

                    Toast.makeText(this, "Huella digital habilitada", Toast.LENGTH_SHORT).show()
                    autenticarBiometricamente(login)
                } else {
                    Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun iniciarAnimacionBiometrica() {
        handler.post(object : Runnable {
            override fun run() {
                if (mostrandoHuella) {
                    btnBiometrico.setImageResource(R.drawable.ic_face)
                } else {
                    btnBiometrico.setImageResource(R.drawable.ic_fingerprint)
                }

                mostrandoHuella = !mostrandoHuella
                handler.postDelayed(this, 1200)
            }
        })
    }

    private fun irAPantallaExito(login: String) {
        val intent = Intent(this, SuccessActivity::class.java)
        intent.putExtra("login", login)
        startActivity(intent)
        finish()
    }
}