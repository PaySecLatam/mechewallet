package com.lamechedelospayment.mechewallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ProfileActivity : AppCompatActivity() {

    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var balanceTextView: TextView
    private lateinit var currentUserId: String

    private lateinit var moreVertIcon: ImageView
    private lateinit var homeOption: TextView
    private lateinit var merchantOption: TextView
    private lateinit var profileOption: TextView
    private lateinit var logoutOption: TextView

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Asigna los nuevos elementos de la interfaz
        usernameTextView = findViewById(R.id.usernameTextView)
        emailTextView = findViewById(R.id.emailTextView)
        balanceTextView = findViewById(R.id.balanceTextView)

        // Asigna los elementos del menú
        moreVertIcon = findViewById(R.id.more_vert_icon)
        homeOption = findViewById(R.id.home_option)
        merchantOption = findViewById(R.id.merchant_option)
        profileOption = findViewById(R.id.profile_option)
        logoutOption = findViewById(R.id.logout_option)

        currentUserId = intent.getStringExtra("USER_ID") ?: ""

        if (currentUserId.isNotEmpty()) {
            fetchUserProfile(currentUserId)
        } else {
            Toast.makeText(this, "ID de usuario no encontrado", Toast.LENGTH_SHORT).show()
            finish()
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
            // El usuario ya está en Perfil, no es necesario hacer nada.
            Toast.makeText(this, "Ya estás en la pantalla de perfil", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "Ya estás en la pantalla de perfil", Toast.LENGTH_SHORT).show()
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


    private fun fetchUserProfile(userId: String) {
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("jwt_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Sesión no válida, inicie sesión de nuevo.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        val request = Request.Builder()
            .url("http://ctf.lamechedelospayments.org:2095/api/profile/$userId")
            .header("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    try {
                        val json = JSONObject(responseData)
                        val username = json.getString("username")
                        val email = json.getString("email")
                        val balance = json.getDouble("balance")

                        runOnUiThread {
                            usernameTextView.text = "@$username"
                            emailTextView.text = "Correo: $email"
                            balanceTextView.text = "Saldo: $${String.format("%.2f", balance)}"
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@ProfileActivity, "Error al parsear los datos del perfil", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ProfileActivity, "Error al cargar el perfil", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
