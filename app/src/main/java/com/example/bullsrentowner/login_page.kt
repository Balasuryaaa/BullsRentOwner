package com.example.bullsrentowner

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest

class LoginPage : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var etMobile: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var logo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        etMobile = findViewById(R.id.etMobile)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        logo = findViewById(R.id.logo)

        // Apply fade-in animation to the logo
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        logo.startAnimation(fadeIn)

        btnLogin.setOnClickListener {
            val phoneNumber = etMobile.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (phoneNumber.isEmpty() || password.isEmpty()) {
                showToast("Please enter phone number and password")
                shakeFields()
                return@setOnClickListener
            }

            loginUserWithPhone(phoneNumber, password)
        }

        btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignupPage::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ResetPasswordActivity::class.java))
        }
    }

    private fun loginUserWithPhone(phone: String, enteredPassword: String) {
        db.collection("users").whereEqualTo("phone", phone).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userDoc = documents.documents[0]
                    val storedPassword = userDoc.getString("password") ?: ""

                    if (storedPassword.isEmpty()) {
                        showToast("Password not found in database")
                        return@addOnSuccessListener
                    }

                    if (hashPassword(enteredPassword) == storedPassword) {
                        showToast("Login Successful!")
                        navigateToDashboard(phone)
                    } else {
                        showIncorrectPasswordDialog()
                    }
                } else {
                    showNoRegistrationDialog(phone)
                }
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
            }
    }

    private fun navigateToDashboard(phone: String) {
        val intent = Intent(this, DashBoard::class.java).apply {
            putExtra("USER_PHONE", phone)
        }
        startActivity(intent)
        finish()
    }

    private fun showIncorrectPasswordDialog() {
        AlertDialog.Builder(this)
            .setTitle("Wrong Credentials")
            .setMessage("The password entered is incorrect. Try again or reset your password.")
            .setPositiveButton("Reset Password") { _, _ ->
                startActivity(Intent(this, ResetPasswordActivity::class.java))
            }
            .setNegativeButton("Try Again", null)
            .show()
    }

    private fun showNoRegistrationDialog(phone: String) {
        AlertDialog.Builder(this)
            .setTitle("No Registration Found")
            .setMessage("This phone number is not registered. Do you want to sign up?")
            .setPositiveButton("Sign Up") { _, _ ->
                val intent = Intent(this, SignupPage::class.java)
                intent.putExtra("PHONE_NUMBER", phone)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun shakeFields() {
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
        etMobile.startAnimation(shake)
        etPassword.startAnimation(shake)
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
