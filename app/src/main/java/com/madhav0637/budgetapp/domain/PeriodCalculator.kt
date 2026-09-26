package com.madhav0637.budgetapp.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class Period(
    /** Label on the period pickers. */
    val title: String,
    /** Used in sentences such as "Spent this month". */
    val phrase: String,
    /** The Home period menu: "This month". */
    val menuTitle: String,
    /** Used in "vs same time last month". */
    val previousPhrase: String,
) {
    Week("Week", "this week", "This week", "last week"),
    Month("Month", "this month", "This month", "last month"),
    Year("Year", "this year", "This year", "last year"),
}

/** From [start] up to, but not including, [endExclusive]. */
data class DateRange(val start: Instant, val endExclusive: Instant) {
    operator fun contains(instant: Instant): Boolean = !instant.isBefore(start) && instant.isBefore(endExclusive)

    companion object {
        val Everything = DateRange(Instant.MIN, Instant.MAX)
    }
}

/** Date ranges for the app's periods. Weeks always start on Monday, months on the 1st. */
class PeriodCalculator(val zone: ZoneId = ZoneId.systemDefault()) {
    /** The period containing [containing], from its first instant up to (not including) the next period's first instant. */
    fun range(period: Period, containing: Instant = Instant.now()): DateRange = range(period, 0, containing)

    /** The period [offset] steps away from the one containing [from]: -1 is last week, month or year. */
    fun range(period: Period, offset: Int, from: Instant = Instant.now()): DateRange {
        val first = shift(period, firstDay(period, from.atZone(zone).toLocalDate()), offset.toLong())
        return DateRange(startOf(first), startOf(shift(period, first, 1)))
    }

    /**
     * The start of the previous period up to the same point [now] has reached in the current one.
     * On 26 Sep at 14:00 that's 1 Aug 00:00 to 26 Aug 14:00, so a month in progress is compared fairly.
     * It never runs past the end of the previous period (31 Mar is compared with the whole of February).
     */
    fun samePointInPreviousPeriod(period: Period, now: Instant = Instant.now()): DateRange {
        val current = range(period, now)
        val previous = range(period, -1, now)
        // Measured on the wall clock, so a daylight-saving change can't shift the comparison by an hour.
        val elapsed = Duration.between(current.start.atZone(zone).toLocalDateTime(), now.atZone(zone).toLocalDateTime())
        val end = previous.start.atZone(zone).toLocalDateTime().plus(elapsed).atZone(zone).toInstant()
        return DateRange(previous.start, minOf(end, previous.endExclusive))
    }

    /** Every calendar day in [range]. */
    fun days(range: DateRange): List<LocalDate> {
        val days = mutableListOf<LocalDate>()
        var day = range.start.atZone(zone).toLocalDate()
        while (startOf(day).isBefore(range.endExclusive)) {
            days += day
            day = day.plusDays(1)
        }
        return days
    }

    /** The first instant of [day] in this calculator's time zone. */
    fun startOf(day: LocalDate): Instant = day.atStartOfDay(zone).toInstant()

    private fun firstDay(period: Period, day: LocalDate): LocalDate = when (period) {
        Period.Week -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        Period.Month -> day.withDayOfMonth(1)
        Period.Year -> day.withDayOfYear(1)
    }

    private fun shift(period: Period, first: LocalDate, steps: Long): LocalDate = when (period) {
        Period.Week -> first.plusWeeks(steps)
        Period.Month -> first.plusMonths(steps)
        Period.Year -> first.plusYears(steps)
    }
}
