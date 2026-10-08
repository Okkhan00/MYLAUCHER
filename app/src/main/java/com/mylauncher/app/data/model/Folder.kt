package com.mylauncher.app.data.model

data class Folder(val id: String, val name: String, val appIds: List<String>)

/**
 * Folders sit in the same ordered home list as favorite apps. A folder is stored there as
 * "folder:<id>"; package names can never contain ':' so the two cannot be confused.
 */
const val FOLDER_TOKEN_PREFIX = "folder:"

fun folderToken(id: String): String = FOLDER_TOKEN_PREFIX + id

fun isFolderToken(token: String): Boolean = token.startsWith(FOLDER_TOKEN_PREFIX)

fun folderIdOf(token: String): String = token.removePrefix(FOLDER_TOKEN_PREFIX)
