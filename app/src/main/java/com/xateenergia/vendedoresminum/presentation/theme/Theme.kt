package com.xateenergia.vendedoresminum.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = MinumColorTokens.Brand.PrimaryDark,
    onPrimary = MinumColorTokens.Text.Inverse,
    primaryContainer = MinumColorTokens.Brand.Light,
    onPrimaryContainer = MinumColorTokens.Brand.PrimaryDark,
    secondary = MinumColorTokens.Brand.Primary,
    onSecondary = MinumColorTokens.Text.Inverse,
    secondaryContainer = MinumColorTokens.Surface.Subtle,
    onSecondaryContainer = MinumColorTokens.Brand.PrimaryDark,
    tertiary = MinumColorTokens.Brand.Blue,
    onTertiary = MinumColorTokens.Text.Inverse,
    tertiaryContainer = Color(0xFFE5EBFF),
    background = MinumColorTokens.Surface.Default,
    onBackground = MinumColorTokens.Text.Primary,
    surface = MinumColorTokens.Surface.Elevated,
    onSurface = MinumColorTokens.Text.Primary,
    surfaceVariant = MinumColorTokens.Surface.Subtle,
    onSurfaceVariant = MinumColorTokens.Text.Secondary,
    outline = MinumColorTokens.Border.Strong,
    error = MinumColorTokens.Feedback.Error,
    onError = MinumColorTokens.Text.Inverse,
    errorContainer = Color(0xFFFCE8E5),
    onErrorContainer = Color(0xFF5D1611)
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(MinumRadii.Small),
    small = androidx.compose.foundation.shape.RoundedCornerShape(MinumRadii.Small),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(MinumRadii.Medium),
    large = androidx.compose.foundation.shape.RoundedCornerShape(MinumRadii.Medium),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(MinumRadii.Medium)
)

// A Carbona nao foi fornecida em formato instalavel. SansSerif e o fallback temporario seguro.
private val MinumFontFamily = FontFamily.SansSerif

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = MinumFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = MinumFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = MinumFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = MinumFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = MinumFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = MinumFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = MinumFontFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = MinumFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = MinumFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = MinumFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = MinumFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)

@Composable
fun VendedoresMinumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
