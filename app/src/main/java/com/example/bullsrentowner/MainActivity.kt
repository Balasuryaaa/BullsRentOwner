import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.bullsrentowner.DashBoard
import com.example.bullsrentowner.LoginPage
import com.example.bullsrentowner.R
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Enable full-screen mode in a backward-compatible way
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // Load animations
        val fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val scaleAnim = AnimationUtils.loadAnimation(this, R.anim.scale_anim)

        findViewById<ImageView>(R.id.logo).startAnimation(scaleAnim)
        findViewById<TextView>(R.id.slogan).startAnimation(fadeInAnim)
        findViewById<TextView>(R.id.name).startAnimation(fadeInAnim)

        // Delay and navigate to the next screen
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = if (auth.currentUser != null) {
                Intent(this, DashBoard::class.java)
            } else {
                Intent(this, LoginPage::class.java)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }, 5000)
    }
}
