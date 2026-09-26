package com.madhav0637.budgetapp.domain

import com.madhav0637.budgetapp.data.Category
import com.madhav0637.budgetapp.data.ExpenseWithCategory

/** What Home shows for one period: the total, spending per category, and the latest expenses. */
class SpendingSummary(all: List<ExpenseWithCategory>, range: DateRange, recentLimit: Int = 5) {
    data class CategoryTotal(
        val category: Category,
        val amount: Long,
        /** How many expenses make up the amount. */
        val count: Int,
    )

    /** Every expense in the period, in no particular order. */
    val expenses: List<ExpenseWithCategory> = all.filter { it.expense.date in range }

    val total: Long = expenses.sumOf { it.expense.amount }

    /** Highest amount first; ties sorted by category name. Categories with no spending are left out. */
    val categoryTotals: List<CategoryTotal> = expenses
        .groupBy { it.category.id }
        .map { (_, items) -> CategoryTotal(items.first().category, items.sumOf { it.expense.amount }, items.size) }
        .sortedWith(compareByDescending<CategoryTotal> { it.amount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.category.name })

    /** Newest first. */
    val recent: List<ExpenseWithCategory> = expenses.sortedByDescending { it.expense.date }.take(recentLimit)

    /** This category's share of the total, from 0 to 1. */
    fun share(of: CategoryTotal): Double = if (total > 0) of.amount.toDouble() / total else 0.0
}
