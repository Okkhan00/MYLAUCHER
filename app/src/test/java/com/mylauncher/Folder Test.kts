package com.mylauncher.app

import com.mylauncher.app.data.model.Folder
import com.mylauncher.app.data.model.folderIdOf
import com.mylauncher.app.data.model.folderToken
import com.mylauncher.app.data.model.isFolderToken
import com.mylauncher.app.data.preferences.FolderCodec
import com.mylauncher.app.data.preferences.FolderOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderTest {
    @Test fun codecRoundTripsNamesWithSpacesAndSymbols() {
        val folders = listOf(
            Folder("1", "Work & Study", listOf("com.a", "com.b")),
            Folder("2", "Caf\u00E9\tTab\nLine", emptyList()),
        )
        assertEquals(folders, FolderCodec.decode(FolderCodec.encode(folders)))
    }

    @Test fun codecSurvivesCorruptInput() {
        assertTrue(FolderCodec.decode(null).isEmpty())
        assertTrue(FolderCodec.decode("").isEmpty())
        assertEquals(listOf("ok"), FolderCodec.decode("garbage\n\nok\tName\tcom.a").map { it.id })
        assertEquals("", FolderCodec.decode("id\t%ZZ\t").first().name)
    }

    @Test fun tokensAreUnambiguous() {
        assertTrue(isFolderToken(folderToken("abc")))
        assertEquals("abc", folderIdOf(folderToken("abc")))
        assertFalse(isFolderToken("com.example.app"))
    }

    @Test fun createRenameDeleteAndNameCleaning() {
        var folders = FolderOps.create(emptyList(), "1", "  Social  ")
        assertEquals("Social", folders.single().name)
        folders = FolderOps.create(folders, "1", "Duplicate id")
        assertEquals(1, folders.size)
        folders = FolderOps.rename(folders, "1", "x".repeat(60))
        assertEquals(FolderOps.MAX_NAME_LENGTH, folders.single().name.length)
        folders = FolderOps.rename(folders, "1", "   ")
        assertEquals("Folder", folders.single().name)
        assertTrue(FolderOps.delete(folders, "1").isEmpty())
    }

    @Test fun anAppLivesInOnlyOneFolder() {
        var folders = FolderOps.create(FolderOps.create(emptyList(), "1", "A"), "2", "B")
        folders = FolderOps.addApp(folders, "1", "pkg")
        folders = FolderOps.addApp(folders, "1", "pkg")
        assertEquals(listOf("pkg"), folders[0].appIds)
        folders = FolderOps.addApp(folders, "2", "pkg")
        assertTrue(folders[0].appIds.isEmpty())
        assertEquals(listOf("pkg"), folders[1].appIds)
    }

    @Test fun addingToUnknownFolderChangesNothing() {
        val folders = listOf(Folder("1", "A", listOf("p")))
        assertEquals(folders, FolderOps.addApp(folders, "missing", "p"))
    }

    @Test fun removeMoveAndReorderInsideFolder() {
        var folders = listOf(Folder("1", "A", listOf("a", "b", "c")))
        folders = FolderOps.moveApp(folders, "1", "c", -2)
        assertEquals(listOf("c", "a", "b"), folders[0].appIds)
        folders = FolderOps.reorderApps(folders, "1", listOf("b", "c", "a"))
        assertEquals(listOf("b", "c", "a"), folders[0].appIds)
        folders = FolderOps.removeApp(folders, "1", "c")
        assertEquals(listOf("b", "a"), folders[0].appIds)
    }
}
