package com.madhav0637.budgetapp.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madhav0637.budgetapp.domain.indianGrouped
import com.madhav0637.budgetapp.domain.inr
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType
import com.madhav0637.budgetapp.ui.theme.LogoColors
import kotlin.math.abs
import kotlin.math.roundToInt

/** The page margin used by every screen. */
val PageMargin = 20.dp

/** False when the phone's animations are switched off (Developer options or accessibility), so grow-ins are skipped. */
fun animationsEnabled(): Boolean = ValueAnimator.areAnimatorsEnabled()

// MARK: Press feedback

/** Shrinks a little while pressed, with a spring, instead of a ripple. Used by every Koku button. */
fun Modifier.pressable(
    enabled: Boolean = true,
    scale: Float = 0.96f,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(if (pressed) scale else 1f, spring(dampingRatio = 0.7f, stiffness = 600f), label = "press")
    val alpha by animateFloatAsState(if (pressed) 0.85f else 1f, spring(stiffness = 600f), label = "pressAlpha")
    graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
        this.alpha = alpha
    }.clickable(
        interactionSource = interaction,
        indication = null,
        enabled = enabled,
        role = role,
        onClickLabel = onClickLabel,
        onClick = onClick,
    )
}

// MARK: Numbers

/**
 * A rupee amount with a smaller, quieter ₹ in front: the style of every hero number. Changed digits roll,
 * and the text shrinks (down to half size) rather than wrap when it's too wide.
 */
@Composable
fun AmountText(
    amount: Long,
    modifier: Modifier = Modifier,
    size: TextUnit = 56.sp,
    color: Color = Koku.colors.ink,
    placeholder: Boolean = false,
) {
    BoxWithConstraints(modifier.semantics(mergeDescendants = true) {}.clearAndSetSemantics { contentDescription = amount.inr() }) {
        val measurer = rememberTextMeasurer()
        val digits = amount.indianGrouped()
        val density = LocalDensity.current
        val fullWidth = remember(digits, size) {
            val number = measurer.measure(digits, digitStyle(size)).size.width
            val rupee = measurer.measure("₹", rupeeStyle(size)).size.width
            with(density) { (number + rupee).toDp() + (size.value * 0.05f).dp }
        }
        val scale = if (fullWidth > maxWidth && fullWidth > 0.dp) (maxWidth / fullWidth).coerceIn(0.5f, 1f) else 1f
        val shown = size * scale
        // Sits the ₹ on the digits' baseline. Measured directly, because the rolling digits don't pass a baseline up.
        val rupeeLift = remember(shown) {
            val number = measurer.measure("0", digitStyle(shown))
            val rupee = measurer.measure("₹", rupeeStyle(shown))
            with(density) { ((number.size.height - number.firstBaseline) - (rupee.size.height - rupee.firstBaseline)).toDp() }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy((shown.value * 0.05f).dp)) {
            Text("₹", style = rupeeStyle(shown).copy(color = Koku.colors.ink2), modifier = Modifier.offset(y = -rupeeLift))
            RollingText(
                text = digits,
                value = amount,
                style = digitStyle(shown).copy(color = if (placeholder) Koku.colors.ink3 else color),
            )
        }
    }
}

private fun rupeeStyle(size: TextUnit) = KokuType.body.copy(fontSize = size * 0.58f, lineHeight = size * 0.7f, fontWeight = FontWeight.SemiBold)

private fun digitStyle(size: TextUnit) = KokuType.body.copy(
    fontSize = size,
    lineHeight = size * 1.15f,
    fontWeight = FontWeight.Bold,
    letterSpacing = size * -0.02f,
)

/**
 * Text whose characters roll up (or down, for a smaller [value]) when they change, like a counter. Characters are
 * matched from the right, so the units stay in place when the number gets longer.
 */
