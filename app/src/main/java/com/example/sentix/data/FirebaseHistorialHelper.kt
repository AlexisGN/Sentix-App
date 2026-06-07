package com.example.sentix.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FirebaseHistorialHelper {

    private const val TAG = "FIREBASE_HISTORIAL"

    fun guardarEvaluacionEmocional(
        uid: String,
        email: String,
        nivelFinal: String,
        puntajeFinalInterno: Float,

        emocionFacial: String,
        emocionFacialTraducida: String,
        confianzaFacial: Float,

        puntajeTest: Int,
        puntajeMaximoTest: Int,
        nivelTest: String,
        nivelTestVisible: String,
        respuestasTest: String,

        textoUsuario: String,
        textoNormalizado: String,
        etiquetaNlp: String,
        etiquetaNlpTraducida: String,
        confianzaNlp: Float,

        puntajeFacial: Float,
        puntajeTestNormalizado: Float,
        puntajeNlp: Float,

        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (uid.isBlank()) {
            onError(IllegalArgumentException("UID vacío. No se puede guardar la evaluación."))
            return
        }

        val db = FirebaseFirestore.getInstance()
        val fechaActual = Date()

        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formatoFechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val datos = hashMapOf<String, Any>(
            "uid" to uid,
            "email" to email,

            "fecha" to formatoFecha.format(fechaActual),
            "hora" to formatoHora.format(fechaActual),
            "fechaHora" to formatoFechaHora.format(fechaActual),
            "timestamp" to fechaActual.time,
            "creadoEn" to Timestamp.now(),

            "nivelFinal" to nivelFinal,
            "puntajeFinalInterno" to puntajeFinalInterno,

            "emocionFacial" to emocionFacial,
            "emocionFacialTraducida" to emocionFacialTraducida,
            "confianzaFacial" to confianzaFacial,

            "puntajeTest" to puntajeTest,
            "puntajeMaximoTest" to puntajeMaximoTest,
            "nivelTest" to nivelTest,
            "nivelTestVisible" to nivelTestVisible,
            "respuestasTest" to respuestasTest,

            "textoUsuario" to textoUsuario,
            "textoNormalizado" to textoNormalizado,
            "etiquetaNlp" to etiquetaNlp,
            "etiquetaNlpTraducida" to etiquetaNlpTraducida,
            "confianzaNlp" to confianzaNlp,

            "puntajeFacial" to puntajeFacial,
            "puntajeTestNormalizado" to puntajeTestNormalizado,
            "puntajeNlp" to puntajeNlp,

            "versionEvaluacion" to "v1_cnn10_test45_nlp45",
            "origen" to "app_android"
        )

        db.collection("usuarios")
            .document(uid)
            .collection("historial_emocional")
            .add(datos)
            .addOnSuccessListener { documento ->
                Log.d(TAG, "Evaluación guardada correctamente: ${documento.id}")
                onSuccess(documento.id)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar evaluación emocional", e)
                onError(e)
            }
    }
}