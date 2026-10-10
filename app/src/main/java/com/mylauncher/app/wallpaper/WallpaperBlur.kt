package com.mylauncher.app.wallpaper

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Asks Android to blur what is behind the launcher window (the wallpaper) on Android 12+.
 * This is a compositor feature: it is skipped when the device has blur disabled (for example in
 * battery saver) and the system decides how it looks, so it is labelled experimental in settings.
 * Nothing is decoded or processed by the app itself, so it cannot block the UI thread.
 */
@Composable
fun WallpaperBlurEffect(radiusPx: Int) {
    val view = LocalView.current
    DisposableEffect(radiusPx, view) {
        val activity = view.context as? Activity
        var applied = false
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val window = activity.window
                val wm = activity.getSystemService(WindowManager::class.java)
                if (radiusPx > 0 && wm != null && wm.isCrossWindowBlurEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val attrs = window.attributes
                    attrs.blurBehindRadius = radiusPx
                    window.attributes = attrs
                    applied = true
                }
            } catch (e: Exception) {
                // Blur is optional; ignore devices that refuse it.
            }
        }
        onDispose {
            if (applied && activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val attrs = activity.window.attributes
                    attrs.blurBehindRadius = 0
                    activity.window.attributes = attrs
                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                } catch (e: Exception) {
                    // Nothing to restore.
                }
            }
        }
    }
}
