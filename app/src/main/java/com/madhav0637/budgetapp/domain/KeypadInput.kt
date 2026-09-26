package com.madhav0637.budgetapp.domain

/** Typing rupee amounts on the custom keypad, kept free of UI code so it can be tested. */
object KeypadInput {
    sealed interface Key {
        data class Digit(val value: Int) : Key
        data object DoubleZero : Key
        data object Delete : Key
    }

    /** Up to ₹99,99,99,999. */
    const val MAX_DIGITS = 9

    /** The digits after pressing [key]. No leading zeros, and nothing beyond [MAX_DIGITS]. */
    fun apply(key: Key, text: String): String = when (key) {
        is Key.Digit ->
            if ((text.isEmpty() && key.value == 0) || text.length >= MAX_DIGITS) text else text + key.value
        Key.DoubleZero ->
            if (text.isEmpty()) text else text + "0".repeat(minOf(2, MAX_DIGITS - text.length))
        Key.Delete -> text.dropLast(1)
    }
}
