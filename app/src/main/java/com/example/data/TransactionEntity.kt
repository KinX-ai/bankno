package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bankName: String,
    val title: String,
    val content: String,
    val amount: Double,
    val type: String, // "IN" (Nhận tiền), "OUT" (Chuyển tiền), "UNKNOWN"
    val accountNumber: String,
    val balance: Double,
    val timestamp: Long,
    val apiStatus: String, // "PENDING", "SUCCESS", "FAILED"
    val apiResponse: String? = null
)
