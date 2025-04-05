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
                    try {
                        val data = document.data
                        if (data != null) {
                            val listingName = data["productName"] as? String ?: "N/A"
                            val ownerName = data["ownerName"] as? String ?: "N/A"
                            val description = data["description"] as? String ?: "No description available"
                            
                            // Handle different rentPrice formats
                            val rentPrice = when {
                                data["rentPrice"] is Number -> (data["rentPrice"] as Number).toDouble()
                                data["rentPrice"] is String -> (data["rentPrice"] as String).toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            }
                            
                            val rentType = data["rentType"] as? String ?: "N/A"
                            
                            // Handle image from multiple possible sources
                            val imageBase64 = when {
                                data["imageBase64"] is List<*> && (data["imageBase64"] as List<*>).isNotEmpty() -> 
                                    (data["imageBase64"] as List<*>)[0] as? String
                                data["imageBase64"] is String -> data["imageBase64"] as String
                                data["imageUrl"] is String -> data["imageUrl"] as String
                                else -> ""
                            }

                            // Set data to views
                            tvListingName.text = listingName
                            tvOwnerName.text = "Owner: $ownerName"
                            tvDescription.text = description
                            tvRentPrice.text = "₹$rentPrice"
                            tvRentType.text = "/ $rentType"

                            // Load image
                            if (imageBase64?.isNotEmpty() == true) {
                                try {
                                    val decodedString = android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT)
                                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                    ivListingImage.setImageBitmap(bitmap)
                                } catch (e: Exception) {
                                    // If not Base64, try loading as URL
                                    Glide.with(this)
                                        .load(imageBase64)
                                        .placeholder(R.drawable.placeholder_image)
                                        .error(R.drawable.placeholder_image)
                                        .into(ivListingImage)
                                }
                            }
                        } else {
                            Toast.makeText(this, "Listing data is null", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error parsing listing: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Listing not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching listing details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
