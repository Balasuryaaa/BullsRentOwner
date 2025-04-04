package com.example.bullsrentowner

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private val userCollection = db.collection("users")

    private lateinit var ivProfileImage: ImageView
    private lateinit var etPhoneNumber: EditText
    private lateinit var etName: EditText
    private lateinit var etLocation: EditText
    private lateinit var etCompanyName: EditText
    private lateinit var btnSaveUpdate: Button
    private lateinit var btnSignOut: Button
    private lateinit var progressBar: ProgressBar

    private var isProfileLoaded = false
    private var userPhone: String? = null
    private var originalData: Map<String, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        initializeUI()

        // 🔹 Retrieve phone number from Intent and display it
        userPhone = intent.getStringExtra("USER_PHONE")
        if (userPhone.isNullOrEmpty()) {
            showToast("Error fetching phone number.")
            finish()
            return
        }

        etPhoneNumber.setText(userPhone)
        etPhoneNumber.isEnabled = false  // Phone number should not be editable

        fetchUserProfile()  // Auto-fetch user profile on page load
        btnSaveUpdate.setOnClickListener { saveOrUpdateProfile() }
        btnSignOut.setOnClickListener { signOutUser() }

        setupTextWatchers()
    }

    private fun initializeUI() {
        ivProfileImage = findViewById(R.id.ivProfileImage)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etName = findViewById(R.id.etName)
        etLocation = findViewById(R.id.etLocation)
        etCompanyName = findViewById(R.id.etCompanyName)
        btnSaveUpdate = findViewById(R.id.btnSaveUpdate)
        btnSignOut = findViewById(R.id.btnSignOut)
        progressBar = findViewById(R.id.progressBar)

        btnSaveUpdate.isEnabled = false  // Initially disabled until user modifies input
    }

    private fun fetchUserProfile() {
        if (userPhone.isNullOrEmpty()) return

        progressBar.visibility = View.VISIBLE
        userCollection.document(userPhone!!).get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                if (document.exists()) {
                    etName.setText(document.getString("name") ?: "")
                    etLocation.setText(document.getString("location") ?: "")
                    etCompanyName.setText(document.getString("companyName") ?: "")

                    originalData = mapOf(
                        "name" to etName.text.toString(),
                        "location" to etLocation.text.toString(),
                        "companyName" to etCompanyName.text.toString()
                    )

                    isProfileLoaded = true
                    btnSaveUpdate.text = "Update"
                    btnSaveUpdate.isEnabled = false  // Wait for user to modify
                } else {
                    showToast("No profile found. Please enter details and save.")
                    isProfileLoaded = false
                    btnSaveUpdate.text = "Save"
                    btnSaveUpdate.isEnabled = true
                }
            }
            .addOnFailureListener { error ->
                progressBar.visibility = View.GONE
                showToast("Error fetching profile: ${error.message}")
            }
    }

    private fun saveOrUpdateProfile() {
        if (userPhone.isNullOrEmpty()) return

        val name = etName.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val companyName = etCompanyName.text.toString().trim()

        if (name.isEmpty() || location.isEmpty() || companyName.isEmpty()) {
            showToast("Please fill all fields")
            return
        }

        val userProfile = mapOf(
            "name" to name,
            "location" to location,
            "companyName" to companyName
        )

        progressBar.visibility = View.VISIBLE
        userCollection.document(userPhone!!).set(userProfile, SetOptions.merge())
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                showToast(if (isProfileLoaded) "Profile updated successfully!" else "Profile saved successfully!")
                isProfileLoaded = true
                btnSaveUpdate.text = "Update"
                btnSaveUpdate.isEnabled = false  // No changes to save until user modifies
            }
            .addOnFailureListener { error ->
                progressBar.visibility = View.GONE
                showToast("Error saving profile: ${error.message}")
            }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val isModified = originalData["name"] != etName.text.toString().trim() ||
                        originalData["location"] != etLocation.text.toString().trim() ||
                        originalData["companyName"] != etCompanyName.text.toString().trim()

                btnSaveUpdate.text = if (isModified) "Save" else "Update"
                btnSaveUpdate.isEnabled = isModified
            }
        }

        etName.addTextChangedListener(textWatcher)
        etLocation.addTextChangedListener(textWatcher)
        etCompanyName.addTextChangedListener(textWatcher)
    }

    private fun signOutUser() {
        auth.signOut()
        showToast("Signed out successfully")
        startActivity(Intent(this, LoginPage::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
