package com.prijilevschi.library.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prijilevschi.library.data.ApiException
import com.prijilevschi.library.data.AppSettings
import com.prijilevschi.library.data.LibraryRepository
import com.prijilevschi.library.data.LlmPreset
import com.prijilevschi.library.data.SettingsRepository
import com.prijilevschi.library.data.SummaryRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val loading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val testing: Boolean = false,
    val testResult: String? = null,
    val message: String? = null,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val repository: LibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(loading = false, settings = settingsRepository.current()) }
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) = _state.update { it.copy(settings = transform(it.settings)) }

    fun applyPreset(preset: LlmPreset) = update { it.copy(llmBaseUrl = preset.baseUrl, llmModel = preset.model) }

    fun save() {
        viewModelScope.launch {
            settingsRepository.save(_state.value.settings)
            _state.update { it.copy(settings = settingsRepository.current(), message = "Settings saved") }
        }
    }

    /** Saves, then checks the server connection and (if a key is set) asks for a well-known book's summary. */
    fun test() {
        _state.update { it.copy(testing = true, testResult = null) }
        viewModelScope.launch {
            settingsRepository.save(_state.value.settings)
            val settings = settingsRepository.current()
            val result = try {
                val shelves = repository.shelves()
                val server = "✅ Connected to the library server (${shelves.size} shelves)."
                if (!settings.hasLlmKey) {
                    "$server\n\nNo LLM API key set, so descriptions have to be written by hand."
                } else {
                    val summary = repository.summary(SummaryRequest("The Hobbit", "J. R. R. Tolkien"), settings)
                    "$server\n\n✅ The LLM answered:\n$summary"
                }
            } catch (e: ApiException) {
                "❌ ${e.message}"
            }
            _state.update { it.copy(testing = false, testResult = result, settings = settings) }
        }
    }

    fun dismissResult() = _state.update { it.copy(testResult = null) }

    fun messageShown() = _state.update { it.copy(message = null) }
}
