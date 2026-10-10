package com.mylauncher.app.launcher.home

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mylauncher.app.data.model.ClockStyle
import com.mylauncher.app.data.model.LauncherSettings
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits at the start of every minute. Collected with lifecycle, so it stops when the app is hidden. */
private fun minuteTicker(): Flow<LocalDateTime> = flow {
    while (true) {
        val now = LocalDateTime.now()
        emit(now)
        delay(60_000L - (now.second * 1_000L + now.nano / 1_000_000L))
    }
}

@Composable
fun ClockBlock(settings: LauncherSettings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val now by remember { minuteTicker() }.collectAsStateWithLifecycle(initialValue = LocalDateTime.now())
    val is24Hour = DateFormat.is24HourFormat(context)
    val locale = Locale.getDefault()
    val timeFormatter = remember(is24Hour, locale) {
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm", locale)
    }
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"), locale)
    }

    val (size, weight) = when (settings.clockStyle) {
        ClockStyle.DIGITAL -> 64.sp to FontWeight.Light
        ClockStyle.LARGE -> 96.sp to FontWeight.Light
        ClockStyle.MINIMAL -> 32.sp to FontWeight.Normal
    }
    val shadow = Shadow(Color.Black.copy(alpha = 0.5f), blurRadius = 8f)
    // Sticky battery read (no receiver, no permission); refreshed every minute with the clock.
    val battery = if (settings.showBattery) remember(now) { BatteryReader.read(context) } else null

    Column(modifier) {
        Text(
            text = now.format(timeFormatter),
            style = MaterialTheme.typography.displayLarge.copy(
                color = Color.White,
                fontSize = size,
                lineHeight = size,
                fontWeight = weight,
                shadow = shadow,
            ),
        )
        if (battery != null) {
            Text(
                text = if (battery.charging) "${battery.percent}% \u00B7 Charging" else "${battery.percent}%",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.85f), shadow = shadow),
            )
        }
        if (settings.showDate) {
            Text(
                text = now.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium.copy(color = Color.White.copy(alpha = 0.92f), shadow = shadow),
            )
        }
    }
}
