package com.pagewise.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pagewise.app.R
import com.pagewise.app.data.ThemeMode

val Gold = Color(0xFFE5B53B)
val Silver = Color(0xFFA8B0BD)
val Bronze = Color(0xFFC08457)

/** The hand-picked colors a palette is built from; everything else derives from these. */
private class Tones(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    /** background, then four surface steps from low to highest. */
    val bg: Color,
    val low: Color,
    val mid: Color,
    val high: Color,
    val highest: Color,
)

private fun Tones.light(): ColorScheme = lightColorScheme(
    primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = Color.White, secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary, tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
    background = bg, surface = bg, surfaceContainerLowest = Color.White.copy(alpha = 0.6f).compositeOver(bg),
    surfaceContainerLow = low, surfaceContainer = mid, surfaceContainerHigh = high, surfaceContainerHighest = highest, surfaceVariant = high,
)

private fun Tones.dark(): ColorScheme = darkColorScheme(
    primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary, tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
    background = bg, surface = bg, surfaceContainerLowest = Color.Black.copy(alpha = 0.3f).compositeOver(bg),
    surfaceContainerLow = low, surfaceContainer = mid, surfaceContainerHigh = high, surfaceContainerHighest = highest, surfaceVariant = high,
)

private fun Color.compositeOver(bg: Color) = Color(
    red = red * alpha + bg.red * (1 - alpha),
    green = green * alpha + bg.green * (1 - alpha),
    blue = blue * alpha + bg.blue * (1 - alpha),
)

private fun c(hex: Long) = Color(hex)

enum class Palette(val id: String, val label: String, val swatch: Color, private val lightTones: Tones, private val darkTones: Tones) {
    EMBER(
        "ember", "Ember", c(0xFFE0703A),
        Tones(c(0xFFE0703A), Color.White, c(0xFFFFDCC9), c(0xFF4A1A00), c(0xFF2E3A59), c(0xFFDCE2F5), c(0xFF16203A), c(0xFF6BA38E), c(0xFFD3EDE3), c(0xFF0F3326),
            c(0xFFFBF6EE), c(0xFFF7F1E8), c(0xFFF3ECE1), c(0xFFEDE5D8), c(0xFFE6DDCF)),
        Tones(c(0xFFFFA676), c(0xFF4A1A00), c(0xFF7A3510), c(0xFFFFDCC9), c(0xFFB9C5EA), c(0xFF3A4668), c(0xFFDCE2F5), c(0xFF8FCFB6), c(0xFF285444), c(0xFFD3EDE3),
            c(0xFF15161C), c(0xFF1A1B22), c(0xFF1F2029), c(0xFF292A34), c(0xFF30313C)),
    ),
    FOREST(
        "forest", "Forest", c(0xFF3E7D5A),
        Tones(c(0xFF3E7D5A), Color.White, c(0xFFCDEBD8), c(0xFF0D2E1B), c(0xFF5A6B4E), c(0xFFDFE8D3), c(0xFF1B2614), c(0xFFB07A3A), c(0xFFF5DFC2), c(0xFF3A2208),
            c(0xFFF5F7F1), c(0xFFF0F3EB), c(0xFFEAEFE4), c(0xFFE3E9DC), c(0xFFDBE3D3)),
        Tones(c(0xFF8FD3A9), c(0xFF07311A), c(0xFF1F4D33), c(0xFFCDEBD8), c(0xFFBFCBB0), c(0xFF3C4834), c(0xFFDFE8D3), c(0xFFE6B57A), c(0xFF5A3C14), c(0xFFF5DFC2),
            c(0xFF111613), c(0xFF161C18), c(0xFF1A211C), c(0xFF232B25), c(0xFF2B342D)),
    ),
    OCEAN(
        "ocean", "Ocean", c(0xFF2F6E9E),
        Tones(c(0xFF2F6E9E), Color.White, c(0xFFCFE4F5), c(0xFF0A2740), c(0xFF4E6577), c(0xFFD9E4EE), c(0xFF0F1E2B), c(0xFF3E8F8A), c(0xFFCCECE9), c(0xFF07302D),
            c(0xFFF3F7FA), c(0xFFEEF3F7), c(0xFFE8EFF4), c(0xFFE0E8EF), c(0xFFD7E1EA)),
        Tones(c(0xFF8FC6EE), c(0xFF06304D), c(0xFF174566), c(0xFFCFE4F5), c(0xFFB5C8D8), c(0xFF34495A), c(0xFFD9E4EE), c(0xFF86D3CC), c(0xFF1C4F4B), c(0xFFCCECE9),
            c(0xFF0F1418), c(0xFF141A1F), c(0xFF182027), c(0xFF202A33), c(0xFF28333D)),
    ),
    LAVENDER(
        "lavender", "Lavender", c(0xFF7357B5),
        Tones(c(0xFF7357B5), Color.White, c(0xFFE6DDFB), c(0xFF26145A), c(0xFF6A5F80), c(0xFFE8E2F2), c(0xFF221B31), c(0xFFB5577F), c(0xFFFADCE8), c(0xFF4A0F29),
            c(0xFFF8F6FC), c(0xFFF4F1F9), c(0xFFEFEBF6), c(0xFFE8E3F1), c(0xFFE0DAEB)),
        Tones(c(0xFFC7B4F5), c(0xFF2E1A66), c(0xFF46337F), c(0xFFE6DDFB), c(0xFFCDC2E0), c(0xFF4A4060), c(0xFFE8E2F2), c(0xFFF0A3C3), c(0xFF6E2A48), c(0xFFFADCE8),
            c(0xFF141218), c(0xFF19161E), c(0xFF1E1A24), c(0xFF27222E), c(0xFF302A38)),
    ),
    ROSE(
        "rose", "Rose", c(0xFFB4465E),
        Tones(c(0xFFB4465E), Color.White, c(0xFFFBD9DF), c(0xFF4A0E1C), c(0xFF7C5A60), c(0xFFF2E1E3), c(0xFF2E1519), c(0xFF8A6A2E), c(0xFFF5E3C0), c(0xFF2E2004),
            c(0xFFFCF6F6), c(0xFFF9F1F2), c(0xFFF5EBEC), c(0xFFF0E3E5), c(0xFFE9DADD)),
        Tones(c(0xFFF3A5B5), c(0xFF5A1426), c(0xFF6E2537), c(0xFFFBD9DF), c(0xFFE0C0C5), c(0xFF5A3F44), c(0xFFF2E1E3), c(0xFFE0C27F), c(0xFF4F3B0C), c(0xFFF5E3C0),
            c(0xFF171112), c(0xFF1C1517), c(0xFF221A1C), c(0xFF2C2225), c(0xFF352A2D)),
    ),
    SLATE(
        "slate", "Slate", c(0xFF3A4250),
        Tones(c(0xFF3A4250), Color.White, c(0xFFDDE1E8), c(0xFF12161D), c(0xFF5B6472), c(0xFFE2E5EA), c(0xFF171B22), c(0xFF6B7F8F), c(0xFFDCE6EE), c(0xFF15232D),
            c(0xFFF6F7F9), c(0xFFF2F3F6), c(0xFFEDEFF2), c(0xFFE6E9ED), c(0xFFDDE1E6)),
        Tones(c(0xFFC3CAD6), c(0xFF1A1F27), c(0xFF39414E), c(0xFFDDE1E8), c(0xFFB9C0CC), c(0xFF3B424D), c(0xFFE2E5EA), c(0xFFA9BDCC), c(0xFF33444F), c(0xFFDCE6EE),
            c(0xFF121316), c(0xFF17191C), c(0xFF1B1D21), c(0xFF24272C), c(0xFF2D3036)),
    );

