package com.madhav0637.budgetapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madhav0637.budgetapp.BudgetApplication
import com.madhav0637.budgetapp.data.AppSettings
import com.madhav0637.budgetapp.data.Appearance
import com.madhav0637.budgetapp.data.CategoryDao
import com.madhav0637.budgetapp.data.Highlight
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** The Settings tab's own choices: theme, highlight, the budget alerts switch, and how many categories there are. */
class SettingsViewModel(categoryDao: CategoryDao, private val settings: AppSettings) : ViewModel() {
    val appearance: StateFlow<Appearance> = settings.appearance
    val highlight: StateFlow<Highlight> = settings.highlight
    val monthlyBudget: StateFlow<Long> = settings.monthlyBudget
    val budgetAlerts: StateFlow<Boolean> = settings.budgetAlerts
    val categoryCount: StateFlow<Int> = categoryDao.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setAppearance(value: Appearance) = settings.setAppearance(value)
    fun setHighlight(value: Highlight) = settings.setHighlight(value)
    fun setBudgetAlerts(on: Boolean) = settings.setBudgetAlerts(on)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as BudgetApplication
                SettingsViewModel(app.database.categoryDao(), app.settings)
            }
        }
    }
}
