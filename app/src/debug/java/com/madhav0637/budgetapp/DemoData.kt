package com.madhav0637.budgetapp

import com.madhav0637.budgetapp.data.AppDatabase
import com.madhav0637.budgetapp.data.Expense
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

/** About 11 weeks of believable spending for screenshots, like the iOS DemoData. The same every run, relative to today. */
object DemoData {
    private class Habit(
        val merchant: String,
        val category: String,
        val amounts: IntRange,
        /** Chance of happening on any given day. */
        val chance: Double,
        val hours: IntRange,
    )

    private val habits = listOf(
        Habit("Chai Point", "Food", 40..90, 0.45, 9..11),
        Habit("Zomato", "Food", 280..650, 0.32, 19..22),
        Habit("Swiggy", "Food", 220..540, 0.22, 13..14),
        Habit("Blinkit", "Shopping", 180..900, 0.22, 10..21),
        Habit("Uber", "Transport", 120..380, 0.32, 8..20),
        Habit("Rapido", "Transport", 60..160, 0.28, 8..19),
        Habit("Myntra", "Shopping", 900..3_499, 0.04, 12..23),
        Habit("Amazon", "Shopping", 300..2_200, 0.05, 12..23),
        Habit("PVR", "Entertainment", 350..900, 0.05, 18..21),
        Habit("Apollo Pharmacy", "Health", 150..700, 0.04, 10..20),
    )

    /** Paid on the same day every month. */
    private class Bill(val day: Int, val merchant: String, val category: String, val amount: Long)

    private val bills = listOf(
        Bill(5, "Netflix", "Entertainment", 649),
        Bill(8, "Electricity", "Bills", 2_140),
        Bill(12, "Jio recharge", "Bills", 299),
        Bill(18, "Spotify", "Entertainment", 119),
    )

    private val notes = listOf("team dinner", "split with friends", "birthday gift", "late night 🌙", "weekly groceries")

    /** A few quiet days, so "no-spend days" has something to celebrate. */
    private val quietDaysAgo = setOf(3, 11, 17, 26, 40)

    suspend fun seed(db: AppDatabase, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()) {
        val categories = db.categoryDao().getAll().associateBy { it.name }
        val random = Random(2026)
        val today = now.atZone(zone).toLocalDate()
        val expenses = mutableListOf<Expense>()

        fun at(day: LocalDate, hour: Int, minute: Int): Instant = day.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant()

        for (daysAgo in 0 until 80) {
            val day = today.minusDays(daysAgo.toLong())
            if (daysAgo in quietDaysAgo) continue

            for (habit in habits) {
                if (random.nextDouble() >= habit.chance) continue
                val date = at(day, habit.hours.random(random), random.nextInt(60))
                val category = categories[habit.category] ?: continue
                val note = if (random.nextDouble() < 0.06) notes.random(random) else null
                if (date.isAfter(now)) continue
                expenses += Expense(merchant = habit.merchant, amount = habit.amounts.random(random).toLong(), date = date, categoryId = category.id, note = note)
            }

            for (bill in bills) {
                if (bill.day != day.dayOfMonth) continue
                val date = at(day, 9, 30)
                val category = categories[bill.category] ?: continue
                if (date.isAfter(now)) continue
                expenses += Expense(merchant = bill.merchant, amount = bill.amount, date = date, categoryId = category.id)
            }
        }
        db.expenseDao().insertAll(expenses)
    }
}
