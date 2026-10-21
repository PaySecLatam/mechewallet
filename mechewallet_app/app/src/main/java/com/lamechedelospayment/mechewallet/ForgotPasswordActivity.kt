package com.lamechedelospayment.mechewallet

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

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var resetButton: Button
    private lateinit var backToLoginLink: TextView

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        emailInput = findViewById(R.id.email_input_forgot_password)
        resetButton = findViewById(R.id.reset_password_button)
        backToLoginLink = findViewById(R.id.back_to_login_link)

        resetButton.setOnClickListener {
            performPasswordReset()
        }

        backToLoginLink.setOnClickListener {
            finish()
        }
    }

    private fun performPasswordReset() {
        val email = emailInput.text.toString()

        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese su correo electrónico.", Toast.LENGTH_SHORT).show()
            return
        }

        val json = JSONObject().apply {
            put("email", email)
        }
        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("http://ctf.lamechedelospayments.org:2095/api/forgot-password")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordActivity, "Si tu correo existe, recibirás un enlace de restablecimiento.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        })
    }
}
