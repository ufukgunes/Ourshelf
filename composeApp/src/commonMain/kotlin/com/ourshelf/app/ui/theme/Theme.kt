package com.ourshelf.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Sıcak, sinematik bir palet: kehribar vurgu + mürdüm ikincil renk.
private val Amber = Color(0xFFE5A93C)
private val Plum = Color(0xFF6E5BA8)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF2B1B00),
    primaryContainer = Color(0xFF5A3F00),
    onPrimaryContainer = Color(0xFFFFDEA6),
    secondary = Color(0xFFC9BEFF),
    onSecondary = Color(0xFF2E2260),
    secondaryContainer = Color(0xFF453A78),
    onSecondaryContainer = Color(0xFFE6DEFF),
    background = Color(0xFF121014),
    onBackground = Color(0xFFF4EDE4),
    surface = Color(0xFF121014),
    onSurface = Color(0xFFF4EDE4),
    surfaceVariant = Color(0xFF2A2630),
    onSurfaceVariant = Color(0xFFCBC3D0),
    surfaceContainerLowest = Color(0xFF0D0B0F),
    surfaceContainerLow = Color(0xFF1A171D),
    surfaceContainer = Color(0xFF1F1B22),
    surfaceContainerHigh = Color(0xFF29252D),
    surfaceContainerHighest = Color(0xFF343038),
    outline = Color(0xFF948E9A),
    outlineVariant = Color(0xFF49444F),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7C5800),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDEA6),
    onPrimaryContainer = Color(0xFF271900),
    secondary = Plum,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6DEFF),
    onSecondaryContainer = Color(0xFF21154F),
    background = Color(0xFFFBF7F2),
    onBackground = Color(0xFF1E1B18),
    surface = Color(0xFFFBF7F2),
    onSurface = Color(0xFF1E1B18),
    surfaceVariant = Color(0xFFECE4EC),
    onSurfaceVariant = Color(0xFF4B4550),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F1EC),
    surfaceContainer = Color(0xFFF0EBE6),
    surfaceContainerHigh = Color(0xFFEAE5E0),
    surfaceContainerHighest = Color(0xFFE4DFDA),
    outline = Color(0xFF7C7581),
    outlineVariant = Color(0xFFCDC4CF),
)

private val BaseTypography = Typography()

private val UnwindTypography = BaseTypography.copy(
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun OurshelfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = UnwindTypography,
        content = content,
    )
}
