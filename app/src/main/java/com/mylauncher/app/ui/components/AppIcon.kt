package com.mylauncher.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.mylauncher.app.data.model.AppInfo

class IconSource(
    val peek: (AppInfo) -> ImageBitmap?,
    val load: suspend (AppInfo) -> ImageBitmap?,
)

val LocalIconSource = staticCompositionLocalOf { IconSource(peek = { null }, load = { null }) }

/** Shows the real app icon; falls back to a letter badge if it cannot be loaded. */
@Composable
fun AppIcon(app: AppInfo, size: Dp, modifier: Modifier = Modifier) {
    val source = LocalIconSource.current
    val bitmap by produceState<ImageBitmap?>(source.peek(app), app.packageName, app.activityName) {
        value = source.load(app) ?: value
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = app.label.firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = (size.value * 0.4f).sp,
            )
        }
    }
}
