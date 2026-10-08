package com.mylauncher.app.widgets

/** A widget placed on the home screen. [rows] is its height in home-grid rows. */
data class HostedWidget(val id: Int, val rows: Int = DEFAULT_ROWS) {
    companion object {
        const val MIN_ROWS = 1
        const val MAX_ROWS = 4
        const val DEFAULT_ROWS = 2
    }
}

/** Persistence and pure list operations for hosted widgets. One widget per line: id TAB rows. */
object WidgetCodec {
    fun encode(widgets: List<HostedWidget>): String = widgets.joinToString("\n") { "${it.id}\t${it.rows}" }

    fun decode(raw: String?): List<HostedWidget> {
        if (raw.isNullOrEmpty()) return emptyList()
        val seen = HashSet<Int>()
        return raw.lineSequence().mapNotNull { line ->
            val p = line.split("\t")
            val id = p.getOrNull(0)?.toIntOrNull()
            val rows = p.getOrNull(1)?.toIntOrNull()
            if (p.size == 2 && id != null && id > 0 && rows != null && seen.add(id)) {
                HostedWidget(id, rows.coerceIn(HostedWidget.MIN_ROWS, HostedWidget.MAX_ROWS))
            } else {
                null
            }
        }.toList()
    }

    fun add(list: List<HostedWidget>, widget: HostedWidget): List<HostedWidget> =
        if (list.any { it.id == widget.id }) list
        else list + widget.copy(rows = widget.rows.coerceIn(HostedWidget.MIN_ROWS, HostedWidget.MAX_ROWS))

    fun remove(list: List<HostedWidget>, id: Int): List<HostedWidget> = list.filterNot { it.id == id }

    fun move(list: List<HostedWidget>, id: Int, delta: Int): List<HostedWidget> {
        val from = list.indexOfFirst { it.id == id }
        if (from < 0) return list
        val to = (from + delta).coerceIn(0, list.lastIndex)
        if (to == from) return list
        val m = list.toMutableList()
        m.add(to, m.removeAt(from))
        return m
    }

    fun resize(list: List<HostedWidget>, id: Int, delta: Int): List<HostedWidget> =
        list.map {
            if (it.id == id) it.copy(rows = (it.rows + delta).coerceIn(HostedWidget.MIN_ROWS, HostedWidget.MAX_ROWS)) else it
        }

    /** Rows needed to show a widget whose provider asks for at least [minHeightDp]. */
    fun rowsFor(minHeightDp: Int, rowHeightDp: Int): Int =
        if (minHeightDp <= 0 || rowHeightDp <= 0) HostedWidget.DEFAULT_ROWS
        else ((minHeightDp + rowHeightDp - 1) / rowHeightDp).coerceIn(HostedWidget.MIN_ROWS, HostedWidget.MAX_ROWS)
}