@Composable
fun RollingText(text: String, value: Long, style: TextStyle, modifier: Modifier = Modifier) {
    val last = remember { mutableLongStateOf(value) }
    val goingUp = value >= last.longValue
    LaunchedEffect(value) { last.longValue = value }
    Row(modifier) {
        text.forEachIndexed { index, char ->
            key(text.length - index) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        val enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { if (goingUp) it else -it } + fadeIn(tween(160))
                        val exit = slideOutVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { if (goingUp) -it else it } + fadeOut(tween(120))
                        (enter togetherWith exit).using(SizeTransform(clip = true))
                    },
                    label = "digit",
                ) { shown -> Text(shown.toString(), style = style, maxLines = 1, softWrap = false) }
            }
        }
    }
}

/** A blinking text cursor in the highlight colour. */
@Composable
fun Caret(color: Color, height: Dp = 52.dp, modifier: Modifier = Modifier) {
    val blink = rememberInfiniteTransition(label = "caret")
    val alpha by blink.animateFloat(1f, 0f, infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "caretAlpha")
    Box(modifier.width(3.dp).height(height).graphicsLayer { this.alpha = alpha }.background(color, CircleShape))
}

// MARK: Surfaces

/** A white (or near-black) rounded card. Light mode adds a hairline and a whisper of shadow; dark mode stays flat. */
@Composable
fun KokuCard(
    modifier: Modifier = Modifier,
    padding: Dp = 20.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Koku.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        verticalArrangement = verticalArrangement,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (colors.isDark) {
                    Modifier
                } else {
                    Modifier.shadow(4.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.25f))
                },
            )
            .background(colors.surface, shape)
            .then(if (colors.isDark) Modifier else Modifier.border(1.dp, colors.hairline, shape))
            .clip(shape)
            .padding(padding),
        content = content,
    )
}

/** A thin line between rows, starting after the emoji tile so it lines up with the text. */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier, startIndent: Dp = 0.dp) {
    HorizontalDivider(modifier.padding(start = startIndent), thickness = 1.dp, color = Koku.colors.hairline)
}

/** A category's emoji on a soft rounded tile. */
@Composable
fun EmojiTile(emoji: String, modifier: Modifier = Modifier, size: Dp = 42.dp, background: Color = Koku.colors.surface2) {
    val fontSize = with(LocalDensity.current) { (size * 0.5f).toSp() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(background, RoundedCornerShape(size * 0.3f))
            .clearAndSetSemantics {},
    ) {
        Text(emoji, fontSize = fontSize, lineHeight = fontSize * 1.2f, textAlign = TextAlign.Center)
    }
}

/** An icon on a soft rounded tile, for settings rows. */
@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(32.dp).background(Koku.colors.surface2, RoundedCornerShape(9.dp)),
    ) {
        Icon(icon, contentDescription = null, tint = Koku.colors.ink, modifier = Modifier.size(18.dp))
    }
}

// MARK: Controls

/** A segmented control whose thumb slides between options. */
@Composable
fun <T> PillPicker(
    selection: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable RowScope.(option: T, selected: Boolean) -> Unit,
) {
    val colors = Koku.colors
    val haptics = LocalHapticFeedback.current
    BoxWithConstraints(modifier.fillMaxWidth().background(colors.surface2, CircleShape).padding(4.dp)) {
        val itemWidth = maxWidth / options.size
        val index = options.indexOf(selection).coerceAtLeast(0)
        val offset by animateDpAsState(itemWidth * index, spring(dampingRatio = 0.85f, stiffness = 500f), label = "thumb")
        Box(
            Modifier
                .offset(x = offset)
                .width(itemWidth)
                .height(38.dp)
                .shadow(if (colors.isDark) 0.dp else 2.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
                .background(colors.thumb, CircleShape),
        )
        Row {
            options.forEach { option ->
                val selected = option == selection
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(CircleShape)
                        .semantics { this.selected = selected }
                        .clickable(interactionSource = null, indication = null, role = Role.Tab) {
                            if (!selected) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelect(option)
                            }
                        },
                ) { label(option, selected) }
            }
        }
    }
}

/** The label style for [PillPicker] options. */
@Composable
fun PillLabel(text: String, selected: Boolean, icon: ImageVector? = null) {
    val color = if (selected) Koku.colors.ink else Koku.colors.ink2
    if (icon != null) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
    }
    Text(text, style = KokuType.subheadline.copy(fontWeight = FontWeight.SemiBold), color = color, maxLines = 1)
}

