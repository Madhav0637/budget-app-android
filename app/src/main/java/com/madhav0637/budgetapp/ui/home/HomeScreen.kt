package com.madhav0637.budgetapp.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madhav0637.budgetapp.data.ExpenseWithCategory
import com.madhav0637.budgetapp.domain.BudgetPace
import com.madhav0637.budgetapp.domain.Period
import com.madhav0637.budgetapp.domain.PeriodInsights
import com.madhav0637.budgetapp.domain.SpendingSummary
import com.madhav0637.budgetapp.domain.counted
import com.madhav0637.budgetapp.domain.inr
import com.madhav0637.budgetapp.domain.percent
import com.madhav0637.budgetapp.ui.components.AmountText
import com.madhav0637.budgetapp.ui.components.ChangePill
import com.madhav0637.budgetapp.ui.components.EmojiTile
import com.madhav0637.budgetapp.ui.components.EmptyStateCard
import com.madhav0637.budgetapp.ui.components.ExpenseRow
import com.madhav0637.budgetapp.ui.components.ExpenseRowTextIndent
import com.madhav0637.budgetapp.ui.components.FloatingActions
import com.madhav0637.budgetapp.ui.components.HairlineDivider
import com.madhav0637.budgetapp.ui.components.KokuCard
import com.madhav0637.budgetapp.ui.components.KokuLogo
import com.madhav0637.budgetapp.ui.components.PageMargin
import com.madhav0637.budgetapp.ui.components.ProgressBar
import com.madhav0637.budgetapp.ui.components.RollingText
import com.madhav0637.budgetapp.ui.components.SectionHeader
import com.madhav0637.budgetapp.ui.components.animationsEnabled
import com.madhav0637.budgetapp.ui.components.growFromBottom
import com.madhav0637.budgetapp.ui.components.pressable
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * The first tab: what's been spent this week, month or year, the monthly budget, the last 7 days,
 * where the money went and the latest expenses.
 */
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onEdit: (ExpenseWithCategory) -> Unit,
    onEditBudget: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenActivity: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val summary = state.summary

    Box(Modifier.fillMaxSize().background(Koku.colors.canvas)) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(28.dp),
            contentPadding = PaddingValues(start = PageMargin, end = PageMargin, top = 8.dp, bottom = 110.dp),
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        ) {
            item(key = "header") { Header(state.period, viewModel::setPeriod) }
            // Nothing below the header until the first data arrives, so a set budget never flashes "Set a budget".
            if (summary == null) return@LazyColumn
            item(key = "hero") { Hero(state) }
            item(key = "budget") {
                val budget = state.budget
                if (budget != null) BudgetCard(budget, state.monthName, onEditBudget) else SetBudgetCard(onEditBudget)
            }
            if (state.lastWeek.any { it.amount > 0 }) {
                item(key = "lastWeek") { LastDaysChart(state.lastWeek) }
            }
            if (summary.total > 0) {
                item(key = "categories") { TopCategories(summary, onOpenInsights) }
                item(key = "recent") { Recent(summary, onEdit, onOpenActivity) }
            } else {
                item(key = "empty") {
                    EmptyStateCard(
                        emoji = "🪴",
                        title = "Nothing spent ${state.period.phrase}",
                        detail = "Tap + to log an expense, or use the Log Expense tile or your phone's gesture.",
                    )
                }
            }
        }
        FloatingActions(onAdd = onAdd, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Header(period: Period, onSelect: (Period) -> Unit) {
    val colors = Koku.colors
    val haptics = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KokuLogo(size = 34.dp)
        Text(
            "koku",
            style = KokuType.body.copy(fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
            color = colors.ink,
        )
        Spacer(Modifier.weight(1f))
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .pressable(onClickLabel = "Change period") { menuOpen = true }
                    .semantics { contentDescription = "Period, ${period.phrase}" }
                    .background(colors.surface2, CircleShape)
                    .padding(start = 14.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
            ) {
                Text(period.menuTitle, style = KokuType.subheadline.copy(fontWeight = FontWeight.SemiBold), color = colors.ink)
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = RoundedCornerShape(18.dp),
                containerColor = colors.surface,
            ) {
                Period.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.menuTitle, style = KokuType.body, color = colors.ink) },
                        trailingIcon = {
                            if (option == period) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = colors.highlightText)
                        },
                        onClick = {
                            menuOpen = false
                            if (option != period) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelect(option)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Hero(state: HomeUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.animateContentSize()) {
        Text("Spent ${state.period.phrase}", style = KokuType.subheadline.copy(fontWeight = FontWeight.Medium), color = Koku.colors.ink2)
        AmountText(state.summary?.total ?: 0, size = 60.sp)
        state.comparison?.change?.let { change ->
            ChangePill(change, comparedWith = "same time ${state.period.previousPhrase}")
        }
    }
}

@Composable
private fun BudgetCard(pace: BudgetPace, monthName: String, onEdit: () -> Unit) {
    val colors = Koku.colors
    KokuCard(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.pressable(onClickLabel = "Change the monthly budget", onClick = onEdit),
    ) {
        Row {
            val style = KokuType.subheadline.copy(fontWeight = FontWeight.Medium)
            Text("$monthName budget", style = style, color = colors.ink2, modifier = Modifier.weight(1f))
            Text(pace.budget.inr(), style = style, color = colors.ink2)
        }
        ProgressBar(pace.progress, tint = if (pace.isOver) colors.warning else colors.highlightFill)
        Row(verticalAlignment = Alignment.Bottom) {
            val remaining = pace.remaining
            RollingText(
                text = if (pace.isOver) "${(-remaining).inr()} over" else "${remaining.inr()} left",
                value = remaining,
                style = KokuType.title3.copy(color = if (pace.isOver) colors.warning else colors.ink),
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.weight(1f))
            if (!pace.isOver && pace.daysLeft > 0) {
                Text(
                    "≈ ${pace.perDay.inr()}/day for ${counted(pace.daysLeft, "day")}",
                    style = KokuType.footnote,
                    color = colors.ink2,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun SetBudgetCard(onEdit: () -> Unit) {
    val colors = Koku.colors
    KokuCard(padding = 16.dp, modifier = Modifier.pressable(onClick = onEdit)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(42.dp).background(colors.highlightFill, CircleShape)) {
                Icon(Icons.Rounded.TrackChanges, contentDescription = null, tint = colors.onHighlight, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Set a monthly budget", style = KokuType.body.copy(fontWeight = FontWeight.SemiBold), color = colors.ink)
                Text("See what's safe to spend each day", style = KokuType.footnote, color = colors.ink2)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.ink3)
        }
    }
}

/** Seven bars, today's in the highlight colour with its amount above it. */
@Composable
private fun LastDaysChart(buckets: List<PeriodInsights.Bucket>) {
    val colors = Koku.colors
    val peak = maxOf(buckets.maxOf { it.amount }, 1L)
    val average = buckets.sumOf { it.amount } / maxOf(buckets.size, 1)
    KokuCard(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Last 7 days", style = KokuType.headline, color = colors.ink, modifier = Modifier.weight(1f))
            Text("avg ${average.inr()}/day", style = KokuType.footnote, color = colors.ink2)
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().height(150.dp)) {
            buckets.forEachIndexed { index, bucket ->
                val isToday = index == buckets.lastIndex
                val weekday = bucket.start.dayOfWeek
                val grow = remember { Animatable(if (animationsEnabled()) 0.05f else 1f) }
                LaunchedEffect(Unit) {
                    delay(index * 40L)
                    grow.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 120f))
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "${weekday.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}, ${bucket.amount.inr()}"
                        },
                ) {
                    if (isToday && bucket.amount > 0) {
                        Text(
                            bucket.amount.inr(),
                            style = KokuType.caption2.copy(fontWeight = FontWeight.Bold),
                            color = colors.canvas,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.background(colors.ink, CircleShape).padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                    Box(
                        Modifier
                            .width(18.dp)
                            .height(maxOf(8f, 96f * bucket.amount / peak).dp)
                            .growFromBottom(grow.value)
                            .background(if (isToday) colors.highlightFill else colors.chartBar, CircleShape),
                    )
                    Text(
                        weekday.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                        style = KokuType.caption.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium),
                        color = if (isToday) colors.ink else colors.ink2,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopCategories(summary: SpendingSummary, onOpenInsights: () -> Unit) {
    val colors = Koku.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Where it went", actionTitle = "Insights", onAction = onOpenInsights)
        KokuCard(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            summary.categoryTotals.take(3).forEach { item ->
                val share = summary.share(item)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                ) {
                    EmojiTile(item.category.emoji)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        Row {
                            val style = KokuType.body.copy(fontWeight = FontWeight.SemiBold)
                            Text(item.category.name, style = style, color = colors.ink, modifier = Modifier.weight(1f))
                            Text(item.amount.inr(), style = style, color = colors.ink)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProgressBar(share, tint = colors.ink, height = 4.dp, modifier = Modifier.weight(1f))
                            Text(
                                percent(share),
                                style = KokuType.caption.copy(fontWeight = FontWeight.Medium),
                                color = colors.ink2,
                                textAlign = TextAlign.End,
                                modifier = Modifier.widthIn(min = 34.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Recent(summary: SpendingSummary, onEdit: (ExpenseWithCategory) -> Unit, onSeeAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Recent", actionTitle = "See all", onAction = onSeeAll)
        KokuCard(padding = 16.dp) {
            summary.recent.forEachIndexed { index, item ->
                if (index > 0) HairlineDivider(startIndent = ExpenseRowTextIndent)
                ExpenseRow(
                    item,
                    showsDay = true,
                    modifier = Modifier.pressable(onClickLabel = "Edit") { onEdit(item) }.padding(vertical = 12.dp),
                )
            }
        }
    }
}
