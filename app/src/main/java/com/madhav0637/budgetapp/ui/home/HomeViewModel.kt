package com.madhav0637.budgetapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madhav0637.budgetapp.BudgetApplication
import com.madhav0637.budgetapp.data.AppSettings
import com.madhav0637.budgetapp.data.ExpenseDao
import com.madhav0637.budgetapp.domain.BudgetPace
import com.madhav0637.budgetapp.domain.Period
import com.madhav0637.budgetapp.domain.PeriodCalculator
import com.madhav0637.budgetapp.domain.PeriodComparison
import com.madhav0637.budgetapp.domain.PeriodInsights
import com.madhav0637.budgetapp.domain.SpendingSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.format.TextStyle
import java.util.Locale

data class HomeUiState(
    val period: Period = Period.Month,
    val summary: SpendingSummary? = null,
    val comparison: PeriodComparison? = null,
    /** Spending on each of the last 7 days, ending with today. */
    val lastWeek: List<PeriodInsights.Bucket> = emptyList(),
    /** Null when no monthly budget is set. */
    val budget: BudgetPace? = null,
    /** "September", for "September budget". */
    val monthName: String = "",
)

class HomeViewModel(expenseDao: ExpenseDao, private val settings: AppSettings) : ViewModel() {
    val state: StateFlow<HomeUiState> =
        combine(expenseDao.observeAllWithCategory(), settings.homePeriod, settings.monthlyBudget) { expenses, period, budget ->
            val now = Instant.now()
            val calculator = PeriodCalculator()
            val month = calculator.range(Period.Month, now)
            HomeUiState(
                period = period,
                summary = SpendingSummary(expenses, calculator.range(period, now)),
                comparison = PeriodComparison(expenses, period, now = now, calculator = calculator),
                lastWeek = PeriodInsights.lastDays(7, expenses, now, calculator),
                budget = if (budget > 0) {
                    BudgetPace(budget, expenses.filter { it.expense.date in month }.sumOf { it.expense.amount }, month, now, calculator.zone)
                } else {
                    null
                },
                monthName = now.atZone(calculator.zone).month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(period = settings.homePeriod.value))

    /** Remembered between launches, like iOS. */
    fun setPeriod(value: Period) = settings.setHomePeriod(value)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as BudgetApplication
                HomeViewModel(app.database.expenseDao(), app.settings)
            }
        }
    }
}
