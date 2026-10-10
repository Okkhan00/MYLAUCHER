package com.mylauncher.app.launcher.appdrawer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mylauncher.app.categories.CategoryGroup
import com.mylauncher.app.categories.CategoryPreview
import com.mylauncher.app.categories.CategoryVisuals
import com.mylauncher.app.ui.components.AppIcon
import com.mylauncher.app.ui.components.IconBadge

private val PREVIEW_ICON = 28.dp
private val PREVIEW_GAP = 4.dp

/** The two drawer views, always visible at the top: Categories and All Apps. */
@Composable
fun DrawerModeTabs(categoriesMode: Boolean, onSelect: (categories: Boolean) -> Unit, modifier: Modifier = Modifier) {
    TabRow(
        selectedTabIndex = if (categoriesMode) 0 else 1,
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
    ) {
        Tab(
            selected = categoriesMode,
            onClick = { onSelect(true) },
            modifier = Modifier.heightIn(min = 48.dp),
            text = { Text("Categories", style = MaterialTheme.typography.titleSmall) },
        )
        Tab(
            selected = !categoriesMode,
            onClick = { onSelect(false) },
            modifier = Modifier.heightIn(min = 48.dp),
            text = { Text("All Apps", style = MaterialTheme.typography.titleSmall) },
        )
    }
}

/**
 * Large category cards. The grid is adaptive: two columns on a normal phone, more on wide screens,
 * so cards stay comfortably sized instead of being squeezed or stretched.
 */
@Composable
fun CategoryCardGrid(
    groups: List<CategoryGroup>,
    animate: Boolean,
    onOpen: (CategoryGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 152.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        items(groups, key = { it.entry.category.id }, contentType = { "category" }) { group ->
            CategoryCard(group = group, animate = animate, onClick = { onOpen(group) })
        }
    }
}

@Composable
fun CategoryCard(group: CategoryGroup, animate: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // A very small press-in effect, drawn in the graphics layer so it never triggers recomposition of the card.
    val scale by animateFloatAsState(if (pressed && animate) 0.97f else 1f, label = "categoryCardPress")
    val name = group.entry.displayName
    val count = group.apps.size
    val countText = if (count == 1) "1 app" else "$count apps"

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 148.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics(mergeDescendants = true) { contentDescription = "$name, $countText" },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(CategoryVisuals.emoji(group.entry.category))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(countText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Previews read icons straight from the in-memory icon cache (no PackageManager work here).
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val shown = CategoryPreview.count(maxWidth.value, count)
                val preview = remember(group.apps, shown) { group.apps.take(shown) }
                Row(horizontalArrangement = Arrangement.spacedBy(PREVIEW_GAP)) {
                    preview.forEach { app -> AppIcon(app, PREVIEW_ICON) }
                }
            }
        }
    }
}

/** Header of an opened category: a clear way back to the category cards. */
@Composable
fun CategoryAppsHeader(group: CategoryGroup, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val count = group.apps.size
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to categories")
        }
        IconBadge(CategoryVisuals.emoji(group.entry.category), size = 36.dp)
        Column(Modifier.weight(1f)) {
            Text(group.entry.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (count == 1) "1 app" else "$count apps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
