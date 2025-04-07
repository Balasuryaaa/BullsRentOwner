package com.example.bullsrentowner

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class OwnerOrdersActivity : BaseActivity() {

    private lateinit var tvOwnerMobile: TextView
    private lateinit var statusFilterSpinner: Spinner
    private lateinit var recyclerViewOrders: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var orderAdapter: OrderAdapter
    
    private val db = FirebaseFirestore.getInstance()
    private var ownerPhone: String? = null
    private var currentFilter: String = "All Orders"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_orders)

        // Initialize views
        initViews()
        
        // Get owner phone from intent
        ownerPhone = intent.getStringExtra("USER_PHONE")
        
        if (ownerPhone.isNullOrEmpty()) {
            showToast("No phone number provided!")
            finish()
            return
        }
        
        // Setup owner info
        tvOwnerMobile.text = "Mobile: $ownerPhone"
        
        // Setup status filter spinner
        setupStatusFilter()
        
        // Setup recycler view
        setupRecyclerView()
        
        // Fetch orders
        fetchOrders(null)
    }
    
    private fun initViews() {
        tvOwnerMobile = findViewById(R.id.tvOwnerMobile)
        statusFilterSpinner = findViewById(R.id.statusFilterSpinner)
        recyclerViewOrders = findViewById(R.id.recyclerViewOrders)
        tvEmptyState = findViewById(R.id.tvEmptyState)
    }
    
    private fun setupStatusFilter() {
        val statusOptions = resources.getStringArray(R.array.order_status_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statusOptions)
        statusFilterSpinner.adapter = adapter
        
        statusFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStatus = statusOptions[position]
                currentFilter = selectedStatus
                
                // Filter is "All Orders" or a specific status
                val statusFilter = if (selectedStatus == "All Orders") null else selectedStatus
                fetchOrders(statusFilter)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    
    private fun setupRecyclerView() {
        recyclerViewOrders.layoutManager = LinearLayoutManager(this)
        orderAdapter = OrderAdapter(emptyList())
        recyclerViewOrders.adapter = orderAdapter
    }
    
    private fun fetchOrders(statusFilter: String?) {
        ownerPhone?.let { phone ->
            Log.d(TAG, "Fetching orders for owner: $phone, filter: $statusFilter")
            
            val query = db.collection("bookings")
                .whereEqualTo("ownerPhone", phone)
            
            // Apply status filter if specified
            val filteredQuery = if (statusFilter != null) {
                query.whereEqualTo("status", statusFilter.lowercase())
            } else {
                query
            }
            
            filteredQuery.get()
                .addOnSuccessListener { documents ->
                    Log.d(TAG, "Query executed successfully. Found ${documents.size()} documents")
                    val ordersList = mutableListOf<Order>()
                    
                    if (documents.isEmpty) {
                        showEmptyState()
                        return@addOnSuccessListener
                    }
                    
                    hideEmptyState()
                    
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

                            // Get the customer information
                            val customerPhone = data["customerPhone"] as? String ?: "Unknown"
                            
                            // Look up the product name from the listing ID
                            val listingId = data["listingId"] as? String
                            
                            if (listingId != null) {
                                val order = Order(
                                    id = document.id,
                                    machineName = data["productName"] as? String ?: "Loading...",
                                    status = data["status"] as? String ?: "Unknown",
                                    companyName = "Customer: $customerPhone",
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
                                
                                // Fetch product and listing details
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
                                            
                                            // Fetch customer name
                                            db.collection("customers")
                                                .document(customerPhone)
                                                .get()
                                                .addOnSuccessListener { customerDoc ->
                                                    val customerName = if (customerDoc.exists()) {
                                                        customerDoc.getString("name") ?: "Unknown"
                                                    } else "Unknown"
                                                    
                                                    val index = ordersList.indexOfFirst { it.id == document.id }
                                                    if (index >= 0) {
                                                        val updatedOrder = ordersList[index].copy(
                                                            machineName = productName,
                                                            companyName = "Customer: $customerName ($customerPhone)",
                                                            totalAmount = totalAmount
                                                        )
                                                        ordersList[index] = updatedOrder
                                                        orderAdapter.updateOrders(ordersList.sortedByDescending { it.bookingDate })
                                                    }
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
                                    companyName = "Customer: $customerPhone",
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
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching orders: ${e.message}", e)
                    showToast("Error loading orders: ${e.message}")
                    showEmptyState()
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
    
    private fun showEmptyState() {
        recyclerViewOrders.visibility = View.GONE
        tvEmptyState.visibility = View.VISIBLE
        tvEmptyState.text = when (currentFilter) {
            "All Orders" -> "No orders found"
            else -> "No ${currentFilter.lowercase()} orders found"
        }
    }
    
    private fun hideEmptyState() {
        recyclerViewOrders.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    companion object {
        private const val TAG = "OwnerOrdersActivity"
    }
} 