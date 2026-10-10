package com.mylauncher.app.launcher.gestures

enum class SwipeDirection { UP, DOWN }

/** Turns a total vertical drag distance into a swipe direction. Negative = up. */
object SwipeResolver {
    fun resolve(totalDy: Float, thresholdPx: Float): SwipeDirection? = when {
        totalDy <= -thresholdPx -> SwipeDirection.UP
        totalDy >= thresholdPx -> SwipeDirection.DOWN
        else -> null
    }
}
