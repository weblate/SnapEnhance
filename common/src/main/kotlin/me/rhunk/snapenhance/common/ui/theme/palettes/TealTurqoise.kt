package me.rhunk.snapenhance.common.ui.theme.palettes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import me.rhunk.snapenhance.common.data.theme.AppPalette

/**
 * Colors for Teal Turqoise theme
 */
private val primaryLight = Color(0xFF008080)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFF008080)
private val onPrimaryContainerLight = Color(0xFFFFFFFF)
private val secondaryLight = Color(0xFF008080)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFCFE5E4)
private val onSecondaryContainerLight = Color(0xFF008080)
private val tertiaryLight = Color(0xFFFF7F7F)
private val onTertiaryLight = Color(0xFF000000)
private val tertiaryContainerLight = Color(0xFF2A1616)
private val onTertiaryContainerLight = Color(0xFFFF7F7F)
private val backgroundLight = Color(0xFFFAFAFA)
private val onBackgroundLight = Color(0xFF050505)
private val surfaceLight = Color(0xFFFAFAFA)
private val surfaceTintLight = Color(0xFFBFDFDF)
private val onSurfaceLight = Color(0xFF050505)
private val surfaceVariantLight = Color(0xFFEBF3F1)
private val onSurfaceVariantLight = Color(0xFF050505)
private val outlineLight = Color(0xFF6F7977)
private val inverseSurfaceLight = Color(0xFF050505)
private val inverseOnSurfaceLight = Color(0xFFFAFAFA)
private val inversePrimaryLight = Color(0xFF40E0D0)
private val surfaceContainerLowestLight = Color(0xFFE1E9E7)
private val surfaceContainerLowLight = Color(0xFFE6EEEC)
private val surfaceContainerLight = Color(0xFFEBF3F1)
private val surfaceContainerHighLight = Color(0xFFF0F8F6)
private val surfaceContainerHighestLight = Color(0xFFF7FFFD)

private val primaryDark = Color(0xFF40E0D0)
private val onPrimaryDark = Color(0xFF000000)
private val primaryContainerDark = Color(0xFF40E0D0)
private val onPrimaryContainerDark = Color(0xFF000000)
private val secondaryDark = Color(0xFF40E0D0)
private val onSecondaryDark = Color(0xFF000000)
private val secondaryContainerDark = Color(0xFF18544E)
private val onSecondaryContainerDark = Color(0xFF40E0D0)
private val tertiaryDark = Color(0xFFBF1F2F)
private val onTertiaryDark = Color(0xFFFFFFFF)
private val tertiaryContainerDark = Color(0xFF200508)
private val onTertiaryContainerDark = Color(0xFFBF1F2F)
private val backgroundDark = Color(0xFF202125)
private val onBackgroundDark = Color(0xFFDFDEDA)
private val surfaceDark = Color(0xFF202125)
private val surfaceTintDark = Color(0xFF40E0D0)
private val onSurfaceDark = Color(0xFFDFDEDA)
private val surfaceVariantDark = Color(0xFF233133)
private val onSurfaceVariantDark = Color(0xFFDFDEDA)
private val outlineDark = Color(0xFF899391)
private val inverseSurfaceDark = Color(0xFFDFDEDA)
private val inverseOnSurfaceDark = Color(0xFF202125)
private val inversePrimaryDark = Color(0xFF008080)
private val surfaceContainerLowestDark = Color(0xFF202C2E)
private val surfaceContainerLowDark = Color(0xFF222F31)
private val surfaceContainerDark = Color(0xFF233133)
private val surfaceContainerHighDark = Color(0xFF28383A)
private val surfaceContainerHighestDark = Color(0xFF2F4244)

val TealTurqoise = AppPalette(
    key = "teal_turqoise",
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
        background = backgroundDark,
        onBackground = onBackgroundDark,
        surface = surfaceDark,
        surfaceTint = surfaceTintDark,
        onSurface = onSurfaceDark,
        surfaceVariant = surfaceVariantDark,
        onSurfaceVariant = onSurfaceVariantDark,
        outline = outlineDark,
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
