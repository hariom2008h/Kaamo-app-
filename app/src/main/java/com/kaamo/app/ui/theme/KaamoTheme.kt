package com.kaamo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object KaamoColors {
    val Forest = Color(0xFF064E3B)
    val Teal = Color(0xFF0D9488)
    val Ink = Color(0xFF111827)     // headings
    val Muted = Color(0xFF6B7280)   // secondary text
    val Border = Color(0xFFE5E7EB)  // card / input borders
    val White = Color(0xFFFFFFFF)
    val Error = Color(0xFFDC2626)
    val Soft = Color(0xFFF9FAFB)    // info banners, segmented controls
    val Tint = Color(0xFFF3FAF9)    // success / receipt background
}

/** Brand gradient. Use ONLY for primary actions, selected cards and key interactive highlights. */
val KaamoGradient: Brush = Brush.horizontalGradient(listOf(KaamoColors.Forest, KaamoColors.Teal))

private val KaamoScheme = lightColorScheme(
    primary = KaamoColors.Forest,
    onPrimary = KaamoColors.White,
    secondary = KaamoColors.Teal,
    background = KaamoColors.White,
    onBackground = KaamoColors.Ink,
    surface = KaamoColors.White,
    onSurface = KaamoColors.Ink,
    onSurfaceVariant = KaamoColors.Muted,
    outline = KaamoColors.Border,
    outlineVariant = KaamoColors.Border,
    error = KaamoColors.Error,
    surfaceTint = Color.Transparent,
)

// Swap for Inter / Google Sans by adding fonts under res/font and building a FontFamily.
private val Sans = FontFamily.SansSerif

private val KaamoTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 30.sp,
        lineHeight = 36.sp, letterSpacing = (-0.6).sp, color = KaamoColors.Ink,
    ),
    titleLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
        lineHeight = 28.sp, letterSpacing = (-0.3).sp, color = KaamoColors.Ink,
    ),
    titleMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
        lineHeight = 22.sp, color = KaamoColors.Ink,
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp,
        lineHeight = 24.sp, color = KaamoColors.Muted,
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp,
        lineHeight = 20.sp, color = KaamoColors.Muted,
    ),
    labelLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp,
    ),
)

@Composable
fun KaamoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KaamoScheme, typography = KaamoTypography, content = content)
}
