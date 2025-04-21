package com.example.bullsrentowner

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class PaymentActivity : AppCompatActivity(), PaymentResultListener {
    private lateinit var progressBar: ProgressBar
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "PaymentActivity"
    private var checkout: Checkout? = null
    private var amount: Double = 0.0
    private var paymentType: String? = null
    private var listingName: String? = null
    private var duration: String? = null
    private var rentType: String? = null
    private var customerPhone: String? = null
    private var ownerPhone: String? = null
    private var startDate: String? = null
    private var endDate: String? = null
    private var startTime: String? = null
    private var endTime: String? = null
    private var listingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // Initialize Razorpay Checkout
        Checkout.preload(applicationContext)
        checkout = Checkout()

        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        // Get payment details from intent
        amount = intent.getDoubleExtra("amount", 0.0)
        paymentType = intent.getStringExtra("paymentType")
        listingName = intent.getStringExtra("listingName")
        duration = intent.getStringExtra("duration")
        rentType = intent.getStringExtra("rentType")
        customerPhone = intent.getStringExtra("customerPhone")
        ownerPhone = intent.getStringExtra("ownerPhone")
        startDate = intent.getStringExtra("startDate")
        endDate = intent.getStringExtra("endDate")
        startTime = intent.getStringExtra("startTime")
        endTime = intent.getStringExtra("endTime")
        listingId = intent.getStringExtra("listingId")

        Log.d(TAG, "Received payment details - Amount: $amount, Type: $paymentType")

        // Validate amount
        if (amount <= 0) {
            Log.e(TAG, "Invalid amount: $amount")
            showError("Please enter a valid amount greater than 0")
            finish()
            return
        }

        startPayment()
    }

    private fun startPayment() {
        try {
            Log.d(TAG, "Starting payment with amount: $amount")
            
            // Convert amount to paise (smallest currency unit)
            val amountInPaise = (amount * 100).toInt()
            Log.d(TAG, "Amount in paise: $amountInPaise")

            // Validate minimum amount (Razorpay minimum is 1 INR)
            if (amountInPaise < 100) {
                showError("Minimum payment amount is 1 INR")
                finish()
                return
            }

            val options = JSONObject().apply {
                put("name", "Bulls Rent")
                put("description", getPaymentDescription())
                put("currency", "INR")
                put("amount", amountInPaise)
                put("prefill", JSONObject().apply {
                    put("email", auth.currentUser?.email ?: "")
                    put("contact", auth.currentUser?.phoneNumber ?: "")
                })
            }

            Log.d(TAG, "Payment options: ${options.toString(2)}")
            checkout?.setKeyID("rzp_test_dsJ0S4SOsOEhJD")
            checkout?.open(this, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting payment: ${e.message}")
            e.printStackTrace()
            showError("Error initiating payment: ${e.message}")
            finish()
        }
    }

    private fun getPaymentDescription(): String {
        return when (paymentType) {
            "full" -> "Full payment for $listingName ($duration $rentType)"
            "advance" -> "Advance payment for $listingName ($duration $rentType)"
            else -> "Payment for $listingName ($duration $rentType)"
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String) {
        try {
            Log.d(TAG, "Payment successful: $razorpayPaymentId")
            showSuccessDialog {
                createBooking(razorpayPaymentId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPaymentSuccess: ${e.message}")
            showError("Error processing successful payment")
            finish()
        }
    }

    private fun createBooking(paymentId: String) {
        progressBar.visibility = View.VISIBLE

        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        // Determine the booking status based on payment type
        val bookingStatus = when (paymentType) {
            "full" -> "approved"
            "advance" -> "pending_advance"
            else -> "pending"
        }
        
        val bookingData = hashMapOf(
            "listingId" to listingId,
            "productName" to listingName,
            "customerPhone" to customerPhone,
            "ownerPhone" to ownerPhone,
            "status" to bookingStatus,
            "paymentStatus" to if (paymentType == "full") "paid" else "partial_paid",
            "bookingTime" to currentTime,
            "startDate" to startDate,
            "endDate" to endDate,
            "duration" to duration,
            "rentType" to rentType,
            "totalAmount" to amount,
            "paymentType" to paymentType,
            "paymentId" to paymentId,
            "paymentTimestamp" to System.currentTimeMillis()
        )
        
        if (rentType?.contains("hour") == true && startTime != null && endTime != null) {
            bookingData["startTime"] = startTime
            bookingData["endTime"] = endTime
        }

        // Store in appropriate collection based on payment type
        val targetCollection = when (paymentType) {
            "full" -> "bookings"
            "advance" -> "pending_bookings"
            else -> "bookings"
        }

        db.collection(targetCollection)
            .add(bookingData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Booking created successfully with ID: ${documentReference.id} in collection: $targetCollection")
                
                // If it's an advance payment, also store in the main bookings collection
                if (paymentType == "advance") {
                    db.collection("bookings")
                        .add(bookingData)
                        .addOnSuccessListener { 
                            Log.d(TAG, "Advance booking also stored in main bookings collection")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error storing advance booking in main collection: ${e.message}")
                        }
                }

                val message = when (paymentType) {
                    "full" -> "Booking confirmed!"
                    "advance" -> "Advance payment received! Please pay the remaining amount before the booking date."
                    else -> "Booking created!"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating booking: ${e.message}")
                showError("Error creating booking")
                finish()
            }
    }

    override fun onPaymentError(code: Int, description: String) {
        try {
            Log.e(TAG, "Payment failed: Code: $code, Description: $description")
            val message = when (code) {
                Checkout.PAYMENT_CANCELED -> "Payment cancelled by user"
                Checkout.NETWORK_ERROR -> "Network error occurred"
                Checkout.INVALID_OPTIONS -> "Invalid payment options"
                else -> "Payment failed: $description"
            }
            showFailureDialog(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPaymentError: ${e.message}")
            showError("Error handling payment failure")
            finish()
        }
    }

    private fun showSuccessDialog(onSuccess: () -> Unit) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.payment_success_dialog)
        dialog.setCancelable(false)

        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)

        dialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
            onSuccess.invoke()
        }

        dialog.show()
    }

    private fun showFailureDialog(errorMessage: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.payment_failure_dialog)
        dialog.setCancelable(false)

        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)

        dialog.findViewById<TextView>(R.id.tvErrorMessage).text = errorMessage
        dialog.findViewById<Button>(R.id.btnRetry).setOnClickListener {
            dialog.dismiss()
            startPayment()
        }

        dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        checkout = null
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }
} 