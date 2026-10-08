package com.mylauncher.app.wallpaper

import com.mylauncher.app.data.model.OverlayStyle
import com.mylauncher.app.data.model.WallpaperSettings

/** Pure wallpaper numbers (unit-testable). */
object WallpaperMath {
    /** Overlay opacity 0..1. */
    fun alpha(settings: WallpaperSettings): Float =
        if (settings.overlay == OverlayStyle.NONE) 0f
        else settings.overlayPercent.coerceIn(WallpaperSettings.MIN_OVERLAY_PERCENT, WallpaperSettings.MAX_OVERLAY_PERCENT) / 100f

    /** Blur radius in pixels to request from the window compositor; 0 when off or not allowed (Battery Saver). */
    fun blurRadiusPx(settings: WallpaperSettings, allowEffects: Boolean): Int =
        if (allowEffects) settings.blur.radiusPx else 0
}
