package com.lamechedelospayment.mechewallet

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val description: TextView = itemView.findViewById(R.id.txDescription)
        val account: TextView = itemView.findViewById(R.id.txAccount)
        val amount: TextView = itemView.findViewById(R.id.txAmount)
        val date: TextView = itemView.findViewById(R.id.txDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val tx = transactions[position]
        holder.description.text = "De ${tx.sender_username} a ${tx.receiver_username}"
        holder.account.text = tx.sender_username
        holder.amount.text = String.format("%.2f", tx.amount)
        holder.amount.setTextColor(if (tx.amount < 0) Color.RED else Color.GREEN)
        holder.date.text = tx.timestamp

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, TransactionDetailActivity::class.java)

            // Pasar todos los datos importantes incluyendo transaction_id y receiver_id
            intent.putExtra("transaction_id", tx.transaction_id)
            intent.putExtra("transaction_amount", tx.amount)
            intent.putExtra("transaction_date", tx.timestamp)
            intent.putExtra("transaction_sender", tx.sender_username)
            intent.putExtra("transaction_receiver", tx.receiver_username)
            intent.putExtra("transaction_receiver_id", tx.receiver_id) // necesario para mostrar el botón solo al receptor

            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = transactions.size
}
