package com.mylauncher.app.privacy

import com.mylauncher.app.data.model.SecuritySettings

/**
 * Launcher-level App Lock rules. Android does not let a normal launcher intercept apps that are
 * opened some other way (notifications, recent apps, widgets, shortcuts, other launchers), so this
 * only decides whether launching *from My Launcher* needs the launcher PIN or biometrics.
 */
object AppLockPolicy {
    /** True when [packageName] is protected and a launcher PIN exists to protect it with. */
    fun requiresAuth(packageName: String, security: SecuritySettings, lockedApps: Set<String>): Boolean =
        security.lockEnabled && packageName in lockedApps

    /** Locked apps whose protection is currently inactive because no PIN is set. */
    fun inactiveLocks(security: SecuritySettings, lockedApps: Set<String>): Set<String> =
        if (security.lockEnabled) emptySet() else lockedApps

    const val LIMITATION_TEXT =
        "App Lock protects opening an app from My Launcher only. Android does not allow a launcher to " +
            "block apps opened from notifications, recent apps, widgets, app shortcuts or another " +
            "launcher, and this app does not use an Accessibility Service to try. For full protection " +
            "use your phone's own app lock or work/private profile."

    const val PRIVATE_TEXT =
        "Private apps are hidden inside My Launcher only. They stay installed and can still appear in " +
            "Android Settings, other launchers and system app lists."
}
