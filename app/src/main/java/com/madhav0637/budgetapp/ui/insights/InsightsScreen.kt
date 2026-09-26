package com.madhav0637.budgetapp.ui.insights

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madhav0637.budgetapp.domain.Period
import com.madhav0637.budgetapp.domain.PeriodInsights
import com.madhav0637.budgetapp.domain.counted
import com.madhav0637.budgetapp.domain.inr
import com.madhav0637.budgetapp.domain.percent
import com.madhav0637.budgetapp.ui.components.AmountText
import com.madhav0637.budgetapp.ui.components.ChangePill
import com.madhav0637.budgetapp.ui.components.CircleIconButton
import com.madhav0637.budgetapp.ui.components.EmojiTile
import com.madhav0637.budgetapp.ui.components.EmptyStateCard
import com.madhav0637.budgetapp.ui.components.ExpenseRowTextIndent
import com.madhav0637.budgetapp.ui.components.HairlineDivider
import com.madhav0637.budgetapp.ui.components.KokuCard
import com.madhav0637.budgetapp.ui.components.PageMargin
import com.madhav0637.budgetapp.ui.components.PillLabel
import com.madhav0637.budgetapp.ui.components.PillPicker
import com.madhav0637.budgetapp.ui.components.ScreenTitle
import com.madhav0637.budgetapp.ui.components.SectionHeader
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

/**
 * A week, month or year at a glance, now or in the past: the total and how it compares, spending day by day,
 * categories, a few highlights and the top merchants.
 */
@Composable
fun InsightsScreen(viewModel: InsightsViewModel = viewModel(factory = InsightsViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val insights = state.insights

    fun step(back: Boolean) {
        if (!back && state.offset >= 0) return
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        viewModel.step(back)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Koku.colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PageMargin)
            .padding(bottom = 32.dp),
    ) {
        ScreenTitle("Insights")
        PillPicker(state.period, Period.entries, onSelect = viewModel::setPeriod) { option, selected ->
            PillLabel(option.title, selected)
        }
        Navigator(state, ::step)
        if (insights != null) {
            Hero(state, insights, onSwipe = ::step)
            if (insights.total > 0) {
                SpendingChart(insights, state.period)
                CategoryBreakdown(insights)
                Highlights(insights)
                TopMerchants(insights)
            } else {
                EmptyStateCard(
                    emoji = "🌱",
                    title = if (state.offset == 0) "Nothing spent ${state.period.phrase}" else "Nothing spent in this ${state.period.title.lowercase()}",
                    detail = "Charts and highlights appear once there's spending to show.",
                )
            }
        }
    }
}

@Composable
private fun Navigator(state: InsightsUiState, onStep: (back: Boolean) -> Unit) {
    val unit = state.period.title.lowercase()
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous $unit", onClick = { onStep(true) })
        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).clipToBounds()) {
            AnimatedContent(
                targetState = state.title,
                transitionSpec = {
                    val from = if (state.movingBack) -1 else 1
                    (slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = 400f)) { it * from / 2 } + fadeIn()) togetherWith
                        (slideOutHorizontally(spring(dampingRatio = 0.85f, stiffness = 400f)) { -it * from / 2 } + fadeOut())
                },
                label = "periodTitle",
            ) { title ->
                Text(title, style = KokuType.headline, color = Koku.colors.ink, maxLines = 1)
            }
        }
        CircleIconButton(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            "Next $unit",
            onClick = { onStep(false) },
            enabled = state.offset < 0,
        )
    }
}

@Composable
private fun Hero(state: InsightsUiState, insights: PeriodInsights, onSwipe: (back: Boolean) -> Unit) {
    val swipe by rememberUpdatedState(onSwipe)
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            // Swipe right for the period before, left for the one after.
            .pointerInput(Unit) {
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = {
                        if (dragged > 60.dp.toPx()) swipe(true)
                        if (dragged < -60.dp.toPx()) swipe(false)
                    },
                ) { _, amount -> dragged += amount }
            },
    ) {
        AmountText(insights.total, size = 52.sp)
        state.comparison?.change?.let { ChangePill(it, state.comparedWith) }
        if (insights.count > 0) {
            Text(
                "${counted(insights.count, "expense")} · ${insights.averagePerDay.inr()} a day on average",
                style = KokuType.footnote,
                color = Koku.colors.ink2,
            )
        }
    }
}

