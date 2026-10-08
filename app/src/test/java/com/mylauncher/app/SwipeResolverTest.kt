package com.mylauncher.app

import com.mylauncher.app.launcher.gestures.SwipeDirection
import com.mylauncher.app.launcher.gestures.SwipeResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeResolverTest {
    @Test fun swipeUpIsDetected() {
        assertEquals(SwipeDirection.UP, SwipeResolver.resolve(-200f, 100f))
        assertEquals(SwipeDirection.UP, SwipeResolver.resolve(-100f, 100f))
    }

    @Test fun swipeDownIsDetected() {
        assertEquals(SwipeDirection.DOWN, SwipeResolver.resolve(200f, 100f))
    }

    @Test fun shortDragsDoNothing() {
        assertNull(SwipeResolver.resolve(40f, 100f))
        assertNull(SwipeResolver.resolve(-40f, 100f))
        assertNull(SwipeResolver.resolve(0f, 100f))
    }
}
