package com.example.sentix

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews

object SentixWidgetUpdater {

    private const val PREFS_WIDGET = "sentix_widget_estado"
    private const val KEY_NIVEL = "ultimoNivel"
    private const val KEY_FECHA = "ultimaFecha"
    private const val KEY_MENSAJE = "ultimoMensaje"

    fun guardarUltimoEstado(
        context: Context,
        nivel: String,
        fechaHora: String,
        mensaje: String
    ) {
        context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NIVEL, nivel)
            .putString(KEY_FECHA, fechaHora)
            .putString(KEY_MENSAJE, mensaje)
            .apply()

        actualizarTodosLosWidgets(context)
    }

    fun actualizarTodosLosWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, SentixEstadoWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

        for (widgetId in widgetIds) {
            actualizarWidget(
                context = context,
                appWidgetManager = appWidgetManager,
                appWidgetId = widgetId
            )
        }
    }

    fun actualizarWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)

        val nivel = prefs.getString(KEY_NIVEL, "Realiza una evaluación")
            ?: "Realiza una evaluación"

        val fecha = prefs.getString(KEY_FECHA, "Sin evaluación reciente")
            ?: "Sin evaluación reciente"

        val mensaje = prefs.getString(KEY_MENSAJE, "Toca para abrir Sentix.")
            ?: "Toca para abrir Sentix."

        val views = RemoteViews(
            context.packageName,
            R.layout.sentix_estado_widget
        )

        views.setTextViewText(R.id.txtWidgetResultado, nivel)
        views.setTextViewText(R.id.txtWidgetFecha, fecha)
        views.setTextViewText(R.id.txtWidgetMensaje, mensaje)

        val colorEstado = obtenerColorEstado(nivel)

        views.setTextColor(R.id.txtWidgetResultado, colorEstado)
        views.setInt(
            R.id.viewWidgetIndicador,
            "setBackgroundColor",
            colorEstado
        )

        val intentAbrirApp = Intent(context, SuccessActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            3001,
            intentAbrirApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(
            R.id.rootWidgetEstadoSentix,
            pendingIntent
        )

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun obtenerMensajePreventivo(nivel: String): String {
        return when (nivel) {
            "Estado favorable" -> "Mantén tus hábitos positivos."
            "Señales leves" -> "Observa cómo te sientes hoy."
            "Seguimiento recomendado" -> "Revisa tus recomendaciones."
            "Atención recomendada" -> "Busca apoyo si lo necesitas."
            else -> "Toca para abrir Sentix."
        }
    }

    private fun obtenerColorEstado(nivel: String): Int {
        return when (nivel) {
            "Estado favorable" -> Color.parseColor("#10B981")
            "Señales leves" -> Color.parseColor("#2563EB")
            "Seguimiento recomendado" -> Color.parseColor("#F97316")
            "Atención recomendada" -> Color.parseColor("#F43F5E")
            else -> Color.parseColor("#2563EB")
        }
    }
}