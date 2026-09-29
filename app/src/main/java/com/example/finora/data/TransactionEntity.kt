package com.example.finora.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,

    val amount: Double,

    // Use "INCOME" or "EXPENSE"
    val type: String,

    val category: String,

    // Date and time stored as Unix milliseconds
    val date: Long = System.currentTimeMillis(),

    val note: String = ""
)