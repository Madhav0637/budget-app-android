package com.madhav0637.budgetapp.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madhav0637.budgetapp.data.ExpenseWithCategory
import com.madhav0637.budgetapp.domain.HistoryFilter
import com.madhav0637.budgetapp.domain.inr
import com.madhav0637.budgetapp.ui.components.Chip
import com.madhav0637.budgetapp.ui.components.ExpenseRow
import com.madhav0637.budgetapp.ui.components.ExpenseRowTextIndent
import com.madhav0637.budgetapp.ui.components.FloatingActions
import com.madhav0637.budgetapp.ui.components.HairlineDivider
import com.madhav0637.budgetapp.ui.components.KokuTextField
import com.madhav0637.budgetapp.ui.components.LocalToasts
import com.madhav0637.budgetapp.ui.components.PageMargin
import com.madhav0637.budgetapp.ui.components.ScreenTitle
import com.madhav0637.budgetapp.ui.components.ToastCenter
import com.madhav0637.budgetapp.ui.components.pressable
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType

/**
 * Every expense, grouped by day with each day's total. Search by merchant, filter by category,
 * swipe to delete (with Undo), tap to edit.
 */
@Composable
fun ActivityScreen(
    onAdd: () -> Unit,
    onEdit: (ExpenseWithCategory) -> Unit,
    viewModel: ActivityViewModel = viewModel(factory = ActivityViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toasts = LocalToasts.current
    val focus = LocalFocusManager.current

    // The search text lives here (not in the ViewModel's combined state) so typing never lags or jumps.
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) { viewModel.setSearch(query) }

    fun deleteWithUndo(item: ExpenseWithCategory) {
        val expense = item.expense
        val showError: (String) -> Unit = { toasts.show(it, icon = Icons.Rounded.ErrorOutline) }
        viewModel.delete(expense, onError = showError)
        toasts.show(
            "Deleted ${expense.merchant} · ${expense.amount.inr()}",
            icon = Icons.Rounded.Delete,
            actionTitle = "Undo",
            durationMillis = ToastCenter.UNDO_MILLIS,
        ) { viewModel.restore(expense, onError = showError) }
    }

    Box(Modifier.fillMaxSize().background(Koku.colors.canvas)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = PageMargin)) {
                ScreenTitle("Activity")
                KokuTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search merchants",
                    leadingIcon = Icons.Rounded.Search,
                    showsClear = true,
                    background = Koku.colors.surface,
                    shape = CircleShape,
                    borderColor = Koku.colors.hairline,
                    height = 48.dp,
                    textStyle = KokuType.body,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.hasAnyExpenses) CategoryChips(state, onSelect = viewModel::setCategory)

            when {
                state.isLoading -> Unit
                !state.hasAnyExpenses -> EmptyMessage(
                    Icons.AutoMirrored.Rounded.ReceiptLong,
                    "No expenses yet",
                    "Tap + to log one, or use the Log Expense tile or your phone's gesture.",
                )
                state.groups.isEmpty() -> EmptyMessage(Icons.Rounded.Search, "Nothing matches", "Try a different search or category.")
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = PageMargin, end = PageMargin, top = 4.dp, bottom = 100.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    state.groups.forEach { group ->
                        item(key = "day-${group.day}") { DayHeader(group, Modifier.animateItem()) }
                        itemsIndexed(group.expenses, key = { _, item -> item.expense.id }) { index, item ->
                            SwipeToDeleteRow(
                                item = item,
                                isFirst = index == 0,
                                isLast = index == group.expenses.lastIndex,
                                onDelete = { deleteWithUndo(item) },
                                onClick = { onEdit(item) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
        FloatingActions(onAdd = onAdd, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun CategoryChips(state: ActivityUiState, onSelect: (String?) -> Unit) {
    val haptics = LocalHapticFeedback.current
    fun select(id: String?) {
        if (id != state.filter.categoryId) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onSelect(id)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = PageMargin, vertical = 14.dp),
    ) {
        Chip("All", selected = state.filter.categoryId == null, onClick = { select(null) })
        state.categories.forEach { category ->
            Chip(category.name, emoji = category.emoji, selected = state.filter.categoryId == category.id, onClick = { select(category.id) })
        }
    }
}

@Composable
private fun DayHeader(group: HistoryFilter.DayGroup, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
    ) {
        Text(
            HistoryFilter.title(group.day),
            style = KokuType.subheadline.copy(fontWeight = FontWeight.SemiBold),
            color = Koku.colors.ink,
            modifier = Modifier.weight(1f),
        )
        Text(group.total.inr(), style = KokuType.subheadline, color = Koku.colors.ink2)
    }
}

/** A row in a day's rounded group. Swiping it left deletes it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(
    item: ExpenseWithCategory,
    isFirst: Boolean,
    isLast: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Koku.colors
    val haptics = LocalHapticFeedback.current
    val corner = 22.dp
    val shape = RoundedCornerShape(
        topStart = if (isFirst) corner else 0.dp,
        topEnd = if (isFirst) corner else 0.dp,
        bottomStart = if (isLast) corner else 0.dp,
        bottomEnd = if (isLast) corner else 0.dp,
    )
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onDelete()
            // The list remembers each row's swipe state by expense id. Reset it now, or an expense brought back
            // by Undo (same id) would reappear already swiped away and be deleted again straight away.
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier.clip(shape),
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier.fillMaxSize().background(colors.warning).padding(horizontal = 24.dp),
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
    ) {
        Column(Modifier.background(colors.surface).pressable(scale = 0.98f, onClickLabel = "Edit", onClick = onClick)) {
            if (!isFirst) HairlineDivider(startIndent = 16.dp + ExpenseRowTextIndent)
            ExpenseRow(item, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        }
    }
}

@Composable
private fun EmptyMessage(icon: ImageVector, title: String, detail: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = Koku.colors.ink3, modifier = Modifier.size(44.dp))
            Text(title, style = KokuType.title3, color = Koku.colors.ink)
            Text(detail, style = KokuType.subheadline, color = Koku.colors.ink2, textAlign = TextAlign.Center)
        }
    }
}
