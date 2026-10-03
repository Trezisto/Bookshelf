package com.prijilevschi.library.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun formatDate(iso: String?): String = iso?.let {
    runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }.getOrDefault(it)
} ?: ""

/** Date part of an ISO instant (2026-10-03T12:00:00Z) in the phone's time zone. */
fun formatInstant(iso: String?): String = iso?.let {
    runCatching {
        Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalDate()
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }.getOrDefault(it)
} ?: ""

/** Material date picker that works with ISO date strings (yyyy-MM-dd). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsoDatePickerDialog(initial: String?, onDismiss: () -> Unit, onPicked: (String) -> Unit) {
    val initialMillis = (initial?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now())
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onPicked(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}
