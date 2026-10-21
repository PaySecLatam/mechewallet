package com.lamechedelospayment.mechewallet

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var forgotPasswordText: TextView
    private lateinit var registerText: TextView

    private lateinit var sharedPreferences: SharedPreferences
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        forgotPasswordText = findViewById(R.id.forgot_password_text)
        registerText = findViewById(R.id.register_text)

        // Maneja el clic en el botón de Iniciar Sesión
        loginButton.setOnClickListener {
            performLogin()
        }

        // Maneja el clic en los enlaces de "Registro" y "Olvidé Contraseña"
        registerText.setOnClickListener {
            // Navega a la pantalla de registro
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin() {
        val email = emailInput.text.toString()
        val password = passwordInput.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val json = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("http://ctf.lamechedelospayments.org:2095/api/login")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    try {
                        val jsonResponse = JSONObject(responseData)
                        val token = jsonResponse.getString("token")
                        val userId = jsonResponse.getString("userId")
                        val isMerchant = jsonResponse.getBoolean("is_merchant")

                        sharedPreferences.edit().putString("jwt_token", token).apply()
                        sharedPreferences.edit().putString("user_id", userId).apply()


                        runOnUiThread {
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.putExtra("USER_ID", userId)
                            intent.putExtra("IS_MERCHANT", isMerchant)
                            startActivity(intent)
                            finish()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Error al procesar la respuesta del servidor", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        val error = responseData ?: "Error desconocido"
                        Toast.makeText(this@LoginActivity, "Error de inicio de sesión: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
