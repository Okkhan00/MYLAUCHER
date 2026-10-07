package com.mylauncher.app

import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.launcher.search.AppSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {
    private fun app(label: String, pkg: String = "com.example.${label.lowercase().replace(" ", "")}") =
        AppInfo(pkg, "$pkg.Main", label)

    private val apps = listOf(
        app("Calculator"),
        app("Chrome"),
        app("Settings", "com.android.settings"),
        app("Smart Home"),
        app("WhatsApp", "com.whatsapp"),
        app("Wallet"),
    )

    @Test fun blankQueryReturnsEverything() {
        assertEquals(apps, AppSearch.filter(apps, ""))
        assertEquals(apps, AppSearch.filter(apps, "   "))
    }

    @Test fun partialMatchIsCaseInsensitive() {
        assertEquals(listOf("WhatsApp"), AppSearch.filter(apps, "wh").map { it.label })
        assertEquals(listOf("WhatsApp"), AppSearch.filter(apps, "WHATS").map { it.label })
        assertEquals(listOf("Settings"), AppSearch.filter(apps, "set").map { it.label })
    }

    @Test fun prefixMatchesRankBeforeContainsMatches() {
        val result = AppSearch.filter(apps, "s").map { it.label }
        assertEquals("Settings", result[0])
        assertEquals("Smart Home", result[1])
    }

    @Test fun wordStartMatchesBeatInnerMatches() {
        // "home" starts the second word of "Smart Home"
        assertEquals(listOf("Smart Home"), AppSearch.filter(apps, "home").map { it.label })
    }

    @Test fun matchesPackageNames() {
        assertEquals(listOf("WhatsApp"), AppSearch.filter(apps, "com.whatsapp").map { it.label })
    }

    @Test fun noMatchReturnsEmptyList() {
        assertTrue(AppSearch.filter(apps, "zzzz").isEmpty())
    }
}
