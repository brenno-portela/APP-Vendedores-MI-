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
    // Paleta oficial Minum: verde profundo, verde de acao e menta.
    primary = Color(0xFF00463A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA4E0CE),
    onPrimaryContainer = Color(0xFF00382F),
    secondary = Color(0xFF009279),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9F8EF),
    onSecondaryContainer = Color(0xFF003C32),
    tertiary = Color(0xFF5889FB),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5EBFF),
    background = Color(0xFFF2F4FA),
    onBackground = Color(0xFF12342F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF12342F),
    surfaceVariant = Color(0xFFE6EFEC),
    onSurfaceVariant = Color(0xFF526761),
    outline = Color(0xFF71857F),
    error = Color(0xFFB9382F),
    onError = Color.White,
    errorContainer = Color(0xFFFCE8E5),
    onErrorContainer = Color(0xFF5D1611)
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
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
