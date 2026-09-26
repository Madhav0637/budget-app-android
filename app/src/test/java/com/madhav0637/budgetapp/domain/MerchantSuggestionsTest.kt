package com.madhav0637.budgetapp.domain

import com.madhav0637.budgetapp.domain.KeypadInput.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mirrors the iOS MerchantSuggestionsTests. */
class MerchantSuggestionsTest {
    private val food = category("Food")
    private val transport = category("Transport")
    private val suggestions = MerchantSuggestions(
        listOf(
            expense(300, food, "Zomato", at(2026, 9, 20)),
            expense(180, transport, "Uber", at(2026, 9, 21)),
            expense(90, food, "Café Coffee Day", at(2026, 9, 22)),
            expense(400, transport, "zomato", at(2026, 9, 23)), // same merchant, newer
        ).shuffled(),
    )

    @Test
    fun uniqueMostRecentFirst() {
        assertEquals(listOf("zomato", "Café Coffee Day", "Uber"), suggestions.all.map { it.name })
    }

    @Test
    fun emptyTextGivesTheMostRecent() {
        assertEquals(listOf("zomato", "Café Coffee Day"), suggestions.matching("", limit = 2).map { it.name })
    }

    @Test
    fun matchesIgnoreCaseAndAccentsAndPreferPrefixes() {
        assertEquals(listOf("Café Coffee Day"), suggestions.matching("cafe").map { it.name })
        assertEquals(listOf("zomato", "Café Coffee Day"), suggestions.matching("o").map { it.name }) // no prefix match: recency
        assertEquals(listOf("Uber"), suggestions.matching("U").map { it.name })
    }

    @Test
    fun namesStartingWithTheTextComeBeforeMoreRecentOnes() {
        val subway = MerchantSuggestions(
            listOf(expense(200, food, "Subway", at(2026, 9, 1)), expense(250, food, "Lunch at Subway", at(2026, 9, 20))),
        )
        assertEquals(listOf("Subway", "Lunch at Subway"), subway.matching("sub").map { it.name })
    }

    @Test
    fun exactMatchIsLeftOut() {
        assertTrue(suggestions.matching(" UBER ").isEmpty())
    }

    @Test
    fun categoryComesFromTheLatestUse() {
        assertEquals(transport.id, suggestions.categoryId("ZOMATO "))
        assertEquals(transport.id, suggestions.categoryId("uber"))
        assertNull(suggestions.categoryId("Blinkit"))
    }

    @Test
    fun blankMerchantsAreIgnored() {
        assertTrue(MerchantSuggestions(listOf(expense(10, food, "  "))).all.isEmpty())
    }
}

/** Mirrors the iOS KeypadInputTests. */
class KeypadInputTest {
    private fun type(keys: List<Key>, from: String = ""): String = keys.fold(from) { text, key -> KeypadInput.apply(key, text) }

    @Test
    fun digitsAppend() {
        assertEquals("420", type(listOf(Key.Digit(4), Key.Digit(2), Key.Digit(0))))
    }

    @Test
    fun noLeadingZeros() {
        assertEquals("5", type(listOf(Key.Digit(0), Key.DoubleZero, Key.Digit(5))))
    }

    @Test
    fun doubleZeroAddsTwoZeros() {
        assertEquals("500", type(listOf(Key.Digit(5), Key.DoubleZero)))
    }

    @Test
    fun deleteRemovesTheLastDigit() {
        assertEquals("42", type(listOf(Key.Delete), from = "420"))
        assertEquals("", type(listOf(Key.Delete), from = ""))
    }

    @Test
    fun stopsAtNineDigits() {
        assertEquals("123456789", type(listOf(Key.Digit(1)), from = "123456789"))
        assertEquals("123456780", type(listOf(Key.DoubleZero), from = "12345678"))
        assertEquals("123456789", type(listOf(Key.DoubleZero), from = "123456789"))
    }
}
