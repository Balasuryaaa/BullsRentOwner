package com.example.bullsrentowner

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot

class ViewListingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var rvListings: RecyclerView
    private lateinit var tvNoListings: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var listingsAdapter: ListingsAdapter

    private var userPhone: String? = null
    private val listings = mutableListOf<Listing>()

    private val editListingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh listings after edit
            fetchUserListings()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_listings)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        rvListings = findViewById(R.id.rvListings)
        tvNoListings = findViewById(R.id.tvNoListings)
        progressBar = findViewById(R.id.progressBar)

        userPhone = intent.getStringExtra("USER_PHONE")?.trim()

        if (userPhone.isNullOrEmpty()) {
            Log.e(TAG, "Error: User phone number is missing!")
            showToast("Session expired. Please log in again.")
            finish()
            return
        }

        Log.d(TAG, "Received USER_PHONE: '$userPhone'")

        setupRecyclerView()
        fetchUserListings()
    }

    private fun setupRecyclerView() {
        listingsAdapter = ListingsAdapter(
            this,
            listings,
            onDeleteClick = { listing -> deleteListing(listing.id) },
            onEditClick = { listing -> editListing(listing) }
        )
        rvListings.layoutManager = LinearLayoutManager(this)
        rvListings.adapter = listingsAdapter
    }

    private fun fetchUserListings() {
        userPhone?.let { phone ->
            progressBar.visibility = View.VISIBLE
            rvListings.visibility = View.GONE
            tvNoListings.visibility = View.GONE

            Log.d(TAG, "Fetching listings for ownerPhone: '$phone'")

            firestore.collection("listings")
                .whereEqualTo("ownerPhone", phone)
                .get()
                .addOnSuccessListener { documents ->
                    progressBar.visibility = View.GONE
                    listings.clear()

                    if (documents.isEmpty) {
                        Log.d(TAG, "No listings found for user.")
                        tvNoListings.visibility = View.VISIBLE
                    } else {
                        listings.addAll(documents.mapNotNull { parseListing(it) })
                        if (listings.isEmpty()) {
                            tvNoListings.visibility = View.VISIBLE
                        } else {
                            listingsAdapter.notifyDataSetChanged()
                            rvListings.visibility = View.VISIBLE
                        }
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Error fetching listings: ", e)
                    showToast("Error fetching listings. Please try again.")
                    tvNoListings.visibility = View.VISIBLE
                }
        }
    }

    private fun deleteListing(listingId: String) {
        firestore.collection("listings").document(listingId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Listing deleted successfully: $listingId")
                showToast("Listing deleted successfully")

                listings.indexOfFirst { it.id == listingId }.takeIf { it != -1 }?.let { index ->
                    listings.removeAt(index)
                    listingsAdapter.notifyItemRemoved(index)
                    if (listings.isEmpty()) tvNoListings.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting listing: ", e)
                showToast("Failed to delete listing")
            }
    }

    private fun editListing(listing: Listing) {
        val intent = Intent(this, EditListingActivity::class.java).apply {
            putExtra("LISTING_ID", listing.id)
        }
        editListingLauncher.launch(intent)
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
                location = data["location"] as? String ?: "Unknown Location",
                ownerId = data["ownerId"] as? String ?: "",
                ownerName = data["ownerName"] as? String ?: "",
                ownerPhone = data["ownerPhone"] as? String ?: "",
                timestamp = data["timestamp"] as? com.google.firebase.Timestamp
            ).also {
                Log.d(TAG, "Successfully parsed listing: ${it.id}, ${it.productName}, ${it.rentPrice}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing listing ${document.id}: ${e.message}", e)
            null
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "ViewListingsActivity"
    }
}
