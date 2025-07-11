package com.example.bullsrentowner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

data class Order(
    val id: String,
    val machineName: String,
    val status: String,
    val companyName: String,
    val bookingDate: Long,
    val startDate: String = "",
    val endDate: String = "",
    val duration: String = "",
    val rentType: String = "",
    val startTime: String? = null,
    val endTime: String? = null,
    val totalAmount: String = ""
)

class CustomerOrderManagement : BaseActivity() {

    private lateinit var customerNameTextView: TextView
    private lateinit var customerMobileTextView: TextView
    private lateinit var customerLocationTextView: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var orderRecyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private val db = FirebaseFirestore.getInstance()

    private var customerMobile: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_ordermanagment)

        initViews()

        customerMobile = getSharedPreferences("CustomerProfile", MODE_PRIVATE)
            .getString("mobile", null)

        if (!customerMobile.isNullOrEmpty()) {
            fetchCustomerDetails(customerMobile!!)
            setupRecyclerView()
            fetchAllOrders()
        } else {
            showToast("No Mobile Number Found!")
        }

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
        orderAdapter = OrderAdapter(emptyList())
        orderRecyclerView.adapter = orderAdapter
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_listings -> {
                    navigateTo(AllListingsActivity::class.java)
                    true
                }
                R.id.navigation_orders -> true
                R.id.navigation_settings -> {
                    navigateTo(customer_settings::class.java)
                    true
                }
                R.id.navigation_profile -> {
                    navigateTo(customer_profile::class.java)
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

    private fun fetchAllOrders() {
        customerMobile?.let { mobile ->
            Log.d(TAG, "Fetching orders for customer: $mobile")
            
            db.collection("bookings")
                .whereEqualTo("customerPhone", mobile)
                .get()
                .addOnSuccessListener { documents ->
                    Log.d(TAG, "Query executed successfully. Found ${documents.size()} documents")
                    val ordersList = mutableListOf<Order>()
                    
                    for (document in documents) {
                        try {
                            val data = document.data
                            Log.d(TAG, "Processing document: ${document.id}")
                            
                            // Parse booking time
                            val bookingTimeStr = data["bookingTime"] as? String ?: ""
                            val bookingDate = if (bookingTimeStr.isNotEmpty()) {
                                try {
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        .parse(bookingTimeStr)?.time ?: System.currentTimeMillis()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing date: $bookingTimeStr", e)
                                    System.currentTimeMillis()
                                }
                            } else {
                                System.currentTimeMillis()
                            }

                            val listingId = data["listingId"] as? String
                            
                            if (listingId != null) {
                                val order = Order(
                                    id = document.id,
                                    machineName = data["productName"] as? String ?: "Loading...",
                                    status = data["status"] as? String ?: "Unknown",
                                    companyName = "Owner: " + (data["ownerPhone"] as? String ?: "N/A"),
                                    bookingDate = bookingDate,
                                    startDate = data["startDate"] as? String ?: "",
                                    endDate = data["endDate"] as? String ?: "",
                                    duration = data["duration"] as? String ?: "",
                                    rentType = data["rentType"] as? String ?: "",
                                    startTime = data["startTime"] as? String,
                                    endTime = data["endTime"] as? String
                                )
                                ordersList.add(order)
                                
                                if (ordersList.isNotEmpty()) {
                                    orderAdapter.updateOrders(ordersList.sortedByDescending { it.bookingDate })
                                }
                                
                                // Fetch additional listing details
                                db.collection("listings").document(listingId)
                                    .get()
                                    .addOnSuccessListener { listingDoc ->
                                        if (listingDoc.exists()) {
                                            val productName = listingDoc.getString("productName") ?: "Unknown Product"
                                            val rentPrice = listingDoc.getString("rentPrice") ?: "0"
                                            
                                            // Calculate total amount
                                            val totalAmount = calculateTotalAmount(
                                                rentPrice,
                                                data["duration"] as? String ?: "",
                                                data["rentType"] as? String ?: ""
                                            )
                                            
                                            val index = ordersList.indexOfFirst { it.id == document.id }
                                            if (index >= 0) {
                                                val updatedOrder = ordersList[index].copy(
                                                    machineName = productName,
                                                    companyName = "Owner: " + (listingDoc.getString("ownerName") ?: "N/A"),
                                                    totalAmount = totalAmount
                                                )
                                                ordersList[index] = updatedOrder
                                                orderAdapter.updateOrders(ordersList.sortedByDescending { it.bookingDate })
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Error fetching listing details: ${e.message}", e)
                                    }
                            } else {
                                val order = Order(
                                    id = document.id,
                                    machineName = "Unknown Product",
                                    status = data["status"] as? String ?: "Unknown",
                                    companyName = "Owner: " + (data["ownerPhone"] as? String ?: "N/A"),
                                    bookingDate = bookingDate,
                                    startDate = data["startDate"] as? String ?: "",
                                    endDate = data["endDate"] as? String ?: "",
                                    duration = data["duration"] as? String ?: "",
                                    rentType = data["rentType"] as? String ?: "",
                                    startTime = data["startTime"] as? String,
                                    endTime = data["endTime"] as? String
                                )
                                ordersList.add(order)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing order document: ${e.message}", e)
                        }
                    }
                    
                    if (ordersList.isEmpty()) {
                        showToast("No orders found")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching orders: ${e.message}", e)
                    showToast("Error loading orders: ${e.message}")
                }
        }
    }

    private fun calculateTotalAmount(rentPrice: String, duration: String, rentType: String): String {
        return try {
            val price = rentPrice.toDouble()
            val durationValue = duration.toDouble()
            val total = price * durationValue
            "₹%.2f".format(total)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating total amount: ${e.message}")
            "₹0.00"
        }
    }

    private fun formatBookingPeriod(order: Order): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDate = try {
            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(order.startDate)
            dateFormat.format(parsedDate!!)
        } catch (e: Exception) {
            order.startDate
        }

        return when {
            order.rentType.contains("hour") && order.startTime != null -> {
                "Booked for ${order.duration} hours on $startDate\nTime: ${order.startTime} - ${order.endTime}"
            }
            order.rentType.contains("day") -> {
                val endDate = try {
                    val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(order.endDate)
                    dateFormat.format(parsedDate!!)
                } catch (e: Exception) {
                    order.endDate
                }
                "Booked for ${order.duration} days\nFrom: $startDate\nTo: $endDate"
            }
            else -> "Booking period not specified"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun logError(message: String, e: Exception) {
        Log.e("Firestore", message, e)
    }

    companion object {
        private const val TAG = "CustomerOrderManagement"
    }
}
