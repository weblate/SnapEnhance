package me.rhunk.snapenhance.common.ui.theme.palettes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import me.rhunk.snapenhance.common.data.theme.AppPalette

/**
 * Colors for Nord theme
 * https://www.nordtheme.com/docs/colors-and-palettes
 * for the light theme, the primary color is switched with the tertiary for better contrast in some case
 */
private val primaryLight = Color(0xFF5E81AC)
private val onPrimaryLight = Color(0xFF000000)
private val primaryContainerLight = Color(0xFF5E81AC)
private val onPrimaryContainerLight = Color(0xFF000000)
private val secondaryLight = Color(0xFF81A1C1)
private val onSecondaryLight = Color(0xFF2E3440)
private val secondaryContainerLight = Color(0xFF91B4D7)
private val onSecondaryContainerLight = Color(0xFF2E3440)
private val tertiaryLight = Color(0xFF88C0D0)
private val onTertiaryLight = Color(0xFF2E3440)
private val tertiaryContainerLight = Color(0xFF88C0D0)
private val onTertiaryContainerLight = Color(0xFF2E3440)
private val onErrorLight = Color(0xFFECEFF4)
private val errorContainerLight = Color(0xFFBF616A)
private val onErrorContainerLight = Color(0xFF000000)
private val backgroundLight = Color(0xFFECEFF4)
private val onBackgroundLight = Color(0xFF2E3440)
private val surfaceLight = Color(0xFFE5E9F0)
private val surfaceTintLight = Color(0xFF5E81AC)
private val onSurfaceLight = Color(0xFF2E3440)
private val surfaceVariantLight = Color(0xFFDAE0EA)
private val onSurfaceVariantLight = Color(0xFF2E3440)
private val outlineLight = Color(0xFF2E3440)
private val inverseSurfaceLight = Color(0xFF3B4252)
private val inverseOnSurfaceLight = Color(0xFFECEFF4)
private val inversePrimaryLight = Color(0xFF8CA8CD)
private val surfaceContainerLowestLight = Color(0xFFD1D7E0)
private val surfaceContainerLowLight = Color(0xFFD6DCE6)
private val surfaceContainerLight = Color(0xFFDAE0EA)
private val surfaceContainerHighLight = Color(0xFFE9EDF3)
private val surfaceContainerHighestLight = Color(0xFFF2F4F8)

private val primaryDark = Color(0xFF88C0D0)
private val onPrimaryDark = Color(0xFF2E3440)
private val primaryContainerDark = Color(0xFF88C0D0)
private val onPrimaryContainerDark = Color(0xFF2E3440)
private val secondaryDark = Color(0xFF81A1C1)
private val onSecondaryDark = Color(0xFF2E3440)
private val secondaryContainerDark = Color(0xFF506275)
private val onSecondaryContainerDark = Color(0xFF88C0D0)
private val tertiaryDark = Color(0xFF5E81AC)
private val onTertiaryDark = Color(0xFF000000)
private val tertiaryContainerDark = Color(0xFF5E81AC)
private val onTertiaryContainerDark = Color(0xFF000000)
private val onErrorDark = Color(0xFF2E3440)
private val errorContainerDark = Color(0xFFBF616A)
private val onErrorContainerDark = Color(0xFF000000)
private val backgroundDark = Color(0xFF2E3440)
private val onBackgroundDark = Color(0xFFECEFF4)
private val surfaceDark = Color(0xFF2E3440)
private val surfaceTintDark = Color(0xFF88C0D0)
private val onSurfaceDark = Color(0xFFECEFF4)
private val surfaceVariantDark = Color(0xFF414C5C)
private val onSurfaceVariantDark = Color(0xFFECEFF4)
private val outlineDark = Color(0xFF6d717b)
private val outlineVariantDark = Color(0xFF90939a)
private val inverseSurfaceDark = Color(0xFFD8DEE9)
private val inverseOnSurfaceDark = Color(0xFF2E3440)
private val inversePrimaryDark = Color(0xFF397E91)
private val surfaceContainerLowestDark = Color(0xFF373F4D)
private val surfaceContainerLowDark = Color(0xFF3E4756)
private val surfaceContainerDark = Color(0xFF414C5C)
private val surfaceContainerHighDark = Color(0xFF4E5766)
private val surfaceContainerHighestDark = Color(0xFF505968)

val Nord = AppPalette(
    key = "nord",
    lightColors = lightColorScheme(
        primary = primaryLight,
        onPrimary = onPrimaryLight,
        primaryContainer = primaryContainerLight,
        onPrimaryContainer = onPrimaryContainerLight,
        secondary = secondaryLight,
        onSecondary = onSecondaryLight,
        secondaryContainer = secondaryContainerLight,
        onSecondaryContainer = onSecondaryContainerLight,
        tertiary = tertiaryLight,
        onTertiary = onTertiaryLight,
        tertiaryContainer = tertiaryContainerLight,
        onTertiaryContainer = onTertiaryContainerLight,
        onError = onErrorLight,
        errorContainer = errorContainerLight,
        onErrorContainer = onErrorContainerLight,
        background = backgroundLight,
        onBackground = onBackgroundLight,
        surface = surfaceLight,
        surfaceTint = surfaceTintLight,
        onSurface = onSurfaceLight,
        surfaceVariant = surfaceVariantLight,
        onSurfaceVariant = onSurfaceVariantLight,
        outline = outlineLight,
        inverseSurface = inverseSurfaceLight,
        inverseOnSurface = inverseOnSurfaceLight,
        inversePrimary = inversePrimaryLight,
        surfaceContainerLowest = surfaceContainerLowestLight,
        surfaceContainerLow = surfaceContainerLowLight,
        surfaceContainer = surfaceContainerLight,
        surfaceContainerHigh = surfaceContainerHighLight,
        surfaceContainerHighest = surfaceContainerHighestLight,
    ),

    darkColors = darkColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = backgroundDark,
        onBackground = onBackgroundDark,
        surface = surfaceDark,
        surfaceTint = surfaceTintDark,
        onSurface = onSurfaceDark,
        surfaceVariant = surfaceVariantDark,
        onSurfaceVariant = onSurfaceVariantDark,
        outline = outlineDark,
        outlineVariant = outlineVariantDark,
        inverseSurface = inverseSurfaceDark,
        inverseOnSurface = inverseOnSurfaceDark,
        inversePrimary = inversePrimaryDark,
        surfaceContainerLowest = surfaceContainerLowestDark,
        surfaceContainerLow = surfaceContainerLowDark,
        surfaceContainer = surfaceContainerDark,
        surfaceContainerHigh = surfaceContainerHighDark,
        surfaceContainerHighest = surfaceContainerHighestDark,
    )
)