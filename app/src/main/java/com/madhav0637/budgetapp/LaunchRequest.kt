package com.madhav0637.budgetapp

/** Where the app opens. Normally Home; debug builds can ask for another tab or the Add screen, for screenshots. */
data class LaunchRequest(
    /** "home", "activity", "insights" or "settings". */
    val tab: String? = null,
    val openAdd: Boolean = false,
)
