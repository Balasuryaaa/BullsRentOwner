package com.example.bullsrentowner

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class customer_settings : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var tvCustomerMobile: TextView  // To show Customer Mobile Number

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customer_settings)

        // Setting up Edge-to-Edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("CustomerProfile", MODE_PRIVATE)

        // Retrieve Customer Mobile Number
        val customerMobile = sharedPreferences.getString("mobile", "Not Found")

        // Display Toast Message with Customer Mobile Number
        Toast.makeText(this, "Mobile Number: $customerMobile", Toast.LENGTH_LONG).show()

        // Initialize TextView and Set Customer Mobile Number
        tvCustomerMobile = findViewById(R.id.tvCustomerMobile) // Ensure this ID exists in XML
        tvCustomerMobile.text = "Mobile Number: $customerMobile"

        // Bottom Navigation View setup
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavBar)

        // Set selected item for Settings
        bottomNavigationView.selectedItemId = R.id.navigation_settings

        // Handle Bottom Navigation item selection
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_listings -> {
                    startActivity(Intent(this, AllListingsActivity::class.java))
                    true
                }
                R.id.navigation_orders -> {
                    startActivity(Intent(this, CustomerOrderManagement::class.java))
                    true
                }
                R.id.navigation_settings -> true
                R.id.navigation_profile -> {
                    startActivity(Intent(this, customer_profile::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
