package com.example.bullsrentowner

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import org.checkerframework.checker.units.qual.C

class customer_profile : AppCompatActivity() {

    private lateinit var customerNameEditText: EditText
    private lateinit var customerMobileEditText: EditText
    private lateinit var customerLocationEditText: EditText
    private lateinit var btnSaveProfile: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private val db = FirebaseFirestore.getInstance()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_profile)

        try {
            // Initialize views
            customerMobileEditText = findViewById(R.id.etCustomerMobile)
            customerNameEditText = findViewById(R.id.etCustomerName)
            customerLocationEditText = findViewById(R.id.etCustomerLocation)
            btnSaveProfile = findViewById(R.id.btnSaveProfile)
            bottomNavigationView = findViewById(R.id.bottomNavBar)

            // SharedPreferences for storing customer ID and details
            sharedPreferences = getSharedPreferences("CustomerProfile", MODE_PRIVATE)

            // Restore saved data
            loadCustomerFromPreferences()

            // Set selected item for profile
            bottomNavigationView.selectedItemId = R.id.navigation_profile

            // Handle Bottom Navigation
            bottomNavigationView.setOnItemSelectedListener { item ->
                saveCustomerToPreferences() // Save data before switching pages
                when (item.itemId) {
                    R.id.navigation_listings -> startActivity(Intent(this, AllListingsActivity::class.java))
                    R.id.navigation_orders -> startActivity(Intent(this, CustomerOrderManagement::class.java))
                    R.id.navigation_settings -> startActivity(Intent(this, customer_settings::class.java))
                    R.id.navigation_profile -> {} // Stay on the same page
                }
                true
            }

            // Check if customer exists when phone number is entered
            customerMobileEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val mobileNumber = customerMobileEditText.text.toString().trim()
                    if (mobileNumber.isNotEmpty()) {
                        checkIfCustomerExists(mobileNumber)
                    }
                }
            }

            // Save customer data when "Save Profile" button is clicked
            btnSaveProfile.setOnClickListener {
                val customerName = customerNameEditText.text.toString().trim()
                val customerMobile = customerMobileEditText.text.toString().trim()
                val customerLocation = customerLocationEditText.text.toString().trim()

                if (customerName.isNotEmpty() && customerMobile.isNotEmpty() && customerLocation.isNotEmpty()) {
                    saveCustomerToFirestore(customerMobile, customerName, customerLocation)
                    saveCustomerToPreferences()
                } else {
                    Toast.makeText(this, "Please fill in all details.", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            Log.e("customer_profile", "Error in onCreate: ${e.message}")
            Toast.makeText(this, "An error occurred while loading the page.", Toast.LENGTH_LONG).show()
        }
    }

    // Load stored data when returning to profile
    private fun loadCustomerFromPreferences() {
        customerMobileEditText.setText(sharedPreferences.getString("mobile", ""))
        customerNameEditText.setText(sharedPreferences.getString("name", ""))
        customerLocationEditText.setText(sharedPreferences.getString("location", ""))
    }

    // Save data to SharedPreferences
    private fun saveCustomerToPreferences() {
        val editor = sharedPreferences.edit()
        editor.putString("mobile", customerMobileEditText.text.toString())
        editor.putString("name", customerNameEditText.text.toString())
        editor.putString("location", customerLocationEditText.text.toString())
        editor.apply()
    }

    private fun checkIfCustomerExists(mobileNumber: String) {
        db.collection("customers").document(mobileNumber).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    customerNameEditText.setText(document.getString("name"))
                    customerLocationEditText.setText(document.getString("location"))
                    saveCustomerId(mobileNumber) // Save the customer ID for global access
                    saveCustomerToPreferences()
                    Toast.makeText(this, "Customer found!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Customer not found! Please enter details.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error checking customer", e)
                Toast.makeText(this, "Error checking database.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveCustomerToFirestore(mobile: String, name: String, location: String) {
        val customerData = hashMapOf(
            "name" to name,
            "location" to location
        )

        db.collection("customers").document(mobile).set(customerData)
            .addOnSuccessListener {
                saveCustomerId(mobile) // Save the customer ID globally
                Toast.makeText(this, "Profile Saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error saving customer", e)
                Toast.makeText(this, "Error saving profile.", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to save customer ID for use in all activities
    private fun saveCustomerId(customerId: String) {
        val editor = sharedPreferences.edit()
        editor.putString("customerId", customerId)
        editor.apply()
    }

    // Function to retrieve customer ID from SharedPreferences (Call this in other activities)
    fun getCustomerId(): String? {
        return sharedPreferences.getString("customerId", null)
    }
}
