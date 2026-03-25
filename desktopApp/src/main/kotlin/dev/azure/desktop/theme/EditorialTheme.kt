package dev.azure.desktop.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val EditorialLight: ColorScheme = lightColorScheme(
    primary = EditorialColors.primary,
    onPrimary = EditorialColors.onPrimary,
    primaryContainer = EditorialColors.primaryContainer,
    onPrimaryContainer = EditorialColors.onPrimaryContainer,
    secondary = EditorialColors.secondary,
    onSecondary = EditorialColors.onSecondary,
    secondaryContainer = EditorialColors.secondaryContainer,
    onSecondaryContainer = EditorialColors.onSecondaryContainer,
    tertiary = EditorialColors.tertiary,
    onTertiary = EditorialColors.onTertiary,
    tertiaryContainer = EditorialColors.tertiaryContainer,
    onTertiaryContainer = EditorialColors.onTertiaryContainer,
    error = EditorialColors.error,
    onError = EditorialColors.onError,
    errorContainer = EditorialColors.errorContainer,
    onErrorContainer = EditorialColors.onErrorContainer,
    background = EditorialColors.background,
    onBackground = EditorialColors.onBackground,
    surface = EditorialColors.surface,
    onSurface = EditorialColors.onSurface,
    surfaceVariant = EditorialColors.surfaceVariant,
    onSurfaceVariant = EditorialColors.onSurfaceVariant,
    outline = EditorialColors.outline,
    outlineVariant = EditorialColors.outlineVariant,
    inverseSurface = EditorialColors.inverseSurface,
    inverseOnSurface = EditorialColors.inverseOnSurface,
    inversePrimary = EditorialColors.inversePrimary,
)

private val EditorialTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
    ),
)

@Composable
fun EditorialTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EditorialLight,
        typography = EditorialTypography,
        content = content,
    )
}
