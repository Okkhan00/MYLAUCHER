package com.mylauncher.app.launcher.appdrawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mylauncher.app.categories.CategoryEntry
import com.mylauncher.app.categories.CategoryVisuals
import com.mylauncher.app.ui.components.IconBadge

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
 * Title of one category page: icon, name, app count and a small position indicator (dots, or "3 / 14" when there
 * are many categories). Tapping the title opens the category list so a far-away category is one tap away.
 */
@Composable
fun CategoryPageHeader(
    entry: CategoryEntry,
    appCount: Int,
    index: Int,
    total: Int,
    onJump: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val countText = if (appCount == 1) "1 app" else "$appCount apps"
    Column(modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClickLabel = "Choose a category", onClick = onJump),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadge(CategoryVisuals.emoji(entry), size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text(entry.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(countText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "\u2039 \u203A",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = "Swipe left or right for other categories" },
            )
        }
        PageIndicator(index = index, total = total, modifier = Modifier.padding(top = 2.dp))
    }
}

/** Subtle dots for the current category; a plain "n / total" when there are too many for dots. */
@Composable
fun PageIndicator(index: Int, total: Int, modifier: Modifier = Modifier) {
    if (total <= 1) return
    if (total > MAX_DOTS) {
        Text(
            "${index + 1} / $total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val active = i == index
            Box(
                Modifier
                    .size(if (active) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    ),
            )
        }
    }
}

private const val MAX_DOTS = 14
