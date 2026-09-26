package com.madhav0637.budgetapp.ui.expenseform

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madhav0637.budgetapp.domain.BudgetAlerts
import com.madhav0637.budgetapp.domain.HistoryFilter
import com.madhav0637.budgetapp.domain.Period
import com.madhav0637.budgetapp.domain.PeriodCalculator
import com.madhav0637.budgetapp.domain.inr
import com.madhav0637.budgetapp.ui.components.AmountText
import com.madhav0637.budgetapp.ui.components.Caret
import com.madhav0637.budgetapp.ui.components.Chip
import com.madhav0637.budgetapp.ui.components.ChipStyle
import com.madhav0637.budgetapp.ui.components.CircleIconButton
import com.madhav0637.budgetapp.ui.components.Keypad
import com.madhav0637.budgetapp.ui.components.KokuTextField
import com.madhav0637.budgetapp.ui.components.LocalToasts
import com.madhav0637.budgetapp.ui.components.PageMargin
import com.madhav0637.budgetapp.ui.components.ToastCenter
import com.madhav0637.budgetapp.ui.components.pressable
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Adds a new expense, or edits one. Amount first on the keypad, then "On what?" (recent merchants are one tap away
 * and bring their usual category with them), a category, and optionally a note and a different date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormSheet(
    mode: ExpenseFormMode,
    onDismiss: () -> Unit,
    viewModel: ExpenseFormViewModel = viewModel(factory = ExpenseFormViewModel.Factory),
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val colors = Koku.colors
    val toasts = LocalToasts.current
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val editing = (mode as? ExpenseFormMode.Edit)?.item
    var amountText by rememberSaveable(mode.key) { mutableStateOf(editing?.expense?.amount?.toString() ?: "") }
    var merchant by rememberSaveable(mode.key) { mutableStateOf(editing?.expense?.merchant ?: "") }
    var categoryId by rememberSaveable(mode.key) { mutableStateOf(editing?.category?.id) }
    var date by rememberSaveable(mode.key) { mutableStateOf(editing?.expense?.date ?: Instant.now()) }
    var note by rememberSaveable(mode.key) { mutableStateOf(editing?.expense?.note ?: "") }
    var showsNote by rememberSaveable(mode.key) { mutableStateOf(editing?.expense?.note != null) }
    // Once a category is tapped, merchant suggestions stop changing it.
    var categoryChosenByHand by rememberSaveable(mode.key) { mutableStateOf(editing != null) }
    // The merchant whose usual category was filled in, for the "picked from Zomato" hint.
    var categoryHint by rememberSaveable(mode.key) { mutableStateOf<String?>(null) }
    var merchantFocused by remember { mutableStateOf(false) }
    var noteFocused by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val amount = amountText.toLongOrNull()?.takeIf { it > 0 }
    val category = data.categories.firstOrNull { it.id == categoryId }
    val canSave = amount != null && merchant.isNotBlank() && category != null
    val keypadVisible = !merchantFocused && !noteFocused

    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            then()
        }
    }

    /** When the merchant matches one used before, pick its usual category, unless one was chosen by hand. */
    fun fillCategory(text: String) {
        if (categoryChosenByHand) return
        val id = data.suggestions.categoryId(text) ?: return
        if (data.categories.none { it.id == id }) return
        categoryId = id
        categoryHint = text.trim()
    }

    fun save() {
        val value = amount ?: return
        val chosen = category ?: return
        focusManager.clearFocus()
        viewModel.save(
            mode, merchant, value, chosen.id, date, note,
            onSaved = { alert ->
                saved = true
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                scope.launch {
                    delay(450)
                    close {
                        alert?.let { toasts.show(BudgetAlerts.message(it).title, icon = Icons.Rounded.ErrorOutline, durationMillis = 5_000) }
                    }
                }
            },
            onError = { errorMessage = it },
        )
    }

    fun deleteExpense() {
        val expense = editing?.expense ?: return
        viewModel.delete(
            expense,
            onDeleted = {
                close {
                    toasts.show(
                        "Deleted ${expense.merchant} · ${expense.amount.inr()}",
                        icon = Icons.Rounded.Delete,
                        actionTitle = "Undo",
                        durationMillis = ToastCenter.UNDO_MILLIS,
                    ) { viewModel.restore(expense) { toasts.show(it, icon = Icons.Rounded.ErrorOutline) } }
                }
            },
            onError = { errorMessage = it },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Leaves a strip of the screen showing above the sheet, like a card on top.
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 8.dp),
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
        containerColor = colors.surface,
        dragHandle = { SheetHandle() },
    ) {
        Column(Modifier.fillMaxHeight()) {
            TopBar(
                title = if (editing == null) "New expense" else "Edit expense",
                date = date,
                onCancel = { close() },
                onDelete = if (editing != null) ::deleteExpense else null,
                onPickDate = { pickingDate = true },
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 12.dp),
            ) {
                // Tapping the amount goes back to the keypad.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focusManager.clearFocus() },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = PageMargin),
                    ) {
                        AmountText(amount ?: 0, size = 68.sp, placeholder = amount == null, modifier = Modifier.weight(1f, fill = false))
                        Caret(colors.highlightFill, height = 56.dp, modifier = Modifier.alpha(if (keypadVisible) 1f else 0f))
                    }
                    BudgetCaption(data, editingId = editing?.expense?.id, amount = amount, date = date)
                }

                MerchantField(
                    merchant = merchant,
                    suggestions = data.suggestions.matching(merchant).map { it.name },
                    onChange = {
                        merchant = it
                        fillCategory(it)
                    },
                    onClear = {
                        merchant = ""
                        if (!categoryChosenByHand) categoryHint = null
                    },
                    onPick = {
                        merchant = it
                        fillCategory(it)
                        focusManager.clearFocus()
                    },
                    onDone = { focusManager.clearFocus() },
                    onFocus = { merchantFocused = it },
                )

                CategoryPicker(
                    categories = data.categories,
                    selectedId = categoryId,
                    hint = categoryHint,
                    onSelect = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        categoryChosenByHand = true
                        categoryHint = null
                        categoryId = it
                    },
                )

                NoteField(
                    note = note,
                    shown = showsNote,
                    onShow = { showsNote = true },
                    onChange = { note = it },
                    onDone = { focusManager.clearFocus() },
                    onFocus = { noteFocused = it },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                AnimatedVisibility(visible = keypadVisible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Keypad(amountText, onTextChange = { amountText = it }, modifier = Modifier.padding(horizontal = 12.dp))
                }
                SaveButton(
                    title = when {
                        saved -> "Saved"
                        amount == null -> "Enter an amount"
                        merchant.isBlank() -> "Add what it was for"
                        category == null -> "Pick a category"
                        editing == null -> "Save ${amount.inr()}"
                        else -> "Save changes"
                    },
                    enabled = canSave && !saved,
                    saved = saved,
                    onClick = ::save,
                )
            }
        }
    }

    if (pickingDate) {
        DateTimeDialog(
            initial = date,
            onDismiss = { pickingDate = false },
            onConfirm = {
                date = it
                pickingDate = false
            },
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            containerColor = colors.surface,
            title = { Text("Couldn't save") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } },
        )
    }
}

