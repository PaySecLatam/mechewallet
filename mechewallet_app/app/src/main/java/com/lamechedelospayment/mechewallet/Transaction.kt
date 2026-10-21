package com.lamechedelospayment.mechewallet

data class Transaction(
    val sender_username: String,
    val receiver_username: String,
    val amount: Double,
    val timestamp: String,
    val receiver_id: String,
    val transaction_id: Long

)