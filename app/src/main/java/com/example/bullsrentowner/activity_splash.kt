package com.example.bullsrentowner

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo: ImageView = findViewById(R.id.logo)
        val appName: TextView = findViewById(R.id.app_name)

        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in)
        val zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in)

        // Apply animations
        logo.startAnimation(zoomIn)
        appName.startAnimation(slideIn)

        // Delay and navigate to LoginPage
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, NavigationActivity::class.java)) // Navigate to LoginPage
            finish()
        }, 3000) // 3 seconds delay
    }
}
