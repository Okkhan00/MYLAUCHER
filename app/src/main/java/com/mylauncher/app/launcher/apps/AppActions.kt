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

    /** Returns false when the app could not be started (uninstalled, disabled or no launchable activity). */
    fun launch(context: Context, app: AppInfo): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.packageName, app.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return start(context, intent, "Can't open ${app.label}")
    }

    fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        start(context, intent, "Can't open app info")
    }

    /** Uses the standard system uninstall confirmation. */
    fun uninstall(context: Context, packageName: String) {
        if (packageName == context.packageName) {
            toast(context, "My Launcher can't uninstall itself from here")
            return
        }
        if (!isInstalled(context, packageName)) {
            toast(context, "This app is no longer installed")
            return
        }
        if (isSystemApp(context, packageName)) {
            toast(context, "This app came with your phone and can't be uninstalled here")
            return
        }
        start(context, uninstallIntent(packageName), "Can't uninstall this app")
    }

    /**
     * Uninstall request that asks Android to report back (RESULT_OK / RESULT_CANCELED / RESULT_FIRST_USER).
     * It still opens Android's own confirmation screen; nothing is ever uninstalled silently.
     */
    fun uninstallIntent(packageName: String): Intent =
        Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)

    /** Apps that came with the phone: Android does not let a launcher uninstall them. */
    fun isSystemApp(context: Context, packageName: String): Boolean = try {
        @Suppress("DEPRECATION")
        (context.packageManager.getApplicationInfo(packageName, 0).flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
    } catch (e: Exception) {
        false
    }

    /** What Android says right now, used after an uninstall screen closed (the result code alone is not trusted). */
    fun isInstalled(context: Context, packageName: String): Boolean = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: Exception) {
        false
    }

    /** Shares a link to the app through the standard Android share sheet (no APK is shared). */
    fun share(context: Context, app: AppInfo) {
        val text = "${app.label}: https://play.google.com/store/apps/details?id=${app.packageName}"
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        start(context, Intent.createChooser(send, "Share ${app.label}"), "Can't share this app")
    }

    /** Opens an official Android settings screen by its public intent action. */
    fun openSettingsAction(context: Context, action: String, label: String) {
        start(context, Intent(action), "Can't open $label")
    }

    /** Hands a search URL to the user's browser. Needs no INTERNET permission. */
    fun openWebSearch(context: Context, url: String) {
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE), "No browser available")
    }

    fun openWallpaperPicker(context: Context) {
        val chooser = Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Choose wallpaper")
        start(context, chooser, "No wallpaper picker available")
    }

    fun start(context: Context, intent: Intent, errorMessage: String): Boolean {
        return try {
            if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            toast(context, errorMessage)
            false
        } catch (e: SecurityException) {
            toast(context, errorMessage)
            false
        } catch (e: Exception) {
            toast(context, errorMessage)
            false
        }
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
