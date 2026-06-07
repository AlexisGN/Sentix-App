package com.example.sentix

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object HistorialCacheManager {

    private const val PREF_NAME = "sentix_historial_cache"
    private const val KEY_UID = "uid"
    private const val KEY_HISTORIAL = "historial_json"
    private const val KEY_ULTIMA_CARGA = "ultima_carga"

    fun guardar(
        context: Context,
        uid: String,
        evaluaciones: List<HistorialEmocionalActivity.EvaluacionHistorial>
    ) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val array = JSONArray()

        evaluaciones.forEach { evaluacion ->
            val obj = JSONObject()
            obj.put("id", evaluacion.id)
            obj.put("fecha", evaluacion.fecha)
            obj.put("hora", evaluacion.hora)
            obj.put("fechaHora", evaluacion.fechaHora)
            obj.put("timestamp", evaluacion.timestamp)
            obj.put("nivelFinal", evaluacion.nivelFinal)
            obj.put("puntajeFinalInterno", evaluacion.puntajeFinalInterno)
            obj.put("nivelTestVisible", evaluacion.nivelTestVisible)
            obj.put("etiquetaNlp", evaluacion.etiquetaNlp)
            obj.put("etiquetaNlpTraducida", evaluacion.etiquetaNlpTraducida)
            obj.put("textoUsuario", evaluacion.textoUsuario)
            obj.put("emocionFacialTraducida", evaluacion.emocionFacialTraducida)
            array.put(obj)
        }

        prefs.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_HISTORIAL, array.toString())
            .putLong(KEY_ULTIMA_CARGA, System.currentTimeMillis())
            .apply()
    }

    fun obtener(
        context: Context,
        uid: String
    ): List<HistorialEmocionalActivity.EvaluacionHistorial> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val uidCache = prefs.getString(KEY_UID, "") ?: ""
        if (uidCache != uid) return emptyList()

        val json = prefs.getString(KEY_HISTORIAL, "") ?: ""
        if (json.isBlank()) return emptyList()

        return try {
            val array = JSONArray(json)
            val lista = mutableListOf<HistorialEmocionalActivity.EvaluacionHistorial>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                lista.add(
                    HistorialEmocionalActivity.EvaluacionHistorial(
                        id = obj.optString("id"),
                        fecha = obj.optString("fecha"),
                        hora = obj.optString("hora"),
                        fechaHora = obj.optString("fechaHora"),
                        timestamp = obj.optLong("timestamp"),
                        nivelFinal = obj.optString("nivelFinal"),
                        puntajeFinalInterno = obj.optDouble("puntajeFinalInterno", 0.0).toFloat(),
                        nivelTestVisible = obj.optString("nivelTestVisible"),
                        etiquetaNlp = obj.optString("etiquetaNlp"),
                        etiquetaNlpTraducida = obj.optString("etiquetaNlpTraducida"),
                        textoUsuario = obj.optString("textoUsuario"),
                        emocionFacialTraducida = obj.optString("emocionFacialTraducida")
                    )
                )
            }

            lista
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun limpiar(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}