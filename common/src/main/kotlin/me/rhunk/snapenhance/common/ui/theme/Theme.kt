package me.rhunk.snapenhance.common.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat.getInsetsController
import me.rhunk.snapenhance.common.data.theme.AppPalette
import me.rhunk.snapenhance.common.data.theme.ThemeMode
import me.rhunk.snapenhance.common.ui.rememberMutableBooleanPreferenceState
import me.rhunk.snapenhance.common.ui.rememberMutableStringPreferenceState
import me.rhunk.snapenhance.common.ui.theme.palettes.*

object Themes {
    val pureBlackDarkModeBackground = Color(0xFF000000)
    val pureBlackDarkModeOnBackground = Color(0xFFFFFFFF)
    val pureBlackDarkModeSurface = Color(0xFF000000)
    val pureBlackDarkModeOnSurface = Color(0xFFFFFFFF)
    val pureBlackDarkModeSurfaceContainer = Color(0xFF0C0C0C)
    val pureBlackDarkModeSurfaceContainerHigh = Color(0xFF131313)
    val pureBlackDarkModeSurfaceContainerHighest = Color(0xFF1B1B1B)

    private val themeRegistry = mapOf(
        "monochrome" to Monochrome,
        "purraurora" to PurrAurora,
        "strawberry_daiquiri" to StrawberryDaiquiri,
        "tidal_wave" to TidalWave,
        "green_apple" to GreenApple,
        "midnight_dusk" to MidnightDusk,
        "nord" to Nord,
        "teal_turqoise" to TealTurqoise
    )

    @Composable
    fun getPalettes(context: Context): List<AppPalette> {
        val palettes = mutableListOf<AppPalette>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            palettes.add(AppPalette(
                key = "dynamic",
                lightColors = dynamicLightColorScheme(context),
                darkColors = dynamicDarkColorScheme(context)
            ))
        }

        palettes.addAll(themeRegistry.values)
        return palettes
    }

    fun getPalettesByName(key: String, context: Context): AppPalette {
        themeRegistry[key]?.let { return it }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AppPalette(
                key = "dynamic",
                lightColors = dynamicLightColorScheme(context),
                darkColors = dynamicDarkColorScheme(context)
            )
        } else {
            Monochrome
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppMaterialTheme(
    preferences: SharedPreferences,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val defaultPalettesName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Dynamic" else Monochrome.key
    val selectedPaletteName by preferences.rememberMutableStringPreferenceState(
        key = "theme_name",
        defaultValue = defaultPalettesName
    )
    val selectedTheme = Themes.getPalettesByName(selectedPaletteName, context)
    val themeMode by preferences.rememberMutableStringPreferenceState(
        key = "theme_mode",
        defaultValue = ThemeMode.SYSTEM.name
    )

    val usePureDarkMode by preferences.rememberMutableBooleanPreferenceState(
        key = "appearance_use_pure_dark_mode",
        defaultValue = false
    )

    val systemDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT.name -> false
        ThemeMode.DARK.name -> true
        ThemeMode.SYSTEM.name -> systemDarkTheme
        else -> systemDarkTheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var currentContext = context
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is android.app.Activity) break
                currentContext = currentContext.baseContext
            }

            if (currentContext is android.app.Activity) {
                val window = currentContext.window
                val insetsController = getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !useDarkTheme
                insetsController.isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    val colorScheme: ColorScheme = if (useDarkTheme) {
        val darkBase = selectedTheme.darkColors
        if (usePureDarkMode) {
            darkBase.copy(
                background = Themes.pureBlackDarkModeBackground,
                onBackground = Themes.pureBlackDarkModeOnBackground,
                surface = Themes.pureBlackDarkModeSurface,
                onSurface = Themes.pureBlackDarkModeOnSurface,
                surfaceContainer = Themes.pureBlackDarkModeSurfaceContainer,
                surfaceContainerHigh = Themes.pureBlackDarkModeSurfaceContainerHigh,
                surfaceContainerHighest = Themes.pureBlackDarkModeSurfaceContainerHighest,
            )
        } else {
            darkBase
        }
    } else {
        selectedTheme.lightColors
    }

    val promoModePreferences = remember { context.getSharedPreferences("promo_mode", Context.MODE_PRIVATE) }
    var promoMode by promoModePreferences.rememberMutableBooleanPreferenceState(
        key = "promo_mode",
        defaultValue = false
    )

    MaterialExpressiveTheme(
        typography = if (promoMode) ProductSansTypography else Typography(),
        colorScheme = colorScheme,
        content = content,
        motionScheme = MotionScheme.expressive()
    )
}