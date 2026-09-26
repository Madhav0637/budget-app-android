package com.madhav0637.budgetapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Mirrors the iOS PeriodInsightsTests: "now" is 26 Sep 2026, 14:00 IST. */
class PeriodInsightsTest {
    private val calculator = PeriodCalculator(IST)
    private val now = at(2026, 9, 26, 14)
    private val food = category("Food", "🍔")
    private val transport = category("Transport", "🚕")

    private fun insights(expenses: List<com.madhav0637.budgetapp.data.ExpenseWithCategory>, period: Period = Period.Month, offset: Int = 0) =
        PeriodInsights(expenses, period, calculator.range(period, offset, now), now, calculator)

    // MARK: Period offsets

    @Test
    fun offsetMovesWholePeriods() {
        assertEquals(at(2026, 8, 1, 0), calculator.range(Period.Month, -1, now).start)
        assertEquals(at(2025, 12, 1, 0), calculator.range(Period.Month, -9, now).start)
        assertEquals(at(2026, 9, 14, 0), calculator.range(Period.Week, -1, now).start)
        assertEquals(at(2025, 1, 1, 0), calculator.range(Period.Year, -1, now).start)
    }

    @Test
    fun offsetFromTheLastDayOfALongMonth() {
        // 31 March minus one month must be February, not early March.
        assertEquals(at(2026, 2, 1, 0), calculator.range(Period.Month, -1, at(2026, 3, 31)).start)
    }

    @Test
    fun offsetZeroIsTheCurrentPeriod() {
        assertEquals(calculator.range(Period.Week, now), calculator.range(Period.Week, 0, now))
    }

    @Test
    fun samePointLastMonth() {
        val range = calculator.samePointInPreviousPeriod(Period.Month, now)
        assertEquals(at(2026, 8, 1, 0), range.start)
        assertEquals(at(2026, 8, 26, 14), range.endExclusive)
    }

    @Test
    fun samePointNeverRunsPastThePreviousPeriod() {
        val range = calculator.samePointInPreviousPeriod(Period.Month, at(2026, 3, 31, 12))
        assertEquals(at(2026, 3, 1, 0), range.endExclusive)
    }

    @Test
    fun samePointLastWeek() {
        // Saturday 14:00 → the previous Monday 00:00 to Saturday 14:00.
        val range = calculator.samePointInPreviousPeriod(Period.Week, now)
        assertEquals(at(2026, 9, 14, 0), range.start)
        assertEquals(at(2026, 9, 19, 14), range.endExclusive)
    }

    // MARK: Comparison

    @Test
    fun runningMonthIsComparedWithTheSameStretchOfLastMonth() {
        val expenses = listOf(
            expense(900, food, date = at(2026, 9, 10)),
            expense(1000, food, date = at(2026, 8, 10)),
            expense(5000, food, date = at(2026, 8, 28)), // after 26 Aug: not counted
        )
        val comparison = PeriodComparison(expenses, Period.Month, now = now, calculator = calculator)
        assertTrue(comparison.isPartial)
        assertEquals(900L, comparison.current)
        assertEquals(1000L, comparison.previous)
        assertEquals(-0.1, comparison.change!!, 1e-9)
    }

    @Test
    fun pastMonthIsComparedWithTheWholeMonthBefore() {
        val expenses = listOf(expense(1500, food, date = at(2026, 8, 28)), expense(1000, food, date = at(2026, 7, 30)))
        val comparison = PeriodComparison(expenses, Period.Month, offset = -1, now = now, calculator = calculator)
        assertFalse(comparison.isPartial)
        assertEquals(1500L, comparison.current)
        assertEquals(1000L, comparison.previous)
        assertEquals(0.5, comparison.change!!, 1e-9)
    }

    @Test
    fun noChangeWithoutEarlierSpending() {
        val expenses = listOf(expense(900, food, date = at(2026, 9, 10)))
        assertNull(PeriodComparison(expenses, Period.Month, now = now, calculator = calculator).change)
    }

    // MARK: Buckets and stats

    @Test
    fun monthHasOneBucketPerDayIncludingFutureDays() {
        val result = insights(listOf(expense(100, food, date = at(2026, 9, 2, 9)), expense(50, food, date = at(2026, 9, 2, 20))))
        assertEquals(30, result.buckets.size)
        assertEquals(150L, result.buckets[1].amount)
        assertEquals(0L, result.buckets.last().amount)
        assertFalse(result.bucketsAreMonths)
    }

