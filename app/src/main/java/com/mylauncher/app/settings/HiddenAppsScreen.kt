package com.mylauncher.app.settings

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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.launcher.search.AppSearch
import com.mylauncher.app.privacy.AppLockPolicy
import com.mylauncher.app.ui.components.AppIcon
import com.mylauncher.app.ui.components.EmptyState
import com.mylauncher.app.ui.components.ScreenHeader

@Composable
fun HiddenAppsScreen(
    apps: List<AppInfo>,
    hiddenIds: Set<String>,
    onSetHidden: (AppInfo, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val base = remember(apps, hiddenIds, showAll) {
        if (showAll) apps else apps.filter { it.packageName in hiddenIds }
    }
    val results = remember(base, query) { AppSearch.filter(base, query) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Private apps", onBack)
            Text(
                AppLockPolicy.PRIVATE_TEXT,
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search apps") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showAll,
                    onClick = { showAll = false },
                    label = { Text("Private (${hiddenIds.size})") },
                )
                FilterChip(selected = showAll, onClick = { showAll = true }, label = { Text("All apps") })
            }

            when {
                !showAll && base.isEmpty() -> EmptyState(
                    title = "No private apps",
                    subtitle = "Choose All apps to make one private.",
                    modifier = Modifier.weight(1f),
                )
                results.isEmpty() -> EmptyState(title = "No apps found", modifier = Modifier.weight(1f))
                else -> LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
                    items(results, key = { it.packageName }) { app ->
                        val hidden = app.packageName in hiddenIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable { onSetHidden(app, !hidden) }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            AppIcon(app, 40.dp)
                            Text(
                                app.label,
                                Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Switch(checked = hidden, onCheckedChange = null)
                        }
                    }
                }
            }
        }
    }
}
