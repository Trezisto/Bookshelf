package com.prijilevschi.library.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prijilevschi.library.R
import com.prijilevschi.library.data.LlmPreset
import com.prijilevschi.library.ui.appViewModel
import com.prijilevschi.library.ui.components.AppIcon
import com.prijilevschi.library.ui.components.BackButton
import com.prijilevschi.library.ui.components.Loading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = appViewModel { SettingsViewModel(it.settingsRepository, it.libraryRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showKey by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); vm.messageShown() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { BackButton(onBack) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.loading) {
            Loading(Modifier.padding(padding))
            return@Scaffold
        }
        val s = state.settings
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Library server", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = s.serverUrl,
                onValueChange = { v -> vm.update { it.copy(serverUrl = v) } },
                label = { Text("Server URL") },
                supportingText = { Text("Emulator: http://10.0.2.2:8080/ · Phone: http://<computer's IP>:8080/") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("AI book summaries", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Text(
                "Any OpenAI-compatible provider works. Get a free API key from one of these and paste it below. " +
                    "Without a key you write descriptions yourself.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LlmPreset.entries.forEach { preset ->
                    AssistChip(onClick = { vm.applyPreset(preset) }, label = { Text(preset.label) })
                }
            }
            OutlinedTextField(
                value = s.llmApiKey,
                onValueChange = { v -> vm.update { it.copy(llmApiKey = v) } },
                label = { Text("API key") },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) { AppIcon(R.drawable.ic_visibility, if (showKey) "Hide key" else "Show key") }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.llmBaseUrl,
                onValueChange = { v -> vm.update { it.copy(llmBaseUrl = v) } },
                label = { Text("Base URL") },
                placeholder = { Text(LlmPreset.GROQ.baseUrl) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.llmModel,
                onValueChange = { v -> vm.update { it.copy(llmModel = v) } },
                label = { Text("Model") },
                placeholder = { Text(LlmPreset.GROQ.model) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LlmPreset.entries.forEach { preset ->
                    TextButton(onClick = { runCatching { uriHandler.openUri(preset.keyUrl) } }) { Text("${preset.label} key") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = vm::save) { Text("Save") }
                OutlinedButton(onClick = vm::test, enabled = !state.testing) {
                    Text(if (state.testing) "Testing…" else "Save & test")
                }
            }
            Text(
                "The key is stored on this phone and sent to your library server only when you save a book or generate a summary.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    state.testResult?.let { result ->
        AlertDialog(
            onDismissRequest = vm::dismissResult,
            title = { Text("Connection test") },
            text = { Text(result) },
            confirmButton = { TextButton(onClick = vm::dismissResult) { Text("OK") } },
        )
    }
}
