package com.madhav0637.budgetapp.domain

object DefaultCategories {
    data class Item(val emoji: String, val name: String)

    val all = listOf(
        Item("🍔", "Food"),
        Item("🚕", "Transport"),
        Item("🛍️", "Shopping"),
        Item("🧾", "Bills"),
        Item("🎬", "Entertainment"),
        Item("💊", "Health"),
        Item("📦", "Other"),
    )
}

sealed class ExpenseError(message: String) : Exception(message) {
    data object EmptyMerchant : ExpenseError("Please enter what you spent on.")
    data object NonPositiveAmount : ExpenseError("Amount must be more than ₹0.")
    data object CategoryNotFound : ExpenseError("That category no longer exists.")
}

object ExpenseRules {
    /** Returns the trimmed merchant, or throws if the merchant or amount isn't allowed. */
    fun validated(merchant: String, amount: Long): String {
        val trimmed = merchant.trim()
        if (trimmed.isEmpty()) throw ExpenseError.EmptyMerchant
        if (amount <= 0) throw ExpenseError.NonPositiveAmount
        return trimmed
    }

    /** Trims a note, turning a blank one into null. */
    fun cleanedNote(note: String?): String? = note?.trim()?.ifEmpty { null }
}
