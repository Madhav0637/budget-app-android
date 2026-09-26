package com.madhav0637.budgetapp.ui.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.madhav0637.budgetapp.domain.Period
import com.madhav0637.budgetapp.domain.PeriodInsights
import com.madhav0637.budgetapp.domain.inr
import com.madhav0637.budgetapp.ui.components.KokuCard
import com.madhav0637.budgetapp.ui.components.animationsEnabled
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val dayCaption = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)

/**
 * Bars for each day (or each month of a year), drawn on a Canvas. The tapped bar, or else the peak, is in the
 * highlight colour and named in the caption; a dashed line marks the average so far. Drag across to scrub.
 */
@Composable
fun SpendingChart(insights: PeriodInsights, period: Period, modifier: Modifier = Modifier) {
    val colors = Koku.colors
    val haptics = LocalHapticFeedback.current
    val buckets = insights.buckets
    var selected by remember(insights.range) { mutableStateOf<Int?>(null) }
    val peakIndex = insights.peak?.let { buckets.indexOf(it) }
    val focused = selected ?: peakIndex

    val grow = remember(insights.range.start) { Animatable(if (animationsEnabled()) 0f else 1f) }
    LaunchedEffect(insights.range.start) { grow.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 90f)) }

    val measurer = rememberTextMeasurer()
    val labelStyle = KokuType.caption2.copy(fontWeight = FontWeight.Medium, color = colors.ink2)
    val axisLabels = remember(buckets, period, labelStyle) {
        buckets.mapIndexedNotNull { index, bucket ->
            val text = when (period) {
                Period.Week -> bucket.start.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)
                Period.Month -> if (index % 7 == 0) bucket.start.dayOfMonth.toString() else null
                Period.Year -> bucket.start.month.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)
            }
            text?.let { index to measurer.measure(it, labelStyle) }
        }
    }
    val averageLabel = remember(insights.averagePerBucket, labelStyle) {
        measurer.measure("avg ${insights.averagePerBucket.inr()}", labelStyle)
    }

    fun select(index: Int?) {
        if (index != selected) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            selected = index
        }
    }
    val currentSelect by rememberUpdatedState(::select)
    val currentSelected by rememberUpdatedState(selected)

    KokuCard(modifier) {
        Text(if (insights.bucketsAreMonths) "Month by month" else "Day by day", style = KokuType.headline, color = colors.ink)
        Text(
            caption(focused?.let { buckets[it] }, isSelection = selected != null, months = insights.bucketsAreMonths),
            style = KokuType.footnote,
            color = colors.ink2,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .semantics {
                    contentDescription = "Spending chart. " + caption(focused?.let { buckets[it] }, selected != null, insights.bucketsAreMonths)
                }
                .pointerInput(buckets.size) {
                    detectTapGestures { offset ->
                        val index = (offset.x / (size.width.toFloat() / buckets.size)).toInt().coerceIn(0, buckets.lastIndex)
                        // Tapping the selected bar again goes back to showing the peak.
                        currentSelect(if (index == currentSelected) null else index)
                    }
                }
                .pointerInput(buckets.size) {
                    detectHorizontalDragGestures { change, _ ->
                        val index = (change.position.x / (size.width.toFloat() / buckets.size)).toInt().coerceIn(0, buckets.lastIndex)
                        currentSelect(index)
                    }
                },
        ) {
            val labelArea = 22.dp.toPx()
            val plotHeight = size.height - labelArea
            val slot = size.width / buckets.size
            val barWidth = slot * 0.62f
            val top = maxOf(buckets.maxOf { it.amount }, insights.averagePerBucket).toFloat() * 1.12f

            buckets.forEachIndexed { index, bucket ->
                if (bucket.amount <= 0 || top <= 0f) return@forEachIndexed
                val height = maxOf(barWidth, bucket.amount / top * plotHeight) * grow.value
                drawRoundRect(
                    color = if (index == focused) colors.highlightFill else colors.chartBar,
                    topLeft = Offset(index * slot + (slot - barWidth) / 2, plotHeight - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(barWidth / 2),
                )
            }

            if (insights.averagePerBucket > 0 && top > 0f) {
                val y = plotHeight - insights.averagePerBucket / top * plotHeight
                drawLine(
                    color = colors.ink3,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())),
                )
                val labelY = (y - averageLabel.size.height - 2.dp.toPx()).let { if (it < 0) y + 2.dp.toPx() else it }
                drawText(averageLabel, topLeft = Offset(size.width - averageLabel.size.width, labelY))
            }

            for ((index, layout) in axisLabels) {
                val x = (index * slot + slot / 2 - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
                drawText(layout, topLeft = Offset(x, plotHeight + 6.dp.toPx()))
            }
        }
    }
}

private fun caption(bucket: PeriodInsights.Bucket?, isSelection: Boolean, months: Boolean): String {
    if (bucket == null) return "Tap a bar to see its total"
    val whenText = if (months) bucket.start.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) else dayCaption.format(bucket.start)
    return "${if (isSelection) "" else "Peak · "}$whenText · ${bucket.amount.inr()}"
}
