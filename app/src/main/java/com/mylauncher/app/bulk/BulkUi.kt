package com.mylauncher.app.bulk

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mylauncher.app.categories.AppCategory
import com.mylauncher.app.categories.CategoryCodec
import com.mylauncher.app.categories.CategoryEntry
import com.mylauncher.app.categories.CategoryVisuals
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.Folder

/**
 * Everything the bulk action bar needs from the rest of the launcher: what is currently true (hidden, locked,
 * on the home screen...) and what to run. Built once in LauncherApp so the drawer stays simple.
 */
class BulkActions(
    val categories: List<CategoryEntry>,
    val manualCategoryApps: Set<String>,
    val homeTokens: List<String>,
    val hidden: Set<String>,
    val locked: Set<String>,
    val folderApps: Set<String>,
    val folders: List<Folder>,
    /** App lock needs a launcher PIN; without one the lock actions are not offered. */
    val lockAvailable: Boolean,
    val assignCategory: (List<String>, AppCategory?) -> Unit,
    /** Returns null through the callback on success, otherwise a message for the user. */
    val createCategory: (name: String, emoji: String?, packages: List<String>, onResult: (String?) -> Unit) -> Unit,
    val addFavorites: (List<String>) -> Unit,
    val removeFavorites: (List<String>) -> Unit,
    val hide: (List<String>) -> Unit,
    val unhide: (List<String>) -> Unit,
    val lock: (List<String>) -> Unit,
    /** Unlocking asks for the launcher PIN first (handled by the launcher, never skipped here). */
    val requestUnlock: (List<String>) -> Unit,
    val addToFolder: (folderId: String, packages: List<String>) -> Unit,
    val removeFromFolders: (List<String>) -> Unit,
    val uninstall: (List<AppInfo>) -> Unit,
    val notify: (String) -> Unit,
)

/** Header shown instead of the search bar while apps are being selected. */
@Composable
fun SelectionTopBar(
    count: Int,
    allSelected: Boolean,
    onToggleAll: () -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        Text(
            if (count == 0) "Select apps" else "$count selected",
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onToggleAll, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (allSelected) "None" else "Select all")
        }
        TextButton(onClick = onClear, enabled = count > 0, modifier = Modifier.heightIn(min = 48.dp)) { Text("Clear") }
    }
}

@Composable
private fun BarAction(emoji: String, label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.38f
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 2.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
    }
}

/**
 * The bulk action bar plus every dialog it opens. [selected] are the chosen apps in selection order;
 * [onFinished] leaves selection mode after an action completed.
 */
