package com.madhav0637.budgetapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madhav0637.budgetapp.data.Category
import com.madhav0637.budgetapp.domain.CategoryError
import com.madhav0637.budgetapp.domain.CategoryRules
import com.madhav0637.budgetapp.domain.counted
import com.madhav0637.budgetapp.ui.components.CircleIconButton
import com.madhav0637.budgetapp.ui.components.EmojiTile
import com.madhav0637.budgetapp.ui.components.HairlineDivider
import com.madhav0637.budgetapp.ui.components.KokuButton
import com.madhav0637.budgetapp.ui.components.KokuCard
import com.madhav0637.budgetapp.ui.components.KokuTextField
import com.madhav0637.budgetapp.ui.components.LocalToasts
import com.madhav0637.budgetapp.ui.components.PageMargin
import com.madhav0637.budgetapp.ui.components.SubScreen
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType

private fun expenses(count: Int) = counted(count, "expense")

/** All categories, most-used first, with how many expenses each has. */
@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel, onBack: () -> Unit, onOpen: (Category) -> Unit) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val colors = Koku.colors
    var adding by remember { mutableStateOf(false) }

    SubScreen(
        title = "Categories",
        onBack = onBack,
        actions = { CircleIconButton(Icons.Rounded.Add, "Add category", onClick = { adding = true }) },
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = PageMargin).padding(top = 8.dp, bottom = 96.dp)) {
            KokuCard(padding = 0.dp) {
                categories.forEachIndexed { index, item ->
                    if (index > 0) HairlineDivider(startIndent = 66.dp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(item.category) }.padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        EmojiTile(item.category.emoji, size = 38.dp)
                        Text(
                            item.category.name,
                            style = KokuType.body.copy(fontWeight = FontWeight.Medium),
                            color = colors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Text(expenses(item.expenseCount), style = KokuType.subheadline, color = colors.ink2)
                        Chevron()
                    }
                }
            }
        }
    }

    if (adding) {
        AddCategoryDialog(
            onAdd = { name, emoji, onError -> viewModel.add(name, emoji, onDone = { adding = false }, onError = onError) },
            onDismiss = { adding = false },
        )
    }
}

@Composable
private fun AddCategoryDialog(
    onAdd: (name: String, emoji: String, onError: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Koku.colors
    var name by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val canSave = name.isNotBlank() && CategoryRules.isValidEmoji(emoji)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = { Text("New category", style = KokuType.title3, color = colors.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryFields(name, { name = it; error = null }, emoji, { emoji = it; error = null })
                Text(
                    error ?: "Tap Emoji, then use the 🙂 key on the keyboard to pick one.",
                    style = KokuType.footnote,
                    color = if (error != null) colors.warning else colors.ink2,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(name, emoji) { error = it } }, enabled = canSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Name and emoji fields, shared by adding and editing. The emoji box always keeps exactly one character. */
@Composable
private fun CategoryFields(name: String, onName: (String) -> Unit, emoji: String, onEmoji: (String) -> Unit) {
    val colors = Koku.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Name", style = KokuType.footnote.copy(fontWeight = FontWeight.Medium), color = colors.ink2)
        KokuTextField(
            value = name,
            onValueChange = onName,
            placeholder = "e.g. Rent",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Emoji", style = KokuType.footnote.copy(fontWeight = FontWeight.Medium), color = colors.ink2)
        KokuTextField(value = emoji, onValueChange = { onEmoji(CategoryRules.lastCharacter(it)) }, placeholder = "🙂")
    }
}

/** Edit a category's name and emoji, move all its expenses elsewhere, or delete it once it's empty. */
@Composable
fun CategoryDetailScreen(categoryId: String, viewModel: CategoriesViewModel, onBack: () -> Unit) {
    val all by viewModel.categories.collectAsStateWithLifecycle()
    val item = all.firstOrNull { it.category.id == categoryId }
    val colors = Koku.colors
    val toasts = LocalToasts.current
    val showError: (String) -> Unit = { message -> toasts.show(message, icon = Icons.Rounded.ErrorOutline) }

    SubScreen(title = item?.category?.name ?: "", onBack = onBack) {
        if (item == null) return@SubScreen // just deleted; onBack has already been called
        val category = item.category
        val others = all.filter { it.category.id != categoryId }.map { it.category }

        var name by rememberSaveable(category.id) { mutableStateOf(category.name) }
        var emoji by rememberSaveable(category.id) { mutableStateOf(category.emoji) }
        var moveTarget by remember { mutableStateOf<Category?>(null) }
        var moveMenuOpen by remember { mutableStateOf(false) }
        var confirmingDelete by remember { mutableStateOf(false) }

        val changed = name.trim() != category.name || emoji != category.emoji
        val canSave = changed && name.isNotBlank() && CategoryRules.isValidEmoji(emoji)
        val deleteBlockedReason = when {
            item.expenseCount > 0 -> CategoryError.InUse(item.expenseCount).message
            others.isEmpty() -> CategoryError.LastCategory.message
            else -> null
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = PageMargin).padding(top = 8.dp, bottom = 96.dp),
        ) {
            KokuCard(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CategoryFields(name, { name = it }, emoji, { emoji = it })
            }
            KokuButton(
                "Save changes",
                onClick = { viewModel.update(category, name, emoji, onDone = {}, onError = showError) },
                enabled = canSave,
            )

            if (item.expenseCount > 0) {
                Box {
                    KokuButton(
                        "Move all ${expenses(item.expenseCount)} to…",
                        onClick = { moveMenuOpen = true },
                        enabled = others.isNotEmpty(),
                        primary = false,
                    )
                    DropdownMenu(
                        expanded = moveMenuOpen,
                        onDismissRequest = { moveMenuOpen = false },
                        containerColor = colors.surface,
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        others.forEach { target ->
                            DropdownMenuItem(
                                text = { Text("${target.emoji}  ${target.name}", style = KokuType.body, color = colors.ink) },
                                onClick = {
                                    moveTarget = target
                                    moveMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }

            KokuButton(
                "Delete category",
                onClick = { confirmingDelete = true },
                enabled = deleteBlockedReason == null,
                primary = false,
                contentColor = colors.warning,
            )
            deleteBlockedReason?.let {
                Text(it, style = KokuType.footnote, color = colors.ink2, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }

        moveTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { moveTarget = null },
                containerColor = colors.surface,
                title = { Text("Move ${expenses(item.expenseCount)}?") },
                text = { Text("From ${category.emoji} ${category.name} to ${target.emoji} ${target.name}.") },
                confirmButton = {
                    TextButton(onClick = {
                        moveTarget = null
                        viewModel.moveAll(category, target, onDone = {}, onError = showError)
                    }) { Text("Move") }
                },
                dismissButton = { TextButton(onClick = { moveTarget = null }) { Text("Cancel") } },
            )
        }

        if (confirmingDelete) {
            AlertDialog(
                onDismissRequest = { confirmingDelete = false },
                containerColor = colors.surface,
                title = { Text("Delete ${category.emoji} ${category.name}?") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmingDelete = false
                        viewModel.delete(category, onDone = onBack, onError = showError)
                    }) { Text("Delete", color = colors.warning) }
                },
                dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
            )
        }
    }
}
