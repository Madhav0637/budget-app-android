package com.madhav0637.budgetapp.domain

import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Whole rupees with Indian digit grouping, e.g. ₹1,23,456. */
fun Long.inr(): String = (if (this < 0) "-₹" else "₹") + kotlin.math.abs(this).indianGrouped()

/**
 * Indian digit grouping without the ₹, e.g. 1,23,456: the last three digits, then groups of two.
 * Used where the ₹ is drawn separately. Written by hand because Java's number formatting only supports equal groups.
 */
fun Long.indianGrouped(): String {
    val digits = kotlin.math.abs(this).toString()
    val grouped = if (digits.length <= 3) {
        digits
    } else {
        val head = digits.dropLast(3)
        val firstGroup = head.length % 2
        val pairs = head.drop(firstGroup).chunked(2)
        (listOfNotNull(head.take(firstGroup).ifEmpty { null }) + pairs + digits.takeLast(3)).joinToString(",")
    }
    return (if (this < 0) "-" else "") + grouped
}

/** Keeps only 0–9, for the amount box. */
fun digitsOnly(text: String): String = text.filter { it in '0'..'9' }

/** "1 expense", "3 expenses". */
fun counted(count: Int, singular: String, plural: String = singular + "s"): String =
    "$count ${if (count == 1) singular else plural}"

/** A share from 0 to 1 as a whole percentage, e.g. "26%". */
fun percent(share: Double): String = "${(share * 100).roundToInt()}%"

/** Lowercase with accents removed, so "Café" matches "cafe". Used by every merchant search. */
fun foldForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)

private val timeOnly = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
private val dayAndMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** The time of day, e.g. "9:39 PM". */
fun timeOfDay(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String = timeOnly.format(instant.atZone(zone))

/** For rows not grouped under a day heading: the time today, "Yesterday", or a date like "21 Sep". */
fun dayOrTime(instant: Instant, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String {
    val day = instant.atZone(zone).toLocalDate()
    val today = now.atZone(zone).toLocalDate()
    return when {
        day == today -> timeOfDay(instant, zone)
        day == today.minusDays(1) -> "Yesterday"
        day.year == today.year -> dayAndMonth.format(day)
        else -> dayMonthYear.format(day)
    }
}
