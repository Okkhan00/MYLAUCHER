package com.mylauncher.app.bulk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Review, progress and result of a bulk uninstall. Apps are handled strictly one at a time: Android's own
 * confirmation screen opens only when the user taps "Uninstall next", so the system dialogs never pile up.
 */
@Composable
fun BulkUninstallDialog(
    session: UninstallSession,
    started: Boolean,
    onUninstallNext: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
) {
    val body: @Composable (List<Pair<String, List<String>>>) -> Unit = { sections ->
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sections.forEach { (heading, lines) ->
                if (heading.isNotEmpty()) Text(heading, style = MaterialTheme.typography.titleSmall)
                lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
    val skipped = session.skipped.map { (item, why) -> "• ${item.label}: $why" }

    when {
        !started -> AlertDialog(
            onDismissRequest = onClose,
            title = { Text("Review before uninstalling") },
            text = {
                body(
                    buildList {
                        if (session.queue.isEmpty()) {
                            add("" to listOf("None of the selected apps can be uninstalled from here."))
                        } else {
                            add("Android will ask about each of these, one at a time (${BulkText.apps(session.queue.size)}):" to session.queue.map { "• ${it.label}" })
                        }
                        if (skipped.isNotEmpty()) add("Skipped:" to skipped)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = onUninstallNext, enabled = session.queue.isNotEmpty()) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
        )

        !session.finished -> AlertDialog(
            // Not dismissible by tapping outside: stopping is an explicit choice.
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("Uninstalling ${session.position} of ${session.queue.size}") },
            text = {
                body(
                    listOf(
                        "" to listOf("Next: ${session.next?.label.orEmpty()}"),
                        "So far:" to resultLines(session).ifEmpty { listOf("Nothing yet") },
                    ),
                )
            },
            confirmButton = { TextButton(onClick = onUninstallNext) { Text("Uninstall next") } },
            dismissButton = { TextButton(onClick = onStop) { Text("Stop") } },
        )

        else -> AlertDialog(
            onDismissRequest = onClose,
            title = { Text("Uninstall finished") },
            text = {
                body(
                    buildList {
                        if (session.removed().isNotEmpty()) add("Removed (${session.removed().size}):" to session.removed().map { "• ${it.label}" })
                        if (session.cancelled().isNotEmpty()) add("Cancelled by you (${session.cancelled().size}):" to session.cancelled().map { "• ${it.label}" })
                        if (session.failed().isNotEmpty()) add("Android could not remove (${session.failed().size}):" to session.failed().map { "• ${it.label}" })
                        if (session.stillInstalled().isNotEmpty()) add("Still installed (${session.stillInstalled().size}):" to session.stillInstalled().map { "• ${it.label}" })
                        if (session.notAttempted.isNotEmpty()) add("Not started (${session.notAttempted.size}):" to session.notAttempted.map { "• ${it.label}" })
                        if (skipped.isNotEmpty()) add("Skipped:" to skipped)
                        if (session.results.isEmpty() && session.notAttempted.isEmpty() && skipped.isEmpty()) add("" to listOf("Nothing was uninstalled."))
                    },
                )
            },
            confirmButton = { TextButton(onClick = onClose) { Text("Done") } },
        )
    }
}

private fun resultLines(session: UninstallSession): List<String> = session.results.map { (item, outcome) ->
    val text = when (outcome) {
        UninstallOutcome.REMOVED -> "removed"
        UninstallOutcome.CANCELLED -> "cancelled"
        UninstallOutcome.FAILED -> "Android could not remove it"
        UninstallOutcome.STILL_INSTALLED -> "still installed"
    }
    "• ${item.label}: $text"
}
