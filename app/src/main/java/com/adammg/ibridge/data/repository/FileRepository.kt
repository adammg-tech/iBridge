package com.adammg.ibridge.data.repository

import android.content.Context
import com.adammg.ibridge.core.engine.IBridgeEngine
import com.adammg.ibridge.data.model.AfcFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileRepository(context: Context) {

    private val engine = IBridgeEngine(context)

    suspend fun listFiles(remotePath: String = "/"): List<AfcFile> = withContext(Dispatchers.IO) {
        val result = engine.executeCommand("afcclient", listOf("ls", remotePath))
        if (!result.isSuccess || result.stdout.isBlank()) {
            return@withContext defaultAfcDirectories()
        }

        val items = mutableListOf<AfcFile>()
        result.stdout.lines().forEach { line ->
            val cleanName = line.trim()
            if (cleanName.isNotBlank() && cleanName != "." && cleanName != "..") {
                val isDir = cleanName.endsWith("/") || !cleanName.contains(".")
                val name = cleanName.removeSuffix("/")
                val fullPath = if (remotePath == "/") "/$name" else "$remotePath/$name"

                items.add(
                    AfcFile(
                        name = name,
                        path = fullPath,
                        isDirectory = isDir
                    )
                )
            }
        }
        if (items.isEmpty()) defaultAfcDirectories() else items
    }

    private fun defaultAfcDirectories(): List<AfcFile> {
        return listOf(
            AfcFile("DCIM (Photos)", "/DCIM", isDirectory = true),
            AfcFile("Downloads", "/Downloads", isDirectory = true),
            AfcFile("Books", "/Books", isDirectory = true),
            AfcFile("Podcasts", "/Podcasts", isDirectory = true),
            AfcFile("Recordings", "/Recordings", isDirectory = true),
            AfcFile("Photos", "/Photos", isDirectory = true)
        )
    }

    suspend fun uploadFile(localPath: String, remotePath: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val result = engine.executeCommand("afcclient", listOf("put", localPath, remotePath))
        val success = result.isSuccess
        val msg = if (success) "File uploaded successfully" else result.stderr
        Pair(success, msg)
    }

    suspend fun downloadFile(remotePath: String, localPath: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val result = engine.executeCommand("afcclient", listOf("get", remotePath, localPath))
        val success = result.isSuccess
        val msg = if (success) "File downloaded successfully to $localPath" else result.stderr
        Pair(success, msg)
    }

    suspend fun downloadFileToOutputStream(remotePath: String, outputStream: java.io.OutputStream, context: Context): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val tempFile = java.io.File(context.cacheDir, "temp_download_${System.currentTimeMillis()}")
        try {
            val result = engine.executeCommand("afcclient", listOf("get", remotePath, tempFile.absolutePath))
            if (result.isSuccess && tempFile.exists()) {
                tempFile.inputStream().use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile.delete()
                Pair(true, "File extracted successfully!")
            } else {
                Pair(false, result.stderr.ifEmpty { "Download failed" })
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "Download failed")
        }
    }

    suspend fun deleteFile(remotePath: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val result = engine.executeCommand("afcclient", listOf("rm", remotePath))
        val success = result.isSuccess
        val msg = if (success) "File deleted" else result.stderr
        Pair(success, msg)
    }
}
