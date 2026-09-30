
package com.example.finora.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Name of the savings goal
    val name: String,

    // Target amount in INR
    val targetAmount: Double,

    // Amount saved so far in INR
    val savedAmount: Double = 0.0,

    // Optional target completion date (Unix milliseconds)
    val targetDate: Long? = null,

    // Goal creation timestamp
    val createdAt: Long = System.currentTimeMillis()
)
