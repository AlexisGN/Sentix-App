package com.example.sentix

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.view.View

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val rootSplash = findViewById<View>(R.id.rootSplash)

        SystemBarsHelper.aplicarInsets(
            activity = this,
            rootView = rootSplash,
            aplicarArriba = true,
            aplicarAbajo = true
        )
        val logo = findViewById<ImageView>(R.id.imgLogoSplash)
        val animacion = AnimationUtils.loadAnimation(this, R.anim.splash_zoom_fade)
        logo.startAnimation(animacion)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1800)
    }
}