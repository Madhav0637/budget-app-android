package com.madhav0637.budgetapp.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madhav0637.budgetapp.data.Appearance
import com.madhav0637.budgetapp.data.Highlight
import com.madhav0637.budgetapp.domain.inr
import com.madhav0637.budgetapp.notifications.BudgetNotifier
import com.madhav0637.budgetapp.ui.components.GroupHeader
import com.madhav0637.budgetapp.ui.components.HairlineDivider
import com.madhav0637.budgetapp.ui.components.IconTile
import com.madhav0637.budgetapp.ui.components.KokuCard
import com.madhav0637.budgetapp.ui.components.KokuLogo
import com.madhav0637.budgetapp.ui.components.PageMargin
import com.madhav0637.budgetapp.ui.components.PillLabel
import com.madhav0637.budgetapp.ui.components.PillPicker
import com.madhav0637.budgetapp.ui.components.ScreenTitle
import com.madhav0637.budgetapp.ui.components.ToastSlot
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType
import com.madhav0637.budgetapp.ui.theme.bright

/**
 * The Settings tab. It has its own small back stack: Settings → Categories → one category,
 * plus the quick-entry guide and Export.
 */
@Composable
fun SettingsTab(onEditBudget: () -> Unit) {
    // "settings", "categories", "guide", "export", or the id of the category being edited.
    var screen by rememberSaveable { mutableStateOf(SETTINGS) }
    val categoriesViewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory)

    BackHandler(enabled = screen != SETTINGS) {
        screen = if (screen in listOf(CATEGORIES, GUIDE, EXPORT)) SETTINGS else CATEGORIES
    }

    Box(Modifier.fillMaxSize()) {
        when (screen) {
            SETTINGS -> SettingsScreen(
                onEditBudget = onEditBudget,
                onCategories = { screen = CATEGORIES },
                onGuide = { screen = GUIDE },
                onExport = { screen = EXPORT },
            )
            GUIDE -> QuickEntryGuideScreen(onBack = { screen = SETTINGS })
            EXPORT -> ExportScreen(onBack = { screen = SETTINGS })
            CATEGORIES -> CategoriesScreen(
                viewModel = categoriesViewModel,
                onBack = { screen = SETTINGS },
                onOpen = { screen = it.id },
            )
            else -> CategoryDetailScreen(
                categoryId = screen,
                viewModel = categoriesViewModel,
                onBack = { screen = CATEGORIES },
            )
        }
        ToastSlot(Modifier.align(Alignment.BottomCenter).padding(horizontal = PageMargin, vertical = 12.dp))
    }
}

private const val SETTINGS = "settings"
private const val CATEGORIES = "categories"
private const val GUIDE = "guide"
private const val EXPORT = "export"

