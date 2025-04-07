package com.example.bullsrentowner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class DashBoard : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var userPhone: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dash_board)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Retrieve the phone number from Intent extras
        userPhone = intent.getStringExtra("USER_PHONE")

        if (userPhone.isNullOrEmpty()) {
            Log.e("DashBoard", "Phone number is null. Redirecting to LoginPage.")
            showToast("Session expired. Please log in again.")
            navigateToLogin()
            return
        }

        Log.d("DashBoard", "User Phone: $userPhone")

        // Apply system bar insets
        applySystemInsets()

        // Initialize UI elements
        setupUI()
    }

    private fun applySystemInsets() {
        findViewById<View>(R.id.main)?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }

    private fun setupUI() {
        findViewById<ImageView>(R.id.profileIcon)?.setOnClickListener { navigateTo(ProfileActivity::class.java) }
        findViewById<ImageView>(R.id.addListing)?.setOnClickListener { navigateTo(AddListingActivity::class.java) }
        findViewById<ImageButton>(R.id.menuButton)?.setOnClickListener { showPopupMenu(it) }
    }

    private fun showPopupMenu(view: View) {
        PopupMenu(this, view).apply {
            menuInflater.inflate(R.menu.menu_options, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.about -> showToast("About section coming soon!")
                    R.id.help -> showToast("Help & FAQs will be available soon!")
                    R.id.viewListings -> navigateTo(ViewListingsActivity::class.java)
                    R.id.viewOrders -> navigateTo(OwnerOrdersActivity::class.java)
                    else -> false
                }
                true
            }
        }.show()
    }

    private fun navigateTo(destination: Class<*>) {
        if (userPhone.isNullOrEmpty()) {
            showToast("Error: User phone number missing!")
            return
        }
        val intent = Intent(this, destination).apply {
            putExtra("USER_PHONE", userPhone)
        }
        startActivity(intent)
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginPage::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
