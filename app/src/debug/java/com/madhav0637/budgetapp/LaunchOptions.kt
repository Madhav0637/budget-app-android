package com.madhav0637.budgetapp

import android.content.Intent
import android.util.Log
import com.madhav0637.budgetapp.data.AppDatabase
import com.madhav0637.budgetapp.data.Appearance
import com.madhav0637.budgetapp.data.Highlight
import kotlinx.coroutines.runBlocking

/**
 * Debug builds only (release builds get the empty version in src/release): options for screenshots and manual
 * testing, read from the launch intent. Start from a stopped app (`-S`) so nothing has opened the real database yet:
 *
 * ```
 * adb shell am start -S -n com.madhav0637.budgetapp/.MainActivity --ez demoData true \
 *     --es startTab insights --es appearance dark --es highlight mint --el monthlyBudget 25000 --ez openAdd true
 * ```
 *
 * `demoData` fills a throwaway in-memory database with about 11 weeks of sample spending and uses a separate
 * settings file. The other options only apply together with it, so they can never change real settings.
 */
object LaunchOptions {
    fun apply(app: BudgetApplication, intent: Intent?): LaunchRequest {
        if (intent?.getBooleanExtra("demoData", false) != true) return LaunchRequest()
        val swapped = app.useSampleData {
            AppDatabase.inMemory(app, seedDefaults = true).also { runBlocking { DemoData.seed(it) } }
        }
        if (!swapped) {
            Log.w("LaunchOptions", "The real database is already open; stop the app first (am start -S) to use demo data.")
            return LaunchRequest()
        }

        val settings = app.settings
        intent.getStringExtra("appearance")?.let { name ->
            Appearance.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let(settings::setAppearance)
        }
        intent.getStringExtra("highlight")?.let { name ->
            Highlight.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let(settings::setHighlight)
        }
        if (intent.hasExtra("monthlyBudget")) {
            // Accepts --el (a Long) or --ei (an Int).
            val rupees = intent.getLongExtra("monthlyBudget", -1).takeIf { it >= 0 } ?: intent.getIntExtra("monthlyBudget", 0).toLong()
            settings.setMonthlyBudget(rupees)
        }
        return LaunchRequest(tab = intent.getStringExtra("startTab"), openAdd = intent.getBooleanExtra("openAdd", false))
    }
}
