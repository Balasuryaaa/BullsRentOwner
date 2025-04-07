package com.example.bullsrentowner

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class AllListingsActivity : BaseActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var rvListings: RecyclerView
    private lateinit var tvNoListings: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var allListingsAdapter: AllListingsAdapter
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var etSearch: EditText
    private lateinit var etLocationSearch: EditText
    private lateinit var btnFilter: ImageButton
    
    // Filter variables
    private var selectedEquipmentType = "All Equipment Types"
    private var selectedRentType = "All Rent Types"
    private var selectedSort = "Default"
    
    // Bottom sheet dialog and views
    private lateinit var filterBottomSheet: BottomSheetDialog
    private lateinit var spinnerEquipmentType: Spinner
    private lateinit var spinnerRentType: Spinner
    private lateinit var spinnerSort: Spinner

    private val listings = mutableListOf<Listing>()
    private val filteredListings = mutableListOf<Listing>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_listings)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Bind Views
        rvListings = findViewById(R.id.rvAllListings)
        tvNoListings = findViewById(R.id.tvNoAllListings)
        progressBar = findViewById(R.id.progressBar)
        bottomNavigationView = findViewById(R.id.bottomNavBar)
        etSearch = findViewById(R.id.etSearch)
        etLocationSearch = findViewById(R.id.etLocationSearch)
        btnFilter = findViewById(R.id.btnFilter)
        
        setupFilterBottomSheet()
        setupRecyclerView()
        setupSearchAndFilters()

        // Check Internet Before Fetching Data
        if (isInternetAvailable()) {
            fetchAllListings()
        } else {
            Toast.makeText(this, "No internet connection!", Toast.LENGTH_SHORT).show()
        }

        // Set "Listings" as the selected tab
        bottomNavigationView.selectedItemId = R.id.navigation_listings

        // Bottom Navigation Handling
        setupBottomNavigation()
    }
    
    private fun setupFilterBottomSheet() {
        // Initialize the bottom sheet dialog
        filterBottomSheet = BottomSheetDialog(this)
        val bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_filters, null)
        filterBottomSheet.setContentView(bottomSheetView)
        
        // Get filter views
        spinnerEquipmentType = bottomSheetView.findViewById(R.id.spinnerEquipmentType)
        spinnerRentType = bottomSheetView.findViewById(R.id.spinnerRentType)
        spinnerSort = bottomSheetView.findViewById(R.id.spinnerSort)
        val btnApplyFilters = bottomSheetView.findViewById<Button>(R.id.btnApplyFilters)
        
        // Setup spinners
        setupEquipmentTypeSpinner()
        setupRentTypeSpinner()
        setupSortSpinner()
        
        // Apply button click listener
        btnApplyFilters.setOnClickListener {
            selectedEquipmentType = spinnerEquipmentType.selectedItem.toString()
            selectedRentType = spinnerRentType.selectedItem.toString()
            selectedSort = spinnerSort.selectedItem.toString()
            
            filterListings()
            filterBottomSheet.dismiss()
        }
        
        // Filter button click listener
        btnFilter.setOnClickListener {
            filterBottomSheet.show()
        }
    }

    private fun setupRecyclerView() {
        allListingsAdapter = AllListingsAdapter(this, filteredListings)
        rvListings.layoutManager = LinearLayoutManager(this)
        rvListings.adapter = allListingsAdapter
    }

    private fun setupSearchAndFilters() {
        // Search by product name
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterListings()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Search by location
        etLocationSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterListings()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupEquipmentTypeSpinner() {
        // Add "All Equipment Types" as first option
        val equipmentTypes = resources.getStringArray(R.array.equipment_types).toMutableList()
        equipmentTypes.add(0, "All Equipment Types")

        val adapter = ArrayAdapter(this, R.layout.spinner_item, equipmentTypes)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerEquipmentType.adapter = adapter
        
        // Set the current selection
        val position = equipmentTypes.indexOf(selectedEquipmentType)
        if (position >= 0) {
            spinnerEquipmentType.setSelection(position)
        }
    }

    private fun setupRentTypeSpinner() {
        // Add "All Rent Types" as first option
        val rentTypes = resources.getStringArray(R.array.rent_types).toMutableList()
        rentTypes.add(0, "All Rent Types")

        val adapter = ArrayAdapter(this, R.layout.spinner_item, rentTypes)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerRentType.adapter = adapter
        
        // Set the current selection
        val position = rentTypes.indexOf(selectedRentType)
        if (position >= 0) {
            spinnerRentType.setSelection(position)
        }
    }

    private fun setupSortSpinner() {
        val sortOptions = arrayOf("Default", "Price: Low to High", "Price: High to Low")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, sortOptions)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerSort.adapter = adapter
        
        // Set the current selection
        val position = sortOptions.indexOf(selectedSort)
        if (position >= 0) {
            spinnerSort.setSelection(position)
        }
    }

    private fun fetchAllListings() {
        progressBar.visibility = View.VISIBLE
        rvListings.visibility = View.GONE
        tvNoListings.visibility = View.GONE

        // Firestore Connection Debugging
        Log.d(TAG, "Attempting to fetch listings...")

        // First, get all active bookings
        fetchActiveBookings { activeBookings ->
            firestore.collection("listings")  // Ensure collection name is correct
                .get()
                .addOnSuccessListener { documents ->
                    progressBar.visibility = View.GONE
                    listings.clear()

                    if (documents.isEmpty) {
                        Log.d(TAG, "No listings found in Firestore.")
                        tvNoListings.visibility = View.VISIBLE
                    } else {
                        documents.forEach { document ->
                            val listing = parseListing(document)
                            // Only add listings that are not currently booked
                            listing?.let { 
                                if (!isListingCurrentlyBooked(listing.id, activeBookings)) {
                                    listings.add(it) 
                                }
                            }
                        }
                        filterListings()  // Apply search & sort immediately after fetching
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Firestore fetch failed: ", e)
                    Toast.makeText(this, "Error fetching listings", Toast.LENGTH_SHORT).show()
                    tvNoListings.visibility = View.VISIBLE
                }
        }
    }

    private fun filterListings() {
        val searchText = etSearch.text.toString().trim().lowercase()
        val locationText = etLocationSearch.text.toString().trim().lowercase()

        // Debug logging
        Log.d(TAG, "Filtering with equipment type: '$selectedEquipmentType', rent type: '$selectedRentType'")
        
        // Apply all filters
        filteredListings.clear()
        filteredListings.addAll(
            listings.filter { listing ->
                // Product Name filter
                val nameMatches = listing.productName.lowercase().contains(searchText)
                
                // Location filter
                val locationMatches = locationText.isEmpty() || 
                    listing.location.lowercase().contains(locationText)
                
                // Equipment Type filter - Fix for older listings that might not have equipmentType set
                val equipmentTypeMatches = when {
                    selectedEquipmentType == "All Equipment Types" -> true
                    listing.equipmentType.isBlank() && listing.rentType.isNotBlank() -> 
                        listing.rentType.lowercase().contains(selectedEquipmentType.lowercase())
                    else -> listing.equipmentType.lowercase().contains(selectedEquipmentType.lowercase())
                }
                
                // Rent Type filter
                val rentTypeMatches = selectedRentType == "All Rent Types" || 
                    listing.rentType.lowercase() == selectedRentType.lowercase()
                
                // Combined filter result
                nameMatches && locationMatches && equipmentTypeMatches && rentTypeMatches
            }
        )

        // Apply sorting
        when (selectedSort) {
            "Price: Low to High" -> filteredListings.sortBy { it.rentPrice }
            "Price: High to Low" -> filteredListings.sortByDescending { it.rentPrice }
        }

        // Update UI
        allListingsAdapter.notifyDataSetChanged()
        rvListings.visibility = if (filteredListings.isEmpty()) View.GONE else View.VISIBLE
        tvNoListings.visibility = if (filteredListings.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun parseListing(document: QueryDocumentSnapshot): Listing? {
        return try {
            val data = document.data
            
            // Handle both String and List imageBase64 formats
            val imageBase64List = when {
                data["imageBase64"] is List<*> -> (data["imageBase64"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                data["imageBase64"] is String -> listOf(data["imageBase64"] as String)
                else -> emptyList()
            }
            
            // Handle different rentPrice formats (String or Number)
            val rentPrice = when {
                data["rentPrice"] is Number -> (data["rentPrice"] as Number).toDouble()
                data["rentPrice"] is String -> (data["rentPrice"] as String).toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            // Get equipment type, or use rentType as fallback for older listings
            val equipmentType = (data["equipmentType"] as? String) ?: ""
            val rentType = (data["rentType"] as? String) ?: ""

            Listing(
                id = document.id,
                productName = data["productName"] as? String ?: "",
                rentType = rentType,
                equipmentType = equipmentType,
                rentPrice = rentPrice,
                description = data["description"] as? String ?: "",
                imageBase64 = imageBase64List,
                location = data["location"] as? String ?: "",
                ownerName = data["ownerName"] as? String ?: "",
                ownerPhone = data["ownerPhone"] as? String ?: "",
                timestamp = data["timestamp"] as? Timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing listing ${document.id}: ${e.message}", e)
            null
        }
    }

    // Check Internet Connection
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_listings -> true  // Stay on this page
                R.id.navigation_orders -> {
                    startActivity(Intent(this, CustomerOrderManagement::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, customer_settings::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(this, customer_profile::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    // Helper class to store booking data
    private data class BookingInfo(
        val listingId: String,
        val startDate: String,
        val endDate: String,
        val status: String,
        val rentType: String = "",
        val startTime: String? = null,
        val endTime: String? = null
    )
    
    // Fetch active bookings (approved or pending)
    private fun fetchActiveBookings(callback: (List<BookingInfo>) -> Unit) {
        val activeBookings = mutableListOf<BookingInfo>()
        
        firestore.collection("bookings")
            .whereIn("status", listOf("approved", "pending"))
            .get()
            .addOnSuccessListener { bookingDocs ->
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                
                bookingDocs.forEach { doc ->
                    val data = doc.data
                    val listingId = data["listingId"] as? String ?: ""
                    val startDate = data["startDate"] as? String ?: ""
                    val endDate = data["endDate"] as? String ?: ""
                    val status = data["status"] as? String ?: ""
                    val rentType = data["rentType"] as? String ?: ""
                    val startTime = data["startTime"] as? String
                    val endTime = data["endTime"] as? String
                    
                    // Only consider bookings that are active (end date is today or in the future)
                    if (listingId.isNotEmpty() && startDate.isNotEmpty() && 
                        endDate.isNotEmpty() && endDate >= today) {
                        activeBookings.add(BookingInfo(
                            listingId, startDate, endDate, status, rentType, startTime, endTime
                        ))
                    }
                }
                
                Log.d(TAG, "Found ${activeBookings.size} active bookings")
                callback(activeBookings)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching bookings: ${e.message}")
                callback(emptyList())
            }
    }
    
    // Check if a listing is currently booked
    private fun isListingCurrentlyBooked(listingId: String, activeBookings: List<BookingInfo>): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        
        return activeBookings.any { booking ->
            // Check if listing ID matches
            if (booking.listingId != listingId) return@any false
            
            // Check if today falls within the booking date range
            if (today < booking.startDate || today > booking.endDate) return@any false
            
            // If per hour rental with time specified, check current time against booked time slot
            if (booking.rentType.contains("hour") && booking.startTime != null && booking.endTime != null) {
                // Only hide if current time falls within the booked time slot
                val isBookedTimeSlot = if (booking.startDate == booking.endDate) {
                    // Same day booking
                    currentTime >= booking.startTime && currentTime <= booking.endTime
                } else {
                    // Multi-day booking
                    if (today == booking.startDate) {
                        // On start date, hide if after start time
                        currentTime >= booking.startTime
                    } else if (today == booking.endDate) {
                        // On end date, hide if before end time
                        currentTime <= booking.endTime
                    } else {
                        // Middle day of multi-day booking, hide the whole day
                        true
                    }
                }
                
                return@any isBookedTimeSlot
            } else {
                // For daily rentals or hourly rentals without specific time,
                // hide for the entire booking period
                return@any true
            }
        }
    }

    companion object {
        private const val TAG = "AllListingsActivity"
    }
}
