package com.example.bullsrentowner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class PaymentHistoryAdapter : ListAdapter<PaymentHistoryItem, PaymentHistoryAdapter.PaymentViewHolder>(PaymentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_history, parent, false)
        return PaymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PaymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvListingName: TextView = itemView.findViewById(R.id.tvListingName)
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvPaymentId: TextView = itemView.findViewById(R.id.tvPaymentId)

        fun bind(payment: PaymentHistoryItem) {
            tvListingName.text = payment.listingName
            tvCustomerName.text = payment.customerName

            tvStatus.text = formatStatus(payment.status)
            tvDate.text = formatDate(payment.timestamp)
            tvPaymentId.text = "Payment ID: ${payment.paymentId}"

            // Set status color
            val statusColor = when (payment.status) {
                "paid" -> android.R.color.holo_green_dark
                "partial_paid" -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_red_dark
            }
            tvStatus.setTextColor(itemView.context.getColor(statusColor))
        }



        private fun formatStatus(status: String): String {
            return when (status) {
                "paid" -> "Paid"
                "partial_paid" -> "Partially Paid"
                else -> "Pending"
            }
        }

        private fun formatDate(date: Date?): String {
            if (date == null) return "Date not available"
            val format = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            return format.format(date)
        }
    }
}

class PaymentDiffCallback : DiffUtil.ItemCallback<PaymentHistoryItem>() {
    override fun areItemsTheSame(oldItem: PaymentHistoryItem, newItem: PaymentHistoryItem): Boolean {
        return oldItem.paymentId == newItem.paymentId
    }

    override fun areContentsTheSame(oldItem: PaymentHistoryItem, newItem: PaymentHistoryItem): Boolean {
        return oldItem == newItem
    }
} 