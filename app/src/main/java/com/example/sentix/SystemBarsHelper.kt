package com.example.sentix

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt

object SystemBarsHelper {

    fun aplicarInsets(
        activity: Activity,
        rootView: View,
        aplicarArriba: Boolean = true,
        aplicarAbajo: Boolean = true,
        paddingExtraArribaDp: Float = 0f,
        paddingExtraAbajoDp: Float = 0f
    ) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        val paddingInicialIzquierda = rootView.paddingLeft
        val paddingInicialArriba = rootView.paddingTop
        val paddingInicialDerecha = rootView.paddingRight
        val paddingInicialAbajo = rootView.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            val extraArriba = dpToPx(activity, paddingExtraArribaDp)
            val extraAbajo = dpToPx(activity, paddingExtraAbajoDp)

            val paddingArribaFinal =
                paddingInicialArriba + if (aplicarArriba) systemBars.top + extraArriba else 0

            val paddingAbajoFinal =
                paddingInicialAbajo + if (aplicarAbajo) systemBars.bottom + extraAbajo else 0

            view.setPadding(
                paddingInicialIzquierda,
                paddingArribaFinal,
                paddingInicialDerecha,
                paddingAbajoFinal
            )

            insets
        }
    }

    fun aplicarInsetsPersonalizado(
        activity: Activity,
        view: View,
        paddingStartDp: Float,
        paddingTopExtraDp: Float,
        paddingEndDp: Float,
        paddingBottomExtraDp: Float
    ) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        ViewCompat.setOnApplyWindowInsetsListener(view) { targetView, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            targetView.setPadding(
                dpToPx(activity, paddingStartDp),
                systemBars.top + dpToPx(activity, paddingTopExtraDp),
                dpToPx(activity, paddingEndDp),
                systemBars.bottom + dpToPx(activity, paddingBottomExtraDp)
            )

            insets
        }
    }

    private fun dpToPx(activity: Activity, dp: Float): Int {
        return (dp * activity.resources.displayMetrics.density).roundToInt()
    }
}