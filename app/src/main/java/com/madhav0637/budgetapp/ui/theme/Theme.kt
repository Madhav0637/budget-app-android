package com.madhav0637.budgetapp.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import com.madhav0637.budgetapp.data.Appearance
import com.madhav0637.budgetapp.data.Highlight

private val LocalKokuColors = staticCompositionLocalOf { lightKokuColors(Highlight.Mint) }

/** Koku's colours for the current theme: `Koku.colors.ink`, `Koku.colors.highlightFill`, ... */
object Koku {
    val colors: KokuColors
        @Composable @ReadOnlyComposable get() = LocalKokuColors.current
}

/**
 * Applies Light / Dark / System and the highlight colour to everything inside it, including Material dialogs,
 * sheets and pickers (through a matching Material colour scheme). Switching theme cross-fades the colours.
 */
@Composable
fun KokuTheme(appearance: Appearance, highlight: Highlight, content: @Composable () -> Unit) {
    val dark = when (appearance) {
        Appearance.System -> isSystemInDarkTheme()
        Appearance.Light -> false
        Appearance.Dark -> true
    }
    val target = if (dark) darkKokuColors(highlight) else lightKokuColors(highlight)
    val colors = animated(target)

    SystemBarsFollowTheme(dark)
    val context = LocalContext.current
    LaunchedEffect(appearance) { applyNightMode(context, appearance) }

    CompositionLocalProvider(LocalKokuColors provides colors) {
        MaterialTheme(colorScheme = materialColors(colors), typography = Typography, content = content)
    }
}

/** Cross-fades between the light and dark palettes (and between highlight colours) instead of snapping. */
@Composable
private fun animated(target: KokuColors): KokuColors {
    @Composable
    fun Color.fade(label: String): Color = animateColorAsState(this, tween(350), label = label).value
    return target.copy(
        canvas = target.canvas.fade("canvas"),
        surface = target.surface.fade("surface"),
        surface2 = target.surface2.fade("surface2"),
        thumb = target.thumb.fade("thumb"),
        ink = target.ink.fade("ink"),
        ink2 = target.ink2.fade("ink2"),
        ink3 = target.ink3.fade("ink3"),
        hairline = target.hairline.fade("hairline"),
        chartBar = target.chartBar.fade("chartBar"),
        warning = target.warning.fade("warning"),
        toast = target.toast.fade("toast"),
        highlightFill = target.highlightFill.fade("highlightFill"),
        highlightText = target.highlightText.fade("highlightText"),
    )
}

/** Material components (dialogs, date and time pickers, switches) drawn in Koku's colours. */
private fun materialColors(c: KokuColors) = (if (c.isDark) darkColorScheme() else lightColorScheme()).copy(
    primary = c.highlightText,
    onPrimary = if (c.isDark) c.onHighlight else Color.White,
    primaryContainer = c.highlightFill,
    onPrimaryContainer = c.onHighlight,
    secondary = c.ink2,
    onSecondary = c.surface,
    secondaryContainer = c.surface2,
    onSecondaryContainer = c.ink,
    tertiary = c.highlightText,
    background = c.canvas,
    onBackground = c.ink,
    surface = c.surface,
    onSurface = c.ink,
    surfaceVariant = c.surface2,
    onSurfaceVariant = c.ink2,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = c.surface,
    surfaceContainerLow = c.surface,
    surfaceContainer = c.surface,
    surfaceContainerHigh = c.surface,
    surfaceContainerHighest = c.surface2,
    inverseSurface = c.toast,
    inverseOnSurface = Color.White,
    inversePrimary = c.highlightFill,
    outline = c.ink3,
    outlineVariant = c.hairline.compositeOver(c.surface),
    error = c.warning,
    onError = Color.White,
)

/** Status and navigation bar icons dark on the light theme and light on the dark theme, whatever the phone uses. */
@Composable
private fun SystemBarsFollowTheme(dark: Boolean) {
    val activity = LocalContext.current as? ComponentActivity ?: return
    LaunchedEffect(dark) {
        val transparent = AndroidColor.TRANSPARENT
        val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}

/**
 * On Android 12+, also tells the system which theme the app uses, so the launch screen and the window behind
 * the app match the choice too. Both activities handle the resulting change themselves (configChanges="uiMode").
 */
private fun applyNightMode(context: Context, appearance: Appearance) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val mode = when (appearance) {
        Appearance.System -> UiModeManager.MODE_NIGHT_AUTO
        Appearance.Light -> UiModeManager.MODE_NIGHT_NO
        Appearance.Dark -> UiModeManager.MODE_NIGHT_YES
    }
    context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(mode)
}
