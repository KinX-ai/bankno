package com.example.data

import com.example.network.BankingApiService
import com.example.network.TransactionPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val apiService: BankingApiService = BankingApiService.create()
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun insert(transaction: TransactionEntity): Long {
        return withContext(Dispatchers.IO) {
            transactionDao.insertTransaction(transaction)
        }
    }

    suspend fun update(transaction: TransactionEntity) {
        withContext(Dispatchers.IO) {
            transactionDao.updateTransaction(transaction)
        }
    }

    suspend fun deleteById(id: Int) {
        withContext(Dispatchers.IO) {
            transactionDao.deleteTransactionById(id)
        }
    }

    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            transactionDao.clearAllTransactions()
        }
    }

    suspend fun sendTransactionToApi(transaction: TransactionEntity, url: String, deviceId: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedTime = sdf.format(Date(transaction.timestamp))

            val payload = TransactionPayload(
                gateway = transaction.bankName,
                amount = transaction.amount,
                type = transaction.type,
                content = transaction.content,
                account = transaction.accountNumber,
                balance = transaction.balance,
                time = formattedTime,
                title = transaction.title,
                deviceId = deviceId
            )

            try {
                val response = apiService.sendTransaction(url, payload)
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: "Success (Empty response)"
                    val updated = transaction.copy(
                        apiStatus = "SUCCESS",
                        apiResponse = "Code: ${response.code()}, Body: $responseBody"
                    )
                    transactionDao.updateTransaction(updated)
                    true
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    val updated = transaction.copy(
                        apiStatus = "FAILED",
                        apiResponse = "Code: ${response.code()}, Error: $errorBody"
                    )
                    transactionDao.updateTransaction(updated)
                    false
                }
            } catch (e: Exception) {
                val updated = transaction.copy(
                    apiStatus = "FAILED",
                    apiResponse = "Exception: ${e.message}"
                )
                transactionDao.updateTransaction(updated)
                false
            }
        }
    }
}
