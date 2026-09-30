package com.example.finora.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // Observe all transactions, newest first
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    // Observe a single transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): Flow<TransactionEntity?>

    // Observe income transactions
    @Query(
        "SELECT * FROM transactions " +
                "WHERE type = 'INCOME' ORDER BY date DESC"
    )
    fun getIncomeTransactions(): Flow<List<TransactionEntity>>

    // Observe expense transactions
    @Query(
        "SELECT * FROM transactions " +
                "WHERE type = 'EXPENSE' ORDER BY date DESC"
    )
    fun getExpenseTransactions(): Flow<List<TransactionEntity>>

    // Calculate total income
    @Query(
        "SELECT COALESCE(SUM(amount), 0) " +
                "FROM transactions WHERE type = 'INCOME'"
    )
    fun getTotalIncome(): Flow<Double>

    // Calculate total expenses
    @Query(
        "SELECT COALESCE(SUM(amount), 0) " +
                "FROM transactions WHERE type = 'EXPENSE'"
    )
    fun getTotalExpenses(): Flow<Double>

    // Add a transaction and return its generated ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(
        transaction: TransactionEntity
    ): Long

    // Update an existing transaction
    @Update
    suspend fun updateTransaction(
        transaction: TransactionEntity
    )

    // Delete a transaction
    @Delete
    suspend fun deleteTransaction(
        transaction: TransactionEntity
    )

    // Delete all transactions
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(items: List<TransactionEntity>)

}