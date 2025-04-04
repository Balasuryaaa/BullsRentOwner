package com.example.bullsrentowner

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class ListingDetailsActivity : AppCompatActivity() {

    private lateinit var tvListingName: TextView
    private lateinit var tvOwnerName: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvRentPrice: TextView
    private lateinit var tvRentType: TextView
    private lateinit var ivListingImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listing_details)

        // Initialize UI elements
        tvListingName = findViewById(R.id.tvListingName)
        tvOwnerName = findViewById(R.id.tvOwnerName)
        tvDescription = findViewById(R.id.tvDescription)
        tvRentPrice = findViewById(R.id.tvRentPrice)
        tvRentType = findViewById(R.id.tvRentType)
        ivListingImage = findViewById(R.id.ivListingImage)

        val listingId = intent.getStringExtra("LISTING_ID")

        if (listingId != null) {
            fetchListingDetails(listingId)
        } else {
            Toast.makeText(this, "Listing ID not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchListingDetails(listingId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("listings").document(listingId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val listingName = document.getString("productName") ?: "N/A"
                    val ownerName = document.getString("ownerName") ?: "N/A"
                    val description = document.getString("description") ?: "No description available"
                    val rentPrice = document.getDouble("rentPrice") ?: 0.0
                    val rentType = document.getString("rentType") ?: "N/A"
                    val imageUrl = document.getString("imageUrl") ?: ""

                    // Set data to views
                    tvListingName.text = listingName
                    tvOwnerName.text = "Owner: $ownerName"
                    tvDescription.text = description
                    tvRentPrice.text = "₹$rentPrice"
                    tvRentType.text = "/ $rentType"

                    // Load image using Glide
                    if (imageUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(imageUrl)
                            .into(ivListingImage)
                    }
                } else {
                    Toast.makeText(this, "Listing not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching listing details", Toast.LENGTH_SHORT).show()
            }
    }
}
