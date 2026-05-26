package com.example.sentix

import android.content.Context
import com.google.firebase.firestore.DocumentSnapshot

data class UsuarioCache(
    val uid: String = "",
    val usuario: String = "",
    val nombre: String = "",
    val apellidoPaterno: String = "",
    val apellidoMaterno: String = "",
    val email: String = "",
    val fechaNacimiento: String = "",
    val fotoPerfilUrl: String = ""
) {
    val nombreCompleto: String
        get() = listOf(nombre, apellidoPaterno, apellidoMaterno)
            .filter { it.isNotBlank() }
            .joinToString(" ")
}

object UsuarioCacheManager {

    private const val PREF_NAME = "usuario_cache"

    fun guardarDesdeFirestore(context: Context, uid: String, documento: DocumentSnapshot) {
        val usuario = documento.getString("usuario") ?: ""
        val nombre = documento.getString("nombre") ?: ""
        val apellidoPaterno = documento.getString("apellidoPaterno") ?: ""
        val apellidoMaterno = documento.getString("apellidoMaterno") ?: ""
        val email = documento.getString("email") ?: ""
        val fechaNacimiento = documento.getString("fechaNacimiento") ?: ""
        val fotoPerfilUrl = documento.getString("fotoPerfilUrl") ?: ""

        guardar(
            context = context,
            usuarioCache = UsuarioCache(
                uid = uid,
                usuario = usuario,
                nombre = nombre,
                apellidoPaterno = apellidoPaterno,
                apellidoMaterno = apellidoMaterno,
                email = email,
                fechaNacimiento = fechaNacimiento,
                fotoPerfilUrl = fotoPerfilUrl
            )
        )
    }

    fun guardar(context: Context, usuarioCache: UsuarioCache) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("uid", usuarioCache.uid)
            .putString("usuario", usuarioCache.usuario)
            .putString("nombre", usuarioCache.nombre)
            .putString("apellidoPaterno", usuarioCache.apellidoPaterno)
            .putString("apellidoMaterno", usuarioCache.apellidoMaterno)
            .putString("email", usuarioCache.email)
            .putString("fechaNacimiento", usuarioCache.fechaNacimiento)
            .putString("fotoPerfilUrl", usuarioCache.fotoPerfilUrl)
            .putString("nombreCompleto", usuarioCache.nombreCompleto)
            .apply()
    }

    fun actualizarFoto(context: Context, fotoPerfilUrl: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("fotoPerfilUrl", fotoPerfilUrl)
            .commit()
    }

    fun obtener(context: Context): UsuarioCache {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        return UsuarioCache(
            uid = prefs.getString("uid", "") ?: "",
            usuario = prefs.getString("usuario", "") ?: "",
            nombre = prefs.getString("nombre", "") ?: "",
            apellidoPaterno = prefs.getString("apellidoPaterno", "") ?: "",
            apellidoMaterno = prefs.getString("apellidoMaterno", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            fechaNacimiento = prefs.getString("fechaNacimiento", "") ?: "",
            fotoPerfilUrl = prefs.getString("fotoPerfilUrl", "") ?: ""
        )
    }

    fun limpiar(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}