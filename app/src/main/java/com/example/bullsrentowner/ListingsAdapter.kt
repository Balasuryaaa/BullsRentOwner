package com.example.bullsrentowner

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.app.AlertDialog
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.viewpager2.widget.ViewPager2

class ListingsAdapter(
    private val context: Context,
    private var listings: MutableList<Listing>,
    private val onDeleteClick: (Listing) -> Unit,
    private val onEditClick: (Listing) -> Unit
) : RecyclerView.Adapter<ListingsAdapter.ViewHolder>() {

    private var fullListings: MutableList<Listing> = ArrayList(listings) // Copy for filtering

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productName: TextView = view.findViewById(R.id.tvProductName)
        val rentPrice: TextView = view.findViewById(R.id.tvRentPrice)
        val rentType: TextView = view.findViewById(R.id.tvRentType)
        val description: TextView = view.findViewById(R.id.tvDescription)
        val location: TextView = view.findViewById(R.id.tvLocation)
        val viewPagerImages: ViewPager2 = view.findViewById(R.id.viewPagerImages)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_listing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listing = listings[position]
        
        holder.productName.text = listing.productName
        holder.rentPrice.text = "₹${listing.rentPrice}"
        holder.rentType.text = listing.rentType
        holder.description.text = listing.description
        holder.location.text = "📍 ${listing.location}"

        // Setup image slider
        val imageAdapter = ImagePagerAdapter(listing.imageBase64)
        holder.viewPagerImages.adapter = imageAdapter

        // Setup buttons
        holder.btnEdit.setOnClickListener { onEditClick(listing) }
        holder.btnDelete.setOnClickListener { 
            onDeleteClick(listing)
            notifyItemRemoved(position)
        }
    }

    override fun getItemCount() = listings.size

    /** **🔍 Filter Listings (Search by Name & Sort by Price)** **/
    fun filterListings(query: String, sortAscending: Boolean?) {
        listings = if (query.isEmpty()) {
            fullListings.toMutableList()
        } else {
            fullListings.filter { it.productName.contains(query, ignoreCase = true) }.toMutableList()
        }

        // Sorting Logic
        sortAscending?.let {
            listings.sortBy { it.rentPrice.toDouble() }
            if (!sortAscending) listings.reverse()
        }

        notifyDataSetChanged()
    }
    fun updateListings(newListings: List<Listing>) {
        listings.clear()
        listings.addAll(newListings)
        notifyDataSetChanged()
    }

    fun removeListing(position: Int) {
        if (position in 0 until listings.size) {
            listings.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, listings.size)
        }
    }
}
