package com.madhav0637.budgetapp.ui.quickentry

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madhav0637.budgetapp.BudgetApplication
import com.madhav0637.budgetapp.notifications.BudgetNotifier
import com.madhav0637.budgetapp.ui.theme.KokuTheme

/**
 * The quick-entry pop-up. Its window is see-through, so it floats over whatever app was on screen.
 * Opened by the Quick Settings tile, the launcher shortcut, or the separate "Log Expense" launcher entry
 * (which brand gestures such as Samsung's side-button double press can be set to open).
 */
class QuickEntryActivity : ComponentActivity() {
    private val viewModel: QuickEntryViewModel by viewModels { QuickEntryViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as BudgetApplication).settings
        setContent {
            val appearance by settings.appearance.collectAsStateWithLifecycle()
            val highlight by settings.highlight.collectAsStateWithLifecycle()
            KokuTheme(appearance, highlight) {
                val categories by viewModel.categories.collectAsStateWithLifecycle()
                QuickEntryScreen(
                    categories = categories,
                    onSave = { merchant, amount, category ->
                        viewModel.save(
                            merchant, amount, category,
                            onSaved = { alert ->
                                // Silent, like iOS, unless this expense takes the month past 80% or 100% of the budget.
                                alert?.let { BudgetNotifier.post(applicationContext, it) }
                                finish()
                            },
                            onError = { Toast.makeText(this, it, Toast.LENGTH_LONG).show() },
                        )
                    },
                    onCancel = ::finish,
                )
            }
        }
    }
}
