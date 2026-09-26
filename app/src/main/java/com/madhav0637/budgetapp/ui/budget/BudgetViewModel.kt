package com.madhav0637.budgetapp.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madhav0637.budgetapp.BudgetApplication
import com.madhav0637.budgetapp.data.AppSettings
import com.madhav0637.budgetapp.data.ExpenseDao
import com.madhav0637.budgetapp.domain.BudgetAlerts
import com.madhav0637.budgetapp.domain.Period
import com.madhav0637.budgetapp.domain.PeriodCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Sets, changes or removes the monthly budget. */
class BudgetViewModel(expenseDao: ExpenseDao, private val settings: AppSettings) : ViewModel() {
    val monthlyBudget: StateFlow<Long> = settings.monthlyBudget
    val alertsOn: StateFlow<Boolean> = settings.budgetAlerts

    /** Last month's total, as a guide. */
    val lastMonthSpent: StateFlow<Long> = expenseDao.observeAllWithCategory()
        .map { expenses ->
            val lastMonth = PeriodCalculator().range(Period.Month, -1)
            expenses.filter { it.expense.date in lastMonth }.sumOf { it.expense.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** A new budget starts the month's alerts afresh. */
    fun save(rupees: Long) {
        settings.setMonthlyBudget(rupees)
        BudgetAlerts(settings).reset()
    }

    fun remove() = save(0)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as BudgetApplication
                BudgetViewModel(app.database.expenseDao(), app.settings)
            }
        }
    }
}
