package com.mylauncher.app.device

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.mylauncher.app.launcher.home.BatteryInfo
import com.mylauncher.app.launcher.home.BatteryReader

data class DeviceSnapshot(
    val androidVersion: String,
    val sdkInt: Int,
    val deviceName: String,
    val battery: BatteryInfo?,
    val storage: StorageInfo?,
    val appVersion: String,
)

/** Reads local device facts. No permissions, nothing is stored or sent anywhere. */
object DeviceReader {
    fun read(context: Context): DeviceSnapshot {
        val storage = try {
            val stat = StatFs(Environment.getDataDirectory().path)
            StorageInfo(stat.totalBytes, stat.availableBytes)
        } catch (e: Exception) {
            null
        }
        val appVersion = try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            null
        } ?: "unknown"
        val maker = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val model = Build.MODEL.orEmpty()
        val name = if (model.startsWith(maker, ignoreCase = true) || maker.isEmpty()) model else "$maker $model"
        return DeviceSnapshot(
            androidVersion = Build.VERSION.RELEASE.orEmpty(),
            sdkInt = Build.VERSION.SDK_INT,
            deviceName = name.ifBlank { "Unknown device" },
            battery = BatteryReader.read(context),
            storage = storage,
            appVersion = appVersion,
        )
    }
}
