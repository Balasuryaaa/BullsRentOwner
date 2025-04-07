package com.example.bullsrentowner

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.util.*

class EditListingActivity : AppCompatActivity() {

    private lateinit var etProductName: TextInputEditText
    private lateinit var etRentPrice: TextInputEditText
    private lateinit var spinnerRentType: Spinner
    private lateinit var etDescription: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var viewPagerImages: ViewPager2
    private lateinit var btnAddImage: Button
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    private val db = FirebaseFirestore.getInstance()
    private var listingId: String? = null
    private var currentImages = mutableListOf<String>() // Base64 strings
    private lateinit var imageAdapter: ImagePagerAdapter

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                addImageFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_listing)

        initViews()
        setupSpinner()
        setupImagePager()

        listingId = intent.getStringExtra("LISTING_ID")
        if (listingId != null) {
            loadListingData()
        }

        setupClickListeners()
    }

    private fun initViews() {
        etProductName = findViewById(R.id.etProductName)
        etRentPrice = findViewById(R.id.etRentPrice)
        spinnerRentType = findViewById(R.id.spinnerRentType)
        etDescription = findViewById(R.id.etDescription)
        etLocation = findViewById(R.id.etLocation)
        viewPagerImages = findViewById(R.id.viewPagerImages)
        btnAddImage = findViewById(R.id.btnAddImage)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupSpinner() {
        val rentTypes = arrayOf("per hour", "per day")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, rentTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRentType.adapter = adapter
    }

    private fun setupImagePager() {
        imageAdapter = ImagePagerAdapter(currentImages)
        viewPagerImages.adapter = imageAdapter
    }

    private fun setupClickListeners() {
        btnAddImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            getContent.launch(intent)
        }

        btnSave.setOnClickListener {
            if (validateInputs()) {
                saveListing()
            }
        }
    }

    private fun loadListingData() {
        progressBar.visibility = View.VISIBLE
        listingId?.let { id ->
            db.collection("listings").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        etProductName.setText(document.getString("productName"))
                        etRentPrice.setText(document.get("rentPrice").toString())
                        etDescription.setText(document.getString("description"))
                        etLocation.setText(document.getString("location"))

                        // Set rent type in spinner
                        val rentType = document.getString("rentType") ?: "per hour"
                        val position = (spinnerRentType.adapter as ArrayAdapter<String>)
                            .getPosition(rentType)
                        spinnerRentType.setSelection(position)

                        // Load images
                        when (val images = document.get("imageBase64")) {
                            is List<*> -> currentImages.addAll(images.filterIsInstance<String>())
                            is String -> currentImages.add(images)
                        }
                        imageAdapter.notifyDataSetChanged()
                    }
                    progressBar.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading listing: ", e)
                    showToast("Error loading listing details")
                    progressBar.visibility = View.GONE
                }
        }
    }

    private fun addImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val resizedBitmap = resizeBitmap(bitmap, 800) // Resize to max width of 800px
            val base64String = bitmapToBase64(resizedBitmap)
            currentImages.add(base64String)
            imageAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image: ", e)
            showToast("Error processing image")
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth) return bitmap
        
        val ratio = width.toFloat() / height.toFloat()
        val newWidth = maxWidth
        val newHeight = (maxWidth / ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (etProductName.text.isNullOrBlank()) {
            (etProductName.parent.parent as? TextInputLayout)?.error = "Product name is required"
            isValid = false
        }

        if (etRentPrice.text.isNullOrBlank()) {
            (etRentPrice.parent.parent as? TextInputLayout)?.error = "Rent price is required"
            isValid = false
        }

        if (etDescription.text.isNullOrBlank()) {
            (etDescription.parent.parent as? TextInputLayout)?.error = "Description is required"
            isValid = false
        }

        if (etLocation.text.isNullOrBlank()) {
            (etLocation.parent.parent as? TextInputLayout)?.error = "Location is required"
            isValid = false
        }

        if (currentImages.isEmpty()) {
            showToast("Please add at least one image")
            isValid = false
        }

        return isValid
    }

    private fun saveListing() {
        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        val listingData = hashMapOf(
            "productName" to etProductName.text.toString(),
            "rentPrice" to etRentPrice.text.toString(),
            "rentType" to spinnerRentType.selectedItem.toString(),
            "description" to etDescription.text.toString(),
            "location" to etLocation.text.toString(),
            "imageBase64" to currentImages,
            "lastUpdated" to Date()
        )

        val operation = if (listingId != null) {
            db.collection("listings").document(listingId!!)
                .update(listingData as Map<String, Any>)
        } else {
            db.collection("listings").add(listingData)
        }

        operation
            .addOnSuccessListener {
                showToast("Listing saved successfully")
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving listing: ", e)
                showToast("Error saving listing")
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "EditListingActivity"
    }
} 