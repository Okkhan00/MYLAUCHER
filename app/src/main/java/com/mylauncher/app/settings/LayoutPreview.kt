package com.mylauncher.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.IconSize

/**
 * A small mock screen that shows what the chosen grid, icon size, names and dock will look like.
 * It draws plain colored shapes (no app list, no icon loading), so changing a setting updates it at
 * once without scanning apps. The colors come from the current theme, so it also follows the accent color.
 */
@Composable
fun LayoutPreview(
    columns: Int,
    iconSize: IconSize,
    showLabels: Boolean,
    modifier: Modifier = Modifier,
    dockCount: Int = 0,
    list: Boolean = false,
    caption: String,
) {
    val scheme = MaterialTheme.colorScheme
    val tints = remember(scheme) {
        listOf(
            scheme.primary, scheme.tertiary, scheme.secondary, scheme.primaryContainer,
            scheme.tertiaryContainer, scheme.secondaryContainer, scheme.inversePrimary, scheme.error,
        )
    }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.coerceAtLeast(300)
    val description = buildString {
        append(caption)
        append(": ")
        append(if (list) "list" else "$columns columns")
        append(", ${iconSize.name.lowercase()} icons, app names ")
        append(if (showLabels) "on" else "off")
        if (dockCount > 0) append(", dock of $dockCount")
    }
    val frameHeight = 290.dp

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(frameHeight)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(scheme.primaryContainer.copy(alpha = 0.55f), scheme.tertiaryContainer.copy(alpha = 0.55f)),
                ),
            )
            .clearAndSetSemantics { contentDescription = description },
    ) {
        // The mock screen is drawn at the same proportions as a real phone screen.
        val scale = maxWidth.value / screenWidthDp
        val icon: Dp = (iconSize.sizeDp * scale).dp
        val labelHeight = if (showLabels) 7.dp else 0.dp
        val rowHeight = icon + labelHeight + 10.dp
        val dockHeight = if (dockCount > 0) icon + 16.dp else 0.dp
        val available = frameHeight - dockHeight - 40.dp
        val rows = (available.value / rowHeight.value).toInt().coerceIn(1, 5)

        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 14.dp)) {
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            if (list) {
                repeat(rows) { r ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PreviewIcon(icon, tints[r % tints.size])
                        if (showLabels) {
                            Box(
                                Modifier
                                    .width(90.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(scheme.onSurface.copy(alpha = 0.45f)),
                            )
                        }
                    }
                }
            } else {
                repeat(rows) { r ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        repeat(columns) { c ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                PreviewIcon(icon, tints[(r * columns + c) % tints.size])
                                if (showLabels) {
                                    Spacer(Modifier.height(3.dp))
                                    Box(
                                        Modifier
                                            .width(icon * 0.8f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(scheme.onSurface.copy(alpha = 0.45f)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (dockCount > 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    repeat(dockCount) { i -> PreviewIcon(icon, tints[(i + 3) % tints.size]) }
                }
            }
        }
    }
}

@Composable
private fun PreviewIcon(size: Dp, tint: Color) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(tint),
    )
}
