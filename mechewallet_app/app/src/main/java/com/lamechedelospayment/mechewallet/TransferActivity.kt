package com.lamechedelospayment.mechewallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class TransferActivity : AppCompatActivity() {

    private lateinit var scanQrButton: Button
    private val client = OkHttpClient()
    private lateinit var currentUserId: String

    private lateinit var moreVertIcon: ImageView
    private lateinit var homeOption: TextView
    private lateinit var merchantOption: TextView
    private lateinit var profileOption: TextView
    private lateinit var logoutOption: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)

        scanQrButton = findViewById(R.id.scanQrButton)
        currentUserId = intent.getStringExtra("USER_ID") ?: ""

        // Nuevas referencias a los elementos del menú en el diseño
        moreVertIcon = findViewById(R.id.more_vert_icon)
        homeOption = findViewById(R.id.home_option)
        merchantOption = findViewById(R.id.merchant_option)
        profileOption = findViewById(R.id.profile_option)
        logoutOption = findViewById(R.id.logout_option)

        scanQrButton.setOnClickListener {
            val options = ScanOptions()
            options.setPrompt("Escanea el QR del comercio para pagar")
            qrCodeScannerLauncher.launch(options)
        }

        // Asigna la acción a cada opción del menú en la interfaz
        homeOption.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
            finish()
        }

        merchantOption.setOnClickListener {
            val intent = Intent(this, MerchantActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
            finish()
        }

        profileOption.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
            finish()
        }

        logoutOption.setOnClickListener {
            val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Maneja la acción del menú de los 3 puntos
        moreVertIcon.setOnClickListener {
            showPopupMenu(it)
        }
    }

    private fun showPopupMenu(view: android.view.View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.menu_dashboard, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.action_merchant -> {
                    val intent = Intent(this, MerchantActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.action_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.action_logout -> {
                    val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().clear().apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private val qrCodeScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            try {
                val qrData = JSONObject(result.contents)
                val merchantId = qrData.getString("merchantId")
                val amount = qrData.getDouble("amount")

                // **Vulnerabilidad 1 - Corregida:**
                // Se añade validación para asegurar que el monto sea un valor positivo.
                if (amount <= 0) {
                    Toast.makeText(this, "El monto de la transacción debe ser mayor a cero.", Toast.LENGTH_LONG).show()
                    return@registerForActivityResult
                }

                initiateTransfer(currentUserId, merchantId, amount)

            } catch (e: Exception) {
                Toast.makeText(this, "QR inválido o datos incorrectos.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initiateTransfer(senderId: String, receiverId: String, amount: Double) {
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("jwt_token", null)

        if (token == null) {
            Toast.makeText(this, "No se encontró token, inicia sesión de nuevo.", Toast.LENGTH_SHORT).show()
            return
        }

        val json = JSONObject()
        json.put("sender_id", senderId)
        json.put("receiver_id", receiverId)
        json.put("amount", amount)

        val body = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("http://ctf.lamechedelospayments.org:2095/api/transfer")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@TransferActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@TransferActivity, "Pago realizado con éxito.", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@TransferActivity, "Fallo al realizar el pago.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}