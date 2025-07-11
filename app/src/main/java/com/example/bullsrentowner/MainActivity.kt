import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.bullsrentowner.DashBoard
import com.example.bullsrentowner.LoginPage
import com.example.bullsrentowner.R
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply full-screen mode
        setupFullScreen()
        
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Load animations
        val fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val scaleAnim = AnimationUtils.loadAnimation(this, R.anim.scale_anim)

        findViewById<ImageView>(R.id.logo).startAnimation(scaleAnim)
        findViewById<TextView>(R.id.slogan).startAnimation(fadeInAnim)
        findViewById<TextView>(R.id.name).startAnimation(fadeInAnim)

        // Delay and navigate to the next screen
        Handler(Looper.getMainLooper()).postDelayed({
            if (auth.currentUser != null) {
                startActivity(Intent(this, DashBoard::class.java))
            } else {
                startActivity(Intent(this, LoginPage::class.java))
            }
            finish()
        }, 2500)
    }
    
    private fun setupFullScreen() {
        // Make the app full screen with edge-to-edge content
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Control system bars visibility
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Hide both the status bar and navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
        // Make the status bar and navigation bar transparent (as a fallback)
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
