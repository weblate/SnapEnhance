@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages

import android.content.SharedPreferences
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.navigation.NavBackStackEntry
import me.rhunk.snapenhance.common.ui.cardShape
import me.rhunk.snapenhance.common.ui.lazyColumnContentPadding
import me.rhunk.snapenhance.common.ui.rememberMutableBooleanPreferenceState
import me.rhunk.snapenhance.common.ui.rememberMutableFloatPreferenceState
import me.rhunk.snapenhance.common.ui.rememberMutableStringPreferenceState
import me.rhunk.snapenhance.ui.components.LazyColumnBottomSheet
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.manager.data.MenuAction

data class TweakCategoryItem(
    val name: String,
    val key: String
)

object TweakCategoryItems {
    val TweakCategoryItemsList: List<TweakCategoryItem> = listOf(
        TweakCategoryItem(name = "Colors", key = "colors"),
        TweakCategoryItem(name = "Padding", key = "padding"),
        TweakCategoryItem(name = "Tab Width", key = "tabWidth"),
        TweakCategoryItem(name = "Offset Animation", key = "offsetAnimation"),
        TweakCategoryItem(name = "Panel Offset Density", key = "panelOffsetDensity"),
        TweakCategoryItem(name = "Damped Drag Animation", key = "dampedDragAnimation"),
        TweakCategoryItem(name = "Interactive Highlight", key = "interactiveHighlight"),
        TweakCategoryItem(name = "First Row", key = "firstRow"),
        TweakCategoryItem(name = "Second Row", key = "secondRow"),
        TweakCategoryItem(name = "Box", key = "box"),
    )
}

sealed interface TweakItem {
    val key: String
    val category: String
}

data class FloatTweakItem(
    override val key: String,
    override val category: String,
    val defaultValue: Float,
    val valueRange: ClosedFloatingPointRange<Float>
) : TweakItem

data class BooleanTweakItem(
    override val key: String,
    override val category: String,
    val defaultValue: Boolean
) : TweakItem

data class StringTweakItem(
    override val key: String,
    override val category: String,
    val defaultValue: String
) : TweakItem

