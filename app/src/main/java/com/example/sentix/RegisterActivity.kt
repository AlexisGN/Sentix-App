package com.example.sentix

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private val soloLetras = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$")
    private val usuarioValido = Regex("^[a-zA-Z0-9_]+$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val locale = Locale("es", "ES")
        Locale.setDefault(locale)

        setContentView(R.layout.activity_register)

        dbHelper = DBHelper(this)

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
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val confirmar = edtConfirmar.text.toString().trim()

            edtNombre.setText(nombre)
            edtApellidoPaterno.setText(paterno)
            edtApellidoMaterno.setText(materno)
            edtUsuario.setText(usuario)

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

            if (dbHelper.existeNombreUsuario(usuario)) {
                Toast.makeText(this, "El nombre de usuario ya existe", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.existeEmail(email)) {
                Toast.makeText(this, "El correo ya está registrado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val registrado = dbHelper.registrarUsuario(
                    usuario,
                    nombre,
                    paterno,
                    materno,
                    fecha,
                    email,
                    password
                )

                if (registrado) {
                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Error al registrar usuario", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this, "Error específico: ${e.message}", Toast.LENGTH_LONG).show()
            }

        }

    }

    private fun abrirCalendario(edtFecha: EditText) {
        val calendario = Calendar.getInstance()

        val dialogo = DatePickerDialog(
            this,
            R.style.DatePickerStyle,
            { _, year, month, day ->
                val fecha = String.format("%02d/%02d/%04d", day, month + 1, year)
                edtFecha.setText(fecha)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )

        // 🔥 ESTO VA DESPUÉS de crear el diálogo
        dialogo.datePicker.maxDate = System.currentTimeMillis()

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
}