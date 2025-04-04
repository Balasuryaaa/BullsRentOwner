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

class ListingsAdapter(
    private val context: Context,
    private var listings: MutableList<Listing>,
    private val onDeleteClick: (Listing) -> Unit
) : RecyclerView.Adapter<ListingsAdapter.ViewHolder>() {

    private var fullListings: MutableList<Listing> = ArrayList(listings) // Copy for filtering

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvListingName: TextView = view.findViewById(R.id.tvListingName)
        val tvRentPrice: TextView = view.findViewById(R.id.tvRentPrice)
        val tvRentType: TextView = view.findViewById(R.id.tvRentType)
        val ivListingImage: ImageView = view.findViewById(R.id.ivListingImage)
        val ivDelete: ImageButton = view.findViewById(R.id.ivDelete)
        val extraDetailsLayout: View = view.findViewById(R.id.extraDetailsLayout)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvOwnerName: TextView = view.findViewById(R.id.tvOwnerName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_listing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listing = listings[position]

        // Set text values
        holder.tvListingName.text = listing.productName
        holder.tvRentPrice.text = "₹${listing.rentPrice}"
        holder.tvRentType.text = "/ ${listing.rentType}"
        holder.tvDescription.text = listing.description
        holder.tvOwnerName.text = "Owner: ${listing.ownerName}"

        // Load Image (Glide for URL, Base64 fallback)
        if (listing.imageUrls.isNotEmpty()) {
            Glide.with(context)
                .load(listing.imageUrls[0])
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(holder.ivListingImage)
        } else if (listing.imageBase64.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(listing.imageBase64[0], Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.ivListingImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.ivListingImage.setImageResource(R.drawable.placeholder_image)
            }
        } else {
            holder.ivListingImage.setImageResource(R.drawable.placeholder_image)
        }

        // **Toggle More Info (Animated Expand/Collapse)**
        holder.itemView.setOnClickListener {
            val expandAnim = AnimationUtils.loadAnimation(context, R.anim.expand)
            val collapseAnim = AnimationUtils.loadAnimation(context, R.anim.collapse)

            if (holder.extraDetailsLayout.visibility == View.VISIBLE) {
                holder.extraDetailsLayout.startAnimation(collapseAnim)
                holder.extraDetailsLayout.visibility = View.GONE
            } else {
                holder.extraDetailsLayout.startAnimation(expandAnim)
                holder.extraDetailsLayout.visibility = View.VISIBLE
            }
        }

        // **Delete Confirmation Dialog**
        holder.ivDelete.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Delete Listing")
                .setMessage("Are you sure you want to delete '${listing.productName}'?")
                .setPositiveButton("Delete") { _, _ ->
                    onDeleteClick(listing)
                    removeListing(position)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun getItemCount() = listings.size

    /** Remove listing and update RecyclerView **/
    private fun removeListing(position: Int) {
        listings.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, listings.size)
    }

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
    fun updateList(newList: List<Listing>) {
        listings.clear()
        listings.addAll(newList)
        notifyDataSetChanged()
    }

}
