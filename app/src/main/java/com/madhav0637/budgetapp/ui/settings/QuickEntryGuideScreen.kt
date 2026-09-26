package com.madhav0637.budgetapp.ui.settings

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.madhav0637.budgetapp.R
import com.madhav0637.budgetapp.tile.QuickEntryTileService
import com.madhav0637.budgetapp.ui.components.KokuButton
import com.madhav0637.budgetapp.ui.components.KokuCard
import com.madhav0637.budgetapp.ui.components.PageMargin
import com.madhav0637.budgetapp.ui.components.SubScreen
import com.madhav0637.budgetapp.ui.quickentry.QuickEntryActivity
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType

/**
 * How to open the Log Expense pop-up quickly, as numbered steps, with one-tap buttons for the tile and a
 * home-screen icon. Android's version of the iOS Back Tap guide.
 */
@Composable
fun QuickEntryGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = Koku.colors
    SubScreen(title = "Set up quick entry", onBack = onBack) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = PageMargin).padding(top = 4.dp, bottom = 96.dp),
        ) {
            Text(
                "Log an expense in about five seconds, without opening Koku. Any of these opens the same small pop-up.",
                style = KokuType.subheadline,
                color = colors.ink2,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            Step(1, "Add the Quick Settings tile", "Works on every phone. Swipe down from the top twice, tap the pencil (Edit), and drag “Log Expense” into your tiles.") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    KokuButton("Add the tile for me", onClick = { requestTile(context) }, icon = Icons.Rounded.Bolt, height = 48.dp)
                }
            }
            Step(2, "Or put an icon on your home screen", "Long-press the Koku icon and tap “Log Expense”, or drag it onto your home screen.") {
                if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                    KokuButton("Pin a Log Expense icon", onClick = { pinShortcut(context) }, primary = false, icon = Icons.AutoMirrored.Rounded.AddToHomeScreen, height = 48.dp)
                }
            }
            Step(
                3,
                "Or use your phone's own gesture",
                "Most phones can open an app with a gesture. Set it to open “Log Expense”:\n" +
                    "• Samsung: Settings → Advanced features → Side button → Double press → Open app\n" +
                    "• Google Pixel: Settings → System → Gestures → Quick Tap → Open app\n" +
                    "• Other brands: search Settings for “gesture” or “quick launch”",
            )
            Step(4, "Try it", "With your phone unlocked, tap the tile, the icon or your gesture. Answer the three questions and the expense is saved. Koku stays closed.")

            Text(
                "From the lock screen, the tile asks you to unlock first. If an entry takes you past 80% or 100% of your monthly budget, you'll get a notification.",
                style = KokuType.footnote,
                color = colors.ink2,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
            )
        }
    }
}

/** One numbered step: a highlight circle with the number, a title and the details. */
@Composable
private fun Step(number: Int, title: String, detail: String, action: @Composable () -> Unit = {}) {
    val colors = Koku.colors
    KokuCard(padding = 16.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp).background(colors.highlightFill, CircleShape)) {
                Text("$number", style = KokuType.headline.copy(fontWeight = FontWeight.Bold), color = colors.onHighlight)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(title, style = KokuType.headline, color = colors.ink)
                Text(detail, style = KokuType.subheadline, color = colors.ink2)
                Box(Modifier.padding(top = 6.dp)) { action() }
            }
        }
    }
}

/** Android 13+ can show a system prompt that adds our tile to Quick Settings in one tap. */
private fun requestTile(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    context.getSystemService(StatusBarManager::class.java).requestAddTileService(
        ComponentName(context, QuickEntryTileService::class.java),
        context.getString(R.string.log_expense),
        Icon.createWithResource(context, R.drawable.ic_log_expense),
        context.mainExecutor,
    ) { }
}

/** Asks the launcher to pin a separate "Log Expense" icon that opens the pop-up. */
private fun pinShortcut(context: Context) {
    val shortcut = ShortcutInfoCompat.Builder(context, "log_expense_pinned")
        .setShortLabel(context.getString(R.string.log_expense))
        .setLongLabel(context.getString(R.string.log_expense_long))
        .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
        .setIntent(Intent(context, QuickEntryActivity::class.java).setAction(Intent.ACTION_VIEW))
        .build()
    ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
}