/** A small grabber at the top of Koku's sheets. */
@Composable
fun SheetHandle() {
    Box(
        Modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .width(36.dp)
            .height(5.dp)
            .background(Koku.colors.ink3.copy(alpha = 0.5f), CircleShape),
    )
}

@Composable
private fun TopBar(title: String, date: Instant, onCancel: () -> Unit, onDelete: (() -> Unit)?, onPickDate: () -> Unit) {
    val colors = Koku.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = PageMargin).height(44.dp),
    ) {
        Text(
            "Cancel",
            style = KokuType.body.copy(fontWeight = FontWeight.Medium),
            color = colors.ink2,
            modifier = Modifier.pressable(onClick = onCancel).padding(vertical = 8.dp),
        )
        Text(
            title,
            style = KokuType.headline,
            color = colors.ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onDelete != null) CircleIconButton(Icons.Rounded.Delete, "Delete expense", onDelete, tint = colors.warning)
        val day = date.atZone(ZoneId.systemDefault()).toLocalDate()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .pressable(onClickLabel = "Change date", onClick = onPickDate)
                .semantics { contentDescription = "Date, ${HistoryFilter.title(day)}" }
                .height(36.dp)
                .background(colors.surface2, CircleShape)
                .padding(horizontal = 12.dp),
        ) {
            Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = colors.ink, modifier = Modifier.size(16.dp))
            Text(HistoryFilter.title(day), style = KokuType.subheadline.copy(fontWeight = FontWeight.SemiBold), color = colors.ink)
        }
    }
}

/** "₹5,832 left this month after this", when a budget is set and the expense is in this month. */
@Composable
private fun BudgetCaption(data: ExpenseFormData, editingId: String?, amount: Long?, date: Instant) {
    val colors = Koku.colors
    val month = PeriodCalculator().range(Period.Month)
    if (data.monthlyBudget <= 0 || date !in month) return
    val spentElsewhere = data.expenses.filter { it.expense.date in month && it.expense.id != editingId }.sumOf { it.expense.amount }
    val left = data.monthlyBudget - spentElsewhere - (amount ?: 0)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(6.dp).background(if (left >= 0) colors.highlightFill else colors.warning, CircleShape))
        Text(
            if (left >= 0) "${left.inr()} left this month after this" else "${(-left).inr()} over budget after this",
            style = KokuType.footnote.copy(fontWeight = FontWeight.Medium),
            color = colors.ink2,
        )
    }
}

