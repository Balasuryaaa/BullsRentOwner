package com.example.bullsrentowner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class CustomerOrderManagement : AppCompatActivity() {

    private lateinit var customerNameTextView: TextView
    private lateinit var customerMobileTextView: TextView
    private lateinit var customerLocationTextView: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var orderRecyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private val db = FirebaseFirestore.getInstance()

    private var customerMobile: String? = null
    private var currentStatus: String = "waiting" // Default status

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_ordermanagement)

        // Initialize views
        initViews()

        // Retrieve customer mobile number from SharedPreferences
        customerMobile = getSharedPreferences("CustomerProfile", MODE_PRIVATE)
            .getString("mobile", null)

        if (!customerMobile.isNullOrEmpty()) {
            fetchCustomerDetails(customerMobile!!)
            setupRecyclerView()
            fetchOrders(currentStatus) // Load default orders
        } else {
            showToast("No Mobile Number Found!")
        }

        // Handle Bottom Navigation item selection
        bottomNavigationView.selectedItemId = R.id.navigation_orders
        setupBottomNavigation()
    }

    private fun initViews() {
        customerNameTextView = findViewById(R.id.tvCustomerName)
        customerMobileTextView = findViewById(R.id.tvCustomerMobile)
        customerLocationTextView = findViewById(R.id.tvCustomerLocation)
        bottomNavigationView = findViewById(R.id.bottomNavBar)
        orderRecyclerView = findViewById(R.id.recyclerViewOrders)
    }

    private fun setupRecyclerView() {
        orderRecyclerView.layoutManager = LinearLayoutManager(this)
        orderAdapter = OrderAdapter(emptyList()) // Initialize adapter with empty list
        orderRecyclerView.adapter = orderAdapter
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_listings -> {
                    navigateTo(AllListingsActivity::class.java)
                    true
                }
                R.id.navigation_orders -> true // Already on this page
                R.id.navigation_settings -> {
                    navigateTo(CustomerSettings::class.java)
                    true
                }
                R.id.navigation_profile -> {
                    navigateTo(CustomerProfile::class.java)
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateTo(destination: Class<*>) {
        startActivity(Intent(this, destination))
    }

    private fun fetchCustomerDetails(mobile: String) {
        db.collection("customers").document(mobile).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    customerNameTextView.text = "Name: ${document.getString("name") ?: "N/A"}"
                    customerMobileTextView.text = "Mobile: $mobile"
                    customerLocationTextView.text = "Location: ${document.getString("location") ?: "N/A"}"
                } else {
                    showToast("Customer not found in database!")
                }
            }
            .addOnFailureListener { e ->
                logError("Error fetching customer data", e)
                showToast("Error loading customer data.")
            }
    }

    private fun fetchOrders(status: String) {
        customerMobile?.let { mobile ->
            db.collection("orders")
                .whereEqualTo("customerMobile", mobile)
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener { documents ->
                    val orders = documents.map { doc ->
                        Order(
                            id = doc.id,
                            machineName = doc.getString("machineName") ?: "Unknown",
                            status = doc.getString("status") ?: "Unknown"
                        )
                    }
                    orderAdapter.updateOrders(orders) // Update adapter with new orders
                }
                .addOnFailureListener { e ->
                    logError("Error fetching orders", e)
                    showToast("Error loading orders.")
                }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.order_status_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        currentStatus = when (item.itemId) {
            R.id.menu_waiting -> "waiting"
            R.id.menu_success -> "success"
            R.id.menu_pending -> "pending"
            R.id.menu_payment_not_done -> "payment not done"
            else -> return super.onOptionsItemSelected(item)
        }
        fetchOrders(currentStatus)
        return true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun logError(message: String, e: Exception) {
        Log.e("Firestore", message, e)
    }
}
