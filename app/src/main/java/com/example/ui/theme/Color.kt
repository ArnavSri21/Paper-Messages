package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// PaperMessages Design Tokens (Part 2.2)
val PaperBgMain = Color(0xFF080A10)
val PaperBgSecondary = Color(0xFF0D1019)
val PaperSurfaceElevated = Color(0xFF141925)

// Liquid Glass Tokens
val PaperGlassSurfaceL1 = Color(0x0BFFFFFF) // ~4.3%
val PaperGlassSurfaceL2 = Color(0x0FFFFFFF) // ~5.8%
val PaperGlassSurfaceL3 = Color(0x18FFFFFF) // ~9.4%

val PaperGlassBorderL1 = Color(0x14FFFFFF)  // ~8%
val PaperGlassBorderL2 = Color(0x1FFFFFFF)  // ~12%
val PaperGlassBorderL3 = Color(0x2EFFFFFF)  // ~18%

// Brand & Accent Tokens
val PaperAccentPrimary = Color(0xFF8B7CFF)
val PaperAccentBlue = Color(0xFF547BFF)
val PaperAccentSecondary = Color(0xFF54D6E8)
val PaperAccentViolet = Color(0xFF9B75FF)
val PaperHighlight = Color(0xFFB7A9FF)

// Text Hierarchy
val PaperTextPrimary = Color(0xFFF5F6FC)
val PaperTextSecondary = Color(0xFFA0A6B8)
val PaperTextMuted = Color(0xFF697184)

// Semantic Status Colors
val PaperSuccess = Color(0xFF43D9A3)
val PaperWarning = Color(0xFFFFBE69)
val PaperError = Color(0xFFFF667D)

// Light Mode Fallback Tokens (for Appearance Settings toggle)
val PaperLightBgMain = Color(0xFFF4F6FB)
val PaperLightBgSecondary = Color(0xFFE9ECF5)
val PaperLightSurfaceElevated = Color(0xFFFFFFFF)
val PaperLightTextPrimary = Color(0xFF0E1320)
val PaperLightTextSecondary = Color(0xFF4A5368)

// Brand Gradients
val PrimaryBrandGradient = Brush.linearGradient(
    colors = listOf(PaperAccentPrimary, PaperAccentBlue)
)

val SecondaryAmbientGradient = Brush.linearGradient(
    colors = listOf(PaperAccentSecondary, PaperAccentViolet)
)

val OutgoingBubbleGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF5E52C8), Color(0xFF3E59C2))
)

fun parseHexColor(hex: String, fallback: Color = PaperAccentPrimary): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        when (clean.length) {
            6 -> Color(("FF$clean").toLong(16))
            8 -> Color(clean.toLong(16))
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
