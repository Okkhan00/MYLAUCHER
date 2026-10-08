package com.mylauncher.app

import com.mylauncher.app.data.preferences.ListCodec
import com.mylauncher.app.data.preferences.ListOps
import org.junit.Assert.assertEquals
import org.junit.Test

class ListCodecTest {
    @Test fun roundTripKeepsOrder() {
        val list = listOf("b.pkg", "a.pkg", "c.pkg")
        assertEquals(list, ListCodec.decode(ListCodec.encode(list)))
    }

    @Test fun decodeHandlesNullEmptyAndBlankEntries() {
        assertEquals(emptyList<String>(), ListCodec.decode(null))
        assertEquals(emptyList<String>(), ListCodec.decode(""))
        assertEquals(listOf("a", "b"), ListCodec.decode("a\n\n  \nb"))
    }

    @Test fun encodeDropsDuplicatesAndBlanks() {
        assertEquals("a\nb", ListCodec.encode(listOf("a", "", "b", "a", " ")))
    }

    @Test fun addNeverDuplicates() {
        assertEquals(listOf("a", "b"), ListOps.add(listOf("a", "b"), "a"))
        assertEquals(listOf("a", "b", "c"), ListOps.add(listOf("a", "b"), "c"))
    }

    @Test fun removeMissingItemIsNoOp() {
        assertEquals(listOf("a"), ListOps.remove(listOf("a"), "zzz"))
        assertEquals(listOf("a"), ListOps.remove(listOf("a", "b"), "b"))
    }

    @Test fun moveShiftsAndClampsAtEdges() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), ListOps.move(list, "a", 1))
        assertEquals(listOf("a", "c", "b"), ListOps.move(list, "c", -1))
        assertEquals(list, ListOps.move(list, "a", -1))
        assertEquals(list, ListOps.move(list, "c", 1))
        assertEquals(list, ListOps.move(list, "missing", 1))
    }

    @Test fun reorderKeepsInvisibleItemsInPlace() {
        // "h1" and "h2" are hidden/uninstalled: they keep their slots while visible items are rearranged.
        val existing = listOf("a", "h1", "b", "c", "h2")
        assertEquals(listOf("c", "h1", "a", "b", "h2"), ListOps.reorder(existing, listOf("c", "a", "b")))
    }

    @Test fun reorderRejectsInconsistentInput() {
        val existing = listOf("a", "b", "c")
        assertEquals(existing, ListOps.reorder(existing, listOf("a", "zzz")))
        assertEquals(listOf("b", "a", "c"), ListOps.reorder(existing, listOf("b", "a")))
    }
}
