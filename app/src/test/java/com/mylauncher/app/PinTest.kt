package com.mylauncher.app

import com.mylauncher.app.data.model.SecuritySettings
import com.mylauncher.app.security.pin.PinHasher
import com.mylauncher.app.security.pin.PinThrottle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinTest {
    private val fastIterations = 1_000

    @Test fun correctPinVerifiesAndWrongPinDoesNot() {
        val hashed = PinHasher.hash("1234", iterations = fastIterations)
        assertTrue(PinHasher.verify("1234", hashed.hash, hashed.salt, fastIterations))
        assertFalse(PinHasher.verify("1235", hashed.hash, hashed.salt, fastIterations))
        assertFalse(PinHasher.verify("", hashed.hash, hashed.salt, fastIterations))
    }

    @Test fun hashNeverContainsThePlainPin() {
        val hashed = PinHasher.hash("987654", iterations = fastIterations)
        assertFalse(hashed.hash.contains("987654"))
        assertFalse(hashed.salt.contains("987654"))
    }

    @Test fun samePinGetsDifferentSalts() {
        val a = PinHasher.hash("1234", iterations = fastIterations)
        val b = PinHasher.hash("1234", iterations = fastIterations)
        assertNotEquals(a.salt, b.salt)
        assertNotEquals(a.hash, b.hash)
    }

    @Test fun corruptStoredValuesFailSafely() {
        assertFalse(PinHasher.verify("1234", "not base64 !!", "also bad", fastIterations))
        assertFalse(PinHasher.verify("1234", "", "", fastIterations))
    }

    @Test fun pinValidation() {
        assertTrue(PinHasher.isValidPin("1234"))
        assertTrue(PinHasher.isValidPin("12345678"))
        assertFalse(PinHasher.isValidPin("123"))
        assertFalse(PinHasher.isValidPin("123456789"))
        assertFalse(PinHasher.isValidPin("12a4"))
        assertFalse(PinHasher.isValidPin(""))
    }

    @Test fun lockIsOnlyEnabledWhenHashAndSaltExist() {
        assertFalse(SecuritySettings().lockEnabled)
        assertFalse(SecuritySettings(pinHash = "h").lockEnabled)
        assertTrue(SecuritySettings(pinHash = "h", pinSalt = "s").lockEnabled)
    }

    @Test fun throttleBlocksAfterFiveFailuresAndResetsOnSuccess() {
        val throttle = PinThrottle()
        repeat(4) { throttle.onFailure(0L) }
        assertEquals(0L, throttle.remainingMs(0L))
        throttle.onFailure(0L)
        assertEquals(30_000L, throttle.remainingMs(0L))
        assertEquals(20_000L, throttle.remainingMs(10_000L))
        assertEquals(0L, throttle.remainingMs(40_000L))
        throttle.onFailure(40_000L)
        assertEquals(60_000L, throttle.remainingMs(40_000L))
        throttle.onSuccess()
        assertEquals(0L, throttle.remainingMs(40_000L))
        repeat(4) { throttle.onFailure(50_000L) }
        assertEquals(0L, throttle.remainingMs(50_000L))
    }
}
