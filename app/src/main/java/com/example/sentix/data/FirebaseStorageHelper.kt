package com.example.sentix.data

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage

object FirebaseStorageHelper {

    private val storage: FirebaseStorage
        get() = FirebaseStorage.getInstance()

    fun subirFotoPerfil(
        uid: String,
        imagenUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("UID vacío. No se puede subir la foto.")
            return
        }

        val referencia = storage.reference
            .child("profile_images")
            .child(uid)
            .child("foto_perfil.jpg")

        referencia.putFile(imagenUri)
            .addOnSuccessListener {
                referencia.downloadUrl
                    .addOnSuccessListener { uriDescarga ->
                        onSuccess(uriDescarga.toString())
                    }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "No se pudo obtener la URL de la foto.")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "No se pudo subir la foto de perfil.")
            }
    }

    fun eliminarFotoPerfilPorUid(
        uid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uid.isBlank()) {
            onError("UID vacío. No se puede eliminar la foto.")
            return
        }

        val referencia = storage.reference
            .child("profile_images")
            .child(uid)
            .child("foto_perfil.jpg")

        referencia.delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                /*
                 * Si la imagen no existe, Firebase puede devolver error.
                 * En eliminación de cuenta, eso no debería bloquear todo el proceso.
                 */
                val mensaje = e.message ?: "No se pudo eliminar la foto de perfil."
                onError(mensaje)
            }
    }

    fun eliminarFotoPerfilPorUrl(
        fotoPerfilUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (fotoPerfilUrl.isBlank()) {
            onSuccess()
            return
        }

        try {
            val referencia = storage.getReferenceFromUrl(fotoPerfilUrl)

            referencia.delete()
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onError(e.message ?: "No se pudo eliminar la foto de perfil.")
                }

        } catch (e: Exception) {
            onError(e.message ?: "URL de imagen inválida.")
        }
    }

    fun obtenerReferenciaFotoPerfil(uid: String): String {
        return "profile_images/$uid/foto_perfil.jpg"
    }
}