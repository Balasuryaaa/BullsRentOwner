package com.example.bullsrentowner

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AllListingsAdapter(
    private val context: Context,
    private var allListings: MutableList<Listing>
) : RecyclerView.Adapter<AllListingsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAllListingName: TextView = view.findViewById(R.id.tvAllListingName)
        val tvAllRentPrice: TextView = view.findViewById(R.id.tvAllRentPrice)
        val tvAllRentType: TextView = view.findViewById(R.id.tvAllRentType)
        val ivAllListingImage: ImageView = view.findViewById(R.id.ivAllListingImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_all_listings, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listing = allListings[position]

        holder.tvAllListingName.text = listing.productName ?: "Unknown"
        holder.tvAllRentPrice.text = "₹${listing.rentPrice ?: "0"}"
        holder.tvAllRentType.text = "/ ${listing.rentType ?: "N/A"}"

        // Load image (URL or Base64)
        loadListingImage(holder.ivAllListingImage, listing)

        // Open detailed view on click
        holder.itemView.setOnClickListener {
            listing.id?.let { id ->
                Log.d(TAG, "Opening details for Listing ID: $id")
                val intent = Intent(context, Customer_detaillisting::class.java).apply {
                    putExtra("listingId", id)
                }
                context.startActivity(intent)
            } ?: Log.e(TAG, "Listing ID is null!")
        }
    }

    override fun getItemCount(): Int = allListings.size

    /**
     * Efficiently updates the list using DiffUtil.
     */
    fun updateList(newList: List<Listing>) {
        val diffCallback = ListingsDiffCallback(allListings, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        allListings.clear()
        allListings.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    private fun loadListingImage(imageView: ImageView, listing: Listing) {
        when {
            listing.imageUrls?.isNotEmpty() == true && !listing.imageUrls[0].isNullOrBlank() -> {
                Glide.with(context)
                    .load(listing.imageUrls[0])
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .fallback(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(imageView)
            }
            listing.imageBase64?.isNotEmpty() == true && !listing.imageBase64[0].isNullOrBlank() -> {
                base64ToBitmap(listing.imageBase64[0])?.let {
                    imageView.setImageBitmap(it)
                } ?: imageView.setImageResource(R.drawable.placeholder_image)
            }
            else -> {
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        }
    }

    private fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Base64 decoding failed: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "AllListingsAdapter"
    }
}

/**
 * DiffUtil Callback for efficient RecyclerView updates
 */
class ListingsDiffCallback(
    private val oldList: List<Listing>,
    private val newList: List<Listing>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