/** Theme and highlight colour, the monthly budget and its alerts, categories, quick entry and export. */
@Composable
private fun SettingsScreen(
    onEditBudget: () -> Unit,
    onCategories: () -> Unit,
    onGuide: () -> Unit,
    onExport: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val colors = Koku.colors
    val context = LocalContext.current
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val highlight by viewModel.highlight.collectAsStateWithLifecycle()
    val budget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val alerts by viewModel.budgetAlerts.collectAsStateWithLifecycle()
    val categoryCount by viewModel.categoryCount.collectAsStateWithLifecycle()

    // Notifications can be switched off in Android's settings at any time, so check again whenever the app returns.
    var notificationsDenied by remember { mutableStateOf(BudgetNotifier.isDenied(context)) }
    LifecycleResumeEffect(Unit) {
        notificationsDenied = BudgetNotifier.isDenied(context)
        onPauseOrDispose { }
    }
    val askForNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsDenied = BudgetNotifier.isDenied(context)
    }

    fun setAlerts(on: Boolean) {
        viewModel.setBudgetAlerts(on)
        // Android 13+ asks before an app can show notifications.
        if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && BudgetNotifier.needsPermission(context)) {
            askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PageMargin)
            .padding(bottom = 24.dp),
    ) {
        ScreenTitle("Settings")

        GroupHeader("Appearance", Modifier.padding(top = 12.dp))
        KokuCard {
            Text("Theme", style = KokuType.body.copy(fontWeight = FontWeight.Medium), color = colors.ink)
            PillPicker(appearance, Appearance.entries, onSelect = viewModel::setAppearance, modifier = Modifier.padding(top = 12.dp)) { option, selected ->
                PillLabel(option.title, selected, icon = option.icon)
            }
            HairlineDivider(Modifier.padding(vertical = 18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Highlight", style = KokuType.body.copy(fontWeight = FontWeight.Medium), color = colors.ink, modifier = Modifier.weight(1f))
                Text(highlight.name, style = KokuType.subheadline.copy(fontWeight = FontWeight.SemiBold), color = colors.highlightText)
            }
            HighlightSwatches(highlight, viewModel::setHighlight, Modifier.padding(top = 12.dp))
        }

        GroupHeader("Budget", Modifier.padding(top = 20.dp))
        KokuCard(padding = 0.dp) {
            SettingsRow(Icons.Rounded.TrackChanges, "Monthly budget", "Starts again on the 1st", onClick = onEditBudget) {
                Text(if (budget > 0) budget.inr() else "Not set", style = KokuType.body, color = colors.ink2)
                Chevron()
            }
            HairlineDivider(startIndent = 60.dp)
            val alertsEnabled = budget > 0
            SettingsRow(
                Icons.Rounded.NotificationsActive,
                "Budget alerts",
                "At 80% and 100% of your budget",
                enabled = alertsEnabled,
                onClick = { setAlerts(!alerts) },
            ) {
                Switch(
                    checked = alerts,
                    enabled = alertsEnabled,
                    onCheckedChange = ::setAlerts,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colors.highlightFill,
                        checkedThumbColor = Color.White,
                        checkedBorderColor = Color.Transparent,
                        uncheckedTrackColor = colors.surface2,
                        uncheckedThumbColor = colors.ink3,
                        uncheckedBorderColor = Color.Transparent,
                    ),
                )
            }
        }
        if (budget > 0 && alerts && notificationsDenied) {
            Text(
                "Notifications are off for Koku, so alerts only appear inside the app. Tap here to turn them on in Android settings.",
                style = KokuType.footnote,
                color = colors.ink2,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .clickable(role = Role.Button) { openNotificationSettings(context) },
            )
        }

        GroupHeader("Data", Modifier.padding(top = 20.dp))
        KokuCard(padding = 0.dp) {
            SettingsRow(Icons.Rounded.GridView, "Categories", onClick = onCategories) {
                Text("$categoryCount", style = KokuType.body, color = colors.ink2)
                Chevron()
            }
            HairlineDivider(startIndent = 60.dp)
            SettingsRow(Icons.Rounded.TouchApp, "Set up quick entry", "Log in 5 seconds without opening the app", onClick = onGuide) { Chevron() }
            HairlineDivider(startIndent = 60.dp)
            SettingsRow(Icons.Rounded.IosShare, "Export", "CSV spreadsheet or PDF report", onClick = onExport) { Chevron() }
        }

        Footer(Modifier.padding(top = 28.dp))
    }
}

private val Appearance.icon: ImageVector
    get() = when (this) {
        Appearance.System -> Icons.Rounded.Contrast
        Appearance.Light -> Icons.Rounded.LightMode
        Appearance.Dark -> Icons.Rounded.DarkMode
    }

/** A settings row: icon tile, title and optional subtitle, and something on the right. */
@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    val colors = Koku.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        IconTile(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = KokuType.body.copy(fontWeight = FontWeight.Medium), color = colors.ink)
            if (subtitle != null) Text(subtitle, style = KokuType.footnote, color = colors.ink2)
        }
        trailing()
    }
}

@Composable
internal fun Chevron() {
    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Koku.colors.ink3, modifier = Modifier.size(22.dp))
}

/** Six colour dots; the chosen one has a ring and a check. */
@Composable
private fun HighlightSwatches(selection: Highlight, onSelect: (Highlight) -> Unit, modifier: Modifier = Modifier) {
    val colors = Koku.colors
    val haptics = LocalHapticFeedback.current
    Row(modifier.fillMaxWidth()) {
        Highlight.entries.forEach { option ->
            val selected = option == selection
            val scale by animateFloatAsState(if (selected) 1.08f else 1f, label = "swatch")
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .border(2.dp, if (selected) colors.ink else Color.Transparent, CircleShape)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(option.bright)
                        .clickable(role = Role.RadioButton) {
                            if (!selected) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelect(option)
                            }
                        }
                        .semantics {
                            contentDescription = option.name
                            this.selected = selected
                        },
                ) {
                    // The plain version, not Row's, since this sits inside a Box inside the Row.
                    androidx.compose.animation.AnimatedVisibility(visible = selected, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.onHighlight, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Footer(modifier: Modifier = Modifier) {
    val colors = Koku.colors
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        KokuLogo(size = 44.dp)
        Text("koku $version", style = KokuType.headline, color = colors.ink)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(6.dp).background(colors.highlightFill, CircleShape))
            Text("Everything stays on this phone.", style = KokuType.footnote, color = colors.ink2)
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    context.startActivity(intent)
}
