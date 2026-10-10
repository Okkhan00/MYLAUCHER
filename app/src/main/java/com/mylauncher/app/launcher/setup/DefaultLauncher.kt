package com.mylauncher.app.launcher.setup

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.mylauncher.app.launcher.apps.AppActions

/** Detects and requests the Android HOME role using only public, non-privileged APIs. */
object DefaultLauncher {

    data class Status(
        val isDefault: Boolean,
        /** Package of the current default home app, or null when none is chosen. */
        val currentPackage: String?,
        val currentLabel: String?,
    )

    fun status(context: Context): Status {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
        } catch (e: Exception) {
            null
        }
        val pkg = resolved?.activityInfo?.packageName
        // "android" is the system chooser, i.e. the user has not picked a default yet.
        if (pkg == null || pkg == "android") return Status(false, null, null)
        val label = try {
            resolved.activityInfo.applicationInfo.loadLabel(pm).toString()
        } catch (e: Exception) {
            pkg
        }
        return Status(isDefault = pkg == context.packageName, currentPackage = pkg, currentLabel = label)
    }

    /** Android 10+: the system "set as default home app" dialog. Null if unavailable. */
    fun createRoleRequestIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < 29) return null
        return try {
            val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Opens Android's Home app settings, falling back to broader settings screens. */
    fun openHomeSettings(context: Context) {
        val candidates = listOf(
            Settings.ACTION_HOME_SETTINGS,
            Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
            Settings.ACTION_SETTINGS,
        )
        for (action in candidates) {
            val intent = Intent(action)
            if (intent.resolveActivity(context.packageManager) != null) {
                AppActions.start(context, intent, "Can't open settings")
                return
            }
        }
        AppActions.start(context, Intent(Settings.ACTION_SETTINGS), "Can't open settings")
    }
}
