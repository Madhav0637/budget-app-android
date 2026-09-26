package com.madhav0637.budgetapp.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madhav0637.budgetapp.BudgetApplication
import com.madhav0637.budgetapp.data.Category
import com.madhav0637.budgetapp.data.CategoryDao
import com.madhav0637.budgetapp.data.Expense
import com.madhav0637.budgetapp.data.ExpenseDao
import com.madhav0637.budgetapp.domain.ExpenseService
import com.madhav0637.budgetapp.domain.HistoryFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivityUiState(
    val groups: List<HistoryFilter.DayGroup> = emptyList(),
    /** Most-used first, for the filter chips. */
    val categories: List<Category> = emptyList(),
    val filter: HistoryFilter = HistoryFilter(),
    val hasAnyExpenses: Boolean = false,
    val isLoading: Boolean = true,
)

class ActivityViewModel(
    expenseDao: ExpenseDao,
    categoryDao: CategoryDao,
    private val expenseService: ExpenseService,
) : ViewModel() {
    private val filter = MutableStateFlow(HistoryFilter())

    val state: StateFlow<ActivityUiState> =
        combine(expenseDao.observeAllWithCategory(), categoryDao.observeByUsage(), filter) { expenses, categories, filter ->
            ActivityUiState(
                groups = HistoryFilter.groupByDay(filter.apply(expenses)),
                categories = categories,
                filter = filter,
                hasAnyExpenses = expenses.isNotEmpty(),
                isLoading = false,
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    fun setSearch(text: String) = filter.update { it.copy(searchText = text) }

    fun setCategory(categoryId: String?) = filter.update { it.copy(categoryId = categoryId) }

    fun delete(expense: Expense, onError: (String) -> Unit) = viewModelScope.launch {
        runCatching { expenseService.delete(expense) }.onFailure { onError("Couldn't delete: ${it.message}") }
    }

    /** Puts the deleted expense back exactly as it was, same id and note. */
    fun restore(expense: Expense, onError: (String) -> Unit) = viewModelScope.launch {
        runCatching { expenseService.restore(expense) }.onFailure { onError("Couldn't undo: ${it.message}") }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as BudgetApplication
                ActivityViewModel(app.database.expenseDao(), app.database.categoryDao(), app.expenseService)
            }
        }
    }
}
