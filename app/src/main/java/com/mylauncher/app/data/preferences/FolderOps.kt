package com.mylauncher.app.data.preferences

import com.mylauncher.app.data.model.Folder

/** Pure folder operations. An app lives in at most one folder, so it can never be duplicated. */
object FolderOps {
    const val MAX_NAME_LENGTH = 24

    fun cleanName(name: String): String = name.trim().take(MAX_NAME_LENGTH).ifBlank { "Folder" }

    fun create(folders: List<Folder>, id: String, name: String): List<Folder> =
        if (folders.any { it.id == id }) folders else folders + Folder(id, cleanName(name), emptyList())

    fun rename(folders: List<Folder>, id: String, name: String): List<Folder> =
        folders.map { if (it.id == id) it.copy(name = cleanName(name)) else it }

    fun delete(folders: List<Folder>, id: String): List<Folder> = folders.filterNot { it.id == id }

    fun removeAppEverywhere(folders: List<Folder>, pkg: String): List<Folder> =
        folders.map { it.copy(appIds = ListOps.remove(it.appIds, pkg)) }

    /** Adds [pkg] to folder [id] and removes it from every other folder. Unknown folder: no change. */
    fun addApp(folders: List<Folder>, id: String, pkg: String): List<Folder> {
        if (folders.none { it.id == id }) return folders
        return folders.map {
            if (it.id == id) it.copy(appIds = ListOps.add(it.appIds, pkg))
            else it.copy(appIds = ListOps.remove(it.appIds, pkg))
        }
    }

    fun removeApp(folders: List<Folder>, id: String, pkg: String): List<Folder> =
        folders.map { if (it.id == id) it.copy(appIds = ListOps.remove(it.appIds, pkg)) else it }

    fun moveApp(folders: List<Folder>, id: String, pkg: String, delta: Int): List<Folder> =
        folders.map { if (it.id == id) it.copy(appIds = ListOps.move(it.appIds, pkg, delta)) else it }

    fun reorderApps(folders: List<Folder>, id: String, visibleOrder: List<String>): List<Folder> =
        folders.map { if (it.id == id) it.copy(appIds = ListOps.reorder(it.appIds, visibleOrder)) else it }
}
