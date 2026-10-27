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
        val dcimFoldersResult = engine.executeCommand("afcclient", listOf("ls", "/DCIM"))
        if (!dcimFoldersResult.isSuccess || dcimFoldersResult.stdout.isBlank()) {
            return@withContext emptyList()
        }

        val folders = dcimFoldersResult.stdout.lines().map { it.trim().removeSuffix("/") }
            .filter { it.isNotBlank() && it != "." && it != ".." }
            .sortedDescending()

        val allItems = mutableListOf<PhotoItem>()

        for (folder in folders) {
            val folderPath = "/DCIM/$folder"
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
                            val cachedThumbnailFile = File(photosCacheDir, "${folder}_$fileName")

                            allItems.add(
                                PhotoItem(
                                    fileName = fileName,
                                    remotePath = remotePath,
                                    extension = ext,
                                    isVideo = isVideo,
                                    localThumbnailPath = if (cachedThumbnailFile.exists()) cachedThumbnailFile.absolutePath else null,
                                    folderName = folder,
                                    sizeFormatted = if (isVideo) "Video ($ext)" else "Photo ($ext)",
                                    monthYearGroup = "Album $folder"
                                )
                            )
                        }
                    }
                }
            }
        }

        // Sort globally from newest to oldest across all folders
        val sortedItems = allItems.sortedWith(compareByDescending<PhotoItem> { it.folderName }.thenByDescending { it.fileName })

        // Group into clean chronological sections
        val groups = sortedItems.chunked(45).mapIndexed { index, chunk ->
            PhotoTimelineGroup(
                monthYearTitle = "Photos Timeline Group ${index + 1} (${chunk.first().folderName})",
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
}
