package com.mylauncher.app.launcher.folders

class FolderActions(
    val create: (name: String) -> Unit,
    val rename: (folderId: String, name: String) -> Unit,
    val delete: (folderId: String) -> Unit,
    val addApp: (folderId: String, packageName: String) -> Unit,
    val removeApp: (folderId: String, packageName: String) -> Unit,
    val reorder: (folderId: String, visibleOrder: List<String>) -> Unit,
)
