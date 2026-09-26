package com.madhav0637.budgetapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.madhav0637.budgetapp.data.ExpenseWithCategory
import com.madhav0637.budgetapp.domain.dayOrTime
import com.madhav0637.budgetapp.domain.inr
import com.madhav0637.budgetapp.domain.timeOfDay
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType

/** Where the text of an [ExpenseRow] starts, for lining up dividers: the 42dp tile plus 12dp spacing. */
val ExpenseRowTextIndent = 54.dp

/**
 * One expense in a list: category emoji, merchant, category and when, an optional note, and the amount.
 * Activity groups rows under a day heading, so it shows only the time; Home shows the day too.
 */
@Composable
fun ExpenseRow(item: ExpenseWithCategory, modifier: Modifier = Modifier, showsDay: Boolean = false) {
    val colors = Koku.colors
    val expense = item.expense
    val details = buildAnnotatedString {
        append(item.category.name)
        append(" · ")
        append(if (showsDay) dayOrTime(expense.date) else timeOfDay(expense.date))
        expense.note?.let { note ->
            append(" · ")
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(note) }
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    ) {
        EmojiTile(item.category.emoji)
        Column(Modifier.weight(1f)) {
            Text(
                expense.merchant,
                style = KokuType.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(details, style = KokuType.footnote, color = colors.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(expense.amount.inr(), style = KokuType.body.copy(fontWeight = FontWeight.SemiBold), color = colors.ink)
    }
}
