package com.madhav0637.budgetapp.ui.insights

import com.madhav0637.budgetapp.domain.IST
import com.madhav0637.budgetapp.domain.Period
import com.madhav0637.budgetapp.domain.PeriodCalculator
import com.madhav0637.budgetapp.domain.at
import org.junit.Assert.assertEquals
import org.junit.Test

/** The period titles and "vs …" wording on the Insights tab. */
class InsightsTitlesTest {
    private val calculator = PeriodCalculator(IST)
    private val now = at(2026, 9, 26, 14)

    private fun title(period: Period, offset: Int) = InsightsViewModel.title(period, calculator.range(period, offset, now), IST)

    @Test
    fun titles() {
        assertEquals("21 – 27 Sep", title(Period.Week, 0))
        assertEquals("September 2026", title(Period.Month, 0))
        assertEquals("2026", title(Period.Year, 0))
    }

    @Test
    fun aWeekAcrossTwoMonthsNamesBoth() {
        assertEquals("31 Aug – 6 Sep", title(Period.Week, -3))
    }

    @Test
    fun runningPeriodsAreComparedWithTheSameTimeLastPeriod() {
        assertEquals("same time last month", InsightsViewModel.comparedWith(Period.Month, true, calculator.range(Period.Month, -1, now), IST))
    }

    @Test
    fun pastPeriodsNameWhatTheyAreComparedWith() {
        assertEquals("July", InsightsViewModel.comparedWith(Period.Month, false, calculator.range(Period.Month, -2, now), IST))
        assertEquals("the week before", InsightsViewModel.comparedWith(Period.Week, false, calculator.range(Period.Week, -2, now), IST))
        assertEquals("2024", InsightsViewModel.comparedWith(Period.Year, false, calculator.range(Period.Year, -2, now), IST))
    }
}
