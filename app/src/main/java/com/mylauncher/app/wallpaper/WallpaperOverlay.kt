package com.mylauncher.app.wallpaper

import androidx.compose.ui.graphics.Color
import com.mylauncher.app.data.model.OverlayStyle
import com.mylauncher.app.data.model.WallpaperSettings

/** Compose-facing overlay color. The numbers come from [WallpaperMath]. */
object WallpaperOverlay {
    fun color(settings: WallpaperSettings): Color = when (settings.overlay) {
        OverlayStyle.NONE -> Color.Transparent
        OverlayStyle.DARK -> Color.Black.copy(alpha = WallpaperMath.alpha(settings))
        OverlayStyle.LIGHT -> Color.White.copy(alpha = WallpaperMath.alpha(settings))
    }
}
