package com.madhav0637.budgetapp.ui.quickentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madhav0637.budgetapp.BudgetApplication
import com.madhav0637.budgetapp.data.Category
import com.madhav0637.budgetapp.data.CategoryDao
import com.madhav0637.budgetapp.domain.BudgetAlerts
import com.madhav0637.budgetapp.domain.BudgetService
import com.madhav0637.budgetapp.domain.ExpenseService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuickEntryViewModel(
    categoryDao: CategoryDao,
    private val expenseService: ExpenseService,
    private val budgetService: BudgetService,
) : ViewModel() {
    /** Most-used first, the same order as the Back Tap list on iOS. */
    val categories: StateFlow<List<Category>> = categoryDao.observeByUsage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Saves the expense, then calls [onSaved] with the budget alert it triggered (if any),
     * or [onError] with a readable message.
     */
    fun save(merchant: String, amount: Long, category: Category, onSaved: (BudgetAlerts.Alert?) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { budgetService.alertAround { expenseService.add(merchant, amount, category.id) } }
                .onSuccess(onSaved)
                .onFailure { onError(it.message ?: "Couldn't save the expense.") }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as BudgetApplication
                QuickEntryViewModel(app.database.categoryDao(), app.expenseService, app.budgetService)
            }
        }
    }
}
