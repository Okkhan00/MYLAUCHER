package com.mylauncher.app.data.preferences

import com.mylauncher.app.data.model.LaunchEvent

/**
 * Small rolling log of launches (package TAB epoch-millis per line, oldest first). It feeds time-based
 * suggestions and the Today / This week insights. Capped so it never grows into a large dataset.
 */
object LaunchLogCodec {
    const val MAX_EVENTS = 400

    fun encode(events: List<LaunchEvent>): String =
        events.takeLast(MAX_EVENTS).joinToString("\n") { "${it.packageName}\t${it.timeMs}" }

    fun decode(raw: String?): List<LaunchEvent> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val parts = line.split("\t")
            val time = parts.getOrNull(1)?.toLongOrNull()
            if (parts.size == 2 && parts[0].isNotBlank() && time != null && time > 0) LaunchEvent(parts[0], time) else null
        }.toList()
    }

    fun append(events: List<LaunchEvent>, pkg: String, nowMs: Long): List<LaunchEvent> =
        (events + LaunchEvent(pkg, nowMs)).takeLast(MAX_EVENTS)
}
