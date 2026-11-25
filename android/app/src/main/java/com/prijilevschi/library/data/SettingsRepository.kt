package com.prijilevschi.library.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AppSettings(
    /** Backend URL; 10.0.2.2 is the host machine as seen from the Android emulator. */
    val serverUrl: String = DEFAULT_SERVER_URL,
    /** The server's LIBRARY_API_TOKEN, sent as "Authorization: Bearer" with every request to the server. */
    val serverToken: String = "",
    val llmApiKey: String = "",
    /** Blank = the server's default (Groq). */
    val llmBaseUrl: String = "",
    /** Blank = the server's default model. */
    val llmModel: String = "",
) {
    val hasLlmKey: Boolean get() = llmApiKey.isNotBlank()

    companion object {
        const val DEFAULT_SERVER_URL = "http://10.0.2.2:8080/"
    }
}

/** Well-known OpenAI-compatible providers with a free tier. */
enum class LlmPreset(val label: String, val baseUrl: String, val model: String, val keyUrl: String) {
    GROQ("Groq", "https://api.groq.com/openai/v1", "llama-3.1-8b-instant", "https://console.groq.com/keys"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", "meta-llama/llama-3.3-70b-instruct:free", "https://openrouter.ai/keys"),
    GEMINI("Gemini", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.5-flash", "https://aistudio.google.com/apikey"),
}

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val serverTokenKey = stringPreferencesKey("server_token")
    private val apiKeyKey = stringPreferencesKey("llm_api_key")
    private val baseUrlKey = stringPreferencesKey("llm_base_url")
    private val modelKey = stringPreferencesKey("llm_model")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            serverUrl = prefs[serverUrlKey] ?: AppSettings.DEFAULT_SERVER_URL,
            serverToken = prefs[serverTokenKey].orEmpty(),
            llmApiKey = prefs[apiKeyKey].orEmpty(),
            llmBaseUrl = prefs[baseUrlKey].orEmpty(),
            llmModel = prefs[modelKey].orEmpty(),
        )
    }

    /** The last settings read or saved, for code that cannot suspend (the HTTP interceptor). */
    @Volatile
    var latest: AppSettings? = null
        private set

    suspend fun current(): AppSettings = settings.first().also { latest = it }

    suspend fun save(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[serverUrlKey] = normalizeServerUrl(settings.serverUrl)
            prefs[serverTokenKey] = settings.serverToken.trim()
            prefs[apiKeyKey] = settings.llmApiKey.trim()
            prefs[baseUrlKey] = settings.llmBaseUrl.trim()
            prefs[modelKey] = settings.llmModel.trim()
        }
        current()
    }

    companion object {
        fun normalizeServerUrl(raw: String): String {
            var url = raw.trim().ifEmpty { AppSettings.DEFAULT_SERVER_URL }
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
            return if (url.endsWith("/")) url else "$url/"
        }
    }
}
