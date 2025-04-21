package com.example.bullsrentowner

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PaymentHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoPayments: TextView
    private lateinit var paymentAdapter: PaymentHistoryAdapter
    private val db = FirebaseFirestore.getInstance()
    private var userPhone: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_history)

        userPhone = intent.getStringExtra("USER_PHONE")
        if (userPhone.isNullOrEmpty()) {
            Toast.makeText(this, "Error: User phone number missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupRecyclerView()
        fetchPaymentHistory()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.rvPaymentHistory)
        progressBar = findViewById(R.id.progressBar)
        tvNoPayments = findViewById(R.id.tvNoPayments)
    }

    private fun setupRecyclerView() {
        paymentAdapter = PaymentHistoryAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@PaymentHistoryActivity)
            adapter = paymentAdapter
        }
    }

    private fun fetchPaymentHistory() {
        progressBar.visibility = View.VISIBLE
        tvNoPayments.visibility = View.GONE

        db.collection("bookings")
            .whereEqualTo("ownerPhone", userPhone)
            .orderBy("paymentTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val payments = mutableListOf<PaymentHistoryItem>()
                for (document in documents) {
                    val amount = document.getDouble("amount") ?: 0.0
                    val status = document.getString("paymentStatus") ?: "pending"
                    val timestamp = document.getTimestamp("paymentTimestamp")?.toDate()
                    val listingName = document.getString("listingName") ?: "Unknown Listing"
                    val customerName = document.getString("customerName") ?: "Unknown Customer"
                    val paymentId = document.getString("paymentId") ?: ""

                    payments.add(PaymentHistoryItem(
                        listingName = listingName,
                        customerName = customerName,
                        amount = amount,
                        status = status,
                        timestamp = timestamp,
                        paymentId = paymentId
                    ))
                }

                if (payments.isEmpty()) {
                    tvNoPayments.visibility = View.VISIBLE
                } else {
                    paymentAdapter.submitList(payments)
                }
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error fetching payment history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

data class PaymentHistoryItem(
    val listingName: String,
    val customerName: String,
    val amount: Double,
    val status: String,
    val timestamp: Date?,
    val paymentId: String
) 