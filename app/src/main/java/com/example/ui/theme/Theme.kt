package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Polices locales
val LoraFontFamily = FontFamily(
    Font(R.font.lora, FontWeight.Normal)
)

val AlmaraiFontFamily = FontFamily(
    Font(R.font.almarai, FontWeight.Normal)
)

val TajawalFontFamily = FontFamily(
    Font(R.font.tajawal, FontWeight.Normal)
)

val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal)
)

val AmiriFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal)
)

val NotoNastaliqUrduFontFamily = FontFamily(
    Font(R.font.noto_nastaliq_urdu, FontWeight.Normal)
)

val DancingScriptFontFamily = FontFamily(
    Font(R.font.dancing_script, FontWeight.Normal)
)

val GreatVibesFontFamily = FontFamily(
    Font(R.font.great_vibes, FontWeight.Normal)
)

val DiwaniFontFamily = FontFamily(
    Font(R.font.diwani, FontWeight.Normal)
)

fun getFontFamilyByName(name: String, activeLanguage: String = "fr"): FontFamily {
    if (activeLanguage == "ar") {
        return when (name) {
            "Tajawal" -> TajawalFontFamily
            "Almarai" -> AlmaraiFontFamily
            else -> AlmaraiFontFamily
        }
    }
    return when (name) {
        "Times New Roman" -> FontFamily.Serif
        "Lora" -> LoraFontFamily
        else -> FontFamily.Serif
    }
}

// Thème académique harmonieux et professionnel pour le mode clair
private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavyLight,
    onPrimaryContainer = NavyDark,
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = GoldAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),
    background = OffWhite,
    onBackground = Charcoal,
    surface = Color.White,
    onSurface = Charcoal,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

// Thème académique harmonieux et contrasté pour le mode nuit
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF0B1120),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF064E3B),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

@Composable
fun MyApplicationTheme(
    themeName: String = "system",
    fontFamilyName: String = "Times New Roman",
    fontSizeScaleName: String = "small",
    isFontBold: Boolean = false,
    activeLanguage: String = "fr",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val baseScheme = when (themeName) {
        "light" -> LightColorScheme
        "dark" -> DarkColorScheme
        else -> if (isSystemDark) DarkColorScheme else LightColorScheme
    }

    val isArabic = activeLanguage == "ar"
    val fontFamily = getFontFamilyByName(fontFamilyName, activeLanguage)

    val effectiveScaleName = fontSizeScaleName.ifBlank { "small" }
    val baseMultiplier = when (effectiveScaleName) {
        "small" -> 0.88f
        "large" -> 1.15f
        else -> 1.0f
    }
    // Proportions typographiques et interlignage strictement identiques entre Français et Arabe
    val multiplier = baseMultiplier
    val lineMultiplier = 1.0f

    val displayWeight = if (isFontBold) FontWeight.Bold else FontWeight.Normal
    val headlineWeight = if (isFontBold) FontWeight.Bold else FontWeight.Normal
    val titleWeight = if (isFontBold) FontWeight.Bold else FontWeight.Normal
    val bodyWeight = if (isFontBold) FontWeight.Bold else FontWeight.Normal
    val labelWeight = if (isFontBold) FontWeight.Bold else FontWeight.Normal

    val customTypography = Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = displayWeight,
            fontSize = (57 * multiplier).sp,
            lineHeight = (64 * multiplier * lineMultiplier).sp
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = displayWeight,
            fontSize = (45 * multiplier).sp,
            lineHeight = (52 * multiplier * lineMultiplier).sp
        ),
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = displayWeight,
            fontSize = (36 * multiplier).sp,
            lineHeight = (44 * multiplier * lineMultiplier).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = headlineWeight,
            fontSize = (32 * multiplier).sp,
            lineHeight = (40 * multiplier * lineMultiplier).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = headlineWeight,
            fontSize = (28 * multiplier).sp,
            lineHeight = (36 * multiplier * lineMultiplier).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = headlineWeight,
            fontSize = (24 * multiplier).sp,
            lineHeight = (32 * multiplier * lineMultiplier).sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = titleWeight,
            fontSize = (22 * multiplier).sp,
            lineHeight = (28 * multiplier * lineMultiplier).sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = titleWeight,
            fontSize = (16 * multiplier).sp,
            lineHeight = (24 * multiplier * lineMultiplier).sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = titleWeight,
            fontSize = (14 * multiplier).sp,
            lineHeight = (20 * multiplier * lineMultiplier).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = bodyWeight,
            fontSize = (16 * multiplier).sp,
            lineHeight = (24 * multiplier * lineMultiplier).sp,
            letterSpacing = if (isArabic) 0.sp else 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = bodyWeight,
            fontSize = (14 * multiplier).sp,
            lineHeight = (20 * multiplier * lineMultiplier).sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = bodyWeight,
            fontSize = (12 * multiplier).sp,
            lineHeight = (16 * multiplier * lineMultiplier).sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = labelWeight,
            fontSize = (14 * multiplier).sp,
            lineHeight = (20 * multiplier * lineMultiplier).sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = labelWeight,
            fontSize = (12 * multiplier).sp,
            lineHeight = (16 * multiplier * lineMultiplier).sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = labelWeight,
            fontSize = (11 * multiplier).sp,
            lineHeight = (16 * multiplier * lineMultiplier).sp
        )
    )

    MaterialTheme(
        colorScheme = baseScheme,
        typography = customTypography,
        content = content
    )
}
