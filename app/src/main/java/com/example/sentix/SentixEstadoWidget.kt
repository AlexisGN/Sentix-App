package com.example.sentix

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class SentixEstadoWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            SentixWidgetUpdater.actualizarWidget(
                context = context,
                appWidgetManager = appWidgetManager,
                appWidgetId = appWidgetId
            )
        }
    }

    override fun onEnabled(context: Context) {
        SentixWidgetUpdater.actualizarTodosLosWidgets(context)
    }
}