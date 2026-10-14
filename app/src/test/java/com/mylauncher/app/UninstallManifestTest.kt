package com.mylauncher.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the bug where uninstalling failed because the manifest lacked REQUEST_DELETE_PACKAGES. */
class UninstallManifestTest {

    private fun manifest(): String {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        )
        return candidates.first { it.exists() }.readText()
    }

    @Test
    fun manifestDeclaresRequestDeletePackages() {
        assertTrue(manifest().contains("android.permission.REQUEST_DELETE_PACKAGES"))
    }

    @Test
    fun manifestDoesNotUseBroadPackageVisibility() {
        assertTrue(!manifest().contains("QUERY_ALL_PACKAGES\" />"))
    }
}
