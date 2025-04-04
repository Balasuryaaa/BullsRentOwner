package com.example.bullsrentowner

import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImagePagerAdapter(private val images: List<Any>) : RecyclerView.Adapter<ImagePagerAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.imageViewBase64)

        fun bind(image: Any) {
            when (image) {
                is String -> { // Load from URL
                    Glide.with(imageView.context)
                        .load(image)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(imageView)
                    Log.d(TAG, "Loaded URL Image: $image")
                }
                is Bitmap -> { // Load from Bitmap (Base64 converted)
                    imageView.setImageBitmap(image)
                    Log.d(TAG, "Loaded Base64 Bitmap Image")
                }
                else -> { // Fallback
                    imageView.setImageResource(R.drawable.placeholder_image)
                    Log.e(TAG, "Unknown image type detected")
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_base64_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    companion object {
        private const val TAG = "ImagePagerAdapter"
    }
}
