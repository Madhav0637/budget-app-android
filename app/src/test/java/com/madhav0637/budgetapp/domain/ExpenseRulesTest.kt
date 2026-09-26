package com.madhav0637.budgetapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ExpenseRulesTest {
    @Test
    fun trimsTheMerchant() {
        assertEquals("Chai Point", ExpenseRules.validated("  Chai Point \n", 99))
    }

    @Test
    fun rejectsAnEmptyMerchant() {
        for (merchant in listOf("", "   ", "\n\t")) {
            assertThrows(ExpenseError.EmptyMerchant::class.java) { ExpenseRules.validated(merchant, 100) }
        }
    }

    @Test
    fun rejectsAmountsOfZeroOrLess() {
        for (amount in listOf(0L, -1L, -500L)) {
            assertThrows(ExpenseError.NonPositiveAmount::class.java) { ExpenseRules.validated("Uber", amount) }
        }
    }

    @Test
    fun errorMessagesMatchIOS() {
        assertEquals("Please enter what you spent on.", ExpenseError.EmptyMerchant.message)
        assertEquals("Amount must be more than ₹0.", ExpenseError.NonPositiveAmount.message)
        assertEquals("That category no longer exists.", ExpenseError.CategoryNotFound.message)
    }

    @Test
    fun notesAreTrimmed() {
        assertEquals("team dinner", ExpenseRules.cleanedNote("  team dinner \n"))
    }

    @Test
    fun blankNotesBecomeNull() {
        for (note in listOf(null, "", "   ", "\n\t")) assertNull(ExpenseRules.cleanedNote(note))
    }

    @Test
    fun sevenDefaultCategories() {
        assertEquals(
            listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Other"),
            DefaultCategories.all.map { it.name },
        )
    }
}

class FormattingTest {
    @Test
    fun indianDigitGrouping() {
        val cases = mapOf(
            0L to "₹0", 9L to "₹9", 250L to "₹250", 999L to "₹999",
            1_000L to "₹1,000", 12_340L to "₹12,340", 1_23_456L to "₹1,23,456",
            12_34_567L to "₹12,34,567", 1_00_00_000L to "₹1,00,00,000",
        )
        for ((amount, expected) in cases) assertEquals(expected, amount.inr())
    }

    @Test
    fun negativeAmounts() {
        assertEquals("-₹1,250", (-1250L).inr())
    }

    @Test
    fun amountBoxKeepsOnlyDigits() {
        assertEquals("1250", digitsOnly("₹1,250.-x"))
    }

    @Test
    fun groupingWithoutTheRupeeSign() {
        assertEquals("19,168", 19_168L.indianGrouped())
        assertEquals("1,23,456", 1_23_456L.indianGrouped())
        assertEquals("0", 0L.indianGrouped())
    }

    @Test
    fun countsUseTheRightWord() {
        assertEquals("1 expense", counted(1, "expense"))
        assertEquals("3 expenses", counted(3, "expense"))
        assertEquals("2 categories", counted(2, "category", "categories"))
    }

    @Test
    fun sharesAreWholePercentages() {
        assertEquals("26%", percent(0.2649))
        assertEquals("0%", percent(0.0))
        assertEquals("100%", percent(1.0))
    }

    @Test
    fun dayOrTimeForRowsWithoutADayHeading() {
        val now = at(2026, 9, 26, 14)
        assertEquals("9:39 PM", dayOrTime(at(2026, 9, 26, 21, 39), now, IST))
        assertEquals("Yesterday", dayOrTime(at(2026, 9, 25, 9), now, IST))
        assertEquals("21 Sep", dayOrTime(at(2026, 9, 21), now, IST))
        assertEquals("31 Dec 2025", dayOrTime(at(2025, 12, 31), now, IST))
    }
}
