package com.madhav0637.budgetapp

import android.app.Application
import android.content.Context
import com.madhav0637.budgetapp.data.AppDatabase
import com.madhav0637.budgetapp.data.AppSettings
import com.madhav0637.budgetapp.domain.BudgetService
import com.madhav0637.budgetapp.domain.CategoryService
import com.madhav0637.budgetapp.domain.ExpenseService

/** Creates the database, settings and services once, for every screen (and the quick-entry pop-up) to share. */
class BudgetApplication : Application() {
    /** Set only by debug builds launched with sample data. See [useSampleData]. */
    private var sampleDatabase: (() -> AppDatabase)? = null

    private val databaseLazy = lazy { sampleDatabase?.invoke() ?: AppDatabase.create(this) }
    private val settingsLazy = lazy {
        AppSettings(getSharedPreferences(if (sampleDatabase != null) "settings-sample" else "settings", Context.MODE_PRIVATE))
    }

    val database: AppDatabase by databaseLazy
    val settings: AppSettings by settingsLazy
    val expenseService: ExpenseService by lazy { ExpenseService(database.expenseDao()) }
    val categoryService: CategoryService by lazy { CategoryService(database.categoryDao()) }
    val budgetService: BudgetService by lazy { BudgetService(database.expenseDao(), settings) }

    /**
     * Debug builds only (see LaunchOptions in src/debug): swaps in a throwaway in-memory database and a separate
     * settings file, so screenshots never touch real expenses or settings. Only works before anything has opened the
     * real database or settings in this process; returns false otherwise.
     */
    fun useSampleData(createDatabase: () -> AppDatabase): Boolean {
        if (databaseLazy.isInitialized() || settingsLazy.isInitialized()) return false
        sampleDatabase = createDatabase
        return true
    }
}
