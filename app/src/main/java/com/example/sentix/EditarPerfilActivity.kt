package com.example.sentix

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.graphics.Color
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.example.sentix.data.FirebaseAuthHelper
import com.example.sentix.data.FirebaseUserHelper
import java.util.Calendar
import java.util.Locale
import android.text.InputType
class EditarPerfilActivity : BaseMenuActivity() {

    private lateinit var btnVolver: ImageButton
    private lateinit var edtUsuario: EditText
    private lateinit var edtNombre: EditText
    private lateinit var edtApellidoPaterno: EditText
    private lateinit var edtApellidoMaterno: EditText
    private lateinit var edtFechaNacimiento: EditText
    private lateinit var edtCorreo: EditText
    private lateinit var btnGuardar: Button

    override fun getContenidoLayoutId(): Int {
        return R.layout.activity_editar_perfil
    }

    override fun onContenidoCreado() {
        enlazarVistas()
        configurarCamposBloqueados()
        configurarEventos()
        cargarFormularioDesdeCache()
    }

    override fun onResume() {
        super.onResume()

        if (::edtUsuario.isInitialized) {
            cargarFormularioDesdeCache()
            cargarFormularioDesdeFirebase()
        }
    }

    override fun onUsuarioActualizado(cache: UsuarioCache) {
        if (::edtUsuario.isInitialized) {
            cargarFormularioDesdeCache()
        }
    }

    private fun enlazarVistas() {
        btnVolver = findViewById(R.id.btnVolverEditarPerfil)
        edtUsuario = findViewById(R.id.edtUsuarioEditar)
        edtNombre = findViewById(R.id.edtNombreEditar)
        edtApellidoPaterno = findViewById(R.id.edtApellidoPaternoEditar)
        edtApellidoMaterno = findViewById(R.id.edtApellidoMaternoEditar)
        edtFechaNacimiento = findViewById(R.id.edtFechaNacimientoEditar)
        edtCorreo = findViewById(R.id.edtCorreoEditar)
        btnGuardar = findViewById(R.id.btnGuardarEditarPerfil)
    }

