package com.example.bullsrentowner

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class NavigationActivity : AppCompatActivity() {

    private var yStart = 0f // To track swipe direction
    private lateinit var logo: ImageView

    companion object {
        private const val SWIPE_THRESHOLD = 150  // Min distance for a valid swipe
        private const val ANIMATION_DURATION = 500L  // Animation time in milliseconds
        private const val LOGO_MOVE_DISTANCE = 1000f  // Distance logo moves off-screen
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation2)

        logo = findViewById(R.id.logo)

        // Apply a bounce effect when opening
        val bounce = AnimationUtils.loadAnimation(this, R.anim.bounce)
        logo.startAnimation(bounce)

        // Handle touch events on logo
        logo.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    yStart = event.rawY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val yEnd = event.rawY
                    val deltaY = yEnd - yStart

                    if (abs(deltaY) > SWIPE_THRESHOLD) {
                        if (deltaY < 0) navigateToLogin() else navigateToDashboard()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToLogin() {
        animateLogo(-LOGO_MOVE_DISTANCE) {
            startActivity(Intent(this, LoginPage::class.java))
            finish()
        }
    }

    private fun navigateToDashboard() {
        animateLogo(LOGO_MOVE_DISTANCE) {
            startActivity(Intent(this, customer_profile ::class.java))
            finish()
        }
    }

    private fun animateLogo(translationY: Float, endAction: () -> Unit) {
        logo.animate()
            .translationY(translationY)
            .alpha(0f)
            .setDuration(ANIMATION_DURATION)
            .withEndAction {
                logo.translationY = 0f
                logo.alpha = 1f
                endAction()
            }
    }
}
