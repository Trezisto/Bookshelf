package com.prijilevschi.library.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.window.PopupProperties

/** Text field with suggestions from [options] that contain the typed text. */
@Composable
fun SuggestField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    var focused by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    val suggestions = remember(value, options) {
        if (value.isBlank()) emptyList()
        else options.filter { it.contains(value.trim(), ignoreCase = true) && !it.equals(value.trim(), ignoreCase = true) }.take(6)
    }
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { dismissed = false; onValueChange(it) },
            label = { Text(label) },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        )
        DropdownMenu(
            expanded = focused && !dismissed && suggestions.isNotEmpty(),
            onDismissRequest = { dismissed = true },
            properties = PopupProperties(focusable = false),
        ) {
            suggestions.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { dismissed = true; onValueChange(option) })
            }
        }
    }
}