object TweakItems {
    val TweakItemsList: List<TweakItem> = listOf(
        // Colors
        FloatTweakItem("rowOneContainerColorAlphaValue", "colors", 1f, 0f..1f),
        FloatTweakItem("rowTwoContainerColorAlphaValue", "colors", 1f, 0f..1f),

        // Padding
        FloatTweakItem("liquidBottomTabsModifierPaddingStartValue", "padding", 10f, 0f..100f),
        FloatTweakItem("liquidBottomTabsModifierPaddingEndValue", "padding", 10f, 0f..100f),
        FloatTweakItem("liquidBottomTabsModifierPaddingTopValue", "padding", 0f, 0f..100f),
        FloatTweakItem("liquidBottomTabsModifierPaddingBottomValue", "padding", 55f, 0f..100f),

        // Tab & Density
        FloatTweakItem("tabWidthMinusValue", "tabWidth", 8f, 0f..100f),
        FloatTweakItem("offsetAnimationValue", "offsetAnimation", 0f, -100f..100f),
        FloatTweakItem("panelOffsetDensityValue", "panelOffsetDensity", 4f, 0f..20f),

        // Damped Drag Animations
        FloatTweakItem("dampedDragAnimationVisibilityThresholdValue", "dampedDragAnimation", 0.001f, 0.0001f..0.05f),
        FloatTweakItem("dampedDragAnimationInitialScaleValue", "dampedDragAnimation", 1f, 0f..5f),
        FloatTweakItem("dampedDragAnimationFirstPressedScaleValue", "dampedDragAnimation", 78f, 0f..200f),
        FloatTweakItem("dampedDragAnimationSecondPressedScaleValue", "dampedDragAnimation", 56f, 0f..200f),
        FloatTweakItem("dampedDragAnimationOnDragStoppedOffsetAnimationTargetValue", "dampedDragAnimation", 0f, -50f..50f),
        FloatTweakItem("dampedDragAnimationOnDragStoppedOffsetAnimationDampingRatioValue", "dampedDragAnimation", 1f, 0f..2f),
        FloatTweakItem("dampedDragAnimationOnDragStoppedOffsetAnimationStiffnessValue", "dampedDragAnimation", 300f, 0f..2000f),
        FloatTweakItem("dampedDragAnimationOnDragStoppedOffsetAnimationVisibilityThresholdValue", "dampedDragAnimation", 0.5f, 0.01f..2f),
        FloatTweakItem("dampedDragAnimationValueAnimationSpecDampingRatioValue", "dampedDragAnimation", 1f, 0f..2f),
        FloatTweakItem("dampedDragAnimationValueAnimationSpecStiffnessValue", "dampedDragAnimation", 1000f, 0f..3000f),
        FloatTweakItem("dampedDragAnimationVelocityAnimationSpecDampingRatioValue", "dampedDragAnimation", 0.5f, 0f..2f),
        FloatTweakItem("dampedDragAnimationVelocityAnimationSpecStiffnessValue", "dampedDragAnimation", 300f, 0f..2000f),
        FloatTweakItem("dampedDragAnimationVelocityAnimationSpecTimesValue", "dampedDragAnimation", 10f, 0f..50f),
        FloatTweakItem("dampedDragAnimationPressProgressAnimationSpecDampingRatioValue", "dampedDragAnimation", 1f, 0f..2f),
        FloatTweakItem("dampedDragAnimationPressProgressAnimationSpecStiffnessValue", "dampedDragAnimation", 1000f, 0f..3000f),
        FloatTweakItem("dampedDragAnimationPressProgressAnimationSpecVisibilityThresholdValue", "dampedDragAnimation", 0.001f, 0.0001f..0.05f),
        FloatTweakItem("dampedDragAnimationScaleXAnimationSpecDampingRatioValue", "dampedDragAnimation", 0.6f, 0f..2f),
        FloatTweakItem("dampedDragAnimationScaleXAnimationSpecStiffnessValue", "dampedDragAnimation", 250f, 0f..2000f),
        FloatTweakItem("dampedDragAnimationScaleXAnimationSpecVisibilityThresholdValue", "dampedDragAnimation", 0.001f, 0.0001f..0.05f),
        FloatTweakItem("dampedDragAnimationScaleYAnimationSpecDampingRatioValue", "dampedDragAnimation", 0.7f, 0f..2f),
        FloatTweakItem("dampedDragAnimationScaleYAnimationSpecStiffnessValue", "dampedDragAnimation", 250f, 0f..2000f),
        FloatTweakItem("dampedDragAnimationScaleYAnimationSpecVisibilityThresholdValue", "dampedDragAnimation", 0.001f, 0.0001f..0.05f),
        FloatTweakItem("dampedDragAnimationVelocityAnimationInitialValue", "dampedDragAnimation", 0f, -50f..50f),
        FloatTweakItem("dampedDragAnimationVelocityAnimationVisibilityThresholdValue", "dampedDragAnimation", 5f, 0f..50f),
        FloatTweakItem("dampedDragAnimationPressProgressAnimationInitialValue", "dampedDragAnimation", 0f, 0f..1f),
        FloatTweakItem("dampedDragAnimationPressProgressAnimationVisibilityThresholdValue", "dampedDragAnimation", 0.001f, 0.0001f..0.05f),
        FloatTweakItem("dampedDragAnimationScaleXAnimationVisibilityThresholdValue", "dampedDragAnimation", 0.001f, 0.0001f..0.05f),
        FloatTweakItem("dampedDragAnimationScaleYAnimationVisibilityThresholdValue", "dampedDragAnimation", 0.001f, 0.0001f..0.05f),
        FloatTweakItem("dampedDragAnimationPressProgressAnimationValue", "dampedDragAnimation", 1f, 0f..2f),
        FloatTweakItem("dampedDragAnimationReleaseThresholdsTimesValue", "dampedDragAnimation", 0.025f, 0f..0.5f),
        FloatTweakItem("dampedDragAnimationReleaseTargetValue", "dampedDragAnimation", 0f, -10f..10f),
        FloatTweakItem("dampedDragAnimationValueAnimationValue", "dampedDragAnimation", 0f, -10f..10f),
        FloatTweakItem("dampedDragAnimationVelocityTrackerOffsetYValue", "dampedDragAnimation", 0f, -100f..100f),

        // Interactive Highlight
        FloatTweakItem("interactiveHighlightPositionOffsetXIfTrueDampedDragAnimationPlusValue", "interactiveHighlight", 0.5f, 0f..2f),
        FloatTweakItem("interactiveHighlightPositionOffsetXElseDampedDragAnimationPlusValue", "interactiveHighlight", 0.5f, 0f..2f),
        FloatTweakItem("interactiveHighlightPositionOffsetYDivideValue", "interactiveHighlight", 2f, 0.1f..10f),
        FloatTweakItem("interactiveHighlightPressProgressAnimationSpecDampingRatioValue", "interactiveHighlight", 0.5f, 0f..2f),
        FloatTweakItem("interactiveHighlightPressProgressAnimationSpecStiffnessValue", "interactiveHighlight", 300f, 0f..2000f),
        FloatTweakItem("interactiveHighlightPressProgressAnimationSpecVisibilityThresholdValue", "interactiveHighlight", 0.001f, 0.0001f..0.05f),
        FloatTweakItem("interactiveHighlightPositionAnimationSpecDampingRatioValue", "interactiveHighlight", 0.5f, 0f..2f),
        FloatTweakItem("interactiveHighlightPositionAnimationSpecStiffnessValue", "interactiveHighlight", 300f, 0f..2000f),
        FloatTweakItem("interactiveHighlightPressProgressAnimationInitialValue", "interactiveHighlight", 0f, 0f..1f),
        FloatTweakItem("interactiveHighlightPressProgressAnimationVisibilityThresholdValue", "interactiveHighlight", 0.001f, 0.0001f..0.05f),
        FloatTweakItem("interactiveHighlightShaderNotNullDrawRectColorWhiteAlphaValue", "interactiveHighlight", 0.08f, 0f..1f),
        FloatTweakItem("interactiveHighlightShaderSetColorUniformWhiteAlphaValue", "interactiveHighlight", 0.15f, 0f..1f),
        FloatTweakItem("interactiveHighlightShaderSetFloatUniformRadiusTimesValue", "interactiveHighlight", 1.5f, 0f..5f),
        FloatTweakItem("interactiveHighlightShaderIsNullDrawRectColorWhiteAlphaValue", "interactiveHighlight", 0.25f, 0f..1f),
        FloatTweakItem("interactiveHighlightOnDragStartTargetValue", "interactiveHighlight", 1f, 0f..2f),
        FloatTweakItem("interactiveHighlightOnDragEndTargetValue", "interactiveHighlight", 0f, 0f..2f),
        FloatTweakItem("interactiveHighlightOnDragCancelTargetValue", "interactiveHighlight", 0f, 0f..2f),

        // First Row
        BooleanTweakItem("rowOneModifierEnableGraphicsLayer", "firstRow", false),
        FloatTweakItem("rowOneModifierDrawBackdropShapeValue", "firstRow", 32f, 0f..100f),
        FloatTweakItem("rowOneModifierDrawBackdropEffectsBlurRadiusValue", "firstRow", 8f, 0f..50f),
        FloatTweakItem("rowOneModifierDrawBackdropEffectsLensRefractionHeightValue", "firstRow", 24f, 0f..100f),
        FloatTweakItem("rowOneModifierDrawBackdropEffectsLensRefractionAmountValue", "firstRow", 24f, 0f..100f),
        BooleanTweakItem("rowOneModifierDrawBackdropEffectsLensEnableDepthEffect", "firstRow", false),
        BooleanTweakItem("rowOneModifierDrawBackdropEffectsLensEnableChromaticAberration", "firstRow", false),
        FloatTweakItem("rowOneModifierDrawBackdropLayerBlockScaleLerpStartValue", "firstRow", 1f, 0f..2f),
        FloatTweakItem("rowOneModifierDrawBackdropLayerBlockScaleLerpStopValue", "firstRow", 1f, 0f..2f),
        FloatTweakItem("rowOneModifierDrawBackdropLayerBlockScaleLerpPlusValue", "firstRow", 16f, 0f..100f),
        FloatTweakItem("rowOneModifierHeightValue", "firstRow", 64f, 0f..200f),
        FloatTweakItem("rowOneModifierPaddingValue", "firstRow", 4f, 0f..50f),

        // Second Row
        FloatTweakItem("rowTwoLocalLiquidBottomTabScaleLerpStartValue", "secondRow", 1f, 0f..2f),
        FloatTweakItem("rowTwoLocalLiquidBottomTabScaleLerpStopValue", "secondRow", 1.1f, 0f..2f),
        FloatTweakItem("rowTwoModifierAlphaValue", "secondRow", 0f, 0f..1f),
        FloatTweakItem("rowTwoModifierDrawBackdropShapeValue", "secondRow", 32f, 0f..100f),
        FloatTweakItem("rowTwoModifierDrawBackdropEffectsBlurRadiusValue", "secondRow", 8f, 0f..50f),
        FloatTweakItem("rowTwoModifierDrawBackdropEffectsLensRefractionHeightValue", "secondRow", 24f, 0f..100f),
        FloatTweakItem("rowTwoModifierDrawBackdropEffectsLensRefractionAmountValue", "secondRow", 24f, 0f..100f),
        BooleanTweakItem("rowTwoModifierDrawBackdropEffectsLensEnableDepthEffect", "secondRow", false),
        BooleanTweakItem("rowTwoModifierDrawBackdropEffectsLensEnableChromaticAberration", "secondRow", true),
        FloatTweakItem("rowTwoModifierHeightValue", "secondRow", 56f, 0f..200f),
        FloatTweakItem("rowTwoModifierPaddingHorizontalValue", "secondRow", 4f, 0f..50f),

        // Box
        FloatTweakItem("boxModifierPaddingHorizontalValue", "box", 4f, 0f..50f),
        FloatTweakItem("boxModifierGraphicsLayerAlphaIfTrueValue", "box", 0f, 0f..1f),
        FloatTweakItem("boxModifierGraphicsLayerAlphaIfFalseValue", "box", 1f, 0f..1f),
        FloatTweakItem("boxModifierGraphicsLayerTranslationXElsePlusValue", "box", 1f, -50f..50f),
        FloatTweakItem("boxModifierDrawBackdropShapeValue", "box", 32f, 0f..100f),
        FloatTweakItem("boxModifierDrawBackdropEffectsLensRefractionHeightValue", "box", 12f, 0f..100f),
        FloatTweakItem("boxModifierDrawBackdropEffectsLensRefractionAmountValue", "box", 12f, 0f..100f),
        BooleanTweakItem("boxModifierDrawBackdropEffectsLensEnableDepthEffect", "box", false),
        BooleanTweakItem("boxModifierDrawBackdropEffectsLensEnableChromaticAberration", "box", true),
        FloatTweakItem("boxModifierDrawBackdropInnerShadowRadiusValue", "box", 8f, 0f..50f),
        FloatTweakItem("boxModifierDrawBackdropLayerBlockVelocityDivideValue", "box", 10f, 0.1f..50f),
        FloatTweakItem("boxModifierDrawBackdropLayerBlockScaleXNumberValue", "box", 1f, 0f..5f),
        FloatTweakItem("boxModifierDrawBackdropLayerBlockScaleXVelocityTimesValue", "box", 0.75f, 0f..5f),
        FloatTweakItem("boxModifierDrawBackdropLayerBlockScaleXFastCoerceInFirstValue", "box", -0.2f, -1f..1f),
        FloatTweakItem("boxModifierDrawBackdropLayerBlockScaleXFastCoerceInSecondValue", "box", 0.2f, -1f..1f),
        FloatTweakItem("boxModifierDrawBackdropLayerBlockScaleYNumberValue", "box", 1f, 0f..5f),
        FloatTweakItem("boxModifierDrawBackdropLayerBlockScaleYVelocityTimesValue", "box", 0.25f, 0f..5f),
        FloatTweakItem("boxModifierDrawBackdropLayerBlockScaleYFastCoerceInFirstValue", "box", -0.2f, -1f..1f),
        FloatTweakItem("boxModifierDrawBackdropLayerBlockScaleYFastCoerceInSecondValue", "box", 0.2f, -1f..1f),
        FloatTweakItem("boxModifierDrawBackdropOnDrawSurfaceFirstDrawRectIfLightThemeColorBlackAlphaValue", "box", 0.1f, 0f..1f),
        FloatTweakItem("boxModifierDrawBackdropOnDrawSurfaceFirstDrawRectElseColorWhiteAlphaValue", "box", 0.1f, 0f..1f),
        FloatTweakItem("boxModifierDrawBackdropOnDrawSurfaceSecondDrawRectAlphaValue", "box", 1f, 0f..1f),
        FloatTweakItem("boxModifierDrawBackdropOnDrawSurfaceSecondDrawRectColorBlackAlphaValue", "box", 0.03f, 0f..1f),
        FloatTweakItem("boxModifierHeightValue", "box", 56f, 0f..200f),
        FloatTweakItem("boxModifierFillMaxWidthFractionValue", "box", 1f, 0f..1f)
    )
}

