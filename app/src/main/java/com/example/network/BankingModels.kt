package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TransactionPayload(
    val gateway: String,
    val amount: Double,
    val type: String, // "IN" or "OUT"
    val content: String,
    val account: String,
    val balance: Double,
    val time: String, // "yyyy-MM-dd HH:mm:ss"
    val title: String,
    val deviceId: String? = null
)

