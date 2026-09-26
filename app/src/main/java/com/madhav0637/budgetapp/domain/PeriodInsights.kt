package com.madhav0637.budgetapp.domain

import com.madhav0637.budgetapp.data.ExpenseWithCategory
import java.time.Instant
import java.time.LocalDate

/** Everything the Insights tab shows for one week, month or year. Same rules as iOS. */
class PeriodInsights(
    expenses: List<ExpenseWithCategory>,
    period: Period,
    val range: DateRange,
    now: Instant = Instant.now(),
    calculator: PeriodCalculator = PeriodCalculator(),
) {
    /** Spending in one bar of the chart: a day, or a month (starting on its 1st) when looking at a whole year. */
    data class Bucket(val start: LocalDate, val amount: Long)

    /** One merchant's spending in the period. Names are matched ignoring case and surrounding spaces. */
    data class Merchant(
        /** The spelling used most recently. */
        val name: String,
        /** The emoji of the category it was most recently logged under. */
        val emoji: String,
        val amount: Long,
        val count: Int,
    ) {
        val key: String get() = MerchantSuggestions.key(name)
    }

    private val summary = SpendingSummary(expenses, range)
    private val zone = calculator.zone

    val total: Long = summary.total
    val count: Int = summary.expenses.size
    val categoryTotals: List<SpendingSummary.CategoryTotal> = summary.categoryTotals

    val bucketsAreMonths: Boolean = period == Period.Year

    private val bucketStarts: List<LocalDate> = if (bucketsAreMonths) {
        val first = range.start.atZone(zone).toLocalDate()
        (0L until 12L).map { first.plusMonths(it) }.filter { calculator.startOf(it) in range }
    } else {
        calculator.days(range)
    }

    /** Covers the whole period, including days that haven't happened yet (they're zero). */
    val buckets: List<Bucket> = run {
        val amounts = summary.expenses.groupBy { bucketStart(it.expense.date) }.mapValues { (_, items) -> items.sumOf { it.expense.amount } }
        bucketStarts.map { Bucket(it, amounts[it] ?: 0) }
    }

    private val end: Instant = minOf(now, range.endExclusive)
    private val elapsed: List<LocalDate> = calculator.days(range).filter { calculator.startOf(it).isBefore(end) }

    /** Days of the period so far, counting today. The whole period once it's over; zero if it hasn't started. */
    val elapsedDays: Int = elapsed.size

    /** Days so far with nothing spent. */
    val noSpendDays: Int = run {
        val daysWithSpending = summary.expenses.map { it.expense.date.atZone(zone).toLocalDate() }.toSet()
        elapsed.count { it !in daysWithSpending }
    }

    /** Total divided by the days so far, rounded to the nearest rupee. */
    val averagePerDay: Long = rounded(total, elapsedDays)

    /** The average bar height so far: per day, or per month for a year. */
    val averagePerBucket: Long = rounded(
        total,
        if (bucketsAreMonths) bucketStarts.count { calculator.startOf(it).isBefore(end) } else elapsedDays,
    )

    /** The single largest expense; the most recent one wins a tie. */
    val biggest: ExpenseWithCategory? =
        summary.expenses.maxWithOrNull(compareBy<ExpenseWithCategory> { it.expense.amount }.thenBy { it.expense.date })

    private val merchants: List<Merchant> = summary.expenses
        .groupBy { MerchantSuggestions.key(it.expense.merchant) }
        .values
        .map { items ->
            val latest = items.maxBy { it.expense.date }
            Merchant(latest.expense.merchant, latest.category.emoji, items.sumOf { it.expense.amount }, items.size)
        }

    /** Highest total first; then most visits, then by name. */
    val topMerchants: List<Merchant> = merchants.sortedWith(
        compareByDescending<Merchant> { it.amount }.thenByDescending { it.count }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
    )

    /** The merchant visited most often; the higher total wins a tie, then the name that sorts first. */
    val mostFrequent: Merchant? = merchants.maxWithOrNull(
        compareBy<Merchant> { it.count }.thenBy { it.amount }.thenByDescending(String.CASE_INSENSITIVE_ORDER) { it.name },
    )

    /** The bar with the most spending, or null when nothing was spent. The earliest wins a tie. */
    val peak: Bucket? get() = buckets.filter { it.amount > 0 }.maxByOrNull { it.amount }

    private fun bucketStart(instant: Instant): LocalDate {
        val day = instant.atZone(zone).toLocalDate()
        return if (bucketsAreMonths) day.withDayOfMonth(1) else day
    }

    companion object {
        /** Spending on each of the last [count] days, ending with today. */
        fun lastDays(
            count: Int,
            expenses: List<ExpenseWithCategory>,
            now: Instant = Instant.now(),
            calculator: PeriodCalculator = PeriodCalculator(),
        ): List<Bucket> {
            val today = now.atZone(calculator.zone).toLocalDate()
            val range = DateRange(calculator.startOf(today.minusDays(count - 1L)), calculator.startOf(today.plusDays(1)))
            val amounts = expenses
                .filter { it.expense.date in range }
                .groupBy { it.expense.date.atZone(calculator.zone).toLocalDate() }
                .mapValues { (_, items) -> items.sumOf { it.expense.amount } }
            return calculator.days(range).map { Bucket(it, amounts[it] ?: 0) }
        }

        private fun rounded(total: Long, count: Int): Long = if (count > 0) Math.round(total.toDouble() / count) else 0
    }
}

/**
 * Compares a period's spending with the one before it. While a period is still running, it's compared with the
 * same stretch of the previous one (1–26 Sep against 1–26 Aug), so half a month isn't measured against a whole one.
 */
class PeriodComparison(
    expenses: List<ExpenseWithCategory>,
    period: Period,
    offset: Int = 0,
    now: Instant = Instant.now(),
    calculator: PeriodCalculator = PeriodCalculator(),
) {
    private val currentRange = calculator.range(period, offset, now)

    /** True for the period that's still running, where only the same stretch of the previous period counts. */
    val isPartial: Boolean = now in currentRange

    private val previousRange =
        if (isPartial) calculator.samePointInPreviousPeriod(period, now) else calculator.range(period, offset - 1, now)

    val current: Long = expenses.filter { it.expense.date in currentRange }.sumOf { it.expense.amount }
    val previous: Long = expenses.filter { it.expense.date in previousRange }.sumOf { it.expense.amount }

    /** The change as a fraction: -0.12 means 12% less. Null when nothing was spent in the previous period. */
    val change: Double? get() = if (previous > 0) (current - previous).toDouble() / previous else null
}
