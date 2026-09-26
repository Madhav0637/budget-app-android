package com.madhav0637.budgetapp.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.madhav0637.budgetapp.BudgetApplication
import com.madhav0637.budgetapp.data.ExpenseDao
import com.madhav0637.budgetapp.data.ExpenseWithCategory
import com.madhav0637.budgetapp.domain.ExportFormat
import com.madhav0637.budgetapp.domain.counted
import com.madhav0637.budgetapp.export.ExportWriter
import com.madhav0637.budgetapp.ui.components.GroupHeader
import com.madhav0637.budgetapp.ui.components.HairlineDivider
import com.madhav0637.budgetapp.ui.components.IconTile
import com.madhav0637.budgetapp.ui.components.KokuCard
import com.madhav0637.budgetapp.ui.components.LocalToasts
import com.madhav0637.budgetapp.ui.components.PageMargin
import com.madhav0637.budgetapp.ui.components.SubScreen
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ExportViewModel(expenseDao: ExpenseDao) : ViewModel() {
    val expenses: StateFlow<List<ExpenseWithCategory>?> = expenseDao.observeAllWithCategory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer { ExportViewModel((this[APPLICATION_KEY] as BudgetApplication).database.expenseDao()) }
        }
    }
}

/** Choose how to export: a CSV spreadsheet or a PDF report. The file is written, then opened in the share sheet. */
@Composable
fun ExportScreen(onBack: () -> Unit, viewModel: ExportViewModel = viewModel(factory = ExportViewModel.Factory)) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toasts = LocalToasts.current
    val colors = Koku.colors
    val count = expenses?.size ?: 0

    fun export(format: ExportFormat) {
        val items = expenses ?: return
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { ExportWriter.write(format, items, exportDirectory(context)) } }
                .onSuccess { share(context, it, format) }
                .onFailure { toasts.show("Couldn't create the file: ${it.message}", icon = Icons.Rounded.ErrorOutline) }
        }
    }

    SubScreen(title = "Export", onBack = onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = PageMargin).padding(top = 8.dp, bottom = 96.dp)) {
            GroupHeader("Choose a format")
            KokuCard(padding = 0.dp) {
                ExportRow(
                    icon = Icons.Rounded.TableChart,
                    title = "Spreadsheet (CSV)",
                    detail = "Every expense as rows, with notes. Opens in Excel or Google Sheets.",
                    enabled = count > 0,
                    onClick = { export(ExportFormat.Csv) },
                )
                HairlineDivider(startIndent = 60.dp)
                ExportRow(
                    icon = Icons.Rounded.Description,
                    title = "Report (PDF)",
                    detail = "A printable summary: total, spending by category, and every expense.",
                    enabled = count > 0,
                    onClick = { export(ExportFormat.Pdf) },
                )
            }
            Text(
                if (count == 0) {
                    "Nothing to export yet."
                } else {
                    "Includes all ${counted(count, "expense")}. Your data lives only on this phone, so an export is also your backup."
                },
                style = KokuType.footnote,
                color = colors.ink2,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun ExportRow(icon: ImageVector, title: String, detail: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = Koku.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        IconTile(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = KokuType.body.copy(fontWeight = FontWeight.Medium), color = colors.ink)
            Text(detail, style = KokuType.footnote, color = colors.ink2)
        }
        Icon(Icons.Rounded.IosShare, contentDescription = null, tint = colors.ink3, modifier = Modifier.size(20.dp))
    }
}

/** Exports go in the app's cache folder, which [FileProvider] is allowed to share (see res/xml/file_paths.xml). */
private fun exportDirectory(context: Context) = File(context.cacheDir, "export")

private fun share(context: Context, file: File, format: ExportFormat) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Export ${file.name}"))
}