@Composable
private fun MerchantField(
    merchant: String,
    suggestions: List<String>,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    onPick: (String) -> Unit,
    onDone: () -> Unit,
    onFocus: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KokuTextField(
            value = merchant,
            onValueChange = onChange,
            placeholder = "On what?",
            leadingIcon = Icons.Rounded.Storefront,
            showsClear = true,
            onClear = onClear,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.padding(horizontal = PageMargin).onFocusChanged { onFocus(it.isFocused) },
        )
        AnimatedVisibility(visible = suggestions.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = PageMargin),
            ) {
                suggestions.forEach { name ->
                    Chip(name, selected = false, onClick = { onPick(name) }, restingBackground = Koku.colors.surface2)
                }
            }
        }
    }
}

@Composable
private fun CategoryPicker(
    categories: List<com.madhav0637.budgetapp.data.Category>,
    selectedId: String?,
    hint: String?,
    onSelect: (String) -> Unit,
) {
    val colors = Koku.colors
    val listState = rememberLazyListState()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = PageMargin),
        ) {
            val style = KokuType.footnote.copy(fontWeight = FontWeight.Medium)
            Text("Category", style = style, color = colors.ink2)
            AnimatedVisibility(visible = hint != null, enter = fadeIn(), exit = fadeOut()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.highlightText, modifier = Modifier.size(14.dp))
                    Text("picked from ${hint.orEmpty()}", style = style, color = colors.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = PageMargin),
        ) {
            items(categories, key = { it.id }) { item ->
                Chip(
                    item.name,
                    emoji = item.emoji,
                    selected = item.id == selectedId,
                    style = ChipStyle.Highlight,
                    restingBackground = colors.surface2,
                    onClick = { onSelect(item.id) },
                )
            }
        }
    }
    // Brings the chosen category into the middle of the row, e.g. after a suggestion picked it.
    LaunchedEffect(selectedId, categories) {
        val index = categories.indexOfFirst { it.id == selectedId }
        if (index < 0) return@LaunchedEffect
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index }
        if (item == null) {
            listState.animateScrollToItem(index)
        } else {
            val center = (info.viewportEndOffset + info.viewportStartOffset) / 2
            listState.animateScrollBy((item.offset + item.size / 2 - center).toFloat())
        }
    }
}

@Composable
private fun NoteField(
    note: String,
    shown: Boolean,
    onShow: () -> Unit,
    onChange: (String) -> Unit,
    onDone: () -> Unit,
    onFocus: (Boolean) -> Unit,
) {
    val colors = Koku.colors
    val focus = remember { FocusRequester() }
    var focusWhenShown by remember { mutableStateOf(false) }
    if (shown) {
        KokuTextField(
            value = note,
            onValueChange = onChange,
            placeholder = "Add a note",
            leadingIcon = Icons.AutoMirrored.Rounded.Notes,
            height = 50.dp,
            textStyle = KokuType.body,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.padding(horizontal = PageMargin).focusRequester(focus).onFocusChanged { onFocus(it.isFocused) },
        )
        LaunchedEffect(Unit) {
            if (focusWhenShown) focus.requestFocus()
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(horizontal = PageMargin)
                .pressable {
                    focusWhenShown = true
                    onShow()
                }
                .padding(vertical = 4.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(20.dp))
            Text("Add a note", style = KokuType.subheadline.copy(fontWeight = FontWeight.Medium), color = colors.ink2)
        }
    }
}

@Composable
private fun SaveButton(title: String, enabled: Boolean, saved: Boolean, onClick: () -> Unit) {
    val colors = Koku.colors
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PageMargin)
            .pressable(enabled = enabled, onClick = onClick)
            .height(56.dp)
            .background(colors.highlightFill.copy(alpha = if (enabled || saved) 1f else 0.35f), CircleShape),
    ) {
        AnimatedVisibility(visible = saved, enter = scaleIn() + fadeIn()) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.onHighlight, modifier = Modifier.padding(end = 8.dp).size(22.dp))
        }
        AnimatedContent(targetState = title, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "saveTitle") {
            Text(it, style = KokuType.headline.copy(fontWeight = FontWeight.Bold), color = colors.onHighlight)
        }
    }
}
