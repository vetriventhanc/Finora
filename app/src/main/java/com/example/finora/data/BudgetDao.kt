package com.example.finora.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query(
        """
        SELECT * FROM budgets
        WHERE month = :month
        ORDER BY category COLLATE NOCASE ASC
        """
    )
    fun getBudgetsForMonth(
        month: String
    ): Flow<List<BudgetEntity>>

    @Query(
        """
        SELECT * FROM budgets
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getBudgetById(id: Int): BudgetEntity?

    @Query(
        """
        SELECT * FROM budgets
        WHERE category = :category
          AND month = :month
        LIMIT 1
        """
    )
    suspend fun getBudgetForCategoryAndMonth(
        category: String,
        month: String
    ): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(
        budget: BudgetEntity
    ): Long

    @Update
    suspend fun updateBudget(
        budget: BudgetEntity
    )

    @Delete
    suspend fun deleteBudget(
        budget: BudgetEntity
    )

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE type = 'EXPENSE'
          AND category = :category
          AND date >= :startDate
          AND date < :endDate
        """
    )
    fun getCategorySpending(
        category: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double>

    @Query(
        """
        DELETE FROM budgets
        WHERE month = :month
        """
    )
    suspend fun deleteBudgetsForMonth(month: String)

    @Query("SELECT * FROM budgets ORDER BY month DESC, category COLLATE NOCASE ASC")
    suspend fun getAllBudgetsOnce(): List<BudgetEntity>

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(items: List<BudgetEntity>)

}