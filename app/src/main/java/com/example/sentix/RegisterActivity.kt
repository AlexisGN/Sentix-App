package com.example.sentix

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sentix.data.FirebaseAuthHelper
import com.example.sentix.data.FirebaseUserHelper
import java.util.Calendar
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private val soloLetras = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$")
    private val usuarioValido = Regex("^[a-zA-Z0-9_]+$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val locale = Locale("es", "ES")
        Locale.setDefault(locale)

        setContentView(R.layout.activity_register)

        val rootRegister = findViewById<View>(R.id.rootRegistro)

        SystemBarsHelper.aplicarInsets(
            activity = this,
            rootView = rootRegister,
            aplicarArriba = true,
            aplicarAbajo = true
        )

        val edtUsuario = findViewById<EditText>(R.id.edtUsuarioRegistro)
        val edtNombre = findViewById<EditText>(R.id.edtNombre)
        val edtApellidoPaterno = findViewById<EditText>(R.id.edtApellidoPaterno)
        val edtApellidoMaterno = findViewById<EditText>(R.id.edtApellidoMaterno)
        val edtFecha = findViewById<EditText>(R.id.edtFechaNacimiento)
        val edtEmail = findViewById<EditText>(R.id.edtEmailRegistro)
        val edtPassword = findViewById<EditText>(R.id.edtPasswordRegistro)
        val edtConfirmar = findViewById<EditText>(R.id.edtConfirmarPassword)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        edtFecha.setOnClickListener {
            abrirCalendario(edtFecha)
        }

        btnRegistrar.setOnClickListener {
            val usuario = edtUsuario.text.toString().trim().lowercase()
            val nombre = capitalizarTexto(edtNombre.text.toString().trim())
            val paterno = capitalizarTexto(edtApellidoPaterno.text.toString().trim())
            val materno = capitalizarTexto(edtApellidoMaterno.text.toString().trim())
            val fecha = edtFecha.text.toString().trim()
            val email = edtEmail.text.toString().trim().lowercase()
            val password = edtPassword.text.toString().trim()
            val confirmar = edtConfirmar.text.toString().trim()

            edtUsuario.setText(usuario)
            edtNombre.setText(nombre)
            edtApellidoPaterno.setText(paterno)
            edtApellidoMaterno.setText(materno)

            if (usuario.isEmpty()) {
                edtUsuario.error = "Ingrese un usuario"
                return@setOnClickListener
            }

            if (usuario.length < 4) {
                edtUsuario.error = "El usuario debe tener mínimo 4 caracteres"
                return@setOnClickListener
            }

            if (!usuarioValido.matches(usuario)) {
                edtUsuario.error = "Solo letras, números y guion bajo"
                return@setOnClickListener
            }

            if (!validarTexto(nombre, edtNombre, "nombre")) return@setOnClickListener
            if (!validarTexto(paterno, edtApellidoPaterno, "apellido paterno")) return@setOnClickListener
            if (!validarTexto(materno, edtApellidoMaterno, "apellido materno")) return@setOnClickListener

            if (fecha.isEmpty()) {
                edtFecha.error = "Seleccione su fecha de nacimiento"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                edtEmail.error = "Ingrese su correo electrónico"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.error = "Ingrese un correo válido"
                return@setOnClickListener
            }

            if (!validarPassword(password, edtPassword)) return@setOnClickListener

            if (password != confirmar) {
                edtConfirmar.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            btnRegistrar.isEnabled = false
            btnRegistrar.text = "Registrando..."

            verificarUsuarioYRegistrar(
                usuario = usuario,
                nombre = nombre,
                paterno = paterno,
                materno = materno,
                fecha = fecha,
                email = email,
                password = password,
                btnRegistrar = btnRegistrar
            )
        }
    }

    private fun verificarUsuarioYRegistrar(
        usuario: String,
        nombre: String,
        paterno: String,
        materno: String,
        fecha: String,
        email: String,
        password: String,
        btnRegistrar: Button
    ) {
        FirebaseUserHelper.existeUsuarioPorNombreUsuario(
            usuario = usuario,
            onSuccess = { existe ->
                if (existe) {
                    restaurarBotonRegistrar(btnRegistrar)
                    Toast.makeText(
                        this,
                        "El nombre de usuario ya existe",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    registrarEnFirebase(
                        usuario = usuario,
                        nombre = nombre,
                        paterno = paterno,
                        materno = materno,
                        fecha = fecha,
                        email = email,
                        password = password,
                        btnRegistrar = btnRegistrar
                    )
                }
            },
            onError = { mensaje ->
                restaurarBotonRegistrar(btnRegistrar)
                Toast.makeText(
                    this,
                    obtenerMensajeFirestoreDesdeTexto(mensaje, "Error al validar usuario"),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun registrarEnFirebase(
        usuario: String,
        nombre: String,
        paterno: String,
        materno: String,
        fecha: String,
        email: String,
        password: String,
        btnRegistrar: Button
    ) {
        FirebaseAuthHelper.registrarUsuario(
            email = email,
            password = password,
            onSuccess = { firebaseUser ->
                val uid = firebaseUser.uid
                val edad = calcularEdad(fecha)

                FirebaseUserHelper.crearDocumentoUsuario(
                    uid = uid,
                    email = email,
                    usuario = usuario,
                    nombre = nombre,
                    apellidoPaterno = paterno,
                    apellidoMaterno = materno,
                    fechaNacimiento = fecha,
                    edad = edad,
                    biometricEnabled = false,
                    googleSecondFactorEnabled = true,
                    onSuccess = {
                        Toast.makeText(
                            this,
                            "Registro exitoso",
                            Toast.LENGTH_SHORT
                        ).show()

                        FirebaseAuthHelper.cerrarSesion(this)

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    },
                    onError = { mensaje ->
                        restaurarBotonRegistrar(btnRegistrar)

                        FirebaseAuthHelper.eliminarCuentaAuth(
                            onSuccess = {
                                FirebaseAuthHelper.cerrarSesion(this)
                            },
                            onError = {
                                FirebaseAuthHelper.cerrarSesion(this)
                            }
                        )

                        Toast.makeText(
                            this,
                            obtenerMensajeFirestoreDesdeTexto(mensaje, "Error al guardar perfil"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            },
            onError = { mensaje ->
                restaurarBotonRegistrar(btnRegistrar)

                Toast.makeText(
                    this,
                    obtenerMensajeRegistroDesdeTexto(mensaje),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun abrirCalendario(edtFecha: EditText) {
        val calendario = Calendar.getInstance()

        val dialogo = DatePickerDialog(
            this,
            R.style.DatePickerStyle,
            { _, year, month, day ->
                val fecha = String.format(
                    Locale.getDefault(),
                    "%02d/%02d/%04d",
                    day,
                    month + 1,
                    year
                )
                edtFecha.setText(fecha)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )

        dialogo.datePicker.maxDate = System.currentTimeMillis()

        dialogo.setOnShowListener {
            dialogo.getButton(DialogInterface.BUTTON_POSITIVE)?.apply {
                text = "Aceptar"
                setTextColor(Color.parseColor("#2563EB"))
                textSize = 14f
                isAllCaps = false
            }

            dialogo.getButton(DialogInterface.BUTTON_NEGATIVE)?.apply {
                text = "Cancelar"
                setTextColor(Color.parseColor("#7C3AED"))
                textSize = 14f
                isAllCaps = false
            }
        }

        dialogo.show()
    }

    private fun validarTexto(valor: String, campo: EditText, nombreCampo: String): Boolean {
        if (valor.isEmpty()) {
            campo.error = "Ingrese su $nombreCampo"
            return false
        }

        if (!soloLetras.matches(valor)) {
            campo.error = "Solo se permiten letras"
            return false
        }

        return true
    }

    private fun validarPassword(password: String, campo: EditText): Boolean {
        val tieneMayuscula = password.any { it.isUpperCase() }
        val tieneEspecial = password.any { !it.isLetterOrDigit() }

        if (password.length < 6) {
            campo.error = "Mínimo 6 caracteres"
            return false
        }

        if (!tieneMayuscula) {
            campo.error = "Debe tener al menos una mayúscula"
            return false
        }

        if (!tieneEspecial) {
            campo.error = "Debe tener al menos un carácter especial"
            return false
        }

        return true
    }

    private fun capitalizarTexto(texto: String): String {
        return texto.lowercase().split(" ").joinToString(" ") { palabra ->
            palabra.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }
    }

    private fun calcularEdad(fechaNacimiento: String): Int {
        return try {
            val partes = fechaNacimiento.split("/")
            val dia = partes[0].toInt()
            val mes = partes[1].toInt() - 1
            val anio = partes[2].toInt()

            val nacimiento = Calendar.getInstance()
            nacimiento.set(anio, mes, dia)

            val hoy = Calendar.getInstance()
            var edad = hoy.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR)

            if (hoy.get(Calendar.DAY_OF_YEAR) < nacimiento.get(Calendar.DAY_OF_YEAR)) {
                edad--
            }

            edad
        } catch (e: Exception) {
            0
        }
    }

    private fun restaurarBotonRegistrar(btnRegistrar: Button) {
        btnRegistrar.isEnabled = true
        btnRegistrar.text = "REGISTRARME"
    }

    private fun obtenerMensajeRegistroDesdeTexto(mensajeOriginal: String): String {
        val mensaje = mensajeOriginal.lowercase()

        return when {
            mensaje.contains("email address is already in use") ||
                    mensaje.contains("already in use") ||
                    mensaje.contains("correo") && mensaje.contains("registrado") -> {
                "El correo ya está registrado"
            }

            mensaje.contains("badly formatted") ||
                    mensaje.contains("invalid email") ||
                    mensaje.contains("formato") -> {
                "El correo no tiene formato válido"
            }

            mensaje.contains("password") ||
                    mensaje.contains("contraseña") ||
                    mensaje.contains("weak") -> {
                "La contraseña no cumple los requisitos"
            }

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
                "Error al registrar. Inténtelo nuevamente"
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
}