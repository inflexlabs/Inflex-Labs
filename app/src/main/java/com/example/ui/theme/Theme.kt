package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Sophisticated Dark Color Scheme
private val SophisticatedDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),     // Brand accent purple
    onPrimary = Color(0xFF381E72),   // Dark purple contrast
    secondary = Color(0xFFCCC2DC),   // Secondary grey-purple
    onSecondary = Color(0xFF332D41),
    background = Color(0xFF1C1B1F),  // Primary deep dark background
    onBackground = Color(0xFFE6E1E9),// High contrast text
    surface = Color(0xFF2B2930),     // Custom layered surface background
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF49454F), // Secondary node containers
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme for Sophisticated Dark aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SophisticatedDarkColorScheme,
        typography = Typography,
        content = content
    )
}
