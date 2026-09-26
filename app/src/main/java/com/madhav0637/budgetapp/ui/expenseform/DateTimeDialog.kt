package com.madhav0637.budgetapp.ui.expenseform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.madhav0637.budgetapp.ui.components.pressable
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val headlineFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

/** Picks the day and time of an expense. Any date is allowed, as on iOS. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeDialog(initial: Instant, onDismiss: () -> Unit, onConfirm: (Instant) -> Unit) {
    val colors = Koku.colors
    val zone = ZoneId.systemDefault()
    val start = initial.atZone(zone).toLocalDateTime()
    // The date picker works in UTC midnights, so convert to and from a plain calendar date.
    val dateState = rememberDatePickerState(initialSelectedDateMillis = start.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    var time by remember { mutableStateOf(start.toLocalTime().withSecond(0).withNano(0)) }
    var pickingTime by remember { mutableStateOf(false) }
    val pickedDay = dateState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: start.toLocalDate()
    val pickerColors = DatePickerDefaults.colors(containerColor = colors.surface)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalDateTime.of(pickedDay, time).atZone(zone).toInstant()) }) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        colors = pickerColors,
    ) {
        DatePicker(
            state = dateState,
            showModeToggle = false,
            colors = pickerColors,
            title = null,
            headline = {
                Text(
                    headlineFormat.format(pickedDay),
                    style = KokuType.title3,
                    color = colors.ink,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
                )
            },
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) {
            Text("Time", style = KokuType.body.copy(fontWeight = FontWeight.Medium), color = colors.ink)
            Spacer(Modifier.weight(1f))
            Text(
                timeFormat.format(time),
                style = KokuType.subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = colors.ink,
                modifier = Modifier
                    .pressable(onClickLabel = "Change time") { pickingTime = true }
                    .background(colors.surface2, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }

    if (pickingTime) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            containerColor = colors.surface,
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(state.hour, state.minute)
                    pickingTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
}
