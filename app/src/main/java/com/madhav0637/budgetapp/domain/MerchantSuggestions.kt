package com.madhav0637.budgetapp.domain

import com.madhav0637.budgetapp.data.ExpenseWithCategory
import java.util.Locale

/** Merchants logged before, offered as one-tap suggestions in Add Expense, with the category each was last used with. */
class MerchantSuggestions(expenses: List<ExpenseWithCategory>) {
    data class Suggestion(val name: String, val categoryId: String) {
        val key: String get() = key(name)
    }

    /** Unique merchants (ignoring case and surrounding spaces), most recently used first. */
    val all: List<Suggestion> = expenses
        .sortedByDescending { it.expense.date }
        .distinctBy { key(it.expense.merchant) }
        .filter { key(it.expense.merchant).isNotEmpty() }
        .map { Suggestion(it.expense.merchant, it.category.id) }

    /**
     * With nothing typed, the most recent merchants. Otherwise merchants containing the text (ignoring case and
     * accents), those starting with it first, leaving out one that already matches exactly.
     */
    fun matching(text: String, limit: Int = 8): List<Suggestion> {
        val query = text.trim()
        if (query.isEmpty()) return all.take(limit)
        val key = key(query)
        val folded = foldForSearch(query)
        val matches = all.filter { it.key != key && foldForSearch(it.name).contains(folded) }
        val (startsWith, others) = matches.partition { foldForSearch(it.name).startsWith(folded) }
        return (startsWith + others).take(limit)
    }

    /** The category this merchant was most recently logged under, or null for a new merchant. */
    fun categoryId(merchant: String): String? {
        val key = key(merchant)
        return all.firstOrNull { it.key == key }?.categoryId
    }

    companion object {
        /** How merchant names are compared: trimmed and lowercased. */
        fun key(merchant: String): String = merchant.trim().lowercase(Locale.ROOT)
    }
}
