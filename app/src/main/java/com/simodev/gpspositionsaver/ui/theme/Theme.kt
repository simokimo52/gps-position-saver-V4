package com.simodev.gpspositionsaver.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette ported from the Tailwind tokens used in the web edition.
val GpsBlue = Color(0xFF0D47A1)
val GpsBlueDark = Color(0xFF093577)
val GpsBlueLight = Color(0xFF5472D3)
val GpsBackground = Color(0xFFF8F9FF)
val GpsSurface = Color(0xFFFFFFFF)
val GpsOnSurface = Color(0xFF191C20)
val GpsMuted = Color(0xFF64748B)
val GpsSuccess = Color(0xFF2E7D32)
val GpsSuccessBg = Color(0xFFE8F5E9)
val GpsWarning = Color(0xFFF57C00)
val GpsWarningBg = Color(0xFFFFF3E0)
val GpsError = Color(0xFFC62828)
val GpsErrorBg = Color(0xFFFFEBEE)

private val LightColors = lightColorScheme(
    primary = GpsBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE5FF),
    onPrimaryContainer = GpsBlueDark,
    secondary = GpsBlueLight,
    onSecondary = Color.White,
    background = GpsBackground,
    onBackground = GpsOnSurface,
    surface = GpsSurface,
    onSurface = GpsOnSurface,
    surfaceVariant = Color(0xFFEEF1F8),
    onSurfaceVariant = GpsMuted,
    error = GpsError,
    onError = Color.White,
    outline = Color(0xFFCBD5E1),
)

private val GpsTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
)

@Composable
fun GpsSaverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // The design is intentionally light-first; keep a single scheme for a consistent look.
    MaterialTheme(
        colorScheme = LightColors,
        typography = GpsTypography,
        content = content,
    )
}
