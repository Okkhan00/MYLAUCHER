package com.mylauncher.app.launcher.appdrawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.listSaver
import com.mylauncher.app.bulk.BulkActionBar
import com.mylauncher.app.bulk.BulkActions
import com.mylauncher.app.bulk.BulkSelection
import com.mylauncher.app.bulk.SelectionTopBar
import com.mylauncher.app.categories.CategoryVisuals
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mylauncher.app.categories.CategoryGroup
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.DrawerLayout
import com.mylauncher.app.data.model.Folder
import com.mylauncher.app.data.model.LaunchStat
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SmartSettings
import com.mylauncher.app.launcher.apps.AppActionsDialog
import com.mylauncher.app.launcher.apps.AppMenuActions
import com.mylauncher.app.launcher.search.AppSearch
import com.mylauncher.app.launcher.search.SearchResult
import com.mylauncher.app.launcher.search.SearchResultsList
import com.mylauncher.app.launcher.search.SettingShortcut
import com.mylauncher.app.launcher.search.SmartSearch
import com.mylauncher.app.launcher.search.SystemAction
import com.mylauncher.app.ui.components.AppRow
import com.mylauncher.app.ui.components.AppTile
import com.mylauncher.app.ui.components.EmptyState
import com.mylauncher.app.ui.theme.LocalSearchBarShape

