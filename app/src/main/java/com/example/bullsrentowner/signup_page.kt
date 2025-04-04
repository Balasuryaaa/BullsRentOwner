package com.example.bullsrentowner

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest

class SignupPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var spinnerDistrict: Spinner
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSignUp: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_page)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        spinnerDistrict = findViewById(R.id.spinnerDistrict)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSignUp = findViewById(R.id.btnSignUp)

        setupDistrictSpinner()

        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val district = spinnerDistrict.selectedItem.toString()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (!validateInput(name, phone, district, password, confirmPassword)) return@setOnClickListener

            checkIfUserExists(phone, name, district, hashPassword(password))
        }
    }

    private fun setupDistrictSpinner() {
        val districts = arrayOf("Select District", "District 1", "District 2", "District 3")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, districts)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDistrict.adapter = adapter
    }

    private fun checkIfUserExists(phone: String, name: String, district: String, hashedPassword: String) {
        db.collection("users").document(phone).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Toast.makeText(this, "Phone number is already registered!", Toast.LENGTH_LONG).show()
                } else {
                    saveUserData(phone, name, district, hashedPassword)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveUserData(phone: String, name: String, district: String, password: String) {
        val user = hashMapOf(
            "userId" to phone,  // Set phone number as userId
            "name" to name,
            "phone" to phone,
            "district" to district,
            "password" to password,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(phone).set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Sign Up Successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, DashBoard::class.java).apply {
                    putExtra("USER_ID", phone)
                    putExtra("USER_NAME", name)
                    putExtra("USER_PHONE", phone)
                    putExtra("USER_DISTRICT", district)
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun validateInput(name: String, phone: String, district: String, password: String, confirmPassword: String): Boolean {
        if (name.isEmpty() || phone.isEmpty() || district == "Select District" || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!phone.matches(Regex("^[0-9]{10}$"))) {
            Toast.makeText(this, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun hashPassword(password: String): String {
        return MessageDigest.getInstance("SHA-256").digest(password.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
    }
}