    private fun configurarEventos() {
        btnVolver.setOnClickListener {
            finish()
        }

        edtFechaNacimiento.setOnClickListener {
            Toast.makeText(
                this,
                "La fecha de nacimiento no se puede modificar.",
                Toast.LENGTH_SHORT
            ).show()
        }

        edtCorreo.setOnClickListener {
            Toast.makeText(
                this,
                "El correo está vinculado al inicio de sesión y no se puede editar aquí.",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnGuardar.setOnClickListener {
            guardarCambios()
        }
    }
    private fun configurarCamposBloqueados() {
        bloquearCampoSoloLectura(
            campo = edtCorreo,
            textoAyuda = "Correo vinculado al inicio de sesión"
        )

        bloquearCampoSoloLectura(
            campo = edtFechaNacimiento,
            textoAyuda = "Fecha registrada al crear la cuenta"
        )
    }

    private fun bloquearCampoSoloLectura(
        campo: EditText,
        textoAyuda: String
    ) {
        campo.isFocusable = false
        campo.isFocusableInTouchMode = false
        campo.isCursorVisible = false
        campo.isLongClickable = false
        campo.inputType = InputType.TYPE_NULL
        campo.keyListener = null
        campo.alpha = 0.75f
        campo.hint = textoAyuda
    }
    private fun cargarFormularioDesdeCache() {
        val cache = UsuarioCacheManager.obtener(this)

        if (cache.uid.isNotBlank() && uidActual.isBlank()) {
            uidActual = cache.uid
        }

        if (cache.email.isNotBlank() && emailActual.isBlank()) {
            emailActual = cache.email
        }

        if (cache.usuario.isNotBlank()) {
            edtUsuario.setText(cache.usuario)
        }

        if (cache.nombre.isNotBlank()) {
            edtNombre.setText(cache.nombre)
        }

        if (cache.apellidoPaterno.isNotBlank()) {
            edtApellidoPaterno.setText(cache.apellidoPaterno)
        }

        if (cache.apellidoMaterno.isNotBlank()) {
            edtApellidoMaterno.setText(cache.apellidoMaterno)
        }

        if (cache.fechaNacimiento.isNotBlank()) {
            edtFechaNacimiento.setText(cache.fechaNacimiento)
        }

        if (cache.email.isNotBlank()) {
            edtCorreo.setText(cache.email)
        } else {
            edtCorreo.setText(FirebaseAuthHelper.obtenerEmailActual())
        }
    }

    private fun cargarFormularioDesdeFirebase() {
        val usuarioAuth = FirebaseAuthHelper.obtenerUsuarioActual()

        if (usuarioAuth == null) {
            Log.w("EDITAR_PERFIL", "FirebaseAuth.currentUser es null. Se usará caché.")
            return
        }

        if (uidActual.isBlank()) {
            uidActual = usuarioAuth.uid
        }

        if (usuarioAuth.uid != uidActual) {
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

                edtUsuario.setText(cacheActualizado.usuario)
                edtNombre.setText(cacheActualizado.nombre)
                edtApellidoPaterno.setText(cacheActualizado.apellidoPaterno)
                edtApellidoMaterno.setText(cacheActualizado.apellidoMaterno)
                edtFechaNacimiento.setText(cacheActualizado.fechaNacimiento)
                edtCorreo.setText(
                    cacheActualizado.email.ifBlank {
                        FirebaseAuthHelper.obtenerEmailActual()
                    }
                )

                emailActual = cacheActualizado.email

                refrescarMenuDesdeCache()

                Log.d("EDITAR_PERFIL", "Formulario cargado correctamente desde Firebase")
            },
            onError = { mensaje ->
                Log.e("EDITAR_PERFIL", "Error al cargar datos: $mensaje")

                Toast.makeText(
                    this,
                    "No se pudieron actualizar los datos. Se muestran datos guardados.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun guardarCambios() {
        val usuario = edtUsuario.text.toString().trim().lowercase()
        val nombre = capitalizarTexto(edtNombre.text.toString().trim())
        val paterno = capitalizarTexto(edtApellidoPaterno.text.toString().trim())
        val materno = capitalizarTexto(edtApellidoMaterno.text.toString().trim())
        val fecha = edtFechaNacimiento.text.toString().trim()
        val email = edtCorreo.text.toString().trim()

        val usuarioRegex = Regex("^[a-zA-Z0-9_]+$")

        if (usuario.length < 4) {
            edtUsuario.error = "Mínimo 4 caracteres"
            return
        }

        if (!usuarioRegex.matches(usuario)) {
            edtUsuario.error = "Solo letras, números y guion bajo"
            return
        }

        if (nombre.isEmpty()) {
            edtNombre.error = "Ingrese su nombre"
            return
        }

        if (paterno.isEmpty()) {
            edtApellidoPaterno.error = "Ingrese su apellido paterno"
            return
        }

        if (materno.isEmpty()) {
            edtApellidoMaterno.error = "Ingrese su apellido materno"
            return
        }

        if (fecha.isEmpty()) {
            edtFechaNacimiento.error = "Seleccione su fecha de nacimiento"
            return
        }

        if (uidActual.isBlank()) {
            uidActual = FirebaseAuthHelper.obtenerUidActual()
        }

        if (uidActual.isBlank()) {
            Toast.makeText(
                this,
                "No se pudo identificar el usuario activo.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        btnGuardar.isEnabled = false
        btnGuardar.text = "Guardando..."

        verificarUsuarioDisponibleYGuardar(
            usuario = usuario,
            nombre = nombre,
            paterno = paterno,
            materno = materno,
            fecha = fecha,
            email = email
        )
    }

    private fun verificarUsuarioDisponibleYGuardar(
        usuario: String,
        nombre: String,
        paterno: String,
        materno: String,
        fecha: String,
        email: String
    ) {
        FirebaseUserHelper.buscarUsuarioPorEmailOUsuario(
            login = usuario,
            onSuccess = { documento ->
                val existeOtro = documento != null && documento.exists() && documento.id != uidActual

                if (existeOtro) {
                    restaurarBotonGuardar()
                    edtUsuario.error = "Este usuario ya está en uso"
                    return@buscarUsuarioPorEmailOUsuario
                }

                actualizarPerfilEnFirebase(
                    usuario = usuario,
                    nombre = nombre,
                    paterno = paterno,
                    materno = materno,
                    fecha = fecha,
                    email = email
                )
            },
            onError = { mensaje ->
                restaurarBotonGuardar()

                Log.e("EDITAR_PERFIL", "Error al verificar usuario: $mensaje")

                Toast.makeText(
                    this,
                    "No se pudo verificar el usuario",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun actualizarPerfilEnFirebase(
        usuario: String,
        nombre: String,
        paterno: String,
        materno: String,
        fecha: String,
        email: String
    ) {
        FirebaseUserHelper.actualizarDatosPerfil(
            uid = uidActual,
            usuario = usuario,
            nombre = nombre,
            apellidoPaterno = paterno,
            apellidoMaterno = materno,
            fechaNacimiento = fecha,
            edad = calcularEdad(fecha),
            onSuccess = {
                val cacheActual = UsuarioCacheManager.obtener(this)

                UsuarioCacheManager.guardar(
                    context = this,
                    usuarioCache = UsuarioCache(
                        uid = uidActual,
                        usuario = usuario,
                        nombre = nombre,
                        apellidoPaterno = paterno,
                        apellidoMaterno = materno,
                        email = email.ifBlank { cacheActual.email },
                        fechaNacimiento = fecha,
                        fotoPerfilUrl = cacheActual.fotoPerfilUrl
                    )
                )

                refrescarMenuDesdeCache()

                Toast.makeText(
                    this,
                    "Perfil actualizado correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            },
            onError = { mensaje ->
                restaurarBotonGuardar()

                Log.e("EDITAR_PERFIL", "Error al actualizar: $mensaje")

                Toast.makeText(
                    this,
                    "No se pudieron guardar los cambios",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun capitalizarTexto(texto: String): String {
        return texto.lowercase().split(" ").joinToString(" ") { palabra ->
            palabra.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
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

    private fun restaurarBotonGuardar() {
        btnGuardar.isEnabled = true
        btnGuardar.text = "Guardar cambios"
    }
}