/** One bar split by category (the biggest in the highlight colour, the rest in fading ink), then every category. */
@Composable
private fun CategoryBreakdown(insights: PeriodInsights) {
    val colors = Koku.colors
    val totals = insights.categoryTotals
    fun share(amount: Long) = if (insights.total > 0) amount.toDouble() / insights.total else 0.0

    KokuCard(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Categories", style = KokuType.headline, color = colors.ink, modifier = Modifier.weight(1f))
            Text(counted(totals.size, "category", "categories"), style = KokuType.footnote, color = colors.ink2)
        }

        BoxWithConstraints(Modifier.fillMaxWidth().height(10.dp).clipToBounds()) {
            val spacing = 3.dp
            val available = maxWidth - spacing * (totals.size - 1).coerceAtLeast(0)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                totals.forEachIndexed { index, item ->
                    val color = if (index == 0) colors.highlightFill else colors.ink.copy(alpha = maxOf(0.12f, 0.7f - index * 0.15f))
                    Box(
                        Modifier
                            .width(maxOf(4.dp, available * share(item.amount).toFloat()))
                            .height(10.dp)
                            .background(color, CircleShape),
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            totals.forEachIndexed { index, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                ) {
                    EmojiTile(item.category.emoji, size = 38.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.category.name, style = KokuType.body.copy(fontWeight = FontWeight.SemiBold), color = colors.ink)
                        Text(counted(item.count, "expense"), style = KokuType.footnote, color = colors.ink2)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.amount.inr(), style = KokuType.body.copy(fontWeight = FontWeight.SemiBold), color = colors.ink)
                        Text(
                            percent(share(item.amount)),
                            style = KokuType.footnote.copy(fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal),
                            color = if (index == 0) colors.highlightText else colors.ink2,
                        )
                    }
                }
            }
        }
    }
}

/** Biggest spend, most visited, average per day and no-spend days, two by two. */
@Composable
private fun Highlights(insights: PeriodInsights) {
    val tiles = buildList {
        insights.biggest?.let {
            add(Triple("Biggest spend", it.expense.amount.inr(), "${it.expense.merchant} · ${shortDate.format(it.expense.date.atZone(ZoneId.systemDefault()))}"))
        }
        insights.mostFrequent?.let { add(Triple("Most visited", it.name, counted(it.count, "time"))) }
        add(Triple("Average", insights.averagePerDay.inr(), "per day"))
        add(Triple("No-spend days", if (insights.noSpendDays > 0) "${insights.noSpendDays} 🎉" else "0", "of ${counted(insights.elapsedDays, "day")}"))
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (title, value, detail) -> StatTile(title, value, detail, Modifier.weight(1f)) }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

/** A small card with a label, a big value and a detail line. */
@Composable
private fun StatTile(title: String, value: String, detail: String, modifier: Modifier = Modifier) {
    val colors = Koku.colors
    KokuCard(padding = 16.dp, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(title, style = KokuType.footnote.copy(fontWeight = FontWeight.Medium), color = colors.ink2, maxLines = 1)
        Text(value, style = KokuType.title3, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(detail, style = KokuType.caption, color = colors.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TopMerchants(insights: PeriodInsights) {
    val colors = Koku.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Top merchants")
        KokuCard(padding = 16.dp) {
            insights.topMerchants.take(5).forEachIndexed { index, merchant ->
                if (index > 0) HairlineDivider(startIndent = ExpenseRowTextIndent)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 12.dp).semantics(mergeDescendants = true) {},
                ) {
                    EmojiTile(merchant.emoji)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            merchant.name,
                            style = KokuType.body.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(counted(merchant.count, "time"), style = KokuType.footnote, color = colors.ink2)
                    }
                    Text(merchant.amount.inr(), style = KokuType.body.copy(fontWeight = FontWeight.SemiBold), color = colors.ink)
                }
            }
        }
    }
}
