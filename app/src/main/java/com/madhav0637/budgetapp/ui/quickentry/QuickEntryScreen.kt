package com.madhav0637.budgetapp.ui.quickentry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madhav0637.budgetapp.data.Category
import com.madhav0637.budgetapp.domain.digitsOnly
import com.madhav0637.budgetapp.ui.components.EmojiTile
import com.madhav0637.budgetapp.ui.components.KokuButton
import com.madhav0637.budgetapp.ui.components.KokuLogo
import com.madhav0637.budgetapp.ui.components.pressable
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType

private enum class Step(val label: String) { Merchant("On what?"), Amount("How much?"), Category("Which category?") }

/**
 * A pop-up card at the top of the screen over a dimmed background: On what? → Amount → Category.
 * Tapping a category saves. Tapping outside the card cancels.
 */
@Composable
fun QuickEntryScreen(
    categories: List<Category>,
    onSave: (merchant: String, amount: Long, category: Category) -> Unit,
    onCancel: () -> Unit,
) {
    val colors = Koku.colors
    val haptics = LocalHapticFeedback.current
    var step by rememberSaveable { mutableStateOf(Step.Merchant) }
    var merchant by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    val amount = amountText.toLongOrNull()?.takeIf { it > 0 }
    val shape = RoundedCornerShape(32.dp)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .safeDrawingPadding()
                .padding(12.dp)
                .fillMaxWidth()
                .shadow(16.dp, shape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
                .background(colors.surface, shape)
                .border(1.dp, colors.hairline, shape)
                // Swallow taps on the card so they don't reach the background and cancel.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 4.dp)) {
                KokuLogo(size = 20.dp)
                Text("Log expense", style = KokuType.footnote.copy(fontWeight = FontWeight.SemiBold), color = colors.ink2, modifier = Modifier.weight(1f))
                Text("${step.ordinal + 1} of 3", style = KokuType.footnote, color = colors.ink3)
            }
            when (step) {
                Step.Merchant -> {
                    val goNext = { if (merchant.isNotBlank()) step = Step.Amount }
                    BigTextBox(
                        value = merchant,
                        onValueChange = { merchant = it },
                        placeholder = "On what?",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        onImeAction = goNext,
                    )
                    CancelNextRow(nextEnabled = merchant.isNotBlank(), onCancel = onCancel, onNext = goNext)
                }

                Step.Amount -> {
                    val goNext = { if (amount != null) step = Step.Category }
                    BigTextBox(
                        value = amountText,
                        onValueChange = { amountText = digitsOnly(it).take(9) },
                        placeholder = "Amount (₹)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        onImeAction = goNext,
                    )
                    CancelNextRow(nextEnabled = amount != null, onCancel = onCancel, onNext = goNext)
                }

                Step.Category -> {
                    Text("Category", style = KokuType.title3, color = colors.ink, modifier = Modifier.padding(start = 4.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 460.dp)) {
                        items(categories, key = { it.id }) { category ->
                            CategoryRow(category) {
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                amount?.let { onSave(merchant, it, category) }
                            }
                        }
                    }
                    KokuButton("Cancel", onClick = onCancel, primary = false, height = 52.dp)
                }
            }
        }
    }
}

/** The big, bold, centred box used by both text steps, so they look the same. */
@Composable
private fun BigTextBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    onImeAction: () -> Unit,
) {
    val colors = Koku.colors
    val focus = remember { FocusRequester() }
    val style = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        letterSpacing = (-0.5).sp,
        color = colors.ink,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(colors.highlightText),
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        decorationBox = { field ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface2, RoundedCornerShape(24.dp))
                    .padding(vertical = 22.dp, horizontal = 16.dp),
            ) {
                if (value.isEmpty()) Text(placeholder, style = style.copy(color = colors.ink3))
                field()
            }
        },
    )
    // Put the cursor in the box straight away, so the keyboard is ready.
    LaunchedEffect(placeholder) { focus.requestFocus() }
}

@Composable
private fun CancelNextRow(nextEnabled: Boolean, onCancel: () -> Unit, onNext: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KokuButton("Cancel", onClick = onCancel, primary = false, height = 52.dp, modifier = Modifier.weight(1f))
        KokuButton("Next", onClick = onNext, enabled = nextEnabled, height = 52.dp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CategoryRow(category: Category, onClick: () -> Unit) {
    val colors = Koku.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressable(scale = 0.98f, onClick = onClick)
            .background(colors.surface2, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        EmojiTile(category.emoji, size = 40.dp, background = colors.surface)
        Text(category.name, style = KokuType.body.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
    }
}
