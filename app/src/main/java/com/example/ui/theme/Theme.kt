package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class PaperThemeConfig(
    val darkTheme: Boolean = true,
    val accentHex: String = "#8B7CFF",
    val reducedMotion: Boolean = false
)

val LocalPaperThemeConfig = staticCompositionLocalOf { PaperThemeConfig() }

val PaperShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),      // Buttons: 14-18dp
    medium = RoundedCornerShape(20.dp),     // Standard card radius: 18-22dp
    large = RoundedCornerShape(28.dp),      // Modal & Floating nav radius: 26-30dp
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun PaperMessagesTheme(
    darkTheme: Boolean = true,
    accentHex: String = "#8B7CFF",
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val dynamicPrimary = parseHexColor(accentHex, PaperAccentPrimary)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = dynamicPrimary,
            onPrimary = Color.White,
            primaryContainer = PaperSurfaceElevated,
            onPrimaryContainer = PaperHighlight,
            secondary = PaperAccentSecondary,
            onSecondary = PaperBgMain,
            secondaryContainer = Color(0xFF172636),
            onSecondaryContainer = PaperAccentSecondary,
            tertiary = PaperHighlight,
            background = PaperBgMain,
            onBackground = PaperTextPrimary,
            surface = PaperBgSecondary,
            onSurface = PaperTextPrimary,
            surfaceVariant = PaperSurfaceElevated,
            onSurfaceVariant = PaperTextSecondary,
            outline = PaperGlassBorderL2,
            error = PaperError,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = dynamicPrimary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE6E2FF),
            onPrimaryContainer = Color(0xFF281B68),
            secondary = Color(0xFF007B8A),
            onSecondary = Color.White,
            background = PaperLightBgMain,
            onBackground = PaperLightTextPrimary,
            surface = PaperLightSurfaceElevated,
            onSurface = PaperLightTextPrimary,
            surfaceVariant = PaperLightBgSecondary,
            onSurfaceVariant = PaperLightTextSecondary,
            outline = Color(0x220E1320),
            error = PaperError,
            onError = Color.White
        )
    }

    val config = PaperThemeConfig(
        darkTheme = darkTheme,
        accentHex = accentHex,
        reducedMotion = reducedMotion
    )

    CompositionLocalProvider(LocalPaperThemeConfig provides config) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = PaperShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    PaperMessagesTheme(darkTheme = darkTheme, content = content)
}
