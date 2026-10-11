/*
 * Copyright (c) 2026 bocajthomas
 *
 * This file is a derivative work of LiquidBottomTabs.kt from the AndroidLiquidGlass
 * project by Kyant0 (licensed under the Apache License, Version 2.0)
 * Original Source: https://github.com/Kyant0/AndroidLiquidGlass
 *
 * This modified file is licensed under the GNU General Public License v3.0.
 */

package me.rhunk.snapenhance.common.ui.liquidglass

import android.content.SharedPreferences
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.config.impl.RootConfig
import me.rhunk.snapenhance.common.ui.rememberMutableBooleanPreferenceState
import me.rhunk.snapenhance.common.ui.rememberMutableFloatPreferenceState
import kotlin.math.abs
import kotlin.math.sign

@Composable
fun ManagerLiquidNavigationBar(
    sharedPreferences: SharedPreferences,
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val rowOneContainerColorAlphaValue by sharedPreferences.rememberMutableFloatPreferenceState("rowOneContainerColorAlphaValue", 1f)
    val rowTwoContainerColorAlphaValue by sharedPreferences.rememberMutableFloatPreferenceState("rowTwoContainerColorAlphaValue", 1f)

    // tabWidth Values
    val tabWidthMinusValue by sharedPreferences.rememberMutableFloatPreferenceState("tabWidthMinusValue", 8f)

    // offsetAnimation Values
    val offsetAnimationValue by sharedPreferences.rememberMutableFloatPreferenceState("offsetAnimationValue", 0f)

    // panelOffsetDensity Values
    val panelOffsetDensityValue by sharedPreferences.rememberMutableFloatPreferenceState("panelOffsetDensityValue", 4f)

    // dampedDragAnimation Values
    val dampedDragAnimationVisibilityThresholdValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationVisibilityThresholdValue", 0.001f)
    val dampedDragAnimationInitialScaleValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationInitialScaleValue", 1f)
    val dampedDragAnimationFirstPressedScaleValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationFirstPressedScaleValue", 78f)
    val dampedDragAnimationSecondPressedScaleValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationSecondPressedScaleValue", 56f)
    val dampedDragAnimationOnDragStoppedOffsetAnimationTargetValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationOnDragStoppedOffsetAnimationTargetValue", 0f)
    val dampedDragAnimationOnDragStoppedOffsetAnimationDampingRatioValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationOnDragStoppedOffsetAnimationDampingRatioValue", 1f)
    val dampedDragAnimationOnDragStoppedOffsetAnimationStiffnessValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationOnDragStoppedOffsetAnimationStiffnessValue", 300f)
    val dampedDragAnimationOnDragStoppedOffsetAnimationVisibilityThresholdValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationOnDragStoppedOffsetAnimationVisibilityThresholdValue", 0.5f)
    val dampedDragAnimationValueAnimationSpecDampingRatioValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationValueAnimationSpecDampingRatioValue", 1f)
    val dampedDragAnimationValueAnimationSpecStiffnessValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationValueAnimationSpecStiffnessValue", 1000f)
    val dampedDragAnimationVelocityAnimationSpecDampingRatioValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationVelocityAnimationSpecDampingRatioValue", 0.5f)
    val dampedDragAnimationVelocityAnimationSpecStiffnessValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationVelocityAnimationSpecStiffnessValue", 300f)
    val dampedDragAnimationVelocityAnimationSpecTimesValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationVelocityAnimationSpecTimesValue", 10f)
    val dampedDragAnimationPressProgressAnimationSpecDampingRatioValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationPressProgressAnimationSpecDampingRatioValue", 1f)
    val dampedDragAnimationPressProgressAnimationSpecStiffnessValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationPressProgressAnimationSpecStiffnessValue", 1000f)
    val dampedDragAnimationPressProgressAnimationSpecVisibilityThresholdValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationPressProgressAnimationSpecVisibilityThresholdValue", 0.001f)
    val dampedDragAnimationScaleXAnimationSpecDampingRatioValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationScaleXAnimationSpecDampingRatioValue", 0.6f)
    val dampedDragAnimationScaleXAnimationSpecStiffnessValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationScaleXAnimationSpecStiffnessValue", 250f)
    val dampedDragAnimationScaleXAnimationSpecVisibilityThresholdValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationScaleXAnimationSpecVisibilityThresholdValue", 0.001f)
    val dampedDragAnimationScaleYAnimationSpecDampingRatioValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationScaleYAnimationSpecDampingRatioValue", 0.7f)
    val dampedDragAnimationScaleYAnimationSpecStiffnessValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationScaleYAnimationSpecStiffnessValue", 250f)
    val dampedDragAnimationScaleYAnimationSpecVisibilityThresholdValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationScaleYAnimationSpecVisibilityThresholdValue", 0.001f)
    val dampedDragAnimationVelocityAnimationInitialValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationVelocityAnimationInitialValue", 0f)
    val dampedDragAnimationVelocityAnimationVisibilityThresholdValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationVelocityAnimationVisibilityThresholdValue", 5f)
    val dampedDragAnimationPressProgressAnimationInitialValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationPressProgressAnimationInitialValue", 0f)
    val dampedDragAnimationPressProgressAnimationVisibilityThresholdValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationPressProgressAnimationVisibilityThresholdValue", 0.001f)
    val dampedDragAnimationScaleXAnimationVisibilityThresholdValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationScaleXAnimationVisibilityThresholdValue", 0.001f)
    val dampedDragAnimationScaleYAnimationVisibilityThresholdValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationScaleYAnimationVisibilityThresholdValue", 0.001f)
    val dampedDragAnimationPressProgressAnimationValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationPressProgressAnimationValue", 1f)
    val dampedDragAnimationReleaseThresholdsTimesValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationReleaseThresholdsTimesValue", 0.025f)
    val dampedDragAnimationReleaseTargetValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationReleaseTargetValue", 0f)
    val dampedDragAnimationValueAnimationValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationValueAnimationValue", 0f)
    val dampedDragAnimationVelocityTrackerOffsetYValue by sharedPreferences.rememberMutableFloatPreferenceState("dampedDragAnimationVelocityTrackerOffsetYValue", 0f)

    // interactiveHighlight Values
    val interactiveHighlightPositionOffsetXIfTrueDampedDragAnimationPlusValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightPositionOffsetXIfTrueDampedDragAnimationPlusValue", 0.5f)
    val interactiveHighlightPositionOffsetXElseDampedDragAnimationPlusValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightPositionOffsetXElseDampedDragAnimationPlusValue", 0.5f)
    val interactiveHighlightPositionOffsetYDivideValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightPositionOffsetYDivideValue", 2f)
    val interactiveHighlightPressProgressAnimationSpecDampingRatioValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightPressProgressAnimationSpecDampingRatioValue", 0.5f)
    val interactiveHighlightPressProgressAnimationSpecStiffnessValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightPressProgressAnimationSpecStiffnessValue", 300f)
    val interactiveHighlightPressProgressAnimationSpecVisibilityThresholdValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightPressProgressAnimationSpecVisibilityThresholdValue", 0.001f)
    val interactiveHighlightPositionAnimationSpecDampingRatioValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightPositionAnimationSpecDampingRatioValue", 0.5f)
    val interactiveHighlightPositionAnimationSpecStiffnessValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightPositionAnimationSpecStiffnessValue", 300f)
    val interactiveHighlightPressProgressAnimationInitialValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightPressProgressAnimationInitialValue", 0f)
    val interactiveHighlightPressProgressAnimationVisibilityThresholdValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightPressProgressAnimationVisibilityThresholdValue", 0.001f)
    val interactiveHighlightShaderNotNullDrawRectColorWhiteAlphaValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightShaderNotNullDrawRectColorWhiteAlphaValue", 0.08f)
    val interactiveHighlightShaderSetColorUniformWhiteAlphaValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightShaderSetColorUniformWhiteAlphaValue", 0.15f)
    val interactiveHighlightShaderSetFloatUniformRadiusTimesValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightShaderSetFloatUniformRadiusTimesValue", 1.5f)
    val interactiveHighlightShaderIsNullDrawRectColorWhiteAlphaValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightShaderIsNullDrawRectColorWhiteAlphaValue", 0.25f)
    val interactiveHighlightOnDragStartTargetValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightOnDragStartTargetValue", 1f)
    val interactiveHighlightOnDragEndTargetValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightOnDragEndTargetValue", 0f)
    val interactiveHighlightOnDragCancelTargetValue by sharedPreferences.rememberMutableFloatPreferenceState("interactiveHighlightOnDragCancelTargetValue", 0f)

    // First Row Values
    val rowOneModifierEnableGraphicsLayer by sharedPreferences.rememberMutableBooleanPreferenceState("rowOneModifierEnableGraphicsLayer", false)
    val rowOneModifierDrawBackdropShapeValue by sharedPreferences.rememberMutableFloatPreferenceState("rowOneModifierDrawBackdropShapeValue", 32f)
    val rowOneModifierDrawBackdropEffectsBlurRadiusValue by sharedPreferences.rememberMutableFloatPreferenceState("rowOneModifierDrawBackdropEffectsBlurRadiusValue", 8f)
    val rowOneModifierDrawBackdropEffectsLensRefractionHeightValue by sharedPreferences.rememberMutableFloatPreferenceState("rowOneModifierDrawBackdropEffectsLensRefractionHeightValue", 24f)
    val rowOneModifierDrawBackdropEffectsLensRefractionAmountValue by sharedPreferences.rememberMutableFloatPreferenceState("rowOneModifierDrawBackdropEffectsLensRefractionAmountValue", 24f)
    val rowOneModifierDrawBackdropEffectsLensEnableDepthEffect by sharedPreferences.rememberMutableBooleanPreferenceState("rowOneModifierDrawBackdropEffectsLensEnableDepthEffect", false)
    val rowOneModifierDrawBackdropEffectsLensEnableChromaticAberration by sharedPreferences.rememberMutableBooleanPreferenceState("rowOneModifierDrawBackdropEffectsLensEnableChromaticAberration", false)
    val rowOneModifierDrawBackdropLayerBlockScaleLerpStartValue by sharedPreferences.rememberMutableFloatPreferenceState("rowOneModifierDrawBackdropLayerBlockScaleLerpStartValue", 1f)
    val rowOneModifierDrawBackdropLayerBlockScaleLerpStopValue by sharedPreferences.rememberMutableFloatPreferenceState("rowOneModifierDrawBackdropLayerBlockScaleLerpStopValue", 1f)
    val rowOneModifierDrawBackdropLayerBlockScaleLerpPlusValue by sharedPreferences.rememberMutableFloatPreferenceState("rowOneModifierDrawBackdropLayerBlockScaleLerpPlusValue", 16f)
    val rowOneModifierHeightValue by sharedPreferences.rememberMutableFloatPreferenceState("rowOneModifierHeightValue", 64f)
    val rowOneModifierPaddingValue by sharedPreferences.rememberMutableFloatPreferenceState("rowOneModifierPaddingValue", 4f)

    // Second Row Values
    val rowTwoLocalLiquidBottomTabScaleLerpStartValue by sharedPreferences.rememberMutableFloatPreferenceState("rowTwoLocalLiquidBottomTabScaleLerpStartValue", 1f)
    val rowTwoLocalLiquidBottomTabScaleLerpStopValue by sharedPreferences.rememberMutableFloatPreferenceState("rowTwoLocalLiquidBottomTabScaleLerpStopValue", 1.1f)
    val rowTwoModifierAlphaValue by sharedPreferences.rememberMutableFloatPreferenceState("rowTwoModifierAlphaValue", 0f)
    val rowTwoModifierDrawBackdropShapeValue by sharedPreferences.rememberMutableFloatPreferenceState("rowTwoModifierDrawBackdropShapeValue", 32f)
    val rowTwoModifierDrawBackdropEffectsBlurRadiusValue by sharedPreferences.rememberMutableFloatPreferenceState("rowTwoModifierDrawBackdropEffectsBlurRadiusValue", 8f)
    val rowTwoModifierDrawBackdropEffectsLensRefractionHeightValue by sharedPreferences.rememberMutableFloatPreferenceState("rowTwoModifierDrawBackdropEffectsLensRefractionHeightValue", 24f)
    val rowTwoModifierDrawBackdropEffectsLensRefractionAmountValue by sharedPreferences.rememberMutableFloatPreferenceState("rowTwoModifierDrawBackdropEffectsLensRefractionAmountValue", 24f)
    val rowTwoModifierDrawBackdropEffectsLensEnableDepthEffect by sharedPreferences.rememberMutableBooleanPreferenceState("rowTwoModifierDrawBackdropEffectsLensEnableDepthEffect", false)
    val rowTwoModifierDrawBackdropEffectsLensEnableChromaticAberration by sharedPreferences.rememberMutableBooleanPreferenceState("rowTwoModifierDrawBackdropEffectsLensEnableChromaticAberration", true)
    val rowTwoModifierHeightValue by sharedPreferences.rememberMutableFloatPreferenceState("rowTwoModifierHeightValue", 56f)
    val rowTwoModifierPaddingHorizontalValue by sharedPreferences.rememberMutableFloatPreferenceState("rowTwoModifierPaddingHorizontalValue", 4f)

    // Box Values
    val boxModifierPaddingHorizontalValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierPaddingHorizontalValue", 4f)
    val boxModifierGraphicsLayerAlphaIfTrueValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierGraphicsLayerAlphaIfTrueValue", 0f)
    val boxModifierGraphicsLayerAlphaIfFalseValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierGraphicsLayerAlphaIfFalseValue", 1f)
    val boxModifierGraphicsLayerTranslationXElsePlusValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierGraphicsLayerTranslationXElsePlusValue", 1f)
    val boxModifierDrawBackdropShapeValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropShapeValue", 32f)
    val boxModifierDrawBackdropEffectsLensRefractionHeightValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropEffectsLensRefractionHeightValue", 12f)
    val boxModifierDrawBackdropEffectsLensRefractionAmountValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropEffectsLensRefractionAmountValue", 12f)
    val boxModifierDrawBackdropEffectsLensEnableDepthEffect by sharedPreferences.rememberMutableBooleanPreferenceState("boxModifierDrawBackdropEffectsLensEnableDepthEffect", false)
    val boxModifierDrawBackdropEffectsLensEnableChromaticAberration by sharedPreferences.rememberMutableBooleanPreferenceState("boxModifierDrawBackdropEffectsLensEnableChromaticAberration", true)
    val boxModifierDrawBackdropInnerShadowRadiusValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropInnerShadowRadiusValue", 8f)
    val boxModifierDrawBackdropLayerBlockVelocityDivideValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropLayerBlockVelocityDivideValue", 10f)
    val boxModifierDrawBackdropLayerBlockScaleXNumberValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropLayerBlockScaleXNumberValue", 1f)
    val boxModifierDrawBackdropLayerBlockScaleXVelocityTimesValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropLayerBlockScaleXVelocityTimesValue", 0.75f)
    val boxModifierDrawBackdropLayerBlockScaleXFastCoerceInFirstValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropLayerBlockScaleXFastCoerceInFirstValue", -0.2f)
    val boxModifierDrawBackdropLayerBlockScaleXFastCoerceInSecondValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropLayerBlockScaleXFastCoerceInSecondValue", 0.2f)
    val boxModifierDrawBackdropLayerBlockScaleYNumberValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropLayerBlockScaleYNumberValue", 1f)
    val boxModifierDrawBackdropLayerBlockScaleYVelocityTimesValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropLayerBlockScaleYVelocityTimesValue", 0.25f)
    val boxModifierDrawBackdropLayerBlockScaleYFastCoerceInFirstValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropLayerBlockScaleYFastCoerceInFirstValue", -0.2f)
    val boxModifierDrawBackdropLayerBlockScaleYFastCoerceInSecondValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropLayerBlockScaleYFastCoerceInSecondValue", 0.2f)
    val boxModifierDrawBackdropOnDrawSurfaceFirstDrawRectIfLightThemeColorBlackAlphaValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropOnDrawSurfaceFirstDrawRectIfLightThemeColorBlackAlphaValue", 0.1f)
    val boxModifierDrawBackdropOnDrawSurfaceFirstDrawRectElseColorWhiteAlphaValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropOnDrawSurfaceFirstDrawRectElseColorWhiteAlphaValue", 0.1f)
    val boxModifierDrawBackdropOnDrawSurfaceSecondDrawRectAlphaValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropOnDrawSurfaceSecondDrawRectAlphaValue", 1f)
    val boxModifierDrawBackdropOnDrawSurfaceSecondDrawRectColorBlackAlphaValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierDrawBackdropOnDrawSurfaceSecondDrawRectColorBlackAlphaValue", 0.03f)
    val boxModifierHeightValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierHeightValue", 56f)
    val boxModifierFillMaxWidthFractionValue by sharedPreferences.rememberMutableFloatPreferenceState("boxModifierFillMaxWidthFractionValue", 1f)

    val isLightTheme = !isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = colorScheme.primary
    val rowOneContainerColor = colorScheme.surfaceContainer.copy(alpha = rowOneContainerColorAlphaValue)
    val rowTwoContainerColor = colorScheme.surfaceContainer.copy(alpha = rowTwoContainerColorAlphaValue)
    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - tabWidthMinusValue.dp.toPx()) / tabsCount
        }

        val offsetAnimation = remember { Animatable(offsetAnimationValue) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    panelOffsetDensityValue.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val animationScope = rememberCoroutineScope()
        var currentIndex by remember(selectedTabIndex) {
            mutableIntStateOf(selectedTabIndex())
        }

        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = dampedDragAnimationVisibilityThresholdValue,
                initialScale = dampedDragAnimationInitialScaleValue,
                pressedScale = dampedDragAnimationFirstPressedScaleValue / dampedDragAnimationSecondPressedScaleValue,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            dampedDragAnimationOnDragStoppedOffsetAnimationTargetValue,
                            spring(dampedDragAnimationOnDragStoppedOffsetAnimationDampingRatioValue, dampedDragAnimationOnDragStoppedOffsetAnimationStiffnessValue, dampedDragAnimationOnDragStoppedOffsetAnimationVisibilityThresholdValue)
                        )
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                },
                valueAnimationSpecDampingRatio = dampedDragAnimationValueAnimationSpecDampingRatioValue,
                valueAnimationSpecStiffness = dampedDragAnimationValueAnimationSpecStiffnessValue,
                velocityAnimationSpecDampingRatio = dampedDragAnimationVelocityAnimationSpecDampingRatioValue,
                velocityAnimationSpecStiffness = dampedDragAnimationVelocityAnimationSpecStiffnessValue,
                velocityAnimationSpecTimes = dampedDragAnimationVelocityAnimationSpecTimesValue,
                pressProgressAnimationSpecDampingRatio = dampedDragAnimationPressProgressAnimationSpecDampingRatioValue,
                pressProgressAnimationSpecStiffness = dampedDragAnimationPressProgressAnimationSpecStiffnessValue,
                pressProgressAnimationSpecVisibilityThreshold = dampedDragAnimationPressProgressAnimationSpecVisibilityThresholdValue,
                scaleXAnimationSpecDampingRatio = dampedDragAnimationScaleXAnimationSpecDampingRatioValue,
                scaleXAnimationSpecStiffness = dampedDragAnimationScaleXAnimationSpecStiffnessValue,
                scaleXAnimationSpecVisibilityThreshold = dampedDragAnimationScaleXAnimationSpecVisibilityThresholdValue,
                scaleYAnimationSpecDampingRatio = dampedDragAnimationScaleYAnimationSpecDampingRatioValue,
                scaleYAnimationSpecStiffness = dampedDragAnimationScaleYAnimationSpecStiffnessValue,
                scaleYAnimationSpecVisibilityThreshold = dampedDragAnimationScaleYAnimationSpecVisibilityThresholdValue,
                velocityAnimationInitialValue = dampedDragAnimationVelocityAnimationInitialValue,
                velocityAnimationVisibilityThreshold = dampedDragAnimationVelocityAnimationVisibilityThresholdValue,
                pressProgressAnimationInitialValue = dampedDragAnimationPressProgressAnimationInitialValue,
                pressProgressAnimationVisibilityThreshold = dampedDragAnimationPressProgressAnimationVisibilityThresholdValue,
                scaleXAnimationVisibilityThreshold = dampedDragAnimationScaleXAnimationVisibilityThresholdValue,
                scaleYAnimationVisibilityThreshold = dampedDragAnimationScaleYAnimationVisibilityThresholdValue,
                pressProgressAnimationValue = dampedDragAnimationPressProgressAnimationValue,
                releaseThresholdsTimesValue = dampedDragAnimationReleaseThresholdsTimesValue,
                releaseTargetValue = dampedDragAnimationReleaseTargetValue,
                valueAnimationValue = dampedDragAnimationValueAnimationValue,
                velocityTrackerOffsetY = dampedDragAnimationVelocityTrackerOffsetYValue,
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    currentIndex = index
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }
        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, offset ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + interactiveHighlightPositionOffsetXIfTrueDampedDragAnimationPlusValue) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + interactiveHighlightPositionOffsetXElseDampedDragAnimationPlusValue) * tabWidth + panelOffset,
                        size.height / interactiveHighlightPositionOffsetYDivideValue
                    )
                },
                pressProgressAnimationSpecDampingRatio = interactiveHighlightPressProgressAnimationSpecDampingRatioValue,
                pressProgressAnimationSpecStiffness = interactiveHighlightPressProgressAnimationSpecStiffnessValue,
                pressProgressAnimationSpecVisibilityThreshold = interactiveHighlightPressProgressAnimationSpecVisibilityThresholdValue,
                positionAnimationSpecDampingRatio = interactiveHighlightPositionAnimationSpecDampingRatioValue,
                positionAnimationSpecStiffness = interactiveHighlightPositionAnimationSpecStiffnessValue,
                pressProgressAnimationInitialValue = interactiveHighlightPressProgressAnimationInitialValue,
                pressProgressAnimationVisibilityThreshold = interactiveHighlightPressProgressAnimationVisibilityThresholdValue,
                shaderNotNullDrawRectColorWhiteAlpha = interactiveHighlightShaderNotNullDrawRectColorWhiteAlphaValue,
                shaderSetColorUniformWhiteAlpha = interactiveHighlightShaderSetColorUniformWhiteAlphaValue,
                shaderSetFloatUniformRadiusTimes = interactiveHighlightShaderSetFloatUniformRadiusTimesValue,
                shaderIsNullDrawRectColorWhiteAlpha = interactiveHighlightShaderIsNullDrawRectColorWhiteAlphaValue,
                onDragStartTargetValue = interactiveHighlightOnDragStartTargetValue,
                onDragEndTargetValue = interactiveHighlightOnDragEndTargetValue,
                onDragCancelTargetValue = interactiveHighlightOnDragCancelTargetValue,
            )
        }

        // Background
        Row(
            Modifier
                .then(
                    if (rowOneModifierEnableGraphicsLayer) {
                        Modifier.graphicsLayer { translationX = panelOffset }
                    } else {
                        Modifier
                    }
                )
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(rowOneModifierDrawBackdropShapeValue.dp) },
                    effects = {
                        vibrancy()
                        blur(rowOneModifierDrawBackdropEffectsBlurRadiusValue.dp.toPx())
                        lens(
                            rowOneModifierDrawBackdropEffectsLensRefractionHeightValue.dp.toPx(),
                            rowOneModifierDrawBackdropEffectsLensRefractionAmountValue.dp.toPx(),
                            depthEffect = rowOneModifierDrawBackdropEffectsLensEnableDepthEffect,
                            chromaticAberration = rowOneModifierDrawBackdropEffectsLensEnableChromaticAberration
                        )
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(rowOneModifierDrawBackdropLayerBlockScaleLerpStartValue, rowOneModifierDrawBackdropLayerBlockScaleLerpStopValue + rowOneModifierDrawBackdropLayerBlockScaleLerpPlusValue.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(rowOneContainerColor) }
                )
                .then(interactiveHighlight.modifier)
                .height(rowOneModifierHeightValue.dp)
                .fillMaxWidth()
                .padding(rowOneModifierPaddingValue.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        // Secondary Background
        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(rowTwoLocalLiquidBottomTabScaleLerpStartValue, rowTwoLocalLiquidBottomTabScaleLerpStopValue, dampedDragAnimation.pressProgress)
            }
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(rowTwoModifierAlphaValue)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(rowTwoModifierDrawBackdropShapeValue.dp) },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(rowTwoModifierDrawBackdropEffectsBlurRadiusValue.dp.toPx())
                            lens(
                                rowTwoModifierDrawBackdropEffectsLensRefractionHeightValue.dp.toPx() * progress,
                                rowTwoModifierDrawBackdropEffectsLensRefractionAmountValue.dp.toPx() * progress,
                                depthEffect = rowTwoModifierDrawBackdropEffectsLensEnableDepthEffect,
                                chromaticAberration = rowTwoModifierDrawBackdropEffectsLensEnableChromaticAberration
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        onDrawSurface = { drawRect(rowTwoContainerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(rowTwoModifierHeightValue.dp)
                    .fillMaxWidth()
                    .padding(horizontal = rowTwoModifierPaddingHorizontalValue.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        // Bubble Box
        Box(
            Modifier
                .padding(horizontal = boxModifierPaddingHorizontalValue.dp)
                .graphicsLayer {
                    val index = selectedTabIndex()
                    alpha = if (index < 0 || index >= tabsCount) boxModifierGraphicsLayerAlphaIfTrueValue else boxModifierGraphicsLayerAlphaIfFalseValue

                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + boxModifierGraphicsLayerTranslationXElsePlusValue) * tabWidth + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { RoundedCornerShape(boxModifierDrawBackdropShapeValue.dp) },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        lens(
                            boxModifierDrawBackdropEffectsLensRefractionHeightValue.dp.toPx() * progress,
                            boxModifierDrawBackdropEffectsLensRefractionAmountValue.dp.toPx() * progress,
                            depthEffect = boxModifierDrawBackdropEffectsLensEnableDepthEffect,
                            chromaticAberration = boxModifierDrawBackdropEffectsLensEnableChromaticAberration
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(alpha = progress)
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = boxModifierDrawBackdropInnerShadowRadiusValue.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / boxModifierDrawBackdropLayerBlockVelocityDivideValue
                        scaleX /= boxModifierDrawBackdropLayerBlockScaleXNumberValue - (velocity * boxModifierDrawBackdropLayerBlockScaleXVelocityTimesValue).fastCoerceIn(boxModifierDrawBackdropLayerBlockScaleXFastCoerceInFirstValue, boxModifierDrawBackdropLayerBlockScaleXFastCoerceInSecondValue)
                        scaleY *= boxModifierDrawBackdropLayerBlockScaleYNumberValue - (velocity * boxModifierDrawBackdropLayerBlockScaleYVelocityTimesValue).fastCoerceIn(boxModifierDrawBackdropLayerBlockScaleYFastCoerceInFirstValue, boxModifierDrawBackdropLayerBlockScaleYFastCoerceInSecondValue)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            if (isLightTheme) Color.Black.copy(boxModifierDrawBackdropOnDrawSurfaceFirstDrawRectIfLightThemeColorBlackAlphaValue)
                            else Color.White.copy(boxModifierDrawBackdropOnDrawSurfaceFirstDrawRectElseColorWhiteAlphaValue),
                            alpha = boxModifierDrawBackdropOnDrawSurfaceSecondDrawRectAlphaValue - progress
                        )
                        drawRect(Color.Black.copy(alpha = boxModifierDrawBackdropOnDrawSurfaceSecondDrawRectColorBlackAlphaValue * progress))
                    }
                )
                .height(boxModifierHeightValue.dp)
                .fillMaxWidth(boxModifierFillMaxWidthFractionValue / tabsCount)
        )
    }
}

