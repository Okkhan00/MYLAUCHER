package com.mylauncher.app.launcher.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.ui.components.AppIcon
import com.mylauncher.app.ui.components.AppRow
import com.mylauncher.app.ui.components.EmptyState

/** Results of Smart Search 2.0: apps first, then settings shortcuts, launcher actions and web search. */
@Composable
fun SearchResultsList(
    results: List<SearchResult>,
    iconSize: Dp,
    showLabels: Boolean,
    onActivate: (SearchResult) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (results.isEmpty()) {
        EmptyState(title = "No results", subtitle = "Try a different word.", modifier = modifier.fillMaxSize())
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        items(results, key = { it.key }) { result ->
            when (result) {
                is SearchResult.AppResult -> if (result.inFolder == null) {
                    AppRow(
                        app = result.app,
                        iconSize = iconSize,
                        showLabel = showLabels,
                        onClick = { onActivate(result) },
                        onLongClick = { onAppLongClick(result.app) },
                    )
                } else {
                    ResultRow(
                        title = result.app.label,
                        subtitle = "In folder ${result.inFolder}",
                        onClick = { onActivate(result) },
                        leading = { AppIcon(result.app, iconSize) },
                    )
                }
                is SearchResult.SettingResult -> ResultRow(
                    title = result.shortcut.title,
                    subtitle = "Android settings",
                    onClick = { onActivate(result) },
                    leading = { ResultIcon(Icons.Default.Settings) },
                )
                is SearchResult.ActionResult -> ResultRow(
                    title = result.action.title,
                    subtitle = "My Launcher",
                    onClick = { onActivate(result) },
                    leading = { ResultIcon(Icons.Default.Info) },
                )
                is SearchResult.WebResult -> ResultRow(
                    title = "Search the web for \"${result.query}\"",
                    subtitle = "Opens your browser",
                    onClick = { onActivate(result) },
                    leading = { ResultIcon(Icons.Default.Search) },
                )
            }
        }
    }
}

@Composable
private fun ResultIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ResultRow(title: String, subtitle: String, onClick: () -> Unit, leading: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title, $subtitle" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