    val light: ColorScheme by lazy { lightTones.light() }
    val dark: ColorScheme by lazy { darkTones.dark() }

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id } ?: EMBER
    }
}

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

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int) = FontFamily(
    listOf(400, 500, 600, 700).map { w ->
        Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

/** Font choices: a heading face, and optionally a body face (null keeps the system sans). */
enum class AppFont(val id: String, val label: String, val note: String, val heading: () -> FontFamily, val body: (() -> FontFamily)?) {
    CLASSIC("classic", "Classic", "System serif headings", { FontFamily.Serif }, null),
    LITERATA("literata", "Literata", "Made for long reading", { variable(R.font.literata) }, { variable(R.font.literata) }),
    LORA("lora", "Lora", "Elegant serif headings", { variable(R.font.lora) }, null),
    PLAYFAIR("playfair", "Playfair Display", "High-contrast, bookish", { variable(R.font.playfair) }, null),
    NUNITO("nunito", "Nunito", "Soft and rounded", { variable(R.font.nunito) }, { variable(R.font.nunito) }),
    ATKINSON(
        "atkinson", "Atkinson Hyperlegible", "Extra-clear letters",
        { FontFamily(Font(R.font.atkinson_regular, FontWeight.Normal), Font(R.font.atkinson_bold, FontWeight.Bold)) },
        { FontFamily(Font(R.font.atkinson_regular, FontWeight.Normal), Font(R.font.atkinson_bold, FontWeight.Bold)) },
    ),
    MODERN("modern", "Modern", "Clean sans-serif", { FontFamily.SansSerif }, null);

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id } ?: CLASSIC
    }
}

private fun typography(font: AppFont): Typography {
    val base = Typography()
    val heading = font.heading()
    val body = font.body?.invoke()
    fun TextStyle.b() = if (body != null) copy(fontFamily = body) else this
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = heading),
        displayMedium = base.displayMedium.copy(fontFamily = heading),
        displaySmall = base.displaySmall.copy(fontFamily = heading, fontWeight = FontWeight.Bold),
        headlineLarge = base.headlineLarge.copy(fontFamily = heading, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = heading, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = heading, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = heading, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.b(),
        titleSmall = base.titleSmall.b(),
        bodyLarge = base.bodyLarge.b(),
        bodyMedium = base.bodyMedium.b(),
        bodySmall = base.bodySmall.b(),
        labelLarge = base.labelLarge.b(),
        labelMedium = base.labelMedium.b(),
        labelSmall = base.labelSmall.b(),
    )
}

/** The heading face of the chosen font, for big numbers, covers and quotes. */
val DisplayFont: FontFamily
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.typography.headlineMedium.fontFamily ?: FontFamily.Serif

val StatNumber: TextStyle
    @Composable @ReadOnlyComposable
    get() = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 26.sp)

@Composable
fun PagewiseTheme(mode: ThemeMode, palette: Palette, font: AppFont, content: @Composable () -> Unit) {
    val scheme = when (mode) {
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) palette.dark else palette.light
        ThemeMode.LIGHT -> palette.light
        ThemeMode.DARK -> palette.dark
        ThemeMode.NIGHT -> Night
    }
    val type = remember(font) { typography(font) }
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