@Composable
fun CoreLiquidNavigationBar(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    primaryColor: Int,
    surfaceContainerColor: Int,
    content: @Composable RowScope.() -> Unit,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor = remember { Color(primaryColor) }
    val rowOneContainerColor = remember { Color(surfaceContainerColor) }.copy(alpha = 1f)
    val rowTwoContainerColor = remember { Color(surfaceContainerColor) }.copy(alpha = 1f)
    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val animationScope = rememberCoroutineScope()
        var currentIndex by remember(selectedTabIndex) {
            mutableIntStateOf(selectedTabIndex())
        }

        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 93f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            0f,
                            spring(
                                1f,
                                300f,
                                0.5f
                            )
                        )
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                },

                valueAnimationSpecDampingRatio = 1f,
                valueAnimationSpecStiffness = 1000f,
                velocityAnimationSpecDampingRatio = 0.5f,
                velocityAnimationSpecStiffness = 300f,
                velocityAnimationSpecTimes = 10f,
                pressProgressAnimationSpecDampingRatio = 1f,
                pressProgressAnimationSpecStiffness = 1000f,
                pressProgressAnimationSpecVisibilityThreshold = 0.001f,
                scaleXAnimationSpecDampingRatio = 0.6f,
                scaleXAnimationSpecStiffness = 250f,
                scaleXAnimationSpecVisibilityThreshold = 0.001f,
                scaleYAnimationSpecDampingRatio = 0.7f,
                scaleYAnimationSpecStiffness = 250f,
                scaleYAnimationSpecVisibilityThreshold = 0.001f,
                velocityAnimationInitialValue = 0f,
                velocityAnimationVisibilityThreshold = 5f,
                pressProgressAnimationInitialValue = 0f,
                pressProgressAnimationVisibilityThreshold = 0.001f,
                scaleXAnimationVisibilityThreshold = 0.001f,
                scaleYAnimationVisibilityThreshold = 0.001f,
                pressProgressAnimationValue = 1f,
                releaseThresholdsTimesValue = 0.025f,
                releaseTargetValue = 0f,
                valueAnimationValue = 0f,
                velocityTrackerOffsetY = 0f,
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    currentIndex = index
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, offset ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                },
                pressProgressAnimationSpecDampingRatio = 0.5f,
                pressProgressAnimationSpecStiffness = 300f,
                pressProgressAnimationSpecVisibilityThreshold = 0.001f,
                positionAnimationSpecDampingRatio = 0.5f,
                positionAnimationSpecStiffness = 300f,
                pressProgressAnimationInitialValue = 0f,
                pressProgressAnimationVisibilityThreshold = 0.001f,
                shaderNotNullDrawRectColorWhiteAlpha = 0.08f,
                shaderSetColorUniformWhiteAlpha = 0.15f,
                shaderSetFloatUniformRadiusTimes = 1.5f,
                shaderIsNullDrawRectColorWhiteAlpha = 0.25f,
                onDragStartTargetValue = 1f,
                onDragEndTargetValue = 0f,
                onDragCancelTargetValue = 0f,
            )
        }

        // Background
        Row(
            Modifier
                // .then(Modifier.graphicsLayer { translationX = panelOffset })
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(32f.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(
                            24.dp.toPx(),
                            24f.dp.toPx(),
                            depthEffect = false,
                            chromaticAberration = false
                        )
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(rowOneContainerColor) }
                )
                .then(interactiveHighlight.modifier)
                .height(64f.dp)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        // Secondary Background
        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(1f, 1.1f, dampedDragAnimation.pressProgress)
            }
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(32f.dp) },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(
                                24f.dp.toPx() * progress,
                                24f.dp.toPx() * progress,
                                depthEffect = false,
                                chromaticAberration = true
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        onDrawSurface = { drawRect(rowTwoContainerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(64.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 4f.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        // Bubble Box
        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    val index = selectedTabIndex()
                    alpha = if (index < 0 || index >= tabsCount) 0f else 1f

                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { RoundedCornerShape(32f.dp) },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        lens(
                            12f.dp.toPx() * progress,
                            12f.dp.toPx() * progress,
                            depthEffect = false,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(alpha = progress)
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 8f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            if (isLightTheme) Color.Black.copy(0.1f)
                            else Color.White.copy(0.1f),
                            alpha = 1f - progress
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    }
                )
                .height(44f.dp)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}