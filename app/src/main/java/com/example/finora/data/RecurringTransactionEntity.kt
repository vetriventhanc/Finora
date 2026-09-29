package com.example.finora.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,

    val amount: Double,

    // "INCOME" or "EXPENSE"
    val type: String,

    val category: String,

    val note: String = "",

    // DAILY, WEEKLY, MONTHLY, YEARLY
    val frequency: String,

    // Date the next transaction should be created
    val nextRunDate: Long,

    // Optional date after which the rule stops
    val endDate: Long? = null,

    // Paused recurring rules are not processed
    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis()
)