/** A rounded bar that fills from the left, growing in when it first appears. */
@Composable
fun ProgressBar(
    value: Double,
    tint: Color,
    modifier: Modifier = Modifier,
    track: Color = Koku.colors.surface2,
    height: Dp = 8.dp,
) {
    val fraction = value.coerceIn(0.0, 1.0).toFloat()
    var shown by remember { mutableStateOf(!animationsEnabled()) }
    LaunchedEffect(Unit) { shown = true }
    val animated by animateFloatAsState(
        if (shown) fraction else 0f,
        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessVeryLow * 3),
        label = "progress",
    )
    Canvas(modifier.fillMaxWidth().height(height).clearAndSetSemantics {}) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = radius)
        if (fraction > 0f) {
            val width = maxOf(size.height, size.width * animated)
            drawRoundRect(tint, size = Size(width, size.height), cornerRadius = radius)
        }
    }
}

/** How a selected [Chip] looks: solid ink, or the highlight colour with a check. */
enum class ChipStyle { Ink, Highlight }

/** A pill for filters and choices. */
@Composable
fun Chip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    style: ChipStyle = ChipStyle.Ink,
    restingBackground: Color = Koku.colors.surface,
) {
    val colors = Koku.colors
    val background = when {
        !selected -> restingBackground
        style == ChipStyle.Ink -> colors.ink
        else -> colors.highlightFill
    }
    val foreground = when {
        !selected -> colors.ink
        style == ChipStyle.Ink -> colors.canvas
        else -> colors.onHighlight
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .pressable(onClick = onClick)
            .semantics { this.selected = selected }
            .height(36.dp)
            .background(background, CircleShape)
            .then(if (selected) Modifier else Modifier.border(1.dp, colors.hairline, CircleShape))
            .padding(horizontal = 14.dp),
    ) {
        if (emoji != null) Text(emoji, style = KokuType.subheadline)
        Text(
            title,
            style = KokuType.subheadline.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = foreground,
            maxLines = 1,
        )
        if (selected && style == ChipStyle.Highlight) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = foreground, modifier = Modifier.size(15.dp))
        }
    }
}

/** A section title with an optional "See all ›" style link. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, actionTitle: String? = null, onAction: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Text(title, style = KokuType.title3, color = Koku.colors.ink, modifier = Modifier.weight(1f))
        if (actionTitle != null && onAction != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.pressable(onClick = onAction).padding(vertical = 4.dp)) {
                Text(actionTitle, style = KokuType.subheadline.copy(fontWeight = FontWeight.Medium), color = Koku.colors.ink2)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Koku.colors.ink2, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** "↓ 12% vs same time last month". Spending less gets the highlight colour; spending more stays neutral. */
@Composable
fun ChangePill(change: Double, comparedWith: String, modifier: Modifier = Modifier) {
    val colors = Koku.colors
    val percent = (abs(change) * 100).roundToInt()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier.background(colors.surface2, CircleShape).padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        val style = KokuType.footnote.copy(fontWeight = FontWeight.SemiBold)
        if (percent == 0) {
            Text("Same as $comparedWith", style = style, color = colors.ink)
        } else {
            Icon(
                if (change < 0) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                contentDescription = if (change < 0) "Down" else "Up",
                tint = if (change < 0) colors.highlightText else colors.ink2,
                modifier = Modifier.size(15.dp),
            )
            Text("$percent% vs $comparedWith", style = style, color = colors.ink)
        }
    }
}

/** The app's mark: a mint circle, an amber pill and a periwinkle pill on a graphite tile (a 512 grid, as on iOS). */
@Composable
fun KokuLogo(modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Canvas(modifier.size(size).clearAndSetSemantics {}) {
        val unit = this.size.width / 512f
        drawRoundRect(LogoColors.graphite, cornerRadius = CornerRadius(118 * unit))
        drawCircle(LogoColors.mint, radius = 72 * unit, center = Offset(194 * unit, 194 * unit))
        drawRoundRect(LogoColors.amber, topLeft = Offset(122 * unit, 266 * unit), size = Size(144 * unit, 82 * unit), cornerRadius = CornerRadius(41 * unit))
        drawRoundRect(LogoColors.periwinkle, topLeft = Offset(266 * unit, 163 * unit), size = Size(114 * unit, 185 * unit), cornerRadius = CornerRadius(57 * unit))
    }
}

