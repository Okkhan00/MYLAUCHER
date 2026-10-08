package com.mylauncher.app.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.LaunchEvent
import com.mylauncher.app.ui.components.AppIcon
import com.mylauncher.app.ui.components.EmptyState
import com.mylauncher.app.ui.components.ScreenHeader
import com.mylauncher.app.ui.components.SectionTitle
import com.mylauncher.app.ui.components.SwitchRow

/**
 * Usage Insights from the launcher's own on-device launch log. No Usage Access permission is
 * requested: the numbers only cover apps opened from My Launcher, and the screen says so.
 */
@Composable
fun UsageInsightsScreen(
    events: List<LaunchEvent>,
    apps: List<AppInfo>,
    trackingEnabled: Boolean,
    onTrackingChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
) {
    var range by rememberSaveable { mutableStateOf(InsightRange.TODAY) }
    var confirmClear by remember { mutableStateOf(false) }
    // Computed once per input change, never on a timer.
    val insights = remember(events, apps, range) { UsageInsights.compute(events, apps, range, System.currentTimeMillis()) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Usage insights", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                SwitchRow(
                    "Remember apps I open", trackingEnabled, onTrackingChange,
                    subtitle = "Needed for insights and suggestions. Stored only on this device.",
                )
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InsightRange.entries.forEach { r ->
                        FilterChip(selected = range == r, onClick = { range = r }, label = { Text(r.label) })
                    }
                }

                if (!trackingEnabled) {
                    EmptyState(
                        title = "Usage tracking is off",
                        subtitle = "Turn on \"Remember apps I open\" to see insights. Nothing leaves your device.",
                    )
                } else if (insights.totalLaunches == 0) {
                    EmptyState(
                        title = "Nothing yet for ${insights.range.label.lowercase()}",
                        subtitle = "Open apps from the home screen or drawer and they will show up here.",
                    )
                } else {
                    SectionTitle("Most used")
                    insights.mostUsed.forEach { entry ->
                        AppLine(entry.app, "${entry.launches} ${if (entry.launches == 1) "launch" else "launches"}")
                    }
                    SectionTitle("Recently opened")
                    insights.recentlyOpened.forEach { app -> AppLine(app, null) }
                    Text(
                        "${insights.totalLaunches} launches ${insights.range.label.lowercase()}",
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    "Insights count apps opened from My Launcher only. Apps opened from notifications, " +
                        "recent apps or another launcher are not included. No special permission is used " +
                        "and nothing is uploaded.",
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { confirmClear = true }, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Clear usage history")
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear usage history?") },
            text = { Text("This removes recent apps, most used apps, suggestions data and these insights. It cannot be undone.") },
            confirmButton = { TextButton(onClick = { onClearHistory(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AppLine(app: AppInfo, detail: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = app.label + (detail?.let { ", $it" } ?: "") },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppIcon(app, 40.dp)
        Text(app.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
