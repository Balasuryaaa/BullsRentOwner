package com.example.bullsrentowner

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Customer_detaillisting : BaseActivity() {

    private lateinit var productName: TextView
    private lateinit var rentType: TextView
    private lateinit var rentPrice: TextView
    private lateinit var description: TextView
    private lateinit var location: TextView
    private lateinit var ownerName: TextView
    private lateinit var ownerPhone: TextView
    private lateinit var viewPagerImages: ViewPager2
    private lateinit var btnContactCustomer: Button
    private lateinit var btnMakeBooking: Button

    private var listingId: String? = null
    private lateinit var sharedPreferences: SharedPreferences
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_detaillisting)

        // Initialize Views
        productName = findViewById(R.id.tvProductName)
        rentType = findViewById(R.id.tvRentType)
        rentPrice = findViewById(R.id.tvRentPrice)
        description = findViewById(R.id.tvDescription)
        location = findViewById(R.id.tvLocation)
        ownerName = findViewById(R.id.tvOwnerName)
        ownerPhone = findViewById(R.id.tvOwnerPhone)
        viewPagerImages = findViewById(R.id.viewPagerImages)
        btnContactCustomer = findViewById(R.id.btnContactCustomer)
        btnMakeBooking = findViewById(R.id.btnMakeBooking)

        // SharedPreferences to get Customer Mobile Number
        sharedPreferences = getSharedPreferences("CustomerProfile", MODE_PRIVATE)
        val customerMobile = sharedPreferences.getString("mobile", "Unknown") ?: "Unknown"

        // Set empty adapter initially
        viewPagerImages.adapter = ImagePagerAdapter(emptyList())

        // Get listingId from Intent
        listingId = intent.getStringExtra("listingId")
        Log.d(TAG, "Received listingId: $listingId")

        if (listingId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid listing data!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch listing details
        fetchListing(listingId!!)

        // Contact Owner
        btnContactCustomer.setOnClickListener {
            val phoneNumber = ownerPhone.text.toString()
            if (phoneNumber.isNotEmpty() && phoneNumber != "N/A") {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")))
            } else {
                Toast.makeText(this, "Owner phone number not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Make Booking with rent duration
        btnMakeBooking.setOnClickListener {
            val rentTypeText = rentType.text.toString().lowercase()

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enter Duration")

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_NUMBER
            builder.setView(input)

            when {
                rentTypeText.contains("hour") -> builder.setMessage("Enter the number of hours:")
                rentTypeText.contains("day") -> builder.setMessage("Enter the number of days:")
                else -> {
                    Toast.makeText(this, "Invalid rent type!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            builder.setPositiveButton("Confirm") { _, _ ->
                val duration = input.text.toString().trim()
                if (duration.isEmpty() || duration.toIntOrNull() == null || duration.toInt() <= 0) {
                    Toast.makeText(this, "Invalid duration!", Toast.LENGTH_SHORT).show()
                } else {
                    confirmBooking(customerMobile, duration, rentTypeText)
                }
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()
        }
    }

    private fun fetchListing(listingId: String) {
        Log.d(TAG, "Fetching listing with ID: $listingId")
        firestore.collection("listings")
            .document(listingId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        val data = document.data
                        if (data != null) {
                            Log.d(TAG, "Listing data: $data")
                            
                            // Create listing manually instead of using toObject
                            val listing = ListingCustomer(
                                productName = data["productName"] as? String,
                                rentType = data["rentType"] as? String,
                                rentPrice = when {
                                    data["rentPrice"] is Number -> (data["rentPrice"] as Number).toString()
                                    data["rentPrice"] is String -> data["rentPrice"] as String
                                    else -> "0"
                                },
                                description = data["description"] as? String,
                                location = data["location"] as? String,
                                ownerName = data["ownerName"] as? String,
                                ownerPhone = data["ownerPhone"] as? String,
                                imageUrls = when {
                                    data["imageUrls"] is List<*> -> (data["imageUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                                    else -> emptyList()
                                },
                                imageBase64 = when {
                                    data["imageBase64"] is List<*> -> (data["imageBase64"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                                    data["imageBase64"] is String -> listOf(data["imageBase64"] as String)
                                    else -> emptyList()
                                }
                            )
                            
                            Log.d(TAG, "Listing loaded successfully!")
                            showListing(listing)
                        } else {
                            Log.e(TAG, "Listing document data is null!")
                            showNotFound()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing listing data: ${e.message}", e)
                        showNotFound()
                    }
                } else {
                    Log.e(TAG, "Listing document does NOT exist!")
                    showNotFound()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching listing: ${e.message}", e)
                Toast.makeText(this, "Error fetching listing: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun showListing(listing: ListingCustomer) {
        productName.text = listing.productName ?: "N/A"
        rentType.text = listing.rentType ?: "N/A"
        rentPrice.text = if (listing.rentPrice != null) "₹${listing.rentPrice} / ${listing.rentType}" else "N/A"
        description.text = listing.description ?: "N/A"
        location.text = listing.location ?: "N/A"
        ownerName.text = listing.ownerName ?: "N/A"
        ownerPhone.text = listing.ownerPhone ?: "N/A"

        val imageItems = mutableListOf<Any>()
        listing.imageUrls?.let { imageItems.addAll(it.filter { it.isNotEmpty() }) }
        listing.imageBase64?.mapNotNullTo(imageItems) { base64StringToBitmap(it) }

        viewPagerImages.adapter = ImagePagerAdapter(imageItems)
    }

    private fun showNotFound() {
        Toast.makeText(this, "Listing not found! It may have been removed.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun confirmBooking(customerPhone: String, duration: String, rentType: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Booking")
            .setMessage("Do you want to book for $duration $rentType?")
            .setPositiveButton("Yes") { _, _ ->
                makeBooking(customerPhone, duration, rentType)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun makeBooking(customerPhone: String, duration: String, rentType: String) {
        val ownerPhoneNumber = ownerPhone.text.toString()
        val currentTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val bookingData = hashMapOf(
            "listingId" to listingId,
            "customerPhone" to customerPhone,
            "ownerPhone" to ownerPhoneNumber,
            "status" to "pending",
            "bookingTime" to currentTime,
            "duration" to duration,
            "rentType" to rentType
        )

        firestore.collection("bookings")
            .add(bookingData)
            .addOnSuccessListener {
                btnMakeBooking.text = "Booking Requested"
                btnMakeBooking.isEnabled = false
                Toast.makeText(this, "The owner has been notified. Check 'Manage Orders' for updates.", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Booking request failed: ${e.message}")
                Toast.makeText(this, "Failed to send booking request", Toast.LENGTH_SHORT).show()
            }
    }

    private fun base64StringToBitmap(encodedString: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(encodedString, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding Base64: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "CustomerDetailListing"
    }
}
