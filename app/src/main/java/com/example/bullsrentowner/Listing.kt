package com.example.bullsrentowner

import android.util.Log
import com.google.firebase.Timestamp

data class Listing(
    var id: String = "",
    val productName: String = "",
    val rentType: String = "",
    val equipmentType: String = "",
    val rentPrice: Double = 0.0, // Changed from nullable to avoid null checks
    val description: String = "",
    val imageUrls: List<String> = emptyList(), // Firebase image URLs
    val imageBase64: List<String> = emptyList(), // Default to empty list
    val location: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerPhone: String = "",
    val timestamp: Timestamp? = null
) {

    fun isValid(): Boolean {
        val errors = mutableListOf<String>()

        if (id.isEmpty()) errors.add("❌ Listing ID is missing.")
        if (productName.trim().isEmpty()) errors.add("❌ Product Name is missing.")
        if (rentType.trim().isEmpty()) errors.add("❌ Rent Type is missing.")
        // Make equipment type validation optional for backward compatibility
        // if (equipmentType.trim().isEmpty()) errors.add("❌ Equipment Type is missing.")
        if (rentPrice <= 0) errors.add("❌ Rent Price must be greater than zero.")
        if (location.trim().isEmpty()) errors.add("❌ Location is missing.")
        if (ownerPhone.trim().isEmpty()) errors.add("❌ Owner Phone is missing.")

        return if (errors.isEmpty()) {
            Log.d("ListingValidation", "✅ Valid Listing: $this")
            true
        } else {
            errors.forEach { Log.e("ListingValidation", it) }
            false
        }
    }

    override fun toString(): String {
        return "Listing(id='$id', productName='$productName', equipmentType='$equipmentType', rentType='$rentType', rentPrice=$rentPrice, " +
                "location='$location', ownerName='$ownerName', ownerPhone='$ownerPhone', timestamp=$timestamp)"
    }
}
