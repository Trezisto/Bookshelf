package com.prijilevschi.library.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Wood {
    val Light = Color(0xFFB0835A)
    val Mid = Color(0xFF8B5E3C)
    val Dark = Color(0xFF5C3B22)
    val Back = Color(0xFF3E2716)
    val Highlight = Color(0xFFFFD166)
}

/** Spine colours, picked per book from a hash of its title. */
val SpineColors = listOf(
    Color(0xFF264653), Color(0xFF2A9D8F), Color(0xFFE9C46A), Color(0xFFF4A261), Color(0xFFE76F51),
    Color(0xFF6D597A), Color(0xFF355070), Color(0xFFB56576), Color(0xFF588157), Color(0xFF9C6644),
    Color(0xFF3D405B), Color(0xFF81B29A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A4E2D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF2E1500),
    secondary = Color(0xFF2A9D8F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEDE8),
    onSecondaryContainer = Color(0xFF00201C),
    background = Color(0xFFFBF6EE),
    surface = Color(0xFFFBF6EE),
    surfaceContainer = Color(0xFFF3EADF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB77C),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF693C12),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFF80D5C8),
    onSecondary = Color(0xFF003731),
    background = Color(0xFF1C1612),
    surface = Color(0xFF1C1612),
    surfaceContainer = Color(0xFF29211B),
)

@Composable
fun LibraryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