data class PresetOption (
    val name: String,
    val action: () -> Unit
)

class LiquidGlassSettingsRoot : Routes.Route() {
    override val topBarActions: @Composable (RowScope.() -> Unit) = {
        var showActionDropdownMenu by remember { mutableStateOf(false) }
        var showPresetsBottomSheet by remember { mutableStateOf(false) }
         
        val presetOptions = listOf(
            PresetOption(
                name = "Full Glass",
                action = {
                    context.sharedPreferences.edit().apply {
                        TweakItems.TweakItemsList.forEach { tweakItem ->
                            when (tweakItem) {
                                is FloatTweakItem -> putFloat(tweakItem.key, tweakItem.defaultValue)
                                is BooleanTweakItem -> putBoolean(tweakItem.key, tweakItem.defaultValue)
                                is StringTweakItem -> putString(tweakItem.key, tweakItem.defaultValue)
                            }
                        }
                        putFloat("rowOneContainerColorAlphaValue", 0f)
                        putFloat("rowOneModifierDrawBackdropEffectsBlurRadiusValue", 0f)
                        putFloat("rowOneModifierDrawBackdropEffectsLensRefractionHeightValue", 36f)
                        putFloat("rowOneModifierDrawBackdropEffectsLensRefractionAmountValue", 36f)
                        apply()
                    }
                }
            ),
            PresetOption(
                name = "Jiggly",
                action = {
                    context.sharedPreferences.edit().apply {
                        TweakItems.TweakItemsList.forEach { tweakItem ->
                            when (tweakItem) {
                                is FloatTweakItem -> putFloat(tweakItem.key, tweakItem.defaultValue)
                                is BooleanTweakItem -> putBoolean(tweakItem.key, tweakItem.defaultValue)
                                is StringTweakItem -> putString(tweakItem.key, tweakItem.defaultValue)
                            }
                        }
                        putFloat("dampedDragAnimationValueAnimationSpecDampingRatioValue", 0.38f)
                        putFloat("dampedDragAnimationValueAnimationSpecStiffnessValue", 650f)
                        putFloat("dampedDragAnimationFirstPressedScaleValue", 105f)
                        putFloat("boxModifierDrawBackdropLayerBlockScaleXVelocityTimesValue", 1.25f)
                        putFloat("boxModifierDrawBackdropLayerBlockScaleYVelocityTimesValue", 0.45f)
                        putFloat("rowOneModifierDrawBackdropShapeValue", 36f)
                        putFloat("boxModifierDrawBackdropShapeValue", 36f)
                        apply()
                    }
                }
            ),
            PresetOption(
                name = "Retangle",
                action = {
                    context.sharedPreferences.edit().apply {
                        TweakItems.TweakItemsList.forEach { tweakItem ->
                            when (tweakItem) {
                                is FloatTweakItem -> putFloat(tweakItem.key, tweakItem.defaultValue)
                                is BooleanTweakItem -> putBoolean(tweakItem.key, tweakItem.defaultValue)
                                is StringTweakItem -> putString(tweakItem.key, tweakItem.defaultValue)
                            }
                        }
                        putFloat("rowOneModifierDrawBackdropShapeValue", 10f)
                        putFloat("rowTwoModifierDrawBackdropShapeValue", 10f)
                        putFloat("boxModifierDrawBackdropShapeValue", 8f)
                        putFloat("dampedDragAnimationValueAnimationSpecDampingRatioValue", 1.2f)
                        putFloat("dampedDragAnimationValueAnimationSpecStiffnessValue", 1800f)
                        putFloat("boxModifierDrawBackdropLayerBlockScaleXVelocityTimesValue", 0.15f)
                        putFloat("rowOneModifierDrawBackdropEffectsBlurRadiusValue", 5f)
                        apply()
                    }
                }
            ),
            PresetOption(
                name = "Prism Depth",
                action = {
                    context.sharedPreferences.edit().apply {
                        TweakItems.TweakItemsList.forEach { tweakItem ->
                            when (tweakItem) {
                                is FloatTweakItem -> putFloat(tweakItem.key, tweakItem.defaultValue)
                                is BooleanTweakItem -> putBoolean(tweakItem.key, tweakItem.defaultValue)
                                is StringTweakItem -> putString(tweakItem.key, tweakItem.defaultValue)
                            }
                        }
                        putBoolean("rowOneModifierDrawBackdropEffectsLensEnableDepthEffect", true)
                        putBoolean("rowOneModifierDrawBackdropEffectsLensEnableChromaticAberration", true)
                        putFloat("rowOneModifierDrawBackdropEffectsLensRefractionHeightValue", 30f)
                        putBoolean("boxModifierDrawBackdropEffectsLensEnableDepthEffect", true)
                        putBoolean("boxModifierDrawBackdropEffectsLensEnableChromaticAberration", true)
                        putFloat("boxModifierDrawBackdropEffectsLensRefractionHeightValue", 20f)
                        putFloat("boxModifierDrawBackdropInnerShadowRadiusValue", 18f)
                        apply()
                    }
                }
            )
        )

        IconButton(
            onClick = { showActionDropdownMenu = true },
            shapes = IconButtonDefaults.shapes()
        ) {
            Icon(Icons.Rounded.MoreVert, contentDescription = null)
        }

        val actions = remember(translation) {
            listOf(
                MenuAction(
                    name = "Presets",
                    icon = Icons.Rounded.Tune,
                    action = { showPresetsBottomSheet = true }
                ),
                MenuAction(
                    name = "Export",
                    icon = Icons.Rounded.Save,
                    action = {
                        val configText = buildString {
                            TweakItems.TweakItemsList.forEach { tweakItem ->
                                val value = when (tweakItem) {
                                    is FloatTweakItem -> context.sharedPreferences.getFloat(tweakItem.key, tweakItem.defaultValue)
                                    is BooleanTweakItem -> context.sharedPreferences.getBoolean(tweakItem.key, tweakItem.defaultValue)
                                    is StringTweakItem -> context.sharedPreferences.getString(tweakItem.key, tweakItem.defaultValue) ?: tweakItem.defaultValue
                                }
                                appendLine("${tweakItem.key}=$value")
                            }
                        }

                        runCatching {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                val contentValues = android.content.ContentValues().apply {
                                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "liquidglass.txt")
                                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                                    put(
                                        android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                                        DIRECTORY_DOWNLOADS
                                    )
                                }
                                val resolver = context.androidContext.contentResolver
                                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                                uri?.let {
                                    resolver.openOutputStream(it)?.use { os ->
                                        os.write(configText.toByteArray())
                                    }
                                }
                            } else {
                                val file = java.io.File(
                                    getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS),
                                    "liquidglass.txt"
                                )
                                file.writeText(configText)
                            }

                            context.shortToast("Saved liquidglass.txt to Downloads")
                        }.onFailure { e ->
                            context.shortToast("Failed to export: ${e.message}")
                            context.log.error("Failed to export liquidglass.txt", e)
                        }
                    }
                ),
                MenuAction(
                    name = "Reset",
                    icon = Icons.Rounded.Restore,
                    action = {
                        context.sharedPreferences.edit().apply {
                            TweakItems.TweakItemsList.forEach { tweakItem ->
                                when (tweakItem) {
                                    is FloatTweakItem -> putFloat(tweakItem.key, tweakItem.defaultValue)
                                    is BooleanTweakItem -> putBoolean(tweakItem.key, tweakItem.defaultValue)
                                    is StringTweakItem -> putString(tweakItem.key, tweakItem.defaultValue)
                                }
                            }
                            apply()
                        }
                    }
                )
            )
        }
        if (showActionDropdownMenu) {
            DropdownMenuPopup(
                expanded = true,
                onDismissRequest = { showActionDropdownMenu = false }
            ) {
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShape(0, 1)
                ) {
                    actions.fastForEachIndexed { index, menuAction ->
                        DropdownMenuItem(
                            selected = false,
                            onClick = {
                                menuAction.action()
                                showActionDropdownMenu = false
                            },
                            text = { Text(text = menuAction.name) },
                            shapes = MenuDefaults.itemShape(index, actions.size),
                            leadingIcon = {
                                Icon(
                                    imageVector = menuAction.icon,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
        if (showPresetsBottomSheet) {
            LazyColumnBottomSheet(
                onDismiss = { showPresetsBottomSheet = false }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    presetOptions.forEachIndexed { index, presetOption ->
                        Card(
                            shape = cardShape(presetOptions.size, index),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                ),
                                modifier = Modifier
                                    .clickable {
                                        presetOption.action()
                                        showPresetsBottomSheet = false
                                    }
                                    .fillMaxWidth(),
                                headlineContent = { Text(text = presetOption.name) },
                            )
                        }
                    }
                }
            }
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TweakCategoryItems.TweakCategoryItemsList.forEach { category ->
                        val categoryItems = TweakItems.TweakItemsList.filter { it.category == category.key }

                        if (categoryItems.isNotEmpty()) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )

                            categoryItems.forEach { tweakItem ->
                                when (tweakItem) {
                                    is FloatTweakItem -> PreferenceSlider(
                                        key = tweakItem.key,
                                        defaultValue = tweakItem.defaultValue,
                                        valueRange = tweakItem.valueRange,
                                        sharedPreferences = context.sharedPreferences
                                    )
                                    is BooleanTweakItem -> PreferenceSwitch(
                                        key = tweakItem.key,
                                        defaultValue = tweakItem.defaultValue,
                                        sharedPreferences = context.sharedPreferences
                                    )
                                    is StringTweakItem -> PreferenceTextField(
                                        key = tweakItem.key,
                                        defaultValue = tweakItem.defaultValue,
                                        sharedPreferences = context.sharedPreferences
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PreferenceSlider(
        key: String,
        defaultValue: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        sharedPreferences: SharedPreferences
    ) {
        val prefState = sharedPreferences.rememberMutableFloatPreferenceState(key, defaultValue)
        var sliderValue by remember(prefState.value) { mutableStateOf(prefState.value) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = key,
                    modifier = Modifier.fillMaxWidth(0.5f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "%.3f".format(sliderValue),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { prefState.value = sliderValue },
                valueRange = valueRange
            )
        }
    }

    @Composable
    private fun PreferenceSwitch(
        key: String,
        defaultValue: Boolean,
        sharedPreferences: SharedPreferences
    ) {
        val prefState = sharedPreferences.rememberMutableBooleanPreferenceState(key, defaultValue)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = key,
                modifier = Modifier.fillMaxWidth(0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = prefState.value,
                onCheckedChange = { prefState.value = it }
            )
        }
    }

    @Composable
    private fun PreferenceTextField(
        key: String,
        defaultValue: String,
        sharedPreferences: SharedPreferences
    ) {
        val prefState = sharedPreferences.rememberMutableStringPreferenceState(key, defaultValue)
        var textValue by remember(prefState.value) { mutableStateOf(prefState.value) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    prefState.value = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}