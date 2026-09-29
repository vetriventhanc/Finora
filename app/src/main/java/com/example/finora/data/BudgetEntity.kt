package com.example.finora.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index(
            value = ["category", "month"],
            unique = true
        )
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val category: String,

    // Monthly spending limit in INR
    val amount: Double,

    // Format: yyyy-MM
    val month: String,

    val createdAt: Long = System.currentTimeMillis()
)