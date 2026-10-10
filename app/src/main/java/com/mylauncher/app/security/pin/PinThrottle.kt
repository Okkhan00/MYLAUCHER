package com.mylauncher.app.security.pin

class PinAttempt(val success: Boolean, val blockedSeconds: Int)

/** Slows down guessing: after [freeAttempts] wrong PINs the lock pauses. Kept in memory only. */
class PinThrottle(
    private val freeAttempts: Int = 5,
    private val firstBlockMs: Long = 30_000L,
    private val nextBlockMs: Long = 60_000L,
) {
    private var failures = 0
    private var blockedUntilMs = 0L

    fun remainingMs(nowMs: Long): Long = (blockedUntilMs - nowMs).coerceAtLeast(0L)

    fun onFailure(nowMs: Long) {
        failures++
        if (failures == freeAttempts) blockedUntilMs = nowMs + firstBlockMs
        else if (failures > freeAttempts) blockedUntilMs = nowMs + nextBlockMs
    }

    fun onSuccess() {
        failures = 0
        blockedUntilMs = 0L
    }
}
