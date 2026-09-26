package com.madhav0637.budgetapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Koku's text sizes, named like the iOS text styles they mirror. Always the phone's own font (no font files).
 * Numbers use tabular digits so amounts line up and don't jiggle while they animate.
 */
object KokuType {
    private val base = TextStyle(fontFamily = FontFamily.Default, fontFeatureSettings = "tnum")

    val largeTitle = base.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
    val title3 = base.copy(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
    val headline = base.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val body = base.copy(fontSize = 16.sp, lineHeight = 22.sp)
    val subheadline = base.copy(fontSize = 15.sp, lineHeight = 20.sp)
    val footnote = base.copy(fontSize = 13.sp, lineHeight = 18.sp)
    val caption = base.copy(fontSize = 12.sp, lineHeight = 16.sp)
    val caption2 = base.copy(fontSize = 11.sp, lineHeight = 13.sp)
}

/** Material's styles, for the Material components still in use (dialogs, date and time pickers). */
val Typography = Typography()
