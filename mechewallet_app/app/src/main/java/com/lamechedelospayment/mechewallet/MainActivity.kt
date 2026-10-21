package com.lamechedelospayment.mechewallet

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var currentUserId: String
    private lateinit var totalBalance: TextView
    private lateinit var fabAdd: ImageView
    private lateinit var moreVertIcon: ImageView

    private lateinit var homeOption: TextView
    private lateinit var merchantOption: TextView
    private lateinit var profileOption: TextView
    private lateinit var logoutOption: TextView

    private lateinit var recyclerView: RecyclerView
    private val transactions = mutableListOf<Transaction>()

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentUserId = intent.getStringExtra("USER_ID") ?: ""

        totalBalance = findViewById(R.id.totalBalance)
        fabAdd = findViewById(R.id.fab_add)
        moreVertIcon = findViewById(R.id.more_vert_icon)

        // Opciones del menú
        homeOption = findViewById(R.id.home_option)
        merchantOption = findViewById(R.id.merchant_option)
        profileOption = findViewById(R.id.profile_option)
        logoutOption = findViewById(R.id.logout_option)

        // RecyclerView
        recyclerView = findViewById(R.id.transactionsRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = TransactionAdapter(transactions)

        // Botón de enviar dinero
        fabAdd.setOnClickListener {
            val intent = Intent(this, TransferActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }

        // Menú inferior
        homeOption.setOnClickListener {
            Toast.makeText(this, "Ya estás en la pantalla principal", Toast.LENGTH_SHORT).show()
        }
        merchantOption.setOnClickListener {
            val intent = Intent(this, MerchantActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }
        profileOption.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }
        logoutOption.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Menú 3 puntos
        moreVertIcon.setOnClickListener {
            showPopupMenu(it)
        }

        // Llamadas iniciales al backend
        fetchUserProfile(currentUserId)
        fetchUserTransactions(currentUserId)
    }

    private fun showPopupMenu(view: android.view.View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.menu_dashboard, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    Toast.makeText(this, "Ya estás en la pantalla principal", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_merchant -> {
                    val intent = Intent(this, MerchantActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    true
                }
                R.id.action_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    true
                }
                R.id.action_logout -> {
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
                    Toast.makeText(this@MainActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    try {
                        val json = JSONObject(responseData)
                        val balance = json.getDouble("balance")

                        runOnUiThread {
                            totalBalance.text = "$${String.format("%.2f", balance)}"
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error al parsear el perfil", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    // Historial del usuario
    private fun fetchUserTransactions(userId: String) {
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("jwt_token", null) ?: return

        val request = Request.Builder()
            .url("http://ctf.lamechedelospayments.org:2095/api/transactions/$userId")
            .header("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    try {
                        val jsonArray = JSONArray(responseData)
                        transactions.clear()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            transactions.add(
                                Transaction(
                                    obj.getString("sender_username"),
                                    obj.getString("receiver_username"),
                                    obj.getDouble("amount"),
                                    obj.getString("timestamp"),
                                    obj.getString("receiver_id"),
                                    obj.getString("transaction_id").toLong()
                                )
                            )
                        }
                        runOnUiThread {
                            recyclerView.adapter?.notifyDataSetChanged()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error al parsear transacciones", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
}


