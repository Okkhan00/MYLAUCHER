package com.mylauncher.app

import com.mylauncher.app.device.DeviceFormat
import com.mylauncher.app.device.StorageInfo
import com.mylauncher.app.performance.PerformanceMode
import com.mylauncher.app.performance.PerformanceProfile
import com.mylauncher.app.widgets.HostedWidget
import com.mylauncher.app.widgets.WidgetCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetsPerformanceDeviceTest {
    @Test fun widgetCodecRoundTripAndHeal() {
        val list = listOf(HostedWidget(3, 2), HostedWidget(7, 4))
        assertEquals(list, WidgetCodec.decode(WidgetCodec.encode(list)))
        assertEquals(
            listOf(HostedWidget(5, 4), HostedWidget(6, 1)),
            WidgetCodec.decode("5\t99\n5\t1\nx\t2\n-3\t2\n6\t0\nbad\n7"),
        )
        assertTrue(WidgetCodec.decode(null).isEmpty())
    }

    @Test fun widgetOpsNeverDuplicateAndClamp() {
        var l = emptyList<HostedWidget>()
        l = WidgetCodec.add(l, HostedWidget(1)); l = WidgetCodec.add(l, HostedWidget(1)); l = WidgetCodec.add(l, HostedWidget(2, 9))
        assertEquals(listOf(HostedWidget(1, 2), HostedWidget(2, 4)), l)
        l = WidgetCodec.move(l, 2, -5)
        assertEquals(listOf(2, 1), l.map { it.id })
        l = WidgetCodec.resize(l, 1, 10)
        assertEquals(4, l.single { it.id == 1 }.rows)
        l = WidgetCodec.resize(l, 1, -10)
        assertEquals(1, l.single { it.id == 1 }.rows)
        l = WidgetCodec.remove(l, 2)
        assertEquals(listOf(1), l.map { it.id })
        assertEquals(l, WidgetCodec.move(l, 99, 1))
    }

    @Test fun rowsForProviderHeight() {
        assertEquals(1, WidgetCodec.rowsFor(40, 88))
        assertEquals(2, WidgetCodec.rowsFor(110, 88))
        assertEquals(4, WidgetCodec.rowsFor(1000, 88))
        assertEquals(HostedWidget.DEFAULT_ROWS, WidgetCodec.rowsFor(0, 88))
    }

    @Test fun performanceProfiles() {
        val b = PerformanceProfile.of(PerformanceMode.BALANCED)
        val s = PerformanceProfile.of(PerformanceMode.BATTERY_SAVER)
        val m = PerformanceProfile.of(PerformanceMode.SMOOTH)
        assertTrue(b.allowAnimations && b.allowEffects)
        assertFalse(s.allowAnimations); assertFalse(s.allowEffects)
        assertTrue(m.allowAnimations && m.allowEffects)
        assertTrue(s.iconCacheSize < b.iconCacheSize && b.iconCacheSize < m.iconCacheSize)
        assertTrue(s.refreshDebounceMs > b.refreshDebounceMs)
    }

    @Test fun byteFormatting() {
        assertEquals("0 B", DeviceFormat.bytes(0))
        assertEquals("1023 B", DeviceFormat.bytes(1023))
        assertEquals("1.5 KB", DeviceFormat.bytes(1536))
        assertEquals("1.0 GB", DeviceFormat.bytes(1024L * 1024 * 1024))
        assertEquals("unknown", DeviceFormat.bytes(-1))
        assertEquals(50, DeviceFormat.percent(50, 100))
        assertNull(DeviceFormat.percent(1, 0))
        assertEquals(60, StorageInfo(100, 40).usedBytes.toInt())
    }
}
