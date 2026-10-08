package com.mylauncher.app.wallpaper

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.BlurLevel
import com.mylauncher.app.data.model.OverlayStyle
import com.mylauncher.app.data.model.WallpaperSettings
import com.mylauncher.app.launcher.apps.AppActions
import com.mylauncher.app.settings.SliderRow
import com.mylauncher.app.ui.components.ChoiceRow
import com.mylauncher.app.ui.components.ScreenHeader
import com.mylauncher.app.ui.components.SwitchRow

/**
 * The window itself shows the real wallpaper, so the area above the controls is a true live
 * preview (with the overlay applied) and no wallpaper has to be decoded or read from storage.
 * Choosing a wallpaper uses Android's own picker, so no storage permission is needed.
 */
@Composable
fun WallpaperScreen(
    settings: WallpaperSettings,
    allowEffects: Boolean,
    onChange: (WallpaperSettings) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    WallpaperBlurEffect(WallpaperMath.blurRadiusPx(settings, allowEffects))

    Box(Modifier.fillMaxSize().background(WallpaperOverlay.color(settings))) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                ScreenHeader("Wallpaper", onBack)
            }
            // Live preview: the wallpaper (with the overlay) shows through here.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Live preview",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
                    Button(
                        onClick = { AppActions.openWallpaperPicker(context) },
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    ) { Text("Choose wallpaper") }
                    ChoiceRow(
                        "Overlay",
                        OverlayStyle.entries.map { it to it.label },
                        settings.overlay,
                    ) { onChange(settings.copy(overlay = it)) }
                    SliderRow(
                        title = "Overlay strength: ${settings.overlayPercent}%",
                        value = settings.overlayPercent.toFloat(),
                        range = WallpaperSettings.MIN_OVERLAY_PERCENT.toFloat()..WallpaperSettings.MAX_OVERLAY_PERCENT.toFloat(),
                        onChange = { onChange(settings.copy(overlayPercent = it.toInt())) },
                    )
                    if (blurSupported) {
                        ChoiceRow(
                            "Blur (experimental)",
                            BlurLevel.entries.map { it to it.label },
                            settings.blur,
                        ) { onChange(settings.copy(blur = it)) }
                        Text(
                            if (allowEffects) {
                                "Android blurs the wallpaper itself, so results depend on your device."
                            } else {
                                "Blur is paused while Performance mode is Battery Saver."
                            },
                            Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Wallpaper blur needs Android 12 or newer.",
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    SwitchRow(
                        "Overlay behind app drawer", settings.drawerOverlay, { onChange(settings.copy(drawerOverlay = it)) },
                        subtitle = "Shows through when drawer opacity is below 100% (Themes and colors)",
                    )
                    Text(
                        "Wallpaper colors for the launcher theme are in Themes and colors (Android 12+).",
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
