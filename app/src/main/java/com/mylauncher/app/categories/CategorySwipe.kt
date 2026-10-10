package com.mylauncher.app.categories

/**
 * Decides where a horizontal swipe inside an opened category leads. Pure, so it is unit tested.
 * Swiping left (negative drag) goes to the next category, swiping right to the previous one, in the
 * order the categories are shown. The first and last categories have nothing beyond them.
 */
object CategorySwipe {
    /** Direction of the move: +1 next, -1 previous, 0 none. */
    fun direction(totalDragPx: Float, thresholdPx: Float): Int = when {
        totalDragPx <= -thresholdPx -> 1
        totalDragPx >= thresholdPx -> -1
        else -> 0
    }

    /** Id of the category to show after the swipe, or null to stay where we are. */
    fun target(orderedIds: List<String>, currentId: String, totalDragPx: Float, thresholdPx: Float): String? {
        val dir = direction(totalDragPx, thresholdPx)
        if (dir == 0) return null
        val index = orderedIds.indexOf(currentId)
        if (index < 0) return null
        return orderedIds.getOrNull(index + dir)
    }
}
