package com.mylauncher.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.AppInfo

/** Click + long-click behaviour that can be switched off (used while dragging to reorder). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.tileClickable(
    interactive: Boolean,
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier {
    if (!interactive) return this
    val haptic = LocalHapticFeedback.current
    return this.combinedClickable(
        onClickLabel = "Open $label",
        onLongClickLabel = "More actions for $label",
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick()
        },
        onClick = onClick,
    )
}

@Composable
private fun TileLabel(text: String, onWallpaper: Boolean) {
    val textColor = if (onWallpaper) Color.White else MaterialTheme.colorScheme.onSurface
    val shadow = if (onWallpaper) Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 6f) else null
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelMedium.copy(
            color = textColor,
            shadow = shadow,
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier.padding(top = 6.dp).fillMaxWidth(),
    )
}

/** Grid cell: icon with optional label. Tap launches, long press opens the menu. */
@Composable
fun AppTile(
    app: AppInfo,
    iconSize: Dp,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onWallpaper: Boolean = false,
    interactive: Boolean = true,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .tileClickable(interactive, app.label, onClick, onLongClick)
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = app.label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(app, iconSize)
        if (showLabel) TileLabel(app.label, onWallpaper)
    }
}

/** Folder cell with a 2x2 preview of the first apps inside. */
@Composable
fun FolderTile(
    name: String,
    previewApps: List<AppInfo>,
    appCount: Int,
    iconSize: Dp,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onWallpaper: Boolean = false,
    interactive: Boolean = true,
) {
    val miniSize = iconSize * 0.38f
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .tileClickable(interactive, name, onClick, onLongClick)
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Folder $name, $appCount apps" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(iconSize * 0.28f))
                .background(
                    if (onWallpaper) Color.White.copy(alpha = 0.22f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                )
                .padding(iconSize * 0.07f),
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                previewApps.take(4).chunked(2).forEach { rowApps ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        rowApps.forEach { AppIcon(it, miniSize) }
                    }
                }
            }
        }
        if (showLabel) TileLabel(name, onWallpaper)
    }
}

/** List row used by the compact drawer layout. */
@Composable
fun AppRow(
    app: AppInfo,
    iconSize: Dp,
    showLabel: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .tileClickable(true, app.label, onClick, onLongClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = app.label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppIcon(app, iconSize)
        if (showLabel) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
