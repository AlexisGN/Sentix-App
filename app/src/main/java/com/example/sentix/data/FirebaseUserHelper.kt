package com.example.sentix.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseUserHelper {

    private const val COLECCION_USUARIOS = "usuarios"

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    fun crearDocumentoUsuario(
        uid: String,
        email: String,
        usuario: String,
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        fechaNacimiento: String,
        edad: Int = 0,
        biometricEnabled: Boolean = false,
        googleSecondFactorEnabled: Boolean = true,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("UID vacío. No se puede crear el usuario.")
            return
        }

        val datosUsuario = hashMapOf(
            "uid" to uid,
            "email" to email.trim(),
            "usuario" to usuario.trim(),
            "nombre" to nombre.trim(),
            "apellidoPaterno" to apellidoPaterno.trim(),
            "apellidoMaterno" to apellidoMaterno.trim(),
            "fechaNacimiento" to fechaNacimiento.trim(),
            "edad" to edad,
            "fotoPerfilUrl" to "",
            "biometricEnabled" to biometricEnabled,
            "googleSecondFactorEnabled" to googleSecondFactorEnabled,
            "notificacionesEnabled" to true,
            "creadoEn" to FieldValue.serverTimestamp(),
            "actualizadoEn" to FieldValue.serverTimestamp()
        )

        firestore.collection(COLECCION_USUARIOS)
            .document(uid)
            .set(datosUsuario)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo guardar el usuario en Firestore.")
            }
    }

    fun obtenerDocumentoUsuario(
        uid: String,
        onSuccess: (DocumentSnapshot) -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("UID vacío. No se puede obtener el usuario.")
            return
        }

        firestore.collection(COLECCION_USUARIOS)
            .document(uid)
            .get()
            .addOnSuccessListener { documento ->
                onSuccess(documento)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo obtener el usuario.")
            }
    }

    fun actualizarDatosPerfil(
        uid: String,
        usuario: String,
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        fechaNacimiento: String,
        edad: Int = 0,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("UID vacío. No se puede actualizar el perfil.")
            return
        }

        val datosActualizados = hashMapOf<String, Any>(
            "usuario" to usuario.trim(),
            "nombre" to nombre.trim(),
            "apellidoPaterno" to apellidoPaterno.trim(),
            "apellidoMaterno" to apellidoMaterno.trim(),
            "fechaNacimiento" to fechaNacimiento.trim(),
            "edad" to edad,
            "actualizadoEn" to FieldValue.serverTimestamp()
        )

        firestore.collection(COLECCION_USUARIOS)
            .document(uid)
            .update(datosActualizados)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo actualizar el perfil.")
            }
    }

    fun actualizarFotoPerfilUrl(
        uid: String,
        fotoPerfilUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("UID vacío. No se puede actualizar la foto.")
            return
        }

        firestore.collection(COLECCION_USUARIOS)
            .document(uid)
            .update(
                mapOf(
                    "fotoPerfilUrl" to fotoPerfilUrl,
                    "actualizadoEn" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo actualizar la foto de perfil.")
            }
    }

    fun actualizarNotificaciones(
        uid: String,
        notificacionesEnabled: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("UID vacío. No se puede actualizar notificaciones.")
            return
        }

        firestore.collection(COLECCION_USUARIOS)
            .document(uid)
            .update(
                mapOf(
                    "notificacionesEnabled" to notificacionesEnabled,
                    "actualizadoEn" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo actualizar la configuración de notificaciones.")
            }
    }

    fun actualizarBiometria(
        uid: String,
        biometricEnabled: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("UID vacío. No se puede actualizar biometría.")
            return
        }

        firestore.collection(COLECCION_USUARIOS)
            .document(uid)
            .update(
                mapOf(
                    "biometricEnabled" to biometricEnabled,
                    "actualizadoEn" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo actualizar la biometría.")
            }
    }

    fun actualizarSegundoFactorGoogle(
        uid: String,
        googleSecondFactorEnabled: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("UID vacío. No se puede actualizar la verificación Google.")
            return
        }

        firestore.collection(COLECCION_USUARIOS)
            .document(uid)
            .update(
                mapOf(
                    "googleSecondFactorEnabled" to googleSecondFactorEnabled,
                    "actualizadoEn" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo actualizar la verificación Google.")
            }
    }

    fun eliminarDocumentoUsuario(
        uid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("UID vacío. No se puede eliminar el usuario.")
            return
        }

        firestore.collection(COLECCION_USUARIOS)
            .document(uid)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo eliminar el documento del usuario.")
            }
    }

    fun existeUsuarioPorNombreUsuario(
        usuario: String,
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        if (usuario.trim().isBlank()) {
            onSuccess(false)
            return
        }

        firestore.collection(COLECCION_USUARIOS)
            .whereEqualTo("usuario", usuario.trim())
            .limit(1)
            .get()
            .addOnSuccessListener { resultado ->
                onSuccess(!resultado.isEmpty)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo validar el nombre de usuario.")
            }
    }

    fun buscarUsuarioPorEmailOUsuario(
        login: String,
        onSuccess: (DocumentSnapshot?) -> Unit,
        onError: (String) -> Unit
    ) {
        val loginLimpio = login.trim()

        if (loginLimpio.isBlank()) {
            onSuccess(null)
            return
        }

        val campo = if (loginLimpio.contains("@")) "email" else "usuario"

        firestore.collection(COLECCION_USUARIOS)
            .whereEqualTo(campo, loginLimpio)
            .limit(1)
            .get()
            .addOnSuccessListener { resultado ->
                val documento = resultado.documents.firstOrNull()
                onSuccess(documento)
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo buscar el usuario.")
            }
    }
}