package com.lamechedelospayment.mechewallet

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var merchantCheckbox: CheckBox
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView

    private lateinit var sharedPreferences: SharedPreferences
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        usernameInput = findViewById(R.id.username_input_register)
        emailInput = findViewById(R.id.email_input_register)
        passwordInput = findViewById(R.id.password_input_register)
        merchantCheckbox = findViewById(R.id.merchant_checkbox)
        registerButton = findViewById(R.id.register_button)
        loginLink = findViewById(R.id.login_link)

        registerButton.setOnClickListener {
            performRegister()
        }

        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun performRegister() {
        val username = usernameInput.text.toString()
        val email = emailInput.text.toString()
        val password = passwordInput.text.toString()
        val isMerchant = merchantCheckbox.isChecked

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
            return
        }

        val json = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
            put("is_merchant", isMerchant)
        }
        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("http://ctf.lamechedelospayments.org:2095/api/register")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    try {
                        val jsonResponse = JSONObject(responseData)
                        val token = jsonResponse.getString("token")
                        val userId = jsonResponse.getString("userId")

                        sharedPreferences.edit().putString("jwt_token", token).apply()
                        sharedPreferences.edit().putString("user_id", userId).apply()

                        runOnUiThread {
                            Toast.makeText(this@RegisterActivity, "Registro exitoso!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@RegisterActivity, "Error al procesar la respuesta del servidor", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        val error = responseData ?: "Error desconocido"
                        Toast.makeText(this@RegisterActivity, "Error de registro: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
