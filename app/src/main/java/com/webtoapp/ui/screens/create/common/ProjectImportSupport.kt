package com.webtoapp.ui.screens.create.common

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

internal fun parseEnvFile(file: File): Map<String, String> {
    if (!file.exists()) return emptyMap()
    return file.readLines()
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                return@mapNotNull null
            }
            val key = trimmed.substringBefore("=").trim()
            val value = trimmed.substringAfter("=").trim()
            key.takeIf { it.isNotEmpty() }?.let { it to value }
        }
        .toMap()
}

internal fun resolveDocumentTreeDirectory(treeUri: Uri): File {
    val docId = DocumentsContract.getTreeDocumentId(treeUri)
    val relativePath = docId.substringAfter(":")
    val storageRoot = if (docId.startsWith("primary:")) {
        Environment.getExternalStorageDirectory().absolutePath
    } else {
        "/storage/${docId.substringBefore(":")}"
    }
    return File(storageRoot, relativePath)
}

internal fun unwrapSingleDirectoryRoot(dir: File): File {
    val children = dir.listFiles().orEmpty().filter { !it.name.startsWith(".") }
    return if (children.size == 1 && children.first().isDirectory) {
        children.first()
    } else {
        dir
    }
}

internal fun detectPortFromText(text: String?): Int? {
    if (text.isNullOrBlank()) return null
    return Regex("""(?:PORT=|--port[= ]|listen\()(\d{4,5})""")
        .find(text)
        ?.groupValues
        ?.lastOrNull()
        ?.toIntOrNull()
}

internal fun parseVersionedList(lines: Sequence<String>): List<Pair<String, String>> {
    return lines
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("-") }
        .map { line ->
            val parts = line.split(Regex("[>=<~!]+"), limit = 2)
            val name = parts.first().trim()
            val version = if (parts.size > 1) line.substring(name.length).trim() else ""
            name to version
        }
        .toList()
}

internal fun formatProjectName(projectDir: File, fallback: String?): String {
    return fallback?.takeIf { it.isNotBlank() } ?: projectDir.name
}
