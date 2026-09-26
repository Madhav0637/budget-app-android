package com.madhav0637.budgetapp.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.madhav0637.budgetapp.domain.BudgetSettings
import com.madhav0637.budgetapp.domain.Period
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Light, dark, or follow the phone. Chosen in Settings. */
enum class Appearance(val title: String) {
    System("System"),
    Light("Light"),
    Dark("Dark"),
}

/** The one highlight colour. Chosen in Settings; the colours themselves live in the theme. */
enum class Highlight {
    Mint, Lime, Sky, Periwinkle, Coral, Amber,
}

/** Where settings live in SharedPreferences. Same names as the iOS UserDefaults keys. */
object SettingsKeys {
    /** Kept from before the redesign, so the chosen period survives the update. */
    const val HOME_PERIOD = "dashboardPeriod"
    const val INSIGHTS_PERIOD = "insightsPeriod"
    const val APPEARANCE = "appearance"
    const val HIGHLIGHT = "highlight"
    /** Whole rupees; 0 means no budget. */
    const val MONTHLY_BUDGET = "monthlyBudget"
    const val BUDGET_ALERTS = "budgetAlerts"
    /** The month ("yyyy-MM") and level (80 or 100) of the last budget alert, so each is sent once a month. */
    const val LAST_ALERT_MONTH = "budgetAlertMonth"
    const val LAST_ALERT_LEVEL = "budgetAlertLevel"
}

/**
 * The app's settings, shared by every screen and the quick-entry pop-up (they run in the same process, so one
 * instance in [com.madhav0637.budgetapp.BudgetApplication] is enough). Each setting is a [StateFlow], so screens
 * update as soon as it changes.
 */
class AppSettings(private val prefs: SharedPreferences) : BudgetSettings {
    private val homePeriodFlow = MutableStateFlow(readEnum(SettingsKeys.HOME_PERIOD, Period.Month))
    private val insightsPeriodFlow = MutableStateFlow(readEnum(SettingsKeys.INSIGHTS_PERIOD, Period.Month))
    private val appearanceFlow = MutableStateFlow(readEnum(SettingsKeys.APPEARANCE, Appearance.System))
    private val highlightFlow = MutableStateFlow(readEnum(SettingsKeys.HIGHLIGHT, Highlight.Mint))
    private val monthlyBudgetFlow = MutableStateFlow(prefs.getLong(SettingsKeys.MONTHLY_BUDGET, 0).coerceAtLeast(0))
    private val budgetAlertsFlow = MutableStateFlow(prefs.getBoolean(SettingsKeys.BUDGET_ALERTS, true))

    /** Home's period, remembered between launches. Starts on Month. */
    val homePeriod: StateFlow<Period> = homePeriodFlow.asStateFlow()
    val insightsPeriod: StateFlow<Period> = insightsPeriodFlow.asStateFlow()
    val appearance: StateFlow<Appearance> = appearanceFlow.asStateFlow()
    val highlight: StateFlow<Highlight> = highlightFlow.asStateFlow()
    /** Whole rupees; 0 means no budget. */
    val monthlyBudget: StateFlow<Long> = monthlyBudgetFlow.asStateFlow()
    /** On unless turned off in Settings. */
    val budgetAlerts: StateFlow<Boolean> = budgetAlertsFlow.asStateFlow()

    fun setHomePeriod(value: Period) = saveEnum(SettingsKeys.HOME_PERIOD, value, homePeriodFlow)
    fun setInsightsPeriod(value: Period) = saveEnum(SettingsKeys.INSIGHTS_PERIOD, value, insightsPeriodFlow)
    fun setAppearance(value: Appearance) = saveEnum(SettingsKeys.APPEARANCE, value, appearanceFlow)
    fun setHighlight(value: Highlight) = saveEnum(SettingsKeys.HIGHLIGHT, value, highlightFlow)

    fun setMonthlyBudget(rupees: Long) {
        val value = rupees.coerceAtLeast(0)
        prefs.edit { putLong(SettingsKeys.MONTHLY_BUDGET, value) }
        monthlyBudgetFlow.value = value
    }

    fun setBudgetAlerts(on: Boolean) {
        prefs.edit { putBoolean(SettingsKeys.BUDGET_ALERTS, on) }
        budgetAlertsFlow.value = on
    }

    // BudgetSettings, read by BudgetService when an expense is saved.

    override val currentBudget: Long get() = monthlyBudget.value
    override val alertsEnabled: Boolean get() = budgetAlerts.value

    override var lastAlertMonth: String?
        get() = prefs.getString(SettingsKeys.LAST_ALERT_MONTH, null)
        set(value) = prefs.edit { if (value == null) remove(SettingsKeys.LAST_ALERT_MONTH) else putString(SettingsKeys.LAST_ALERT_MONTH, value) }

    override var lastAlertLevel: Int
        get() = prefs.getInt(SettingsKeys.LAST_ALERT_LEVEL, 0)
        set(value) = prefs.edit { if (value == 0) remove(SettingsKeys.LAST_ALERT_LEVEL) else putInt(SettingsKeys.LAST_ALERT_LEVEL, value) }

    private inline fun <reified E : Enum<E>> readEnum(key: String, default: E): E =
        enumValues<E>().firstOrNull { it.name == prefs.getString(key, null) } ?: default

    private fun <E : Enum<E>> saveEnum(key: String, value: E, flow: MutableStateFlow<E>) {
        prefs.edit { putString(key, value.name) }
        flow.value = value
    }
}
