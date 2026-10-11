package com.simodev.gpspositionsaver.platform

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Reads/writes backed-up text via the Storage Access Framework (replaces browser download/upload). */
object FileTransfer {

    suspend fun writeText(context: Context, uri: Uri, content: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
                true
            } catch (e: Exception) {
                false
            }
        }

    suspend fun readText(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    fun defaultFileName(format: com.simodev.gpspositionsaver.io.ExportFormat): String {
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        return "gps_places_$stamp.${format.extension}"
    }
}
