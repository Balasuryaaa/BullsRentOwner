package com.example.bullsrentowner

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.util.*

class AddListingActivity : AppCompatActivity() {

    private lateinit var etProductName: EditText
    private lateinit var etRentPrice: EditText
    private lateinit var etDescription: EditText
    private lateinit var spinnerEquipmentType: Spinner
    private lateinit var spinnerRentType: Spinner
    private lateinit var btnSelectImages: Button
    private lateinit var btnUploadListing: Button
    private lateinit var rvImages: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvLocation: TextView
    private lateinit var tvOwnerName: TextView
    private lateinit var tvOwnerPhone: TextView

    private val imageUris = mutableListOf<Uri>()
    private lateinit var imageAdapter: ImageAdapter
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var firestore: FirebaseFirestore
    private val base64Images = mutableListOf<String>()
    private lateinit var auth: FirebaseAuth

    private var userPhone: String? = null
    private var ownerName: String = ""
    private var location: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_listing)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Retrieve phone number from intent
        userPhone = intent.getStringExtra("USER_PHONE")

        if (userPhone.isNullOrEmpty()) {
            showToast("Error: No phone number provided.")
            finish()
            return
        }

        initializeUI()
        fetchUserProfile()
    }

    private fun initializeUI() {
        etProductName = findViewById(R.id.etProductName)
        etRentPrice = findViewById(R.id.etRentPrice)
        etDescription = findViewById(R.id.etDescription)
        spinnerEquipmentType = findViewById(R.id.spinnerEquipmentType)
        spinnerRentType = findViewById(R.id.spinnerRentType)
        btnSelectImages = findViewById(R.id.btnSelectImages)
        btnUploadListing = findViewById(R.id.btnUploadListing)
        rvImages = findViewById(R.id.rvImages)
        progressBar = findViewById(R.id.progressBar)

        tvLocation = findViewById(R.id.tvLocation)
        tvOwnerName = findViewById(R.id.tvOwnerName)
        tvOwnerPhone = findViewById(R.id.tvOwnerPhone)

        progressBar.visibility = View.GONE
        btnUploadListing.isEnabled = false

        // Setup Image Adapter
        imageAdapter = ImageAdapter(this, imageUris) { position ->
            imageUris.removeAt(position)
            imageAdapter.notifyDataSetChanged()
        }

        rvImages.layoutManager = GridLayoutManager(this, 2)
        rvImages.adapter = imageAdapter

        btnSelectImages.setOnClickListener { openGallery() }
        btnUploadListing.setOnClickListener { uploadListing() }

        // Setup Equipment Type Spinner
        ArrayAdapter.createFromResource(
            this, R.array.equipment_types, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerEquipmentType.adapter = adapter
        }

        // Setup Rent Type Spinner
        ArrayAdapter.createFromResource(
            this, R.array.rent_types, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerRentType.adapter = adapter
        }
    }

    private fun fetchUserProfile() {
        progressBar.visibility = View.VISIBLE

        firestore.collection("users").document(userPhone!!)
            .get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE

                if (document.exists()) {
                    ownerName = document.getString("name") ?: "N/A"
                    location = document.getString("location") ?: "Unknown"

                    tvOwnerName.text = "Owner: $ownerName"
                    tvLocation.text = "Location: $location"
                    tvOwnerPhone.text = "Phone: $userPhone"
                } else {
                    showToast("No user found with this phone number.")
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e("FirestoreError", "Error fetching user profile: ${e.message}")
                showToast("Failed to fetch user data.")
            }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.clipData?.let {
                for (i in 0 until minOf(it.itemCount, 4 - imageUris.size)) {
                    val uri = it.getItemAt(i).uri
                    if (!imageUris.contains(uri)) {
                        imageUris.add(uri)
                    }
                }
            } ?: data?.data?.let { uri ->
                if (!imageUris.contains(uri) && imageUris.size < 4) {
                    imageUris.add(uri)
                }
            }
            imageAdapter.notifyDataSetChanged()
            compressAndConvertToBase64()
        }
    }

    private fun compressAndConvertToBase64() {
        base64Images.clear()
        progressBar.visibility = View.VISIBLE

        for (uri in imageUris) {
            val bitmap = uriToBitmap(uri) ?: continue
            val compressedBitmap = resizeBitmap(bitmap, 800, 800)
            val base64String = encodeBitmapToBase64(compressedBitmap)

            if (base64String.length < 750 * 1024) {
                base64Images.add(base64String)
            } else {
                showToast("One or more images exceed size limit. Please select smaller images.")
                progressBar.visibility = View.GONE
                return
            }
        }

        progressBar.visibility = View.GONE
        btnUploadListing.isEnabled = base64Images.isNotEmpty()
        showToast("Images ready for upload!")
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e("ImageError", "Error decoding URI: ${e.message}")
            null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scaleFactor = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        return Bitmap.createScaledBitmap(bitmap, (width * scaleFactor).toInt(), (height * scaleFactor).toInt(), true)
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun uploadListing() {
        val productName = etProductName.text.toString().trim()
        val rentPrice = etRentPrice.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val equipmentType = spinnerEquipmentType.selectedItem.toString()
        val rentType = spinnerRentType.selectedItem.toString()

        if (productName.isEmpty() || rentPrice.isEmpty() || description.isEmpty()) {
            showToast("Please fill all fields")
            return
        }

        // ✅ Generate Random Listing ID
        val listingId = UUID.randomUUID().toString()

        val listingData = hashMapOf(
            "listingId" to listingId,
            "productName" to productName,
            "rentType" to rentType,
            "equipmentType" to equipmentType,
            "rentPrice" to rentPrice,
            "description" to description,
            "imageBase64" to base64Images,
            "timestamp" to System.currentTimeMillis(),
            "location" to location,
            "ownerName" to ownerName,
            "ownerPhone" to userPhone
        )

        // ✅ Store it using the listingId as Document ID
        firestore.collection("listings").document(listingId).set(listingData)
            .addOnSuccessListener {
                showToast("Listing uploaded successfully!\nID: $listingId")
                finish()
            }
            .addOnFailureListener {
                showToast("Failed to upload listing: ${it.message}")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
