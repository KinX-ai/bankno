package com.example.util

import com.example.data.TransactionEntity
import java.util.Calendar

data class StatsPeriod(
    val name: String, // e.g., "Tháng 07/2026" or "Quý 3/2026"
    val totalIn: Double,
    val totalOut: Double,
    val transactionCount: Int
) {
    val net: Double get() = totalIn - totalOut
}

object StatsUtil {
    fun getMonthlyStats(transactions: List<TransactionEntity>): List<StatsPeriod> {
        val cal = Calendar.getInstance()
        val grouped = transactions.groupBy {
            cal.timeInMillis = it.timestamp
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) // 0-11
            year * 12 + month
        }
        return grouped.map { (monthKey, list) ->
            val year = monthKey / 12
            val month = monthKey % 12
            val totalIn = list.filter { it.type == "IN" }.sumOf { it.amount }
            val totalOut = list.filter { it.type == "OUT" }.sumOf { it.amount }
            StatsPeriod(
                name = "Tháng %02d/%d".format(month + 1, year),
                totalIn = totalIn,
                totalOut = totalOut,
                transactionCount = list.size
            )
        }.sortedByDescending {
            val parts = it.name.removePrefix("Tháng ").split("/")
            val m = parts[0].toInt()
            val y = parts[1].toInt()
            y * 12 + m
        }
    }

    fun getQuarterlyStats(transactions: List<TransactionEntity>): List<StatsPeriod> {
        val cal = Calendar.getInstance()
        val grouped = transactions.groupBy {
            cal.timeInMillis = it.timestamp
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) // 0-11
            val quarter = (month / 3) + 1 // 1-4
            year * 10 + quarter
        }
        return grouped.map { (quarterKey, list) ->
            val year = quarterKey / 10
            val quarter = quarterKey % 10
            val totalIn = list.filter { it.type == "IN" }.sumOf { it.amount }
            val totalOut = list.filter { it.type == "OUT" }.sumOf { it.amount }
            StatsPeriod(
                name = "Quý $quarter/$year",
                totalIn = totalIn,
                totalOut = totalOut,
                transactionCount = list.size
            )
        }.sortedByDescending {
            val parts = it.name.removePrefix("Quý ").split("/")
            val q = parts[0].toInt()
            val y = parts[1].toInt()
            y * 10 + q
        }
    }
}