    @Test
    fun weekHasSevenBucketsStartingMonday() {
        val result = insights(listOf(expense(100, food, date = at(2026, 9, 21, 9))), Period.Week)
        assertEquals(7, result.buckets.size)
        assertEquals(LocalDate.of(2026, 9, 21), result.buckets.first().start)
        assertEquals(100L, result.buckets.first().amount)
    }

    @Test
    fun yearHasOneBucketPerMonth() {
        val result = insights(listOf(expense(100, food, date = at(2026, 2, 14))), Period.Year)
        assertEquals(12, result.buckets.size)
        assertEquals(100L, result.buckets[1].amount)
        assertEquals(LocalDate.of(2026, 2, 1), result.buckets[1].start)
        assertTrue(result.bucketsAreMonths)
    }

    @Test
    fun averagesAndNoSpendDaysCountOnlyDaysSoFar() {
        // 26 days so far in September; spending on 2 of them.
        val result = insights(listOf(expense(1300, food, date = at(2026, 9, 1)), expense(1300, food, date = at(2026, 9, 26, 9))))
        assertEquals(26, result.elapsedDays)
        assertEquals(24, result.noSpendDays)
        assertEquals(100L, result.averagePerDay)
        assertEquals(100L, result.averagePerBucket)
    }

    @Test
    fun yearAveragesPerMonthSoFar() {
        // January to September have started: 9 months.
        val result = insights(listOf(expense(9000, food, date = at(2026, 3, 1))), Period.Year)
        assertEquals(1000L, result.averagePerBucket)
    }

    @Test
    fun pastPeriodCountsEveryDay() {
        val result = insights(listOf(expense(3100, food, date = at(2026, 8, 15))), offset = -1)
        assertEquals(31, result.elapsedDays)
        assertEquals(100L, result.averagePerDay)
    }

    @Test
    fun biggestAndPeak() {
        val result = insights(
            listOf(
                expense(200, food, "Small", at(2026, 9, 3)),
                expense(3499, food, "Myntra", at(2026, 9, 12)),
                expense(300, food, "Chai", at(2026, 9, 12, 18)),
            ),
        )
        assertEquals("Myntra", result.biggest?.expense?.merchant)
        assertEquals(LocalDate.of(2026, 9, 12), result.peak?.start)
        assertEquals(3799L, result.peak?.amount)
    }

    @Test
    fun biggestTieGoesToTheMostRecent() {
        val result = insights(listOf(expense(500, food, "Older", at(2026, 9, 3)), expense(500, food, "Newer", at(2026, 9, 5))))
        assertEquals("Newer", result.biggest?.expense?.merchant)
    }

    @Test
    fun merchantsAreGroupedIgnoringCase() {
        val result = insights(
            listOf(
                expense(300, food, "Zomato", at(2026, 9, 3)),
                expense(200, food, "zomato ", at(2026, 9, 5)),
                expense(100, food, "ZOMATO", at(2026, 9, 7)),
                expense(900, transport, "Uber", at(2026, 9, 8)),
            ),
        )
        assertEquals(listOf("Uber", "ZOMATO"), result.topMerchants.map { it.name }) // latest spelling
        assertEquals(listOf(1, 3), result.topMerchants.map { it.count })
        assertEquals(600L, result.topMerchants.last().amount)
        assertEquals("🚕", result.topMerchants.first().emoji)
        assertEquals("ZOMATO", result.mostFrequent?.name)
    }

    @Test
    fun emptyPeriod() {
        val result = insights(emptyList())
        assertEquals(0L, result.total)
        assertNull(result.peak)
        assertNull(result.biggest)
        assertTrue(result.topMerchants.isEmpty())
        assertEquals(26, result.noSpendDays)
    }

    @Test
    fun lastSevenDaysEndWithToday() {
        val expenses = listOf(
            expense(420, food, date = at(2026, 9, 26, 9)),
            expense(100, food, date = at(2026, 9, 20, 9)),
            expense(999, food, date = at(2026, 9, 19, 9)), // 8 days ago
        )
        val days = PeriodInsights.lastDays(7, expenses, now, calculator)
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 9, 20), days.first().start)
        assertEquals(listOf(100L, 0L, 0L, 0L, 0L, 0L, 420L), days.map { it.amount })
    }
}
