package com.madhav0637.budgetapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

/** Mirrors the iOS BudgetTests. The alert record is a plain object here; the app keeps it in SharedPreferences. */
class BudgetTest {
    private class MemoryRecord : BudgetAlerts.Record {
        override var lastAlertMonth: String? = null
        override var lastAlertLevel: Int = 0
    }

    private val september = PeriodCalculator(IST).range(Period.Month, at(2026, 9, 1))
    private val record = MemoryRecord()
    private val alerts = BudgetAlerts(record)
    private val sep = YearMonth.of(2026, 9)

    // MARK: Pace

    @Test
    fun paceOnThe26th() {
        val pace = BudgetPace(25_000, 18_420, september, at(2026, 9, 26, 14), IST)
        assertEquals(5, pace.daysLeft) // 26th to 30th
        assertEquals(6_580L, pace.remaining)
        assertEquals(1_316L, pace.perDay)
        assertFalse(pace.isOver)
        assertEquals(0.7368, pace.progress, 0.001)
    }

    @Test
    fun paceOnTheLastDay() {
        val pace = BudgetPace(1_000, 400, september, at(2026, 9, 30, 23, 59), IST)
        assertEquals(1, pace.daysLeft)
        assertEquals(600L, pace.perDay)
    }

    @Test
    fun paceBeforeTheMonthStartsCountsTheWholeMonth() {
        assertEquals(30, BudgetPace(1_000, 0, september, at(2026, 8, 20), IST).daysLeft)
    }

    @Test
    fun overBudget() {
        val pace = BudgetPace(1_000, 1_250, september, at(2026, 9, 10), IST)
        assertTrue(pace.isOver)
        assertEquals(-250L, pace.remaining)
        assertEquals(0L, pace.perDay)
        assertEquals(1.25, pace.progress, 1e-9)
    }

    // MARK: Alert levels

    @Test
    fun levels() {
        assertNull(BudgetAlerts.level(1000, 799))
        assertEquals(BudgetAlerts.Level.Nearly, BudgetAlerts.level(1000, 800))
        assertEquals(BudgetAlerts.Level.Nearly, BudgetAlerts.level(1000, 999))
        assertEquals(BudgetAlerts.Level.Over, BudgetAlerts.level(1000, 1000))
        assertNull(BudgetAlerts.level(0, 5000))
    }

    @Test
    fun alertsOnceAt80AndOnceAt100() {
        assertEquals(BudgetAlerts.Level.Nearly, alerts.check(1000, 700, 850, sep)?.level)
        assertNull(alerts.check(1000, 850, 900, sep)) // already told
        assertEquals(BudgetAlerts.Level.Over, alerts.check(1000, 900, 1100, sep)?.level)
        assertNull(alerts.check(1000, 1100, 1500, sep))
    }

    @Test
    fun jumpingPast100AlertsOnlyFor100() {
        assertEquals(BudgetAlerts.Alert(BudgetAlerts.Level.Over, 1000, 1200, sep), alerts.check(1000, 500, 1200, sep))
        assertNull(alerts.check(1000, 1200, 1300, sep))
    }

    @Test
    fun noAlertWhenSpendingGoesDown() {
        assertNull(alerts.check(1000, 1200, 900, sep))
    }

    @Test
    fun aNewMonthStartsAfresh() {
        assertNotNull(alerts.check(1000, 0, 900, sep))
        assertNotNull(alerts.check(1000, 0, 900, YearMonth.of(2026, 10)))
    }

    @Test
    fun resetForgetsWhatWasSent() {
        alerts.check(1000, 0, 900, sep)
        alerts.reset()
        assertEquals(BudgetAlerts.Level.Nearly, alerts.check(1000, 850, 900, sep)?.level)
    }

    @Test
    fun theRecordIsSharedBetweenInstances() {
        // The app and the quick-entry pop-up each make their own BudgetAlerts; they must not both alert.
        assertNotNull(BudgetAlerts(record).check(1000, 0, 850, sep))
        assertNull(BudgetAlerts(record).check(1000, 850, 900, sep))
        assertEquals("2026-09", record.lastAlertMonth)
        assertEquals(80, record.lastAlertLevel)
    }

    @Test
    fun messagesNameTheMonthAndAmounts() {
        val nearly = BudgetAlerts.message(BudgetAlerts.Alert(BudgetAlerts.Level.Nearly, 25_000, 20_500, sep))
        assertEquals("80% of your September budget is used", nearly.title)
        assertEquals("₹20,500 of ₹25,000 spent. ₹4,500 left for the rest of the month.", nearly.body)

        val over = BudgetAlerts.message(BudgetAlerts.Alert(BudgetAlerts.Level.Over, 25_000, 26_000, sep))
        assertEquals("You've gone over your September budget", over.title)
        assertTrue(over.body.contains("₹1,000 more"))

        val exactly = BudgetAlerts.message(BudgetAlerts.Alert(BudgetAlerts.Level.Over, 25_000, 25_000, sep))
        assertEquals("You've spent your whole ₹25,000 budget.", exactly.body)
    }
}
