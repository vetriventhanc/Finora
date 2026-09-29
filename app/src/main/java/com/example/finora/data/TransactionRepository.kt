package com.example.finora.data

import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import java.time.ZoneId

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) {
    val allTransactions: Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    val incomeTransactions: Flow<List<TransactionEntity>> =
        transactionDao.getIncomeTransactions()

    val expenseTransactions: Flow<List<TransactionEntity>> =
        transactionDao.getExpenseTransactions()

    val totalIncome: Flow<Double> =
        transactionDao.getTotalIncome()

    val totalExpenses: Flow<Double> =
        transactionDao.getTotalExpenses()

    fun getTransactionById(id: Int): Flow<TransactionEntity?> =
        transactionDao.getTransactionById(id)

    suspend fun insertTransaction(
        transaction: TransactionEntity
    ): Long {
        require(transaction.amount > 0) {
            "Transaction amount must be greater than zero."
        }

        require(
            transaction.type == "INCOME" ||
                    transaction.type == "EXPENSE"
        ) {
            "Transaction type must be INCOME or EXPENSE."
        }

        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(
        transaction: TransactionEntity
    ) {
        require(transaction.amount > 0) {
            "Transaction amount must be greater than zero."
        }

        require(
            transaction.type == "INCOME" ||
                    transaction.type == "EXPENSE"
        ) {
            "Transaction type must be INCOME or EXPENSE."
        }

        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(
        transaction: TransactionEntity
    ) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    // ---------------- BUDGETS ----------------

    fun getBudgetsForMonth(
        month: String
    ): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(month)

    suspend fun saveBudget(
        category: String,
        amount: Double,
        month: String
    ) {
        val cleanCategory = category.trim()

        require(cleanCategory.isNotEmpty()) {
            "Please enter a budget category."
        }

        require(amount > 0.0 && amount.isFinite()) {
            "Budget amount must be greater than zero."
        }

        require(isValidMonth(month)) {
            "Month must use yyyy-MM format."
        }

        val existing = budgetDao.getBudgetForCategoryAndMonth(
            category = cleanCategory,
            month = month
        )

        if (existing != null) {
            budgetDao.updateBudget(
                existing.copy(amount = amount)
            )
        } else {
            budgetDao.insertBudget(
                BudgetEntity(
                    category = cleanCategory,
                    amount = amount,
                    month = month
                )
            )
        }
    }

    suspend fun updateBudget(
        budget: BudgetEntity
    ) {
        require(budget.category.isNotBlank()) {
            "Please enter a budget category."
        }

        require(budget.amount > 0.0 && budget.amount.isFinite()) {
            "Budget amount must be greater than zero."
        }

        require(isValidMonth(budget.month)) {
            "Month must use yyyy-MM format."
        }

        budgetDao.updateBudget(
            budget.copy(category = budget.category.trim())
        )
    }

    suspend fun deleteBudget(
        budget: BudgetEntity
    ) {
        budgetDao.deleteBudget(budget)
    }

    fun getCategorySpending(
        category: String,
        month: String
    ): Flow<Double> {
        val yearMonth = YearMonth.parse(month)
        val zone = ZoneId.systemDefault()

        val startDate = yearMonth
            .atDay(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        val endDate = yearMonth
            .plusMonths(1)
            .atDay(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        return budgetDao.getCategorySpending(
            category = category,
            startDate = startDate,
            endDate = endDate
        )
    }

    suspend fun deleteBudgetsForMonth(month: String) {
        require(isValidMonth(month)) {
            "Month must use yyyy-MM format."
        }

        budgetDao.deleteBudgetsForMonth(month)
    }

    private fun isValidMonth(month: String): Boolean {
        return try {
            YearMonth.parse(month)
            true
        } catch (_: Exception) {
            false
        }
    }
}