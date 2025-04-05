package com.example.bullsrentowner

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Base activity that implements full-screen behavior.
 * All activities should extend this class to have a consistent look and feel.
 */
open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFullScreen()
    }

    /**
     * Set up full-screen mode with transparent status and navigation bars
     */
    protected fun setupFullScreen() {
        // Make the app full screen with edge-to-edge content
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Control system bars visibility and behavior
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Hide both the status bar and navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
        // Make the status bar and navigation bar transparent
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // For older Android versions
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            )
        }
    }
} 