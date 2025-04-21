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
    private lateinit var tvAvailability: TextView

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
        tvAvailability = findViewById(R.id.tvAvailability)

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

            // First ask for booking date
            showDateSelectionDialog(rentTypeText)
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
        // Set basic listing details with proper formatting
        productName.text = listing.productName ?: "N/A"
        
        // Format rent type and price with better presentation
        val formattedRentType = listing.rentType?.capitalize() ?: "N/A"
        rentType.text = "Rent Basis: $formattedRentType"
        
        // Format price with currency and rent basis
        rentPrice.text = when {
            !listing.rentPrice.isNullOrEmpty() -> {
                val price = listing.rentPrice!!  // Safe call since we checked for null
                val basis = listing.rentType?.lowercase() ?: ""
                when {
                    basis.contains("hour") -> "₹$price per hour"
                    basis.contains("day") -> "₹$price per day"
                    else -> "₹$price"
                }
            }
            else -> "Price not available"
        }

        // Format description with proper sections
        val descriptionText = buildString {
            append("Description:\n")
            append(listing.description ?: "No description available")
            append("\n\nLocation: ")
            append(listing.location ?: "Location not specified")
        }
        description.text = descriptionText

        // Format location for better visibility
        location.text = "📍 ${listing.location ?: "Location not available"}"

        // Format owner details with proper labels
        ownerName.text = "Owner: ${listing.ownerName ?: "N/A"}"
        ownerPhone.text = "Contact: ${listing.ownerPhone ?: "N/A"}"

        // Set up image slider
        val imageItems = mutableListOf<Any>()
        
        // Add URLs first if available
        listing.imageUrls?.let { urls ->
            imageItems.addAll(urls.filter { it.isNotEmpty() })
        }
        
        // Add Base64 images if available
        listing.imageBase64?.forEach { base64String ->
            base64StringToBitmap(base64String)?.let {
                imageItems.add(it)
            }
        }

        // Set up ViewPager with images
        if (imageItems.isNotEmpty()) {
        viewPagerImages.adapter = ImagePagerAdapter(imageItems)
            viewPagerImages.visibility = android.view.View.VISIBLE
        } else {
            viewPagerImages.visibility = android.view.View.GONE
            // Show a placeholder or message when no images are available
            Toast.makeText(this, "No images available for this listing", Toast.LENGTH_SHORT).show()
        }

        // Update button states and labels
        btnContactCustomer.text = "Contact Owner"
        btnContactCustomer.isEnabled = !listing.ownerPhone.isNullOrEmpty()
        
        btnMakeBooking.text = "Book Now"
        btnMakeBooking.isEnabled = true

        // Show availability status
        checkCurrentAvailability(listing)
    }

    private fun checkCurrentAvailability(listing: ListingCustomer) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        firestore.collection("bookings")
            .whereEqualTo("listingId", listingId)
            .whereIn("status", listOf("approved", "pending"))
            .whereGreaterThanOrEqualTo("endDate", currentDate)
            .get()
            .addOnSuccessListener { bookings ->
                if (bookings.isEmpty) {
                    tvAvailability.text = "✅ Currently Available"
                    tvAvailability.setTextColor(getColor(android.R.color.holo_green_dark))
                } else {
                    // Check if there are any current bookings
                    val currentBookings = bookings.documents.filter { doc ->
                        val startDate = doc.getString("startDate") ?: ""
                        val endDate = doc.getString("endDate") ?: ""
                        currentDate in startDate..endDate
                    }
                    
                    if (currentBookings.isNotEmpty()) {
                        tvAvailability.text = "🔴 Currently Booked"
                        tvAvailability.setTextColor(getColor(android.R.color.holo_red_dark))
                    } else {
                        tvAvailability.text = "✅ Currently Available"
                        tvAvailability.setTextColor(getColor(android.R.color.holo_green_dark))
                    }
                }
                tvAvailability.visibility = android.view.View.VISIBLE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking availability: ${e.message}")
            }
    }

    private fun showNotFound() {
        val errorMessage = "This listing is no longer available. It may have been removed or deactivated by the owner."
        AlertDialog.Builder(this)
            .setTitle("Listing Not Found")
            .setMessage(errorMessage)
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun showDateSelectionDialog(rentTypeText: String) {
        // Create DatePicker dialog
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = android.app.DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Format the selected date
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val selectedDate = dateFormat.format(calendar.time)
                
                // Now show duration dialog
                showDurationDialog(rentTypeText, selectedDate)
            },
            year, month, day
        )
        
        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.setTitle("Select Booking Start Date")
        datePickerDialog.show()
    }

    private fun showDurationDialog(rentTypeText: String, startDate: String) {
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
                return
            }
        }

        builder.setPositiveButton("Confirm") { _, _ ->
            val duration = input.text.toString().trim()
            if (duration.isEmpty() || duration.toIntOrNull() == null || duration.toInt() <= 0) {
                Toast.makeText(this, "Invalid duration!", Toast.LENGTH_SHORT).show()
            } else {
                if (rentTypeText.contains("hour")) {
                    // For hourly rentals, ask for start time
                    showTimePickerDialog(startDate, duration.toInt(), rentTypeText)
                } else {
                    // For daily rentals, continue with date-based booking
                    val endDate = calculateEndDate(startDate, duration.toInt(), rentTypeText)
                    confirmBooking(sharedPreferences.getString("mobile", "Unknown") ?: "Unknown", 
                        duration, rentTypeText, startDate, endDate, null, null)
                }
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
    
    private fun showTimePickerDialog(startDate: String, durationHours: Int, rentTypeText: String) {
        // Create TimePicker dialog
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        val timePickerDialog = android.app.TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                // Format the selected time
                val startTimeStr = String.format("%02d:%02d", selectedHour, selectedMinute)
                
                // Calculate end time
                val endTimeCalendar = Calendar.getInstance()
                endTimeCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                endTimeCalendar.set(Calendar.MINUTE, selectedMinute)
                endTimeCalendar.add(Calendar.HOUR_OF_DAY, durationHours)
                
                val endTimeStr = String.format("%02d:%02d", 
                    endTimeCalendar.get(Calendar.HOUR_OF_DAY),
                    endTimeCalendar.get(Calendar.MINUTE))
                
                // Check if booking spans multiple days
                val endDate = if (endTimeCalendar.get(Calendar.HOUR_OF_DAY) < selectedHour) {
                    // If end hour is less than start hour, it means we've moved to next day
                    val calendar = Calendar.getInstance()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    calendar.time = dateFormat.parse(startDate) ?: Date()
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    dateFormat.format(calendar.time)
                } else {
                    startDate
                }
                
                confirmBooking(
                    sharedPreferences.getString("mobile", "Unknown") ?: "Unknown",
                    durationHours.toString(), rentTypeText, startDate, endDate, startTimeStr, endTimeStr
                )
            },
            hour, minute, true
        )
        
        timePickerDialog.setTitle("Select Start Time")
        timePickerDialog.show()
    }
    
    private fun calculateEndDate(startDate: String, duration: Int, rentTypeText: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(startDate) ?: Date()
        
        when {
            rentTypeText.contains("hour") -> {
                // For hours, if it's same day, just store same end date
                // Only advance the date if hours would extend to next day
                val hoursInDay = 24
                val daysToAdd = duration / hoursInDay
                if (daysToAdd > 0) {
                    calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
                }
            }
            rentTypeText.contains("day") -> {
                calendar.add(Calendar.DAY_OF_MONTH, duration)
            }
        }
        
        return dateFormat.format(calendar.time)
    }

    private fun confirmBooking(
        customerPhone: String, 
        duration: String, 
        rentType: String, 
        startDate: String, 
        endDate: String,
        startTime: String?,
        endTime: String?
    ) {
        val bookingSummary = if (rentType.contains("hour") && startTime != null) {
            "Do you want to book for $duration $rentType on $startDate from $startTime?"
        } else {
            "Do you want to book for $duration $rentType starting on $startDate?"
        }
        
        // Calculate total amount
        val totalAmount = calculateTotalAmount(duration, rentType)
        val advanceAmount = totalAmount * 0.5 // 50% advance
        
        AlertDialog.Builder(this)
            .setTitle("Confirm Booking")
            .setMessage("$bookingSummary\n\nTotal Amount: ₹$totalAmount")
            .setPositiveButton("Pay Full Amount") { _, _ ->
                startPaymentActivity(totalAmount, customerPhone, duration, rentType, startDate, endDate, startTime, endTime, "full")
            }
            .setNeutralButton("Pay Advance (50%)") { _, _ ->
                startPaymentActivity(advanceAmount, customerPhone, duration, rentType, startDate, endDate, startTime, endTime, "advance")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun calculateTotalAmount(duration: String, rentType: String): Double {
        val priceText = rentPrice.text.toString()
        val price = priceText.replace("₹", "").replace(" per hour", "").replace(" per day", "").trim().toDoubleOrNull() ?: 0.0
        val durationValue = duration.toIntOrNull() ?: 0
        
        return when {
            rentType.contains("hour") -> price * durationValue
            rentType.contains("day") -> price * durationValue
            else -> 0.0
        }
    }

    private fun startPaymentActivity(
        amount: Double,
        customerPhone: String,
        duration: String,
        rentType: String,
        startDate: String,
        endDate: String,
        startTime: String?,
        endTime: String?,
        paymentType: String
    ) {
        val ownerPhoneNumber = ownerPhone.text.toString()
        val productNameText = productName.text.toString()

        // Start payment activity with the booking details
        val intent = Intent(this, PaymentActivity::class.java).apply {
            putExtra("amount", amount)
            putExtra("paymentType", paymentType)
            putExtra("listingName", productNameText)
            putExtra("duration", duration)
            putExtra("rentType", rentType)
            putExtra("customerPhone", customerPhone)
            putExtra("ownerPhone", ownerPhoneNumber)
            putExtra("startDate", startDate)
            putExtra("endDate", endDate)
            putExtra("startTime", startTime)
            putExtra("endTime", endTime)
            putExtra("listingId", listingId)
        }
        startActivityForResult(intent, PAYMENT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PAYMENT_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                RESULT_CANCELED -> {
                    Toast.makeText(this, "Payment cancelled", Toast.LENGTH_SHORT).show()
                }
            }
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
        private const val PAYMENT_REQUEST_CODE = 1001
    }
}
