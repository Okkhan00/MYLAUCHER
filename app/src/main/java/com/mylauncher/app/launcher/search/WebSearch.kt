package com.mylauncher.app.launcher.search

import java.net.URLEncoder

enum class SearchEngine(val id: String, val label: String, val template: String) {
    GOOGLE("google", "Google", "https://www.google.com/search?q=%s"),
    BING("bing", "Bing", "https://www.bing.com/search?q=%s"),
    DUCKDUCKGO("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    BRAVE("brave", "Brave", "https://search.brave.com/search?q=%s"),
    CUSTOM("custom", "Custom", ""),
}

/**
 * Builds the URL a browser should open for a web search. The launcher never touches the network
 * itself: it hands the URL to the user's browser, so no INTERNET permission is needed.
 */
object WebSearch {
    private const val PLACEHOLDER = "%s"
    const val MAX_QUERY = 200

    /** A custom template must be http(s) and contain exactly one "%s" where the query goes. */
    fun isValidCustomTemplate(template: String): Boolean {
        val t = template.trim()
        val schemeOk = t.startsWith("https://", ignoreCase = true) || t.startsWith("http://", ignoreCase = true)
        return schemeOk && t.length > 10 && t.split(PLACEHOLDER).size == 2 && !t.any { it.isWhitespace() }
    }

    /** Returns null when the query is blank or the custom template is invalid. */
    fun buildUrl(engine: SearchEngine, customTemplate: String, query: String): String? {
        val q = query.trim().take(MAX_QUERY)
        if (q.isEmpty()) return null
        val template = if (engine == SearchEngine.CUSTOM) {
            if (!isValidCustomTemplate(customTemplate)) return null
            customTemplate.trim()
        } else {
            engine.template
        }
        return template.replace(PLACEHOLDER, URLEncoder.encode(q, "UTF-8"))
    }
}
