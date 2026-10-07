package com.mylauncher.app.launcher.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.mylauncher.app.data.model.AppInfo

/** Starts apps and official system screens. Never crashes; shows a short message on failure. */
object AppActions {

    fun launch(context: Context, app: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.packageName, app.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        start(context, intent, "Can't open ${app.label}")
    }

    fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        start(context, intent, "Can't open app info")
    }

    /** Uses the standard system uninstall confirmation. */
    fun uninstall(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))
        start(context, intent, "Can't uninstall this app")
    }

    fun openWallpaperPicker(context: Context) {
        val chooser = Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Choose wallpaper")
        start(context, chooser, "No wallpaper picker available")
    }

    fun start(context: Context, intent: Intent, errorMessage: String) {
        try {
            if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(context, errorMessage)
        } catch (e: SecurityException) {
            toast(context, errorMessage)
        } catch (e: Exception) {
            toast(context, errorMessage)
        }
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
