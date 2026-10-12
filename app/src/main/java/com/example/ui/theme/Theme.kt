package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AcademicLightColorScheme = lightColorScheme(
    primary = AcademicNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = AcademicGold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = AcademicEmerald,
    onTertiary = Color.White,
    background = ScholarLightBg,
    onBackground = ScholarLightTextPrimary,
    surface = ScholarLightSurface,
    onSurface = ScholarLightTextPrimary,
    surfaceVariant = ScholarLightSurfaceVariant,
    onSurfaceVariant = ScholarLightTextSecondary,
    outline = ScholarLightOutline
)

private val AcademicDarkColorScheme = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = AcademicNavy,
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = AcademicGoldLight,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF0F172A),
    background = ScholarDarkBg,
    onBackground = ScholarDarkTextPrimary,
    surface = ScholarDarkSurface,
    onSurface = ScholarDarkTextPrimary,
    surfaceVariant = ScholarDarkSurfaceVariant,
    onSurfaceVariant = ScholarDarkTextSecondary,
    outline = ScholarDarkOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinctive academic theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AcademicDarkColorScheme
        else -> AcademicLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
