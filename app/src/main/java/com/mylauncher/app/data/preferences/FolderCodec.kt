package com.mylauncher.app.data.preferences

import com.mylauncher.app.data.model.Folder
import java.net.URLDecoder
import java.net.URLEncoder

/** One folder per line: id TAB url-encoded-name TAB comma-separated packages. Bad lines are skipped. */
object FolderCodec {
    fun encode(folders: List<Folder>): String = folders.joinToString("\n") { f ->
        listOf(f.id, URLEncoder.encode(f.name, "UTF-8"), f.appIds.joinToString(",")).joinToString("\t")
    }

    fun decode(raw: String?): List<Folder> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.lineSequence()
            .mapNotNull { line ->
                val parts = line.split("\t")
                if (parts.size < 2 || parts[0].isBlank()) {
                    null
                } else {
                    val name = try {
                        URLDecoder.decode(parts[1], "UTF-8")
                    } catch (e: Exception) {
                        ""
                    }
                    val apps = parts.getOrNull(2).orEmpty().split(",").filter { it.isNotBlank() }.distinct()
                    Folder(parts[0], name, apps)
                }
            }
            .distinctBy { it.id }
            .toList()
    }
}
