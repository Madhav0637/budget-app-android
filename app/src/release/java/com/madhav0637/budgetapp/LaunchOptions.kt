package com.madhav0637.budgetapp

import android.content.Intent

/** Release builds ignore launch options and never contain sample data. The debug version is in src/debug. */
object LaunchOptions {
    @Suppress("UNUSED_PARAMETER")
    fun apply(app: BudgetApplication, intent: Intent?): LaunchRequest = LaunchRequest()
}
