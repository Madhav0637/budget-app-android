package com.madhav0637.budgetapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.madhav0637.budgetapp.LaunchRequest
import com.madhav0637.budgetapp.ui.activity.ActivityScreen
import com.madhav0637.budgetapp.ui.budget.BudgetSheet
import com.madhav0637.budgetapp.ui.components.HairlineDivider
import com.madhav0637.budgetapp.ui.components.LocalToasts
import com.madhav0637.budgetapp.ui.components.rememberToastCenter
import com.madhav0637.budgetapp.ui.expenseform.ExpenseFormMode
import com.madhav0637.budgetapp.ui.expenseform.ExpenseFormSheet
import com.madhav0637.budgetapp.ui.home.HomeScreen
import com.madhav0637.budgetapp.ui.insights.InsightsScreen
import com.madhav0637.budgetapp.ui.settings.SettingsTab
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType

enum class AppTab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("Home", Icons.Outlined.Home, Icons.Rounded.Home),
    Activity("Activity", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Rounded.ReceiptLong),
    Insights("Insights", Icons.Outlined.Insights, Icons.Rounded.Insights),
    Settings("Settings", Icons.Outlined.Settings, Icons.Rounded.Settings),
}

/** The app's tabs, plus the Add / Edit and budget sheets and the shared toast banner. */
@Composable
fun RootScreen(launch: LaunchRequest = LaunchRequest()) {
    var tab by rememberSaveable { mutableStateOf(AppTab.entries.firstOrNull { it.name.equals(launch.tab, ignoreCase = true) } ?: AppTab.Home) }
    var form by remember { mutableStateOf<ExpenseFormMode?>(if (launch.openAdd) ExpenseFormMode.Add else null) }
    var editingBudget by rememberSaveable { mutableStateOf(false) }
    val toasts = rememberToastCenter()
    // Keeps each tab's own state (scroll position, search, the Settings screen it's on) while switching tabs.
    val tabStates = rememberSaveableStateHolder()

    // Back from another tab goes to Home first, the usual Android behaviour.
    BackHandler(enabled = tab != AppTab.Home) { tab = AppTab.Home }

    CompositionLocalProvider(LocalToasts provides toasts) {
        Scaffold(
            containerColor = Koku.colors.canvas,
            // Each tab handles the status bar itself, so only the tab bar's space is handled here.
            contentWindowInsets = WindowInsets(0),
            bottomBar = { TabBar(tab, onSelect = { tab = it }) },
        ) { padding ->
            // The tab bar already covers the phone's navigation bar, so stop the tabs adding that space again.
            Box(Modifier.padding(padding).consumeWindowInsets(WindowInsets.navigationBars)) {
                tabStates.SaveableStateProvider(tab.name) {
                    when (tab) {
                        AppTab.Home -> HomeScreen(
                            onAdd = { form = ExpenseFormMode.Add },
                            onEdit = { form = ExpenseFormMode.Edit(it) },
                            onEditBudget = { editingBudget = true },
                            onOpenInsights = { tab = AppTab.Insights },
                            onOpenActivity = { tab = AppTab.Activity },
                        )
                        AppTab.Activity -> ActivityScreen(
                            onAdd = { form = ExpenseFormMode.Add },
                            onEdit = { form = ExpenseFormMode.Edit(it) },
                        )
                        AppTab.Insights -> InsightsScreen()
                        AppTab.Settings -> SettingsTab(onEditBudget = { editingBudget = true })
                    }
                }
            }
        }

        form?.let { mode -> ExpenseFormSheet(mode, onDismiss = { form = null }) }
        if (editingBudget) BudgetSheet(onDismiss = { editingBudget = false })
    }
}

@Composable
private fun TabBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    val colors = Koku.colors
    val haptics = LocalHapticFeedback.current
    Column {
        HairlineDivider()
        NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
            AppTab.entries.forEach { item ->
                val isSelected = item == selected
                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onSelect(item)
                    },
                    icon = { Icon(if (isSelected) item.selectedIcon else item.icon, contentDescription = null) },
                    label = {
                        Text(item.label, style = KokuType.caption.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium))
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.highlightText,
                        selectedTextColor = colors.highlightText,
                        indicatorColor = colors.surface2,
                        unselectedIconColor = colors.ink,
                        unselectedTextColor = colors.ink2,
                    ),
                )
            }
        }
    }
}
