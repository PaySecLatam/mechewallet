package com.lamechedelospayment.mechewallet

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.json.JSONObject

class MerchantActivity : AppCompatActivity() {

    private lateinit var amountEditText: EditText
    private lateinit var generateQrButton: Button
    private lateinit var qrImageView: ImageView
    private lateinit var currentUserId: String

    private lateinit var moreVertIcon: ImageView
    private lateinit var homeOption: TextView
    private lateinit var merchantOption: TextView
    private lateinit var profileOption: TextView
    private lateinit var logoutOption: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merchant)

        amountEditText = findViewById(R.id.amountEditText)
        generateQrButton = findViewById(R.id.generateQrButton)
        qrImageView = findViewById(R.id.qrImageView)
        currentUserId = intent.getStringExtra("USER_ID") ?: ""

        // Nuevas referencias a los elementos del menú en el diseño
        moreVertIcon = findViewById(R.id.more_vert_icon)
        homeOption = findViewById(R.id.home_option)
        merchantOption = findViewById(R.id.merchant_option)
        profileOption = findViewById(R.id.profile_option)
        logoutOption = findViewById(R.id.logout_option)

        generateQrButton.setOnClickListener {
            val amountStr = amountEditText.text.toString().trim()
            if (amountStr.isNotEmpty()) {
                try {
                    val amount = amountStr.toDouble()
                    generateQrCode(currentUserId, amount)
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Por favor, ingrese un monto válido", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, ingrese un monto", Toast.LENGTH_SHORT).show()
            }
        }

        // Asigna la acción a cada opción del menú en la interfaz
        homeOption.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
            finish()
        }

        merchantOption.setOnClickListener {
            Toast.makeText(this, "Ya estás en la pantalla de comercio", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "Ya estás en la pantalla de comercio", Toast.LENGTH_SHORT).show()
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

    private fun generateQrCode(merchantId: String, amount: Double) {
        // Crea el payload JSON para el QR
        val qrData = JSONObject()
        qrData.put("merchantId", merchantId)
        qrData.put("amount", amount)

        val barcodeEncoder = BarcodeEncoder()
        try {
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(qrData.toString(), BarcodeFormat.QR_CODE, 400, 400)
            qrImageView.setImageBitmap(bitmap)
            Toast.makeText(this, "QR generado", Toast.LENGTH_SHORT).show()
        } catch (e: WriterException) {
            Toast.makeText(this, "Error al generar el QR: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}