/*
 * Copyright (c) 2026 bocajthomas
 *
 * This file is a derivative work of DampedDragAnimation.kt from the AndroidLiquidGlass
 * project by Kyant0 (licensed under the Apache License, Version 2.0)
 * Original Source: https://github.com/Kyant0/AndroidLiquidGlass
 *
 * This modified file is licensed under the GNU General Public License v3.0.
 */

package me.rhunk.snapenhance.ui.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
    val valueAnimationSpecDampingRatio: Float,
    val valueAnimationSpecStiffness: Float,
    val velocityAnimationSpecDampingRatio: Float,
    val velocityAnimationSpecStiffness: Float,
    val velocityAnimationSpecTimes: Float,
    val pressProgressAnimationSpecDampingRatio: Float,
    val pressProgressAnimationSpecStiffness: Float,
    val pressProgressAnimationSpecVisibilityThreshold: Float,
    val scaleXAnimationSpecDampingRatio: Float,
    val scaleXAnimationSpecStiffness: Float,
    val scaleXAnimationSpecVisibilityThreshold: Float,
    val scaleYAnimationSpecDampingRatio: Float,
    val scaleYAnimationSpecStiffness: Float,
    val scaleYAnimationSpecVisibilityThreshold: Float,
    val velocityAnimationInitialValue: Float,
    val velocityAnimationVisibilityThreshold: Float,
    val pressProgressAnimationInitialValue: Float,
    val pressProgressAnimationVisibilityThreshold: Float,
    val scaleXAnimationVisibilityThreshold: Float,
    val scaleYAnimationVisibilityThreshold: Float,
    val pressProgressAnimationValue: Float,
    val releaseThresholdsTimesValue: Float,
    val releaseTargetValue: Float,
    val valueAnimationValue: Float,
    val velocityTrackerOffsetY: Float,
) {
    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val pressProgressAnimation = Animatable(pressProgressAnimationInitialValue, pressProgressAnimationVisibilityThreshold)
    private val scaleXAnimation = Animatable(initialScale, scaleXAnimationVisibilityThreshold)
    private val scaleYAnimation = Animatable(initialScale, scaleYAnimationVisibilityThreshold)
    private val velocityAnimation = Animatable(velocityAnimationInitialValue, velocityAnimationVisibilityThreshold)
    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val progress: Float get() = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        inspectDragGestures(
            onDragStart = { down ->
                onDragStarted(down.position)
                press()
            },
            onDragEnd = {
                onDragStopped()
                release()
            },
            onDragCancel = {
                onDragStopped()
                release()
            }
        ) { change, dragAmount ->
            onDrag(size, dragAmount)
        }
    }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(pressProgressAnimationValue, spring(pressProgressAnimationSpecDampingRatio, pressProgressAnimationSpecStiffness, pressProgressAnimationSpecVisibilityThreshold)) }
            launch { scaleXAnimation.animateTo(pressedScale, spring(scaleXAnimationSpecDampingRatio, scaleXAnimationSpecStiffness, scaleXAnimationSpecVisibilityThreshold)) }
            launch { scaleYAnimation.animateTo(pressedScale, spring(scaleYAnimationSpecDampingRatio, scaleYAnimationSpecStiffness, scaleYAnimationSpecVisibilityThreshold)) }
        }
    }

    fun release() {
        animationScope.launch {
            awaitFrame()
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * releaseThresholdsTimesValue
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(releaseTargetValue, spring(pressProgressAnimationSpecDampingRatio, pressProgressAnimationSpecStiffness, pressProgressAnimationSpecVisibilityThreshold)) }
            launch { scaleXAnimation.animateTo(initialScale, spring(scaleXAnimationSpecDampingRatio, scaleXAnimationSpecStiffness, scaleXAnimationSpecVisibilityThreshold)) }
            launch { scaleYAnimation.animateTo(initialScale, spring(scaleYAnimationSpecDampingRatio, scaleYAnimationSpecStiffness, scaleYAnimationSpecVisibilityThreshold)) }
        }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            launch { valueAnimation.animateTo(targetValue, spring(valueAnimationSpecDampingRatio, valueAnimationSpecStiffness, visibilityThreshold)) { updateVelocity() } }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val targetValue = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(targetValue, spring(valueAnimationSpecDampingRatio, valueAnimationSpecStiffness, visibilityThreshold)) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(valueAnimationValue, spring(velocityAnimationSpecDampingRatio, velocityAnimationSpecStiffness, visibilityThreshold * velocityAnimationSpecTimes)) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            System.currentTimeMillis(),
            Offset(value, velocityTrackerOffsetY)
        )
        val targetVelocity = velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, spring(velocityAnimationSpecDampingRatio, velocityAnimationSpecStiffness, visibilityThreshold * velocityAnimationSpecTimes)) }
    }
}