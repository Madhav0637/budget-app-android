package com.madhav0637.budgetapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madhav0637.budgetapp.domain.KeypadInput
import com.madhav0637.budgetapp.domain.KeypadInput.Key
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType

private val rows: List<List<Key>> = listOf(
    listOf(Key.Digit(1), Key.Digit(2), Key.Digit(3)),
    listOf(Key.Digit(4), Key.Digit(5), Key.Digit(6)),
    listOf(Key.Digit(7), Key.Digit(8), Key.Digit(9)),
    listOf(Key.DoubleZero, Key.Digit(0), Key.Delete),
)

/** The number pad for rupee amounts: 1–9, 00, 0 and delete. Hold delete to clear. A light tick on every key. */
@Composable
fun Keypad(text: String, onTextChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    KeypadKey(
                        key = key,
                        modifier = Modifier.weight(1f),
                        onPress = {
                            val next = KeypadInput.apply(key, text)
                            if (next != text) {
                                haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                                onTextChange(next)
                            }
                        },
                        onLongPress = if (key == Key.Delete) {
                            {
                                if (text.isNotEmpty()) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTextChange("")
                                }
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

/** One key: a soft rounded glow behind it and a small shrink while pressed. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadKey(key: Key, onPress: () -> Unit, onLongPress: (() -> Unit)?, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(dampingRatio = 0.7f, stiffness = 900f), label = "keyScale")
    val glow by animateFloatAsState(if (pressed) 1f else 0f, spring(stiffness = 900f), label = "keyGlow")
    val label = when (key) {
        is Key.Digit -> key.value.toString()
        Key.DoubleZero -> "Double zero"
        Key.Delete -> "Delete"
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(Koku.colors.surface2.copy(alpha = Koku.colors.surface2.alpha * glow), RoundedCornerShape(18.dp))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = label,
                onLongClick = onLongPress,
                onClick = onPress,
            )
            .semantics { contentDescription = label },
    ) {
        when (key) {
            is Key.Digit -> KeyText(key.value.toString())
            Key.DoubleZero -> KeyText("00")
            Key.Delete -> Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null, tint = Koku.colors.ink, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun KeyText(text: String) {
    Text(text, style = KokuType.body.copy(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium), color = Koku.colors.ink)
}
