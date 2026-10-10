package com.mylauncher.app.privacy

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.launcher.search.AppSearch
import com.mylauncher.app.ui.components.AppIcon
import com.mylauncher.app.ui.components.EmptyState
import com.mylauncher.app.ui.components.ScreenHeader

/** Choose which apps need the launcher PIN or biometrics before they open from My Launcher. */
@Composable
fun AppLockScreen(
    apps: List<AppInfo>,
    lockedApps: Set<String>,
    pinSet: Boolean,
    onSetLocked: (AppInfo, Boolean) -> Unit,
    onOpenSecurity: () -> Unit,
    onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(apps, query) { AppSearch.filter(apps, query) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("App lock", onBack)
            Text(
                AppLockPolicy.LIMITATION_TEXT,
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!pinSet) {
                EmptyState(
                    title = "Set a launcher PIN first",
                    subtitle = "App lock uses the launcher PIN (and biometrics if you enable them) from Settings > Security.",
                    actionLabel = "Open security settings",
                    onAction = onOpenSecurity,
                    modifier = Modifier.weight(1f),
                )
            } else {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search apps") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (results.isEmpty()) {
                    EmptyState(title = "No apps found", modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(results, key = { it.packageName }) { app ->
                            val locked = app.packageName in lockedApps
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .clickable { onSetLocked(app, !locked) }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "${app.label}, ${if (locked) "locked" else "not locked"}"
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                AppIcon(app, 40.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                                    if (locked) Text("Locked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Switch(checked = locked, onCheckedChange = null)
                            }
                        }
                    }
                }
            }
        }
    }
}
