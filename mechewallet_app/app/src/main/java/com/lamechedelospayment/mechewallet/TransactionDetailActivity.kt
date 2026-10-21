package com.lamechedelospayment.mechewallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class TransactionDetailActivity : AppCompatActivity() {

    private lateinit var txtId: TextView
    private lateinit var txtAmount: TextView
    private lateinit var txtDate: TextView
    private lateinit var txtSender: TextView
    private lateinit var txtReceiver: TextView
    private lateinit var btnRefund: Button

    private val client = OkHttpClient()

    private var transactionId: Long = -1L
    private var transactionReceiverId: String = ""
    private var currentUserId: String = ""
    private var refunded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_detail)

        txtId = findViewById(R.id.txtTransactionId)
        txtAmount = findViewById(R.id.txtAmount)
        txtDate = findViewById(R.id.txtDate)
        txtSender = findViewById(R.id.txtSender)
        txtReceiver = findViewById(R.id.txtReceiver)
        btnRefund = findViewById(R.id.btnRefund)

        transactionId = intent.getLongExtra("transaction_id", -1L)
        val amount = intent.getDoubleExtra("transaction_amount", 0.0)
        val date = intent.getStringExtra("transaction_date") ?: ""
        val sender = intent.getStringExtra("transaction_sender") ?: ""
        val receiver = intent.getStringExtra("transaction_receiver") ?: ""
        transactionReceiverId = intent.getStringExtra("transaction_receiver_id") ?: ""
        refunded = intent.getBooleanExtra("transaction_refunded", false)

        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        currentUserId = sharedPreferences.getString("user_id", "") ?: ""

        txtId.text = "ID: $transactionId"
        txtAmount.text = "Monto: $amount"
        txtDate.text = "Fecha: $date"
        txtSender.text = "De: $sender"
        txtReceiver.text = "Para: $receiver"

        btnRefund.visibility = if (currentUserId == transactionReceiverId && !refunded) Button.VISIBLE else Button.GONE

        btnRefund.setOnClickListener {
            btnRefund.isEnabled = false
            refundTransaction(transactionId)
        }
    }

    private fun refundTransaction(transactionId: Long) {
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("jwt_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Sesión no válida, inicie sesión de nuevo.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val url = "http://ctf.lamechedelospayments.org:2095/refund"
        val json = JSONObject()
        json.put("transaction_id", transactionId)

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    btnRefund.isEnabled = true
                    Toast.makeText(this@TransactionDetailActivity, "Error de red", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@TransactionDetailActivity, "Devolución exitosa", Toast.LENGTH_SHORT).show()
                        btnRefund.visibility = Button.GONE
                    } else {
                        btnRefund.isEnabled = true
                        Toast.makeText(this@TransactionDetailActivity, "Error al devolver", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