@Composable
fun BulkActionBar(
    selected: List<AppInfo>,
    actions: BulkActions,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val packages = remember(selected) { selected.map { it.packageName } }
    var showCategory by remember { mutableStateOf(false) }
    var showNewCategory by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showFolders by remember { mutableStateOf(false) }
    var confirmUninstall by remember { mutableStateOf(false) }

    val toAdd = remember(packages, actions.homeTokens) { BulkPlan.favoritesToAdd(packages, actions.homeTokens) }
    val toRemove = remember(packages, actions.homeTokens) { BulkPlan.favoritesToRemove(packages, actions.homeTokens) }
    val toHide = remember(packages, actions.hidden) { BulkPlan.hideTargets(packages, actions.hidden) }
    val toUnhide = remember(packages, actions.hidden) { BulkPlan.unhideTargets(packages, actions.hidden) }
    val toLock = remember(packages, actions.locked) { BulkPlan.lockTargets(packages, actions.locked) }
    val toUnlock = remember(packages, actions.locked) { BulkPlan.unlockTargets(packages, actions.locked) }
    val inFolder = remember(packages, actions.folderApps) { BulkPlan.inFolder(packages, actions.folderApps) }
    val manual = remember(packages, actions.manualCategoryApps) { BulkPlan.withManualCategory(packages, actions.manualCategoryApps) }
    val hasSelection = packages.isNotEmpty()

    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            BarAction("📂", "Category", hasSelection, Modifier.weight(1f)) { showCategory = true }
            // One tap does the obvious thing: add to favorites, or remove if they are all favorites already.
            val addsFavorites = toAdd.isNotEmpty()
            BarAction(
                if (addsFavorites) "⭐" else "☆",
                if (addsFavorites) "Favorite" else "Unfavorite",
                hasSelection, Modifier.weight(1f),
            ) {
                if (addsFavorites) {
                    actions.addFavorites(toAdd)
                    actions.notify("${BulkText.apps(toAdd.size)} added to favorites" + skippedNote(packages.size - toAdd.size, "already there"))
                } else {
                    actions.removeFavorites(toRemove)
                    actions.notify("${BulkText.apps(toRemove.size)} removed from favorites")
                }
                onFinished()
            }
            BarAction("🙈", if (toHide.isEmpty() && toUnhide.isNotEmpty()) "Unhide" else "Hide", hasSelection, Modifier.weight(1f)) {
                if (toHide.isNotEmpty()) {
                    actions.hide(toHide)
                    actions.notify("${BulkText.apps(toHide.size)} hidden in My Launcher. Undo in Settings > Private apps." + skippedNote(toUnhide.size, "already hidden"))
                } else {
                    actions.unhide(toUnhide)
                    actions.notify("${BulkText.apps(toUnhide.size)} unhidden")
                }
                onFinished()
            }
            BarAction("⋯", "More", hasSelection, Modifier.weight(1f)) { showMore = true }
        }
    }

    if (showCategory) {
        ChooseCategoryDialog(
            categories = actions.categories,
            count = packages.size,
            onPick = { entry ->
                actions.assignCategory(packages, entry.category)
                actions.notify(BulkText.moved(packages.size, entry.displayName))
                showCategory = false
                onFinished()
            },
            onNew = { showCategory = false; showNewCategory = true },
            onDismiss = { showCategory = false },
        )
    }

    if (showNewCategory) {
        NewCategoryDialog(
            count = packages.size,
            onCreate = { name, emoji, report ->
                actions.createCategory(name, emoji, packages) { error ->
                    if (error == null) {
                        actions.notify("Created \"${CategoryCodec.sanitizeName(name)}\" with ${BulkText.apps(packages.size)}")
                        showNewCategory = false
                        onFinished()
                    }
                    report(error)
                }
            },
            onDismiss = { showNewCategory = false },
        )
    }

    if (showMore) {
        AlertDialog(
            onDismissRequest = { showMore = false },
            title = { Text("${BulkText.apps(packages.size)} selected") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    if (toAdd.isNotEmpty()) MoreItem("Add to favorites (${toAdd.size})") {
                        actions.addFavorites(toAdd); actions.notify("${BulkText.apps(toAdd.size)} added to favorites"); showMore = false; onFinished()
                    }
                    if (toRemove.isNotEmpty()) MoreItem("Remove from favorites (${toRemove.size})") {
                        actions.removeFavorites(toRemove); actions.notify("${BulkText.apps(toRemove.size)} removed from favorites"); showMore = false; onFinished()
                    }
                    if (toHide.isNotEmpty()) MoreItem("Hide in My Launcher (${toHide.size})") {
                        actions.hide(toHide); actions.notify("${BulkText.apps(toHide.size)} hidden in My Launcher"); showMore = false; onFinished()
                    }
                    if (toUnhide.isNotEmpty()) MoreItem("Unhide (${toUnhide.size})") {
                        actions.unhide(toUnhide); actions.notify("${BulkText.apps(toUnhide.size)} unhidden"); showMore = false; onFinished()
                    }
                    if (actions.lockAvailable) {
                        if (toLock.isNotEmpty()) MoreItem("Turn on app lock (${toLock.size})") {
                            actions.lock(toLock); actions.notify("App lock turned on for ${BulkText.apps(toLock.size)}"); showMore = false; onFinished()
                        }
                        if (toUnlock.isNotEmpty()) MoreItem("Turn off app lock (${toUnlock.size}) – asks for your PIN") {
                            actions.requestUnlock(toUnlock); showMore = false; onFinished()
                        }
                    }
                    if (actions.folders.isNotEmpty()) MoreItem("Move to a folder…") { showMore = false; showFolders = true }
                    if (inFolder.isNotEmpty()) MoreItem("Remove from folders (${inFolder.size})") {
                        actions.removeFromFolders(inFolder); actions.notify("${BulkText.apps(inFolder.size)} removed from folders"); showMore = false; onFinished()
                    }
                    if (manual.isNotEmpty()) MoreItem("Reset category to automatic (${manual.size})") {
                        actions.assignCategory(manual, null); actions.notify("${BulkText.apps(manual.size)} back to automatic category"); showMore = false; onFinished()
                    }
                    MoreItem("Uninstall…", destructive = true) { showMore = false; confirmUninstall = true }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showMore = false }) { Text("Close") } },
        )
    }

    if (showFolders) {
        AlertDialog(
            onDismissRequest = { showFolders = false },
            title = { Text("Move ${BulkText.apps(packages.size)} to") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    actions.folders.forEach { folder ->
                        MoreItem(folder.name) {
                            actions.addToFolder(folder.id, packages)
                            actions.notify("${BulkText.apps(packages.size)} moved to folder \"${folder.name}\"")
                            showFolders = false
                            onFinished()
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showFolders = false }) { Text("Cancel") } },
        )
    }

    if (confirmUninstall) {
        AlertDialog(
            onDismissRequest = { confirmUninstall = false },
            title = { Text("Uninstall ${BulkText.apps(selected.size)}?") },
            text = {
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Android will ask you to confirm each app one at a time, and you can stop at any point. " +
                            "System apps and My Launcher are skipped.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    selected.forEach { Text("• ${it.label}", style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = {
                TextButton(onClick = { confirmUninstall = false; actions.uninstall(selected) }) { Text("Review and start") }
            },
            dismissButton = { TextButton(onClick = { confirmUninstall = false }) { Text("Cancel") } },
        )
    }
}

private fun skippedNote(n: Int, why: String): String = if (n > 0) " ($n $why)" else ""

@Composable
private fun MoreItem(text: String, destructive: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ChooseCategoryDialog(
    categories: List<CategoryEntry>,
    count: Int,
    onPick: (CategoryEntry) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move ${BulkText.apps(count)} to") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                MoreItem("➕  New category…", onClick = onNew)
                categories.forEach { entry ->
                    MoreItem("${CategoryVisuals.emoji(entry)}  ${entry.displayName}") { onPick(entry) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewCategoryDialog(
    count: Int,
    onCreate: (name: String, emoji: String?, report: (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${BulkText.apps(count)} will be moved into it.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(CategoryCodec.MAX_NAME); error = null },
                    label = { Text("Category name") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Text("Icon (optional)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryVisuals.choices.forEach { choice ->
                        FilterChip(
                            selected = emoji == choice,
                            onClick = { emoji = if (emoji == choice) null else choice },
                            label = { Text(choice, modifier = Modifier.widthIn(min = 24.dp), textAlign = TextAlign.Center) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, emoji) { error = it } }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
