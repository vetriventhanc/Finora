package com.example.finora.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finora.data.BudgetEntity
import com.example.finora.data.RecurringTransactionEntity
import com.example.finora.data.TransactionEntity
import com.example.finora.data.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

class TransactionViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    val transactions: StateFlow<List<TransactionEntity>> =
        repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val incomeTransactions: StateFlow<List<TransactionEntity>> =
        repository.incomeTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val expenseTransactions: StateFlow<List<TransactionEntity>> =
        repository.expenseTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val totalIncome: StateFlow<Double> =
        repository.totalIncome.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    val totalExpenses: StateFlow<Double> =
        repository.totalExpenses.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0.0
        )

    init {
        // Process scheduled transactions whenever this ViewModel is created.
        processDueRecurringTransactions()
    }

    // ---------------- BUDGETS ----------------

    val currentBudgetMonth: String =
        YearMonth.now().toString()

    val budgets: StateFlow<List<BudgetEntity>> =
        repository.getBudgetsForMonth(currentBudgetMonth).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun getBudgetsForMonth(
        month: String
    ): Flow<List<BudgetEntity>> =
        repository.getBudgetsForMonth(month)

    fun getCategorySpending(
        category: String,
        month: String = currentBudgetMonth
    ): Flow<Double> =
        repository.getCategorySpending(category, month)

    fun saveBudget(
        category: String,
        amount: Double,
        month: String = currentBudgetMonth,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.saveBudget(
                    category = category,
                    amount = amount,
                    month = month
                )
            } catch (exception: Exception) {
                onError(
                    exception.message ?: "Unable to save budget."
                )
            }
        }
    }

    fun updateBudget(
        budget: BudgetEntity,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateBudget(budget)
            } catch (exception: Exception) {
                onError(
                    exception.message ?: "Unable to update budget."
                )
            }
        }
    }

    fun deleteBudget(
        budget: BudgetEntity,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteBudget(budget)
            } catch (exception: Exception) {
                onError(
                    exception.message ?: "Unable to delete budget."
                )
            }
        }
    }

    // ---------------- TRANSACTIONS ----------------

    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        date: Long = System.currentTimeMillis(),
        note: String = ""
    ) {
        val transaction = TransactionEntity(
            title = title.trim(),
            amount = amount,
            type = type,
            category = category.trim(),
            date = date,
            note = note.trim()
        )

        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    fun updateTransaction(
        transaction: TransactionEntity
    ) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(
        transaction: TransactionEntity
    ) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            repository.deleteAllTransactions()
        }
    }

    // ---------------- RECURRING TRANSACTIONS ----------------

    val recurringTransactions:
            StateFlow<List<RecurringTransactionEntity>> =
        repository.recurringTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addRecurringTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        note: String,
        frequency: String,
        nextRunDate: Long,
        endDate: Long? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val recurring = RecurringTransactionEntity(
            title = title.trim(),
            amount = amount,
            type = type,
            category = category.trim(),
            note = note.trim(),
            frequency = frequency.uppercase(),
            nextRunDate = nextRunDate,
            endDate = endDate,
            isActive = true
        )

        viewModelScope.launch {
            try {
                repository.saveRecurringTransaction(recurring)
                processDueRecurringTransactions()
                onSuccess()
            } catch (exception: Exception) {
                onError(
                    exception.message
                        ?: "Unable to save recurring transaction."
                )
            }
        }
    }

    fun updateRecurringTransaction(
        recurring: RecurringTransactionEntity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateRecurringTransaction(recurring)
                onSuccess()
            } catch (exception: Exception) {
                onError(
                    exception.message
                        ?: "Unable to update recurring transaction."
                )
            }
        }
    }

    fun deleteRecurringTransaction(
        recurring: RecurringTransactionEntity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.deleteRecurringTransaction(recurring)
                onSuccess()
            } catch (exception: Exception) {
                onError(
                    exception.message
                        ?: "Unable to delete recurring transaction."
                )
            }
        }
    }

    fun setRecurringTransactionActive(
        id: Int,
        isActive: Boolean,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.setRecurringTransactionActive(
                    id = id,
                    isActive = isActive
                )

                if (isActive) {
                    processDueRecurringTransactions()
                }
            } catch (exception: Exception) {
                onError(
                    exception.message
                        ?: "Unable to change recurring transaction status."
                )
            }
        }
    }

    fun processDueRecurringTransactions(
        onComplete: (Int) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val createdCount =
                    repository.processDueRecurringTransactions()

                onComplete(createdCount)
            } catch (exception: Exception) {
                onError(
                    exception.message
                        ?: "Unable to process recurring transactions."
                )
            }
        }
    }

    class Factory(
        private val repository: TransactionRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {
            if (
                modelClass.isAssignableFrom(
                    TransactionViewModel::class.java
                )
            ) {
                return TransactionViewModel(repository) as T
            }

            throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}"
            )
        }
    }
}