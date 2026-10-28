package com.adammg.ibridge.data.repository

import android.content.Context
import android.os.Environment
import com.adammg.ibridge.core.engine.IBridgeEngine
import com.adammg.ibridge.data.model.PhotoItem
import com.adammg.ibridge.data.model.PhotoTimelineGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PhotoRepository(private val context: Context) {

    private val engine = IBridgeEngine(context)

    private val photosCacheDir = File(context.cacheDir, "photos").apply {
        if (!exists()) mkdirs()
    }

    private val publicPhotosDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "iBridge/Photos"
    ).apply {
        if (!exists()) mkdirs()
    }

    suspend fun getTimelineGroups(): List<PhotoTimelineGroup> = withContext(Dispatchers.IO) {
        val dcimResult = engine.executeCommand("afcclient", listOf("ls", "/DCIM"))
        if (!dcimResult.isSuccess || dcimResult.stdout.isBlank()) {
            return@withContext emptyList()
        }

        // Sort DCIM folders numerically descending (e.g. 123APPLE > 119APPLE > 100APPLE) so the largest/latest number comes first
        val folders = dcimResult.stdout.lines().map { it.trim().removeSuffix("/") }
            .filter { it.isNotBlank() && it != "." && it != ".." }
            .sortedByDescending { folder ->
                folder.filter { it.isDigit() }.toIntOrNull() ?: 0
            }

        // Load the newest/highest numbered DCIM folder first for instant loading
        val latestFolder = folders.firstOrNull() ?: "100APPLE"
        val allItems = mutableListOf<PhotoItem>()

        val folderPath = "/DCIM/$latestFolder"
        val filesResult = engine.executeCommand("afcclient", listOf("ls", folderPath))

        if (filesResult.isSuccess && filesResult.stdout.isNotBlank()) {
            filesResult.stdout.lines().forEach { line ->
                val fileName = line.trim().removeSuffix("/")
                if (fileName.isNotBlank() && fileName.contains(".")) {
                    val ext = fileName.substringAfterLast(".").uppercase()
                    val isPhoto = ext in listOf("JPG", "JPEG", "PNG", "HEIC", "AAE")
                    val isVideo = ext in listOf("MOV", "MP4", "M4V")

                    if (isPhoto || isVideo) {
                        val remotePath = "$folderPath/$fileName"
                        val cachedThumbnailFile = File(photosCacheDir, "${latestFolder}_$fileName")

                        allItems.add(
                            PhotoItem(
                                fileName = fileName,
                                remotePath = remotePath,
                                extension = ext,
                                isVideo = isVideo,
                                localThumbnailPath = if (cachedThumbnailFile.exists()) cachedThumbnailFile.absolutePath else null,
                                folderName = latestFolder,
                                sizeFormatted = if (isVideo) "Video ($ext)" else "Photo ($ext)",
                                monthYearGroup = "Camera Roll $latestFolder"
                            )
                        )
                    }
                }
            }
        }

        val sortedItems = allItems.sortedByDescending { it.fileName }

        val groups = sortedItems.chunked(45).map { chunk ->
            PhotoTimelineGroup(
                monthYearTitle = "Camera Roll $latestFolder (${chunk.size} items)",
                photos = chunk
            )
        }

        groups
    }

    suspend fun downloadThumbnail(item: PhotoItem): String? = withContext(Dispatchers.IO) {
        val cachedFile = File(photosCacheDir, "${item.folderName}_${item.fileName}")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return@withContext cachedFile.absolutePath
        }

        val result = engine.executeCommand("afcclient", listOf("get", item.remotePath, cachedFile.absolutePath))
        if (result.isSuccess && cachedFile.exists()) {
            cachedFile.absolutePath
        } else {
            null
        }
    }

    suspend fun exportPhotoToDownloads(item: PhotoItem): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val targetFile = File(publicPhotosDir, item.fileName)
        val result = engine.executeCommand("afcclient", listOf("get", item.remotePath, targetFile.absolutePath))
        val isSuccess = result.isSuccess && targetFile.exists()
        val msg = if (isSuccess) "Saved ${item.fileName} to Downloads/iBridge/Photos!" else result.stderr
        Pair(isSuccess, msg)
    }

    suspend fun uploadPhotoToIphone(localFile: File): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val dcimResult = engine.executeCommand("afcclient", listOf("ls", "/DCIM"))
        val targetFolder = if (dcimResult.isSuccess && dcimResult.stdout.isNotBlank()) {
            dcimResult.stdout.lines().map { it.trim().removeSuffix("/") }
                .filter { it.isNotBlank() && it != "." && it != ".." }
                .maxByOrNull { folder -> folder.filter { it.isDigit() }.toIntOrNull() ?: 0 } ?: "100APPLE"
        } else {
            "100APPLE"
        }

        val remotePath = "/DCIM/$targetFolder/${localFile.name}"
        val result = engine.executeCommand("afcclient", listOf("put", localFile.absolutePath, remotePath))
        val isSuccess = result.isSuccess
        val msg = if (isSuccess) "Successfully uploaded ${localFile.name} to /DCIM/$targetFolder/!" else (result.stderr.ifEmpty { result.stdout })
        Pair(isSuccess, msg)
    }
}
