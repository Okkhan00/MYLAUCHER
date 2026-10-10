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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mylauncher.app.categories.AppCategory
import com.mylauncher.app.categories.CategoryCodec
import com.mylauncher.app.categories.CategoryConfig
import com.mylauncher.app.categories.CategoryEntry
import com.mylauncher.app.categories.CategoryOrganizer
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.launcher.apps.MenuItem
import com.mylauncher.app.ui.components.ScreenHeader

/**
 * Rename, hide and reorder categories, and move apps between them. The All Apps view is never
 * affected by hiding a category; it only changes the Categories view in the drawer.
 */
@Composable
fun CategoriesScreen(
    apps: List<AppInfo>,
    overrides: Map<String, AppCategory>,
    config: CategoryConfig,
    onConfigChange: ((CategoryConfig) -> CategoryConfig) -> Unit,
    onSetCategory: (AppInfo, AppCategory?) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    val groups = remember(apps, overrides, config) {
        CategoryOrganizer.group(apps, overrides, config, includeHidden = true).associateBy { it.entry.category }
    }
    var expanded by remember { mutableStateOf<AppCategory?>(null) }
    var renaming by remember { mutableStateOf<CategoryEntry?>(null) }
    var moving by remember { mutableStateOf<AppInfo?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("App categories", onBack)
            Text(
                "Apps are sorted into categories automatically, using only their package name and label on " +
                    "this device. Tap a category to see its apps and move them.",
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyColumn(Modifier.weight(1f)) {
                items(config.normalized().entries, key = { it.category.id }) { entry ->
                    val categoryApps = groups[entry.category]?.apps.orEmpty()
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable { expanded = if (expanded == entry.category) null else entry.category }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "${entry.displayName}, ${categoryApps.size} apps" +
                                        if (entry.hidden) ", hidden in categories view" else ""
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.displayName, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${categoryApps.size} apps" + if (entry.hidden) " - hidden in categories view" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = !entry.hidden,
                                onCheckedChange = { visible -> onConfigChange { it.setHidden(entry.category, !visible) } },
                                modifier = Modifier.semantics { contentDescription = "Show ${entry.displayName}" },
                            )
                        }
                        Row(
                            Modifier.padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            TextButton(onClick = { renaming = entry }) { Text("Rename") }
                            TextButton(onClick = { onConfigChange { it.move(entry.category, -1) } }) { Text("Move up") }
                            TextButton(onClick = { onConfigChange { it.move(entry.category, 1) } }) { Text("Move down") }
                        }
                        if (expanded == entry.category) {
                            if (categoryApps.isEmpty()) {
                                Text(
                                    "No apps in this category.",
                                    Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            categoryApps.forEach { app ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clickable { moving = app }
                                        .padding(start = 32.dp, end = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(app.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (app.packageName in overrides) "Moved by you" else "Automatic",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
                item {
                    TextButton(onClick = { confirmReset = true }, modifier = Modifier.padding(8.dp)) {
                        Text("Reset categories")
                    }
                }
            }
        }
    }

    renaming?.let { entry ->
        var text by remember(entry.category) { mutableStateOf(entry.customName.orEmpty()) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename ${entry.category.defaultName}") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(CategoryCodec.MAX_NAME) },
                    singleLine = true,
                    label = { Text("Name") },
                    supportingText = { Text("Leave empty to use the original name") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onConfigChange { it.rename(entry.category, text) }
                    renaming = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } },
        )
    }

    moving?.let { app ->
        AlertDialog(
            onDismissRequest = { moving = null },
            title = { Text("Move ${app.label}") },
            text = {
                Column {
                    MenuItem("Automatic (recommended)") { onSetCategory(app, null); moving = null }
                    config.normalized().entries.forEach { entry ->
                        MenuItem(entry.displayName) { onSetCategory(app, entry.category); moving = null }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { moving = null }) { Text("Cancel") } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset categories?") },
            text = { Text("Names, order, hidden categories and apps you moved go back to automatic.") },
            confirmButton = { TextButton(onClick = { onReset(); confirmReset = false }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}
