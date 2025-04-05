package com.example.bullsrentowner

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp

class AllListingsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var rvListings: RecyclerView
    private lateinit var tvNoListings: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var allListingsAdapter: AllListingsAdapter
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var etSearch: EditText
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
        spinnerSort = findViewById(R.id.spinnerSort)

        setupRecyclerView()
        setupSearchAndSort()

        // ✅ Check Internet Before Fetching Data
        if (isInternetAvailable()) {
            fetchAllListings()
        } else {
            Toast.makeText(this, "No internet connection!", Toast.LENGTH_SHORT).show()
        }

        // ✅ Ensure "Listings" is the selected tab
        bottomNavigationView.selectedItemId = R.id.navigation_listings

        // ✅ Bottom Navigation Handling
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

    private fun setupRecyclerView() {
        allListingsAdapter = AllListingsAdapter(this, filteredListings)
        rvListings.layoutManager = LinearLayoutManager(this)
        rvListings.adapter = allListingsAdapter
    }

    private fun setupSearchAndSort() {
        // ✅ Search Functionality
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterListings()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // ✅ Sorting Options
        val sortOptions = arrayOf("Default", "Price: Low to High", "Price: High to Low")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSort.adapter = adapter

        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filterListings()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchAllListings() {
        progressBar.visibility = View.VISIBLE
        rvListings.visibility = View.GONE
        tvNoListings.visibility = View.GONE

        // ✅ Firestore Connection Debugging
        Log.d(TAG, "Attempting to fetch listings...")

        firestore.collection("listings")  // ✅ Ensure collection name is correct
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
                        listing?.let { listings.add(it) }
                    }
                    filterListings()  // ✅ Apply search & sort immediately after fetching
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e(TAG, "Firestore fetch failed: ", e)
                Toast.makeText(this, "Error fetching listings", Toast.LENGTH_SHORT).show()
                tvNoListings.visibility = View.VISIBLE
            }
    }

    private fun filterListings() {
        val searchText = etSearch.text.toString().trim().lowercase()
        val selectedSort = spinnerSort.selectedItem.toString()

        // ✅ Apply search filter
        filteredListings.clear()
        filteredListings.addAll(
            listings.filter {
                it.productName.lowercase().contains(searchText) ||
                        it.rentType.lowercase().contains(searchText)
            }
        )

        // ✅ Apply sorting
        when (selectedSort) {
            "Price: Low to High" -> filteredListings.sortBy { it.rentPrice }
            "Price: High to Low" -> filteredListings.sortByDescending { it.rentPrice }
        }

        // ✅ Update UI
        allListingsAdapter.notifyDataSetChanged()
        rvListings.visibility = if (filteredListings.isEmpty()) View.GONE else View.VISIBLE
        tvNoListings.visibility = if (filteredListings.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun parseListing(document: QueryDocumentSnapshot): Listing? {
        return try {
            val data = document.data
            
            // Log raw data for debugging
            Log.d(TAG, "Raw listing data: ${document.id} - $data")
            
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

            Listing(
                id = document.id,
                productName = data["productName"] as? String ?: "",
                rentType = data["rentType"] as? String ?: "",
                rentPrice = rentPrice,
                description = data["description"] as? String ?: "",
                imageBase64 = imageBase64List,
                location = data["location"] as? String ?: "",
                ownerName = data["ownerName"] as? String ?: "",
                ownerPhone = data["ownerPhone"] as? String ?: "",
                timestamp = data["timestamp"] as? Timestamp
            ).also {
                Log.d(TAG, "Successfully parsed listing: ${it.id}, ${it.productName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing listing ${document.id}: ${e.message}", e)
            null
        }
    }

    // ✅ Check Internet Connection
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        private const val TAG = "AllListingsActivity"
    }
}
