package com.mylauncher.app.launcher.apps

import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.Folder
import com.mylauncher.app.data.model.folderIdOf
import com.mylauncher.app.data.model.folderToken
import com.mylauncher.app.data.model.isFolderToken

/** One cell on the home grid: either an app or a folder. [key] is stable and unique. */
sealed interface HomeEntry {
    val key: String

    data class AppEntry(val app: AppInfo) : HomeEntry {
        override val key: String get() = app.packageName
    }

    data class FolderEntry(val folder: Folder, val apps: List<AppInfo>) : HomeEntry {
        override val key: String get() = folderToken(folder.id)
    }

    companion object {
        /** Resolves saved tokens to entries, skipping uninstalled/hidden apps and missing folders. */
        fun resolve(
            apps: List<AppInfo>,
            tokens: List<String>,
            hidden: Set<String>,
            folders: List<Folder>,
        ): List<HomeEntry> {
            val byPackage = apps.associateBy { it.packageName }
            val folderById = folders.associateBy { it.id }
            return tokens.mapNotNull { token ->
                if (isFolderToken(token)) {
                    folderById[folderIdOf(token)]?.let { folder ->
                        FolderEntry(folder, folder.appIds.mapNotNull { byPackage[it] }.filter { it.packageName !in hidden })
                    }
                } else {
                    byPackage[token]?.takeIf { it.packageName !in hidden }?.let { AppEntry(it) }
                }
            }
        }
    }
}
