package com.madhav0637.budgetapp.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madhav0637.budgetapp.BudgetApplication
import com.madhav0637.budgetapp.data.AppSettings
import com.madhav0637.budgetapp.data.ExpenseDao
import com.madhav0637.budgetapp.domain.DateRange
import com.madhav0637.budgetapp.domain.Period
import com.madhav0637.budgetapp.domain.PeriodCalculator
import com.madhav0637.budgetapp.domain.PeriodComparison
import com.madhav0637.budgetapp.domain.PeriodInsights
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class InsightsUiState(
    val period: Period = Period.Month,
    /** 0 is the current period, -1 the one before, and so on. Never positive. */
    val offset: Int = 0,
    /** "21 – 27 Sep", "September 2026", "2026". */
    val title: String = "",
    val insights: PeriodInsights? = null,
    val comparison: PeriodComparison? = null,
    /** "same time last month", "August", "the week before", "2025". */
    val comparedWith: String = "",
    /** Which way the period title slides when moving between periods. */
    val movingBack: Boolean = true,
)

class InsightsViewModel(expenseDao: ExpenseDao, private val settings: AppSettings) : ViewModel() {
    private val offset = MutableStateFlow(0)
    private var movingBack = true

    val state: StateFlow<InsightsUiState> =
        combine(expenseDao.observeAllWithCategory(), settings.insightsPeriod, offset) { expenses, period, offset ->
            val now = Instant.now()
            val calculator = PeriodCalculator()
            val range = calculator.range(period, offset, now)
            val comparison = PeriodComparison(expenses, period, offset, now, calculator)
            InsightsUiState(
                period = period,
                offset = offset,
                title = title(period, range, calculator.zone),
                insights = PeriodInsights(expenses, period, range, now, calculator),
                comparison = comparison,
                comparedWith = comparedWith(period, comparison.isPartial, calculator.range(period, offset - 1, now), calculator.zone),
                movingBack = movingBack,
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState(period = settings.insightsPeriod.value))

    /** Remembered between launches. Changing it goes back to the current period. */
    fun setPeriod(value: Period) {
        movingBack = true
        offset.value = 0
        settings.setInsightsPeriod(value)
    }

    /** One period back, or forward (never past the current one). */
    fun step(back: Boolean) {
        if (!back && offset.value >= 0) return
        movingBack = back
        offset.value += if (back) -1 else 1
    }

    companion object {
        private val dayOnly = DateTimeFormatter.ofPattern("d", Locale.ENGLISH)
        private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
        private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        private val monthName = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
        private val year = DateTimeFormatter.ofPattern("yyyy", Locale.ENGLISH)

        fun title(period: Period, range: DateRange, zone: ZoneId): String {
            val first = range.start.atZone(zone).toLocalDate()
            return when (period) {
                Period.Week -> {
                    val last = range.endExclusive.atZone(zone).toLocalDate().minusDays(1)
                    val sameMonth = first.month == last.month && first.year == last.year
                    "${(if (sameMonth) dayOnly else dayMonth).format(first)} – ${dayMonth.format(last)}"
                }
                Period.Month -> monthYear.format(first)
                Period.Year -> year.format(first)
            }
        }

        fun comparedWith(period: Period, isPartial: Boolean, previous: DateRange, zone: ZoneId): String {
            if (isPartial) return "same time ${period.previousPhrase}"
            val start = previous.start.atZone(zone)
            return when (period) {
                Period.Week -> "the week before"
                Period.Month -> monthName.format(start)
                Period.Year -> year.format(start)
            }
        }

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as BudgetApplication
                InsightsViewModel(app.database.expenseDao(), app.settings)
            }
        }
    }
}
