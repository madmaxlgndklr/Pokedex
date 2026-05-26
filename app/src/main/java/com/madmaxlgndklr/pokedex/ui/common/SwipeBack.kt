package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.swipeBack(onBack: () -> Unit): Modifier = this.pointerInput(onBack) {
    var startX = 0f
    var totalDrag = 0f
    var triggered = false
    detectHorizontalDragGestures(
        onDragStart = { offset ->
            startX = offset.x
            totalDrag = 0f
            triggered = false
        },
        onHorizontalDrag = { _, amount ->
            totalDrag += amount
            if (!triggered && startX < 120f && totalDrag > 80f) {
                triggered = true
                onBack()
            }
        }
    )
}

fun Modifier.swipeNavigation(
    onBack: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
): Modifier = this.pointerInput(onBack, onSwipeLeft, onSwipeRight) {
    var startX = 0f
    var totalDrag = 0f
    var triggered = false
    detectHorizontalDragGestures(
        onDragStart = { offset ->
            startX = offset.x
            totalDrag = 0f
            triggered = false
        },
        onHorizontalDrag = { _, amount ->
            totalDrag += amount
            if (!triggered) {
                when {
                    startX < 120f && totalDrag > 80f -> { triggered = true; onBack() }
                    totalDrag > 180f                 -> { triggered = true; onSwipeRight() }
                    totalDrag < -180f                -> { triggered = true; onSwipeLeft() }
                }
            }
        }
    )
}
