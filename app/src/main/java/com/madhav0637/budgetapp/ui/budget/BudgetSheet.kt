package com.madhav0637.budgetapp.ui.budget

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.madhav0637.budgetapp.domain.inr
import com.madhav0637.budgetapp.notifications.BudgetNotifier
import com.madhav0637.budgetapp.ui.components.AmountText
import com.madhav0637.budgetapp.ui.components.Caret
import com.madhav0637.budgetapp.ui.components.Chip
import com.madhav0637.budgetapp.ui.components.Keypad
import com.madhav0637.budgetapp.ui.components.KokuButton
import com.madhav0637.budgetapp.ui.components.PageMargin
import com.madhav0637.budgetapp.ui.components.pressable
import com.madhav0637.budgetapp.ui.expenseform.SheetHandle
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType
import kotlinx.coroutines.launch

private val presets = listOf(5_000L, 10_000L, 15_000L, 20_000L, 25_000L, 30_000L, 50_000L)

/** Sets, changes or removes the monthly budget, on the same keypad as Add Expense. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSheet(onDismiss: () -> Unit, viewModel: BudgetViewModel = viewModel(factory = BudgetViewModel.Factory)) {
    val colors = Koku.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val budget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val alertsOn by viewModel.alertsOn.collectAsStateWithLifecycle()
    val lastMonth by viewModel.lastMonthSpent.collectAsStateWithLifecycle()
    var amountText by rememberSaveable { mutableStateOf(if (budget > 0) budget.toString() else "") }
    val amount = amountText.toLongOrNull()?.takeIf { it > 0 }
    // Android 13+ asks before an app can show notifications; the budget alerts need them.
    val askForNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 8.dp),
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
        containerColor = colors.surface,
        dragHandle = { SheetHandle() },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = PageMargin).height(44.dp),
            ) {
                val action = KokuType.body.copy(fontWeight = FontWeight.Medium)
                Text("Cancel", style = action, color = colors.ink2, modifier = Modifier.pressable(onClick = ::close).padding(vertical = 8.dp))
                Text("Monthly budget", style = KokuType.headline, color = colors.ink, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                // Invisible (but still taking its space, so the title stays centred) when there's no budget yet.
                Text(
                    "Remove",
                    style = action,
                    color = if (budget > 0) colors.warning else Color.Transparent,
                    modifier = Modifier
                        .pressable(enabled = budget > 0) {
                            viewModel.remove()
                            close()
                        }
                        .padding(vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(28.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = PageMargin),
            ) {
                AmountText(amount ?: 0, size = 64.sp, placeholder = amount == null, modifier = Modifier.weight(1f, fill = false))
                Caret(colors.highlightFill, height = 54.dp)
            }
            Text(
                if (lastMonth > 0) "Last month you spent ${lastMonth.inr()}" else "What you plan to spend in a month",
                style = KokuType.footnote.copy(fontWeight = FontWeight.Medium),
                color = colors.ink2,
                modifier = Modifier.padding(top = 10.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = PageMargin),
            ) {
                presets.forEach { preset ->
                    Chip(preset.inr(), selected = amount == preset, restingBackground = colors.surface2, onClick = { amountText = preset.toString() })
                }
            }

            Spacer(Modifier.height(28.dp))
            Keypad(amountText, onTextChange = { amountText = it }, modifier = Modifier.padding(horizontal = 12.dp))
            KokuButton(
                text = amount?.let { "Set budget · ${it.inr()}" } ?: "Enter an amount",
                enabled = amount != null,
                height = 56.dp,
                onClick = {
                    val value = amount ?: return@KokuButton
                    viewModel.save(value)
                    if (alertsOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && BudgetNotifier.needsPermission(context)) {
                        askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    close()
                },
                modifier = Modifier.padding(horizontal = PageMargin, vertical = 10.dp),
            )
        }
    }
}
