package com.madhav0637.budgetapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.madhav0637.budgetapp.data.Highlight

// Koku's look: a quiet neutral canvas and one highlight colour, used only for the thing that matters on each
// screen (the add button, progress, the selected item, the peak of a chart). Emoji supply the rest of the colour.
// Same values as the iOS app's Theme.swift.

/** Every colour a screen uses, for light or dark mode, plus the chosen highlight. */
@Immutable
data class KokuColors(
    /** The page background. */
    val canvas: Color,
    /** Cards and rows. */
    val surface: Color,
    /** Tiles, tracks, fields and unselected pills. */
    val surface2: Color,
    /** The sliding thumb of a pill picker. */
    val thumb: Color,
    /** Main text. */
    val ink: Color,
    /** Secondary text. */
    val ink2: Color,
    /** Placeholders and the quietest details. */
    val ink3: Color,
    val hairline: Color,
    /** Chart bars that aren't highlighted. */
    val chartBar: Color,
    /** Text and icons on top of a highlight fill. */
    val onHighlight: Color,
    /** Only for going over budget and deleting. */
    val warning: Color,
    /** The toast banner, dark in both modes. */
    val toast: Color,
    /** The bright highlight, for filled things. Put [onHighlight] on top. */
    val highlightFill: Color,
    /** The highlight for text and icons: a deeper shade in light mode, so it stays readable on white. */
    val highlightText: Color,
    val isDark: Boolean,
)

private val OnHighlight = Color(0xFF0B0B0C)

fun lightKokuColors(highlight: Highlight) = KokuColors(
    canvas = Color(0xFFF5F5F2),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFECECE8),
    thumb = Color(0xFFFFFFFF),
    ink = Color(0xFF111111),
    ink2 = Color(0xFF6E6E73),
    ink3 = Color(0xFFA1A1A6),
    hairline = Color(0xFF111111).copy(alpha = 0.07f),
    chartBar = Color(0xFFDCDCD6),
    onHighlight = OnHighlight,
    warning = Color(0xFFDC3B42),
    toast = Color(0xFF1C1C1E),
    highlightFill = highlight.bright,
    highlightText = highlight.deep,
    isDark = false,
)

fun darkKokuColors(highlight: Highlight) = KokuColors(
    canvas = Color(0xFF0B0B0C),
    surface = Color(0xFF161618),
    surface2 = Color(0xFF232326),
    thumb = Color(0xFF3A3A3F),
    ink = Color(0xFFF4F4F5),
    ink2 = Color(0xFF9B9BA1),
    ink3 = Color(0xFF5F5F66),
    hairline = Color(0xFFFFFFFF).copy(alpha = 0.06f),
    chartBar = Color(0xFF2C2C30),
    onHighlight = OnHighlight,
    warning = Color(0xFFFF5A5F),
    toast = Color(0xFF2C2C30),
    highlightFill = highlight.bright,
    highlightText = highlight.bright,
    isDark = true,
)

/** The bright colour, for filled things (the add button, progress, the highlighted bar). */
val Highlight.bright: Color
    get() = when (this) {
        Highlight.Mint -> Color(0xFF34D399)
        Highlight.Lime -> Color(0xFFC4F042)
        Highlight.Sky -> Color(0xFF5AC8FA)
        Highlight.Periwinkle -> Color(0xFF8B93FF)
        Highlight.Coral -> Color(0xFFFF7A85)
        Highlight.Amber -> Color(0xFFFFC247)
    }

/** A deeper shade for text and icons in light mode. */
val Highlight.deep: Color
    get() = when (this) {
        Highlight.Mint -> Color(0xFF047857)
        Highlight.Lime -> Color(0xFF4D7C0F)
        Highlight.Sky -> Color(0xFF0369A1)
        Highlight.Periwinkle -> Color(0xFF4F46E5)
        Highlight.Coral -> Color(0xFFE11D48)
        Highlight.Amber -> Color(0xFFB45309)
    }

/** The Koku logo's own colours, which never change with the theme. */
object LogoColors {
    val graphite = Color(0xFF111827)
    val mint = Color(0xFF34D399)
    val amber = Color(0xFFFBBF24)
    val periwinkle = Color(0xFF818CF8)
}
