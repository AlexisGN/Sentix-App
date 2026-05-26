package com.example.sentix.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object FirebaseAuthHelper {

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    fun obtenerUsuarioActual(): FirebaseUser? {
        return auth.currentUser
    }

    fun obtenerUidActual(): String {
        return auth.currentUser?.uid ?: ""
    }

    fun obtenerEmailActual(): String {
        return auth.currentUser?.email ?: ""
    }

    fun haySesionActiva(): Boolean {
        return auth.currentUser != null
    }

    fun registrarUsuario(
        email: String,
        password: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { resultado ->
                val usuario = resultado.user

                if (usuario != null) {
                    onSuccess(usuario)
                } else {
                    onError("No se pudo obtener el usuario registrado.")
                }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo registrar el usuario.")
            }
    }

    fun iniciarSesion(
        email: String,
        password: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { resultado ->
                val usuario = resultado.user

                if (usuario != null) {
                    onSuccess(usuario)
                } else {
                    onError("No se pudo obtener el usuario autenticado.")
                }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo iniciar sesión.")
            }
    }

    fun enviarCorreoVerificacion(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val usuario = auth.currentUser

        if (usuario == null) {
            onError("No hay usuario autenticado.")
            return
        }

        usuario.sendEmailVerification()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo enviar el correo de verificación.")
            }
    }

    fun recargarUsuarioActual(
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val usuario = auth.currentUser

        if (usuario == null) {
            onError("No hay usuario autenticado.")
            return
        }

        usuario.reload()
            .addOnSuccessListener {
                val usuarioActualizado = auth.currentUser

                if (usuarioActualizado != null) {
                    onSuccess(usuarioActualizado)
                } else {
                    onError("No se pudo actualizar la sesión del usuario.")
                }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo recargar el usuario.")
            }
    }

    fun enviarRecuperacionPassword(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.trim().isBlank()) {
            onError("Ingrese un correo válido.")
            return
        }

        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo enviar el enlace de recuperación.")
            }
    }

    fun reautenticarConPassword(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val usuario = auth.currentUser

        if (usuario == null) {
            onError("La sesión ha expirado. Vuelva a iniciar sesión.")
            return
        }

        if (email.isBlank() || password.isBlank()) {
            onError("Ingrese su correo y contraseña.")
            return
        }

        val credencial = EmailAuthProvider.getCredential(email.trim(), password)

        usuario.reauthenticate(credencial)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo confirmar la contraseña.")
            }
    }

    fun eliminarCuentaAuth(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val usuario = auth.currentUser

        if (usuario == null) {
            onError("No hay usuario autenticado.")
            return
        }

        usuario.delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo eliminar la cuenta de autenticación.")
            }
    }

    fun cerrarSesion(context: Context? = null) {
        auth.signOut()

        if (context != null) {
            GoogleSignIn.getClient(
                context,
                GoogleSignInOptions.DEFAULT_SIGN_IN
            ).signOut()
        }
    }
}