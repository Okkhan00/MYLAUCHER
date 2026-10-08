package com.mylauncher.app.launcher.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.launcher.search.AppSearch
import com.mylauncher.app.ui.components.AppIcon
import com.mylauncher.app.ui.components.EmptyState

/** Pick which apps appear as favorites on the home screen. */
@Composable
fun AddAppsDialog(
    apps: List<AppInfo>,
    favoriteIds: List<String>,
    onToggle: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(apps, query) { AppSearch.filter(apps, query) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add apps") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search apps") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (results.isEmpty()) {
                    EmptyState("No apps found")
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp).padding(top = 8.dp)) {
                        items(results, key = { it.packageName }) { app ->
                            val checked = app.packageName in favoriteIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable { onToggle(app) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Checkbox(checked = checked, onCheckedChange = null)
                                AppIcon(app, 32.dp)
                                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
