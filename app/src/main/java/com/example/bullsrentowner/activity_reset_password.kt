package com.example.bullsrentowner

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var etPhone: EditText
    private lateinit var etOTP: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var btnSendOTP: Button
    private lateinit var btnVerifyOTP: Button
    private lateinit var btnResetPassword: Button
    private var storedVerificationId: String? = null
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var verifiedPhone: String? = null  // Store verified phone number

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        etPhone = findViewById(R.id.etPhone)
        etOTP = findViewById(R.id.etOTP)
        etNewPassword = findViewById(R.id.etNewPassword)
        btnSendOTP = findViewById(R.id.btnSendOTP)
        btnVerifyOTP = findViewById(R.id.btnVerifyOTP)
        btnResetPassword = findViewById(R.id.btnResetPassword)

        setupCallbacks()

        btnSendOTP.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (!validatePhone(phone)) return@setOnClickListener
            checkUserExists("+91$phone")
        }

        btnVerifyOTP.setOnClickListener {
            val otp = etOTP.text.toString().trim()
            if (storedVerificationId != null && otp.isNotEmpty()) {
                val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, otp)
                signInWithPhoneAuthCredential(credential)
            } else {
                Toast.makeText(this, "Enter a valid OTP", Toast.LENGTH_SHORT).show()
            }
        }

        btnResetPassword.setOnClickListener {
            val newPassword = etNewPassword.text.toString().trim()
            if (verifiedPhone != null && newPassword.isNotEmpty()) {
                updatePassword(verifiedPhone!!, newPassword)
            } else {
                Toast.makeText(this, "Please verify OTP first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCallbacks() {
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(this@ResetPasswordActivity, "Verification Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId
                resendToken = token
                Toast.makeText(this@ResetPasswordActivity, "OTP Sent!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUserExists(phone: String) {
        db.collection("users").document(phone).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    sendOTP(phone)
                } else {
                    showUserNotFoundDialog()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error checking user", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendOTP(phone: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                verifiedPhone = etPhone.text.toString().trim()
                Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show()
                etNewPassword.visibility = View.VISIBLE
                btnResetPassword.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "OTP Verification Failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePassword(phone: String, newPassword: String) {
        val hashedPassword = hashPassword(newPassword)

        db.collection("users").document(phone)
            .update("password", hashedPassword)
            .addOnSuccessListener {
                Toast.makeText(this, "Password Updated Successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginPage::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update password: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validatePhone(phone: String): Boolean {
        return if (!phone.matches(Regex("^[0-9]{10}$"))) {
            Toast.makeText(this, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun hashPassword(password: String): String {
        return MessageDigest.getInstance("SHA-256").digest(password.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
    }

    private fun showUserNotFoundDialog() {
        AlertDialog.Builder(this)
            .setTitle("User Not Found")
            .setMessage("This phone number is not registered. Would you like to sign up?")
            .setPositiveButton("Sign Up") { _, _ ->
                val intent = Intent(this, SignupPage::class.java)
                intent.putExtra("PHONE_NUMBER", etPhone.text.toString().trim())
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
