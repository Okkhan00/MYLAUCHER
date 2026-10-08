package com.mylauncher.app.launcher.appdrawer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged

/** Vertical A-Z strip for fast scrolling: tap or drag along it. */
@Composable
fun AlphabetIndex(letters: List<Char>, onLetter: (Char) -> Unit, modifier: Modifier = Modifier) {
    var heightPx by remember { mutableIntStateOf(1) }
    val currentLetters by rememberUpdatedState(letters)
    val currentCallback by rememberUpdatedState(onLetter)

    fun pick(y: Float) {
        val list = currentLetters
        if (list.isEmpty()) return
        val index = ((y / heightPx) * list.size).toInt().coerceIn(0, list.lastIndex)
        currentCallback(list[index])
    }

    Column(
        modifier = modifier
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .pointerInput(Unit) { detectTapGestures(onPress = { offset -> pick(offset.y) }) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(onDragStart = { offset -> pick(offset.y) }) { change, _ ->
                    pick(change.position.y)
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Letter used for grouping: first letter, or '#' for digits and symbols. */
fun firstLetter(label: String): Char =
    label.trim().firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#'