/** App drawer with live search. Opening via "search" focuses the field and shows the keyboard. */
@Composable
fun DrawerScreen(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    focusSearch: Boolean,
    recentApps: List<AppInfo>,
    mostUsedApps: List<AppInfo>,
    suggestedApps: List<AppInfo>,
    folders: List<Folder>,
    launchStats: Map<String, LaunchStat>,
    smart: SmartSettings,
    categoryGroups: List<CategoryGroup>,
    menu: AppMenuActions,
    bulk: BulkActions,
    onLaunch: (AppInfo) -> Unit,
    onOpenSettings: () -> Unit,
    onGroupByCategoryChange: (Boolean) -> Unit,
    onSettingShortcut: (SettingShortcut) -> Unit,
    onSystemAction: (SystemAction) -> Unit,
    onWebSearch: (String) -> Unit,
    privateCount: Int,
    onOpenPrivate: () -> Unit,
    drawerOpacity: Float,
    wallpaperScrim: Color,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }

    // ---- selecting several apps (V4.5) ----
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selection by rememberSaveable(stateSaver = BulkSelectionSaver) { mutableStateOf(BulkSelection()) }
    val enterSelection: (AppInfo?) -> Unit = { first ->
        query = ""
        selecting = true
        selection = if (first != null) BulkSelection().add(first.packageName) else BulkSelection()
    }
    val exitSelection: () -> Unit = { selecting = false; selection = BulkSelection() }
    // Leaving selection mode is always one Back press away.
    BackHandler(enabled = selecting) { exitSelection() }
    // Apps that disappear meanwhile (uninstalled, hidden) are dropped so the count never lies.
    LaunchedEffect(apps) {
        if (selecting) selection = selection.retainOnly(apps.mapTo(HashSet()) { it.packageName })
    }
    val tapApp: (AppInfo) -> Unit = { app -> if (selecting) selection = selection.toggle(app.packageName) else onLaunch(app) }
    val holdApp: (AppInfo) -> Unit = { app -> if (selecting) selection = selection.toggle(app.packageName) else menuApp = app }
    val selectedOf: (AppInfo) -> Boolean? = { app -> if (selecting) app.packageName in selection else null }

    val searching = query.isNotBlank() && !selecting
    // Categories mode shows one category at a time; swiping moves between them. The id of the category on
    // screen is remembered (and survives rotation), so list changes never throw the user to another page.
    var openCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val grouped = smart.groupDrawerByCategory && !searching
    val groupIds = remember(categoryGroups) { categoryGroups.map { it.entry.category.id } }
    val pagerState = rememberPagerState(initialPage = groupIds.indexOf(openCategoryId).coerceAtLeast(0)) { groupIds.size }
    LaunchedEffect(groupIds) {
        val index = groupIds.indexOf(openCategoryId)
        if (index >= 0 && index != pagerState.currentPage) pagerState.scrollToPage(index)
        snapshotFlow { pagerState.settledPage }.collect { page -> groupIds.getOrNull(page)?.let { openCategoryId = it } }
    }
    var showCategoryJump by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Plain A-Z list used by the alphabet index and the non-search views (search has its own results).
    val results = apps
    // Smart Search 2.0 results; only computed while the user is typing.
    // Search data (lower-cased labels, words) is built once per app list, only when first needed,
    // so typing never re-processes every label and never queries PackageManager.
    val searchIndex = remember(apps) { lazy(LazyThreadSafetyMode.NONE) { AppSearch.Index(apps) } }
    val searchResults = remember(apps, query, folders, launchStats, smart.searchSettingsShortcuts, smart.webSearchEnabled) {
        if (searching) {
            SmartSearch.search(
                query = query,
                apps = apps,
                folders = folders,
                stats = launchStats,
                includeSettings = smart.searchSettingsShortcuts,
                includeActions = smart.searchSettingsShortcuts,
                includeWebFallback = smart.webSearchEnabled,
                index = searchIndex.value,
            )
        } else {
            emptyList()
        }
    }
    val activate: (SearchResult) -> Unit = { result ->
        when (result) {
            is SearchResult.AppResult -> onLaunch(result.app)
            is SearchResult.SettingResult -> onSettingShortcut(result.shortcut)
            is SearchResult.ActionResult -> onSystemAction(result.action)
            is SearchResult.WebResult -> onWebSearch(result.query)
        }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            withFrameNanos { }
            try {
                focusRequester.requestFocus()
                keyboard?.show()
            } catch (e: IllegalStateException) {
                // Field not attached yet; the user can tap it.
            }
        }
    }

    val iconSize = settings.iconSize.sizeDp.dp
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    // The wallpaper overlay only shows through when the drawer is translucent and the user enabled it.
    Box(Modifier.fillMaxSize().background(wallpaperScrim)) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = drawerOpacity),
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            if (selecting) {
                // Listed apps are what "Select all" means: the open category, or the whole All Apps list.
                val listed = if (smart.groupDrawerByCategory) {
                    categoryGroups.getOrNull(pagerState.currentPage)?.apps.orEmpty()
                } else {
                    results
                }
                val listedIds = remember(listed) { listed.map { it.packageName } }
                SelectionTopBar(
                    count = selection.count,
                    allSelected = selection.allSelected(listedIds),
                    onToggleAll = {
                        selection = if (selection.allSelected(listedIds)) {
                            BulkSelection(selection.packages - listedIds.toSet())
                        } else {
                            BulkSelection((selection.packages + listedIds).distinct())
                        }
                    },
                    onClear = { selection = BulkSelection() },
                    onCancel = exitSelection,
                )
            } else Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val trailing: (@Composable () -> Unit)? = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                } else {
                    null
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    singleLine = true,
                    shape = LocalSearchBarShape.current,
                    placeholder = { Text(if (smart.webSearchEnabled || smart.searchSettingsShortcuts) "Search apps, settings and more" else "Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = trailing,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { searchResults.firstOrNull()?.let(activate) }),
                )
                IconButton(onClick = { enterSelection(null) }) {
                    Icon(Icons.Default.Check, contentDescription = "Select several apps")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Launcher settings")
                }
            }

            if (!searching && !selecting) {
                DrawerModeTabs(
                    categoriesMode = smart.groupDrawerByCategory,
                    onSelect = { categories ->
                        onGroupByCategoryChange(categories)
                    },
                )
                if (privateCount > 0) {
                    TextButton(onClick = onOpenPrivate, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("Private apps ($privateCount)")
                    }
                }
            }

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val showIndex = !grouped && query.isEmpty() && results.size >= 24 && maxHeight > 420.dp
                val letters = remember(results) { results.map { firstLetter(it.label) }.distinct() }
                val firstIndexByLetter = remember(results) {
                    val map = LinkedHashMap<Char, Int>()
                    results.forEachIndexed { i, app -> map.putIfAbsent(firstLetter(app.label), i) }
                    map
                }
                val endPadding = if (showIndex) 28.dp else 8.dp
                val showSuggested = smart.smartSuggestions && query.isEmpty() && !selecting && suggestedApps.isNotEmpty() && maxHeight > 520.dp
                // When suggestions are shown the other shortcut rows need a little more room.
                val showRecent = smart.showRecentApps && query.isEmpty() && !selecting && recentApps.isNotEmpty() && maxHeight > (if (showSuggested) 680.dp else 520.dp)
                val showMostUsed = query.isEmpty() && !selecting && mostUsedApps.isNotEmpty() && maxHeight > (if (showSuggested) 880.dp else 720.dp)

                Column(Modifier.fillMaxSize()) {
                if (showSuggested) ShortcutRow("Suggested", suggestedApps, iconSize, settings.showLabels, onLaunch) { menuApp = it }
                if (showRecent) ShortcutRow("Recently used", recentApps, iconSize, settings.showLabels, onLaunch) { menuApp = it }
                if (showMostUsed) ShortcutRow("Most used", mostUsedApps, iconSize, settings.showLabels, onLaunch) { menuApp = it }
                Box(Modifier.weight(1f).fillMaxWidth()) {

                if (searching) {
                    SearchResultsList(
                        results = searchResults,
                        iconSize = iconSize,
                        showLabels = settings.showLabels,
                        onActivate = activate,
                        onAppLongClick = { menuApp = it },
                    )
                } else if (grouped) {
                    if (categoryGroups.isEmpty()) {
                        EmptyState(
                            title = "No categories to show",
                            subtitle = "All categories are hidden or empty. Switch to All Apps, or change this in Settings > App categories.",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            key = { groupIds.getOrNull(it) ?: it },
                        ) { page ->
                            val group = categoryGroups.getOrNull(page)
                            if (group != null) {
                                Column(Modifier.fillMaxSize()) {
                                    CategoryPageHeader(
                                        entry = group.entry,
                                        appCount = group.apps.size,
                                        index = page,
                                        total = categoryGroups.size,
                                        onJump = { showCategoryJump = true },
                                    )
                                    if (group.apps.isEmpty()) {
                                        EmptyState(title = "No apps in this category", modifier = Modifier.fillMaxSize())
                                    } else if (settings.drawerLayout == DrawerLayout.GRID) {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(settings.columns),
                                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 16.dp),
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                        ) {
                                            items(group.apps, key = { it.packageName }, contentType = { "app" }) { app ->
                                                AppTile(
                                                    app = app,
                                                    iconSize = iconSize,
                                                    showLabel = settings.showLabels,
                                                    onClick = { tapApp(app) },
                                                    onLongClick = { holdApp(app) },
                                                    selected = selectedOf(app),
                                                )
                                            }
                                        }
                                    } else {
                                        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            items(group.apps, key = { it.packageName }, contentType = { "app" }) { app ->
                                                AppRow(
                                                    app = app,
                                                    iconSize = iconSize,
                                                    showLabel = settings.showLabels,
                                                    onClick = { tapApp(app) },
                                                    onLongClick = { holdApp(app) },
                                                    selected = selectedOf(app),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (results.isEmpty()) {
                    EmptyState(
                        title = "No apps found",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (settings.drawerLayout == DrawerLayout.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(settings.columns),
                        state = gridState,
                        contentPadding = PaddingValues(start = 8.dp, end = endPadding, bottom = 16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(results, key = { it.packageName }, contentType = { "app" }) { app ->
                            AppTile(
                                app = app,
                                iconSize = iconSize,
                                showLabel = settings.showLabels,
                                onClick = { tapApp(app) },
                                onLongClick = { holdApp(app) },
                                selected = selectedOf(app),
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 0.dp, end = endPadding - 8.dp, bottom = 16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(results, key = { it.packageName }, contentType = { "app" }) { app ->
                            AppRow(
                                app = app,
                                iconSize = iconSize,
                                showLabel = settings.showLabels,
                                onClick = { tapApp(app) },
                                onLongClick = { holdApp(app) },
                                selected = selectedOf(app),
                            )
                        }
                    }
                }

                if (showIndex) {
                    AlphabetIndex(
                        letters = letters,
                        onLetter = { letter ->
                            val index = firstIndexByLetter[letter] ?: return@AlphabetIndex
                            if (settings.drawerLayout == DrawerLayout.GRID) {
                                gridState.requestScrollToItem(index)
                            } else {
                                listState.requestScrollToItem(index)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(24.dp)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp),
                    )
                }
                }
                }
            }

            if (selecting) {
                val selectedApps = remember(selection, apps) {
                    val byPackage = apps.associateBy { it.packageName }
                    selection.packages.mapNotNull { byPackage[it] }
                }
                BulkActionBar(selected = selectedApps, actions = bulk, onFinished = exitSelection)
            }
        }
    }

    }

    menuApp?.let { app ->
        AppActionsDialog(
            app, menu, showMove = false,
            onDismiss = { menuApp = null },
            onSelect = { enterSelection(app) },
        )
    }

    if (showCategoryJump) {
        AlertDialog(
            onDismissRequest = { showCategoryJump = false },
            title = { Text("Go to category") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    categoryGroups.forEachIndexed { i, group ->
                        Text(
                            "${CategoryVisuals.emoji(group.entry)}  ${group.entry.displayName} (${group.apps.size})",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    showCategoryJump = false
                                    scope.launch { pagerState.animateScrollToPage(i) }
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (i == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showCategoryJump = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun ShortcutRow(
    title: String,
    apps: List<AppInfo>,
    iconSize: androidx.compose.ui.unit.Dp,
    showLabels: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
            items(apps, key = { it.packageName }) { app ->
                AppTile(
                    app = app,
                    iconSize = iconSize,
                    showLabel = showLabels,
                    onClick = { onLaunch(app) },
                    onLongClick = { onLongPress(app) },
                    modifier = Modifier.width(76.dp),
                )
            }
        }
    }
}

/** Keeps the selected apps (in order) across rotation. */
private val BulkSelectionSaver = listSaver<BulkSelection, String>(
    save = { it.packages },
    restore = { BulkSelection(it) },
)
