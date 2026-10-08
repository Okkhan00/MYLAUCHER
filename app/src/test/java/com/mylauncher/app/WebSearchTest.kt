package com.mylauncher.app

import com.mylauncher.app.launcher.search.SearchEngine
import com.mylauncher.app.launcher.search.WebSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebSearchTest {
    @Test fun buildsEncodedUrlsForBuiltInEngines() {
        assertEquals("https://www.google.com/search?q=hello+world%26x", WebSearch.buildUrl(SearchEngine.GOOGLE, "", "hello world&x"))
        assertEquals("https://duckduckgo.com/?q=a", WebSearch.buildUrl(SearchEngine.DUCKDUCKGO, "", " a "))
    }

    @Test fun blankQueryHasNoUrl() {
        assertNull(WebSearch.buildUrl(SearchEngine.BING, "", "   "))
    }

    @Test fun customTemplateIsValidated() {
        assertTrue(WebSearch.isValidCustomTemplate("https://example.org/s?q=%s"))
        assertFalse(WebSearch.isValidCustomTemplate("javascript:alert(%s)"))
        assertFalse(WebSearch.isValidCustomTemplate("https://example.org/s?q="))
        assertFalse(WebSearch.isValidCustomTemplate("https://example.org/%s/%s"))
        assertFalse(WebSearch.isValidCustomTemplate("https://exa mple.org/%s"))
        assertEquals("https://example.org/s?q=cat", WebSearch.buildUrl(SearchEngine.CUSTOM, "https://example.org/s?q=%s", "cat"))
        assertNull(WebSearch.buildUrl(SearchEngine.CUSTOM, "nonsense", "cat"))
    }
}
