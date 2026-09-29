package com.example.finora.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Query(
        """
        SELECT * FROM recurring_transactions
        ORDER BY isActive DESC, nextRunDate ASC
        """
    )
    fun getAllRecurringTransactions():
            Flow<List<RecurringTransactionEntity>>

    @Query(
        """
        SELECT * FROM recurring_transactions
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getById(
        id: Int
    ): RecurringTransactionEntity?

    @Query(
        """
        SELECT * FROM recurring_transactions
        WHERE isActive = 1
          AND nextRunDate <= :now
        ORDER BY nextRunDate ASC
        """
    )
    suspend fun getDueTransactions(
        now: Long
    ): List<RecurringTransactionEntity>

    @Insert
    suspend fun insert(
        recurring: RecurringTransactionEntity
    ): Long

    @Update
    suspend fun update(
        recurring: RecurringTransactionEntity
    )

    @Delete
    suspend fun delete(
        recurring: RecurringTransactionEntity
    )

    @Query(
        """
        UPDATE recurring_transactions
        SET isActive = :isActive
        WHERE id = :id
        """
    )
    suspend fun setActive(
        id: Int,
        isActive: Boolean
    )
}