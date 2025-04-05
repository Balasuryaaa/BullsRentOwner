package com.example.bullsrentowner

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

data class Order(
    val id: String,
    val machineName: String,
    val companyName: String,
    val status: String,
    val bookingDate: Long // Store as timestamp (milliseconds)
)

class OrderAdapter(private var orders: List<Order>) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    init {
        sortOrdersByDate()
    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val machineName: TextView = itemView.findViewById(R.id.tvMachineName)
        val companyName: TextView = itemView.findViewById(R.id.tvCompanyName)
        val orderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        val bookingDate: TextView = itemView.findViewById(R.id.tvBookingDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.machineName.text = order.machineName
        holder.companyName.text = order.companyName
        holder.orderStatus.text = "Status: ${order.status}"
        holder.bookingDate.text = "Booked On: ${formatDate(order.bookingDate)}"
    }

    override fun getItemCount(): Int = orders.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        sortOrdersByDate()
        notifyDataSetChanged()
    }

    private fun sortOrdersByDate() {
        orders = orders.sortedByDescending { it.bookingDate }
    }

    private fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}

