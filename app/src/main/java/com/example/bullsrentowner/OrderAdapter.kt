package com.example.bullsrentowner

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter(private var orders: List<Order>) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    init {
        sortOrdersByDate()
    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val machineName: TextView = itemView.findViewById(R.id.tvMachineName)
        val status: TextView = itemView.findViewById(R.id.tvStatus)
        val companyName: TextView = itemView.findViewById(R.id.tvCompanyName)
        val bookingDate: TextView = itemView.findViewById(R.id.tvBookingDate)
        val bookingPeriod: TextView = itemView.findViewById(R.id.tvBookingPeriod)
        val totalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        val statusIndicator: View = itemView.findViewById(R.id.viewStatusIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        val context = holder.itemView.context

        // Set machine name
        holder.machineName.text = order.machineName

        // Set and style status
        holder.status.text = order.status.capitalize()
        val (statusColor, indicatorColor) = getStatusColors(order.status)
        holder.status.setTextColor(ContextCompat.getColor(context, statusColor))
        holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(context, indicatorColor))

        // Set company name
        holder.companyName.text = order.companyName

        // Format and set booking date
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        holder.bookingDate.text = "Booked on: ${dateFormat.format(Date(order.bookingDate))}"

        // Set booking period
        holder.bookingPeriod.text = formatBookingPeriod(order)

        // Set total amount
        if (order.totalAmount.isNotEmpty()) {
            holder.totalAmount.text = "Total: ${order.totalAmount}"
            holder.totalAmount.visibility = View.VISIBLE
        } else {
            holder.totalAmount.visibility = View.GONE
        }
    }

    private fun formatBookingPeriod(order: Order): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDate = try {
            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(order.startDate)
            dateFormat.format(parsedDate!!)
        } catch (e: Exception) {
            order.startDate
        }

        return when {
            order.rentType.contains("hour") && order.startTime != null -> {
                "Booked for ${order.duration} hours on $startDate\nTime: ${order.startTime} - ${order.endTime}"
            }
            order.rentType.contains("day") -> {
                val endDate = try {
                    val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(order.endDate)
                    dateFormat.format(parsedDate!!)
                } catch (e: Exception) {
                    order.endDate
                }
                "Booked for ${order.duration} days\nFrom: $startDate\nTo: $endDate"
            }
            else -> "Booking period not specified"
        }
    }

    private fun getStatusColors(status: String): Pair<Int, Int> {
        return when (status.lowercase()) {
            "pending" -> Pair(R.color.status_pending_text, R.color.status_pending_bg)
            "approved" -> Pair(R.color.status_approved_text, R.color.status_approved_bg)
            "rejected" -> Pair(R.color.status_rejected_text, R.color.status_rejected_bg)
            "completed" -> Pair(R.color.status_completed_text, R.color.status_completed_bg)
            else -> Pair(R.color.status_unknown_text, R.color.status_unknown_bg)
        }
    }

    override fun getItemCount() = orders.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        sortOrdersByDate()
        notifyDataSetChanged()
    }

    private fun sortOrdersByDate() {
        orders = orders.sortedByDescending { it.bookingDate }
    }
}

