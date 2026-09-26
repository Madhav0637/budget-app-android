package com.madhav0637.budgetapp.domain

import com.madhav0637.budgetapp.data.ExpenseDao
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** How this month's spending compares with the monthly budget. */
class BudgetPace(
    val budget: Long,
    val spent: Long,
    month: DateRange,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
) {
    /** Days left in the month, counting today. */
    val daysLeft: Int = run {
        val today = maxOf(now.atZone(zone).toLocalDate(), month.start.atZone(zone).toLocalDate())
        ChronoUnit.DAYS.between(today, month.endExclusive.atZone(zone).toLocalDate()).toInt().coerceAtLeast(0)
    }

    /** Negative once over budget. */
    val remaining: Long get() = budget - spent
    val isOver: Boolean get() = spent > budget

    /** How much of the budget is used: 0.74 is 74%. Goes above 1 when over budget. */
    val progress: Double get() = if (budget > 0) spent.toDouble() / budget else 0.0

    /** What can be spent on each remaining day (including today) to finish the month on budget. */
    val perDay: Long get() = if (daysLeft > 0 && remaining > 0) remaining / daysLeft else 0
}

/** The budget settings that saving an expense needs. [com.madhav0637.budgetapp.data.AppSettings] provides them. */
interface BudgetSettings : BudgetAlerts.Record {
    /** Whole rupees; 0 means no budget. */
    val currentBudget: Long
    val alertsEnabled: Boolean
}

/**
 * Decides when to warn about the monthly budget: once when 80% is used and once when it's all used, each at most
 * once a month. What it has sent is remembered in the settings, so the app and the quick-entry pop-up share the record.
 */
class BudgetAlerts(private val record: Record) {
    /** Where the last alert is remembered. SharedPreferences in the app, a plain object in tests. */
    interface Record {
        /** "yyyy-MM", or null when nothing has been sent. */
        var lastAlertMonth: String?
        /** 80 or 100, or 0 when nothing has been sent. */
        var lastAlertLevel: Int
    }

    enum class Level(val percent: Int) {
        Nearly(80),
        Over(100),
    }

    data class Alert(val level: Level, val budget: Long, val spent: Long, val month: YearMonth)

    data class Message(val title: String, val body: String)

    /**
     * Call after spending in [month] changed from [before] to [after]. Returns an alert when the increase reached a
     * level not yet alerted this month, and records it. Going straight past 100% alerts only once, for 100%.
     */
    fun check(budget: Long, before: Long, after: Long, month: YearMonth): Alert? {
        if (after <= before) return null
        val level = level(budget, after) ?: return null
        val key = month.toString()
        val alreadySent = if (record.lastAlertMonth == key) record.lastAlertLevel else 0
        if (level.percent <= alreadySent) return null
        record.lastAlertMonth = key
        record.lastAlertLevel = level.percent
        return Alert(level, budget, after, month)
    }

    /** Forgets which alerts were sent, e.g. after the budget changes. */
    fun reset() {
        record.lastAlertMonth = null
        record.lastAlertLevel = 0
    }

    companion object {
        /** The highest level reached: 80% or more is [Level.Nearly], 100% or more is [Level.Over]. */
        fun level(budget: Long, spent: Long): Level? = when {
            budget <= 0 -> null
            spent >= budget -> Level.Over
            spent * 100 >= budget * 80 -> Level.Nearly
            else -> null
        }

        /** The notification or banner text for an alert. */
        fun message(alert: Alert): Message {
            val month = alert.month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            return when (alert.level) {
                Level.Nearly -> Message(
                    "80% of your $month budget is used",
                    "${alert.spent.inr()} of ${alert.budget.inr()} spent. ${(alert.budget - alert.spent).inr()} left for the rest of the month.",
                )
                Level.Over -> {
                    val over = alert.spent - alert.budget
                    Message(
                        "You've gone over your $month budget",
                        if (over > 0) {
                            "${alert.spent.inr()} spent, ${over.inr()} more than your ${alert.budget.inr()} budget."
                        } else {
                            "You've spent your whole ${alert.budget.inr()} budget."
                        },
                    )
                }
            }
        }
    }
}

/** Checks for budget alerts around a change to the expenses. Only this month's spending counts. */
class BudgetService(
    private val dao: ExpenseDao,
    private val settings: BudgetSettings,
    private val calculator: PeriodCalculator = PeriodCalculator(),
) {
    suspend fun spent(inMonthOf: Instant = Instant.now()): Long {
        val month = calculator.range(Period.Month, inMonthOf)
        return dao.totalBetween(month.start, month.endExclusive)
    }

    /** Runs [change] (saving an expense), then returns the alert it triggered, if any. */
    suspend fun alertAround(now: Instant = Instant.now(), change: suspend () -> Unit): BudgetAlerts.Alert? {
        val before = spent(now)
        change()
        val budget = settings.currentBudget
        if (!settings.alertsEnabled || budget <= 0) return null
        val after = spent(now)
        return BudgetAlerts(settings).check(budget, before, after, YearMonth.from(now.atZone(calculator.zone)))
    }
}
