package com.example.finora.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class FinoraDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun budgetDao(): BudgetDao

    abstract fun recurringTransactionDao():
            RecurringTransactionDao

    companion object {

        @Volatile
        private var INSTANCE: FinoraDatabase? = null

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS budgets (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            category TEXT NOT NULL,
                            amount REAL NOT NULL,
                            month TEXT NOT NULL,
                            createdAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS
                        index_budgets_category_month
                        ON budgets (category, month)
                        """.trimIndent()
                    )
                }
            }

        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS recurring_transactions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            title TEXT NOT NULL,
                            amount REAL NOT NULL,
                            type TEXT NOT NULL,
                            category TEXT NOT NULL,
                            note TEXT NOT NULL,
                            frequency TEXT NOT NULL,
                            nextRunDate INTEGER NOT NULL,
                            endDate INTEGER,
                            isActive INTEGER NOT NULL,
                            createdAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }
            }

        fun getDatabase(
            context: Context
        ): FinoraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinoraDatabase::class.java,
                    "finora_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3
                    )
                    .build()
                    .also { database ->
                        INSTANCE = database
                    }
            }
        }
    }
}