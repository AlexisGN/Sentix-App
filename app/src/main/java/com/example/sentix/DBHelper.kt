package com.example.sentix

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, "usuarios.db", null, 5) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            create table usuarios(
                id integer primary key autoincrement,
                usuario text unique not null,
                nombre text not null,
                apellido_paterno text not null,
                apellido_materno text not null,
                fecha_nacimiento text not null,
                email text unique not null,
                password text not null
            )
            """
        )
        db.execSQL(
            """
    create table historial_emocional(
        id integer primary key autoincrement,
        id_usuario integer not null,
        fecha text not null,
        emocion text not null,
        riesgo text not null,
        confianza real not null,
        recomendacion text not null,
        foreign key(id_usuario) references usuarios(id)
    )
    """
        )

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 5) {
            db.execSQL(
                """
            create table if not exists historial_emocional(
                id integer primary key autoincrement,
                id_usuario integer not null,
                fecha text not null,
                emocion text not null,
                riesgo text not null,
                confianza real not null,
                recomendacion text not null,
                foreign key(id_usuario) references usuarios(id)
            )
            """
            )
        }
    }

    fun registrarUsuario(
        usuario: String,
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        fechaNacimiento: String,
        email: String,
        password: String
    ): Boolean {
        val db = writableDatabase

        val valores = ContentValues().apply {
            put("usuario", usuario)
            put("nombre", nombre)
            put("apellido_paterno", apellidoPaterno)
            put("apellido_materno", apellidoMaterno)
            put("fecha_nacimiento", fechaNacimiento)
            put("email", email)
            put("password", password)
        }

        val resultado = db.insert("usuarios", null, valores)
        db.close()

        return resultado != -1L
    }

    fun existeEmail(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "select id from usuarios where email = ?",
            arrayOf(email)
        )

        val existe = cursor.count > 0
        cursor.close()
        db.close()
        return existe
    }

    fun existeNombreUsuario(usuario: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "select id from usuarios where usuario = ?",
            arrayOf(usuario)
        )

        val existe = cursor.count > 0
        cursor.close()
        db.close()
        return existe
    }

    fun existeUsuarioOCorreo(valor: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "select id from usuarios where usuario = ? or email = ?",
            arrayOf(valor, valor)
        )

        val existe = cursor.count > 0
        cursor.close()
        db.close()
        return existe
    }

    fun validarPassword(valor: String, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "select id from usuarios where (usuario = ? or email = ?) and password = ?",
            arrayOf(valor, valor, password)
        )

        val valido = cursor.count > 0
        cursor.close()
        db.close()
        return valido
    }

    fun obtenerDatosUsuarioPorLogin(valor: String): UsuarioCompleto? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            select id, usuario, nombre, apellido_paterno, apellido_materno, fecha_nacimiento, email, password
            from usuarios
            where usuario = ? or email = ?
            """,
            arrayOf(valor, valor)
        )

        var usuario: UsuarioCompleto? = null

        if (cursor.moveToFirst()) {
            usuario = UsuarioCompleto(
                id = cursor.getInt(0),
                usuario = cursor.getString(1),
                nombre = cursor.getString(2),
                apellidoPaterno = cursor.getString(3),
                apellidoMaterno = cursor.getString(4),
                fechaNacimiento = cursor.getString(5),
                email = cursor.getString(6),
                password = cursor.getString(7)
            )
        }

        cursor.close()
        db.close()
        return usuario
    }

    fun listarUsuarios(): ArrayList<UsuarioCompleto> {
        val lista = ArrayList<UsuarioCompleto>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            """
            select id, usuario, nombre, apellido_paterno, apellido_materno, fecha_nacimiento, email, password
            from usuarios
            order by id asc
            """,
            null
        )

        while (cursor.moveToNext()) {
            lista.add(
                UsuarioCompleto(
                    id = cursor.getInt(0),
                    usuario = cursor.getString(1),
                    nombre = cursor.getString(2),
                    apellidoPaterno = cursor.getString(3),
                    apellidoMaterno = cursor.getString(4),
                    fechaNacimiento = cursor.getString(5),
                    email = cursor.getString(6),
                    password = cursor.getString(7)
                )
            )
        }

        cursor.close()
        db.close()
        return lista
    }

    fun modificarUsuario(
        id: Int,
        usuario: String,
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        fechaNacimiento: String,
        email: String,
        password: String
    ): Boolean {
        val db = writableDatabase

        val valores = ContentValues().apply {
            put("usuario", usuario)
            put("nombre", nombre)
            put("apellido_paterno", apellidoPaterno)
            put("apellido_materno", apellidoMaterno)
            put("fecha_nacimiento", fechaNacimiento)
            put("email", email)
            put("password", password)
        }

        val resultado = db.update("usuarios", valores, "id = ?", arrayOf(id.toString()))
        db.close()
        return resultado > 0
    }

    fun eliminarUsuario(id: Int): Boolean {
        val db = writableDatabase
        val resultado = db.delete("usuarios", "id = ?", arrayOf(id.toString()))
        db.close()
        return resultado > 0
    }

    fun registrarHistorialEmocional(
        idUsuario: Int,
        fecha: String,
        emocion: String,
        riesgo: String,
        confianza: Double,
        recomendacion: String
    ): Boolean {
        val db = writableDatabase

        val valores = ContentValues().apply {
            put("id_usuario", idUsuario)
            put("fecha", fecha)
            put("emocion", emocion)
            put("riesgo", riesgo)
            put("confianza", confianza)
            put("recomendacion", recomendacion)
        }

        val resultado = db.insert("historial_emocional", null, valores)
        db.close()
        return resultado != -1L
    }

    fun listarHistorialPorUsuario(idUsuario: Int): ArrayList<HistorialEmocional> {
        val lista = ArrayList<HistorialEmocional>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            """
        select id, id_usuario, fecha, emocion, riesgo, confianza, recomendacion
        from historial_emocional
        where id_usuario = ?
        order by id desc
        """,
            arrayOf(idUsuario.toString())
        )

        while (cursor.moveToNext()) {
            lista.add(
                HistorialEmocional(
                    id = cursor.getInt(0),
                    idUsuario = cursor.getInt(1),
                    fecha = cursor.getString(2),
                    emocion = cursor.getString(3),
                    riesgo = cursor.getString(4),
                    confianza = cursor.getDouble(5),
                    recomendacion = cursor.getString(6)
                )
            )
        }

        cursor.close()
        db.close()
        return lista
    }
}

data class UsuarioCompleto(
    val id: Int,
    val usuario: String,
    val nombre: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val fechaNacimiento: String,
    val email: String,
    val password: String
)
data class HistorialEmocional(
    val id: Int,
    val idUsuario: Int,
    val fecha: String,
    val emocion: String,
    val riesgo: String,
    val confianza: Double,
    val recomendacion: String
)