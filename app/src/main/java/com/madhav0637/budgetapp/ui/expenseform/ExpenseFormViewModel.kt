package com.madhav0637.budgetapp.ui.expenseform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madhav0637.budgetapp.BudgetApplication
import com.madhav0637.budgetapp.data.AppSettings
import com.madhav0637.budgetapp.data.Category
import com.madhav0637.budgetapp.data.CategoryDao
import com.madhav0637.budgetapp.data.Expense
import com.madhav0637.budgetapp.data.ExpenseDao
import com.madhav0637.budgetapp.data.ExpenseWithCategory
import com.madhav0637.budgetapp.domain.BudgetAlerts
import com.madhav0637.budgetapp.domain.BudgetService
import com.madhav0637.budgetapp.domain.ExpenseService
import com.madhav0637.budgetapp.domain.MerchantSuggestions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/** Adding a new expense, or editing one. */
sealed interface ExpenseFormMode {
    /** Identifies the form, so its typed-in state is kept per expense. */
    val key: String

    data object Add : ExpenseFormMode {
        override val key = "add"
    }

    data class Edit(val item: ExpenseWithCategory) : ExpenseFormMode {
        override val key: String get() = item.expense.id
    }
}

data class ExpenseFormData(
    /** Newest first. */
    val expenses: List<ExpenseWithCategory> = emptyList(),
    val suggestions: MerchantSuggestions = MerchantSuggestions(emptyList()),
    /** Most-used first. */
    val categories: List<Category> = emptyList(),
    /** Whole rupees; 0 means no budget. */
    val monthlyBudget: Long = 0,
)

/** What the Add / Edit sheet needs. Every change goes through the services, then checks the budget. */
class ExpenseFormViewModel(
    expenseDao: ExpenseDao,
    categoryDao: CategoryDao,
    settings: AppSettings,
    private val expenseService: ExpenseService,
    private val budgetService: BudgetService,
) : ViewModel() {
    val data: StateFlow<ExpenseFormData> =
        combine(expenseDao.observeAllWithCategory(), categoryDao.observeByUsage(), settings.monthlyBudget) { expenses, categories, budget ->
            ExpenseFormData(expenses, MerchantSuggestions(expenses), categories, budget)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseFormData())

    /** Saves, then calls [onSaved] with the budget alert the save triggered, if any. */
    fun save(
        mode: ExpenseFormMode,
        merchant: String,
        amount: Long,
        categoryId: String,
        date: Instant,
        note: String?,
        onSaved: (BudgetAlerts.Alert?) -> Unit,
        onError: (String) -> Unit,
    ) = viewModelScope.launch {
        runCatching {
            budgetService.alertAround {
                when (mode) {
                    ExpenseFormMode.Add -> expenseService.add(merchant, amount, categoryId, date, note)
                    is ExpenseFormMode.Edit -> expenseService.update(mode.item.expense, merchant, amount, categoryId, date, note)
                }
            }
        }
            .onSuccess(onSaved)
            .onFailure { onError(it.message ?: "Couldn't save the expense.") }
    }

    fun delete(expense: Expense, onDeleted: () -> Unit, onError: (String) -> Unit) = viewModelScope.launch {
        runCatching { expenseService.delete(expense) }
            .onSuccess { onDeleted() }
            .onFailure { onError(it.message ?: "Couldn't delete the expense.") }
    }

    /** Puts a deleted expense back exactly as it was, for Undo. */
    fun restore(expense: Expense, onError: (String) -> Unit) = viewModelScope.launch {
        runCatching { expenseService.restore(expense) }.onFailure { onError("Couldn't undo: ${it.message}") }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as BudgetApplication
                ExpenseFormViewModel(
                    app.database.expenseDao(),
                    app.database.categoryDao(),
                    app.settings,
                    app.expenseService,
                    app.budgetService,
                )
            }
        }
    }
}
