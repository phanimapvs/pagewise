package com.pagewise.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pagewise.app.data.ThemeMode

val Ember = Color(0xFFE0703A)
val Ink = Color(0xFF2E3A59)
val Sage = Color(0xFF6BA38E)
val Gold = Color(0xFFE5B53B)
val Silver = Color(0xFFA8B0BD)
val Bronze = Color(0xFFC08457)

private val Light = lightColorScheme(
    primary = Ember,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC9),
    onPrimaryContainer = Color(0xFF4A1A00),
    secondary = Ink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE2F5),
    onSecondaryContainer = Color(0xFF16203A),
    tertiary = Sage,
    tertiaryContainer = Color(0xFFD3EDE3),
    background = Color(0xFFFBF6EE),
    surface = Color(0xFFFBF6EE),
    surfaceContainer = Color(0xFFF3ECE1),
    surfaceContainerHigh = Color(0xFFEDE5D8),
    surfaceContainerLow = Color(0xFFF7F1E8),
    surfaceContainerHighest = Color(0xFFE6DDCF),
    surfaceContainerLowest = Color(0xFFFFFCF7),
    surfaceVariant = Color(0xFFEDE5D8),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFA676),
    onPrimary = Color(0xFF4A1A00),
    primaryContainer = Color(0xFF7A3510),
    onPrimaryContainer = Color(0xFFFFDCC9),
    secondary = Color(0xFFB9C5EA),
    secondaryContainer = Color(0xFF3A4668),
    tertiary = Color(0xFF8FCFB6),
    tertiaryContainer = Color(0xFF285444),
    background = Color(0xFF15161C),
    surface = Color(0xFF15161C),
    surfaceContainer = Color(0xFF1F2029),
    surfaceContainerHigh = Color(0xFF292A34),
    surfaceContainerLow = Color(0xFF1A1B22),
    surfaceContainerHighest = Color(0xFF30313C),
    surfaceContainerLowest = Color(0xFF101116),
    surfaceVariant = Color(0xFF292A34),
)

/** Low-glare evening palette: warm lamplight on deep brown-black, no pure white anywhere. */
private val Night = darkColorScheme(
    primary = Color(0xFFE9A86A),
    onPrimary = Color(0xFF3A1E06),
    primaryContainer = Color(0xFF5A3517),
    onPrimaryContainer = Color(0xFFF5D9BC),
    secondary = Color(0xFFC9B79C),
    onSecondary = Color(0xFF2E2416),
    secondaryContainer = Color(0xFF3E3324),
    onSecondaryContainer = Color(0xFFEADBC4),
    tertiary = Color(0xFFA8C3A0),
    tertiaryContainer = Color(0xFF34432F),
    onTertiaryContainer = Color(0xFFD6E6CF),
    background = Color(0xFF14100C),
    onBackground = Color(0xFFE6D8C4),
    surface = Color(0xFF14100C),
    onSurface = Color(0xFFE6D8C4),
    onSurfaceVariant = Color(0xFFB5A58E),
    surfaceContainer = Color(0xFF1F1913),
    surfaceContainerHigh = Color(0xFF2A221A),
    surfaceContainerLow = Color(0xFF19140F),
    surfaceContainerHighest = Color(0xFF332A20),
    surfaceContainerLowest = Color(0xFF0F0C09),
    surfaceVariant = Color(0xFF2A221A),
    outline = Color(0xFF7D6E5A),
    outlineVariant = Color(0xFF4A3F33),
)

private val Serif = FontFamily.Serif

private val AppTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
        headlineMedium = t.headlineMedium.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontFamily = Serif, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontFamily = Serif, fontWeight = FontWeight.SemiBold),
    )
}

val StatNumber = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp)

@Composable
fun PagewiseTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val scheme = when (mode) {
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) Dark else Light
        ThemeMode.LIGHT -> Light
        ThemeMode.DARK -> Dark
        ThemeMode.NIGHT -> Night
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