/** The big round add button, in the highlight colour. */
@Composable
fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Koku.colors
    val haptics = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .pressable(onClickLabel = "Add expense") {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onClick()
            }
            .semantics { contentDescription = "Add expense" }
            .size(62.dp)
            .shadow(10.dp, CircleShape, ambientColor = colors.highlightFill, spotColor = colors.highlightFill)
            .background(colors.highlightFill, CircleShape),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.onHighlight, modifier = Modifier.size(30.dp))
    }
}

/** A full-width pill button. The primary one is the highlight colour; it fades when disabled. */
@Composable
fun KokuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color? = null,
    height: Dp = 54.dp,
) {
    val colors = Koku.colors
    val background = if (primary) colors.highlightFill.copy(alpha = if (enabled) 1f else 0.35f) else colors.surface2
    val foreground = contentColor ?: if (primary) colors.onHighlight else colors.ink
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .pressable(enabled = enabled, onClick = onClick)
            .height(height)
            .background(background, CircleShape)
            .padding(horizontal = 16.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = KokuType.headline.copy(fontWeight = FontWeight.Bold),
            color = foreground.copy(alpha = if (enabled || primary) 1f else 0.4f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A round icon button on a soft circle, for back, delete and the Insights arrows. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Koku.colors.ink,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .pressable(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .size(36.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
            .background(Koku.colors.surface2, CircleShape),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** A text field on a soft rounded background, with an optional icon and clear button. */
@Composable
fun KokuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    showsClear: Boolean = false,
    background: Color = Koku.colors.surface2,
    shape: Shape = RoundedCornerShape(18.dp),
    borderColor: Color? = null,
    height: Dp = 54.dp,
    textStyle: TextStyle = KokuType.body.copy(fontWeight = FontWeight.Medium),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onClear: () -> Unit = { onValueChange("") },
) {
    val colors = Koku.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.highlightText),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { field ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = height)
                    .background(background, shape)
                    .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
                    .padding(horizontal = 16.dp),
            ) {
                if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = textStyle, color = colors.ink3, maxLines = 1)
                    field()
                }
                if (showsClear && value.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.Cancel,
                        contentDescription = "Clear",
                        tint = colors.ink3,
                        modifier = Modifier.size(20.dp).clip(CircleShape).clickable(onClick = onClear),
                    )
                }
            }
        },
    )
}

// MARK: Page structure

/** A tab's big title at the top of the page, below the status bar. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = KokuType.largeTitle,
        color = Koku.colors.ink,
        modifier = modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 20.dp, bottom = 4.dp),
    )
}

/** A small heading above a group of settings. */
@Composable
fun GroupHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = KokuType.subheadline.copy(fontWeight = FontWeight.SemiBold),
        color = Koku.colors.ink2,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
    )
}

/** A screen inside Settings: canvas background, a back button and a title, then the content. */
@Composable
fun SubScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().background(Koku.colors.canvas)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .fillMaxWidth()
                .padding(horizontal = PageMargin, vertical = 12.dp),
        ) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Text(title, style = KokuType.title3, color = Koku.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            actions()
        }
        content()
    }
}

/** A friendly card for when there's nothing to show yet. */
@Composable
fun EmptyStateCard(emoji: String, title: String, detail: String, modifier: Modifier = Modifier) {
    KokuCard(modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        ) {
            Text(emoji, fontSize = 44.sp)
            Text(title, style = KokuType.headline, color = Koku.colors.ink, textAlign = TextAlign.Center)
            Text(detail, style = KokuType.subheadline, color = Koku.colors.ink2, textAlign = TextAlign.Center)
        }
    }
}

/** Grows a bar up from its bottom edge; used by the chart on Home. */
fun Modifier.growFromBottom(fraction: Float): Modifier = graphicsLayer {
    scaleY = fraction
    transformOrigin = TransformOrigin(0.5f, 1f)
}
