package com.example.finora.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val database: FinoraDatabase
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
        validateTransaction(transaction)
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(
        transaction: TransactionEntity
    ) {
        validateTransaction(transaction)
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

    // ---------------- RECURRING TRANSACTIONS ----------------

    val recurringTransactions: Flow<List<RecurringTransactionEntity>> =
        recurringTransactionDao.getAllRecurringTransactions()

    suspend fun saveRecurringTransaction(
        recurring: RecurringTransactionEntity
    ): Long {
        validateRecurringTransaction(recurring)

        return recurringTransactionDao.insert(
            recurring.copy(
                id = 0,
                title = recurring.title.trim(),
                category = recurring.category.trim(),
                note = recurring.note.trim(),
                frequency = recurring.frequency.uppercase()
            )
        )
    }

    suspend fun updateRecurringTransaction(
        recurring: RecurringTransactionEntity
    ) {
        validateRecurringTransaction(recurring)

        recurringTransactionDao.update(
            recurring.copy(
                title = recurring.title.trim(),
                category = recurring.category.trim(),
                note = recurring.note.trim(),
                frequency = recurring.frequency.uppercase()
            )
        )
    }

    suspend fun deleteRecurringTransaction(
        recurring: RecurringTransactionEntity
    ) {
        recurringTransactionDao.delete(recurring)
    }

    suspend fun setRecurringTransactionActive(
        id: Int,
        isActive: Boolean
    ) {
        recurringTransactionDao.setActive(
            id = id,
            isActive = isActive
        )
    }

    /**
     * Processes all occurrences due at or before [now].
     *
     * The transaction insert and recurring-rule update are committed
     * together. If either operation fails, Room rolls back both.
     */
    suspend fun processDueRecurringTransactions(
        now: Long = System.currentTimeMillis()
    ): Int {
        return database.withTransaction {
            val dueRules =
                recurringTransactionDao.getDueTransactions(now)

            var createdCount = 0

            for (originalRule in dueRules) {
                var rule = recurringTransactionDao.getById(
                    originalRule.id
                ) ?: continue

                if (!rule.isActive) continue

                // If the next date is beyond the end date, stop the rule.
                if (
                    rule.endDate != null &&
                    rule.nextRunDate > rule.endDate
                ) {
                    recurringTransactionDao.update(
                        rule.copy(isActive = false)
                    )
                    continue
                }

                var safetyCounter = 0

                while (
                    rule.isActive &&
                    rule.nextRunDate <= now &&
                    (rule.endDate == null ||
                            rule.nextRunDate <= rule.endDate)
                ) {
                    check(safetyCounter < 10000) {
                        "Too many missed occurrences for recurring transaction ${rule.id}."
                    }
                    safetyCounter++

                    transactionDao.insertTransaction(
                        TransactionEntity(
                            title = rule.title,
                            amount = rule.amount,
                            type = rule.type,
                            category = rule.category,
                            date = rule.nextRunDate,
                            note = rule.note
                        )
                    )

                    createdCount++

                    val nextDate = calculateNextRunDate(
                        currentDate = rule.nextRunDate,
                        frequency = rule.frequency
                    )

                    val shouldStop =
                        rule.endDate != null &&
                                nextDate > rule.endDate

                    rule = rule.copy(
                        nextRunDate = nextDate,
                        isActive = !shouldStop
                    )

                    recurringTransactionDao.update(rule)
                }
            }

            createdCount
        }
    }

    private fun validateRecurringTransaction(
        recurring: RecurringTransactionEntity
    ) {
        require(recurring.title.isNotBlank()) {
            "Please enter a transaction title."
        }

        require(
            recurring.amount > 0.0 &&
                    recurring.amount.isFinite()
        ) {
            "Amount must be greater than zero."
        }

        require(
            recurring.type == "INCOME" ||
                    recurring.type == "EXPENSE"
        ) {
            "Transaction type must be INCOME or EXPENSE."
        }

        require(recurring.category.isNotBlank()) {
            "Please enter a category."
        }

        require(
            recurring.frequency.uppercase() in
                    setOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")
        ) {
            "Frequency must be DAILY, WEEKLY, MONTHLY, or YEARLY."
        }

        require(
            recurring.endDate == null ||
                    recurring.endDate >= recurring.nextRunDate
        ) {
            "End date must be on or after the first scheduled date."
        }
    }

    private fun calculateNextRunDate(
        currentDate: Long,
        frequency: String
    ): Long {
        val zone = ZoneId.systemDefault()

        val dateTime = Instant
            .ofEpochMilli(currentDate)
            .atZone(zone)

        val nextDateTime = when (frequency.uppercase()) {
            "DAILY" -> dateTime.plusDays(1)
            "WEEKLY" -> dateTime.plusWeeks(1)
            "MONTHLY" -> dateTime.plusMonths(1)
            "YEARLY" -> dateTime.plusYears(1)
            else -> throw IllegalArgumentException(
                "Unsupported recurring frequency: $frequency"
            )
        }

        return nextDateTime.toInstant().toEpochMilli()
    }

    private fun validateTransaction(
        transaction: TransactionEntity
    ) {
        require(transaction.amount > 0.0) {
            "Transaction amount must be greater than zero."
        }

        require(
            transaction.type == "INCOME" ||
                    transaction.type == "EXPENSE"
        ) {
            "Transaction type must be INCOME or EXPENSE."
        }
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