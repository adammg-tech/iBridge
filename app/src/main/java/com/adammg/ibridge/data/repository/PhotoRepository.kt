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

    suspend fun getDcimFolders(): List<String> = withContext(Dispatchers.IO) {
        val dcimResult = engine.executeCommand("afcclient", listOf("ls", "/DCIM"))
        if (!dcimResult.isSuccess || dcimResult.stdout.isBlank()) {
            return@withContext emptyList()
        }

        dcimResult.stdout.lines().map { it.trim().removeSuffix("/") }
            .filter { it.isNotBlank() && it != "." && it != ".." }
            .sortedByDescending { folder ->
                folder.filter { it.isDigit() }.toIntOrNull() ?: 0
            }
    }

    suspend fun getPhotosForFolder(folderName: String): List<PhotoItem> = withContext(Dispatchers.IO) {
        val folderPath = "/DCIM/$folderName"
        val filesResult = engine.executeCommand("afcclient", listOf("ls", folderPath))
        if (!filesResult.isSuccess || filesResult.stdout.isBlank()) {
            return@withContext emptyList()
        }

        val allItems = mutableListOf<PhotoItem>()
        filesResult.stdout.lines().forEach { line ->
            val fileName = line.trim().removeSuffix("/")
            if (fileName.isNotBlank() && fileName.contains(".")) {
                val ext = fileName.substringAfterLast(".").uppercase()
                val isPhoto = ext in listOf("JPG", "JPEG", "PNG", "HEIC", "AAE")
                val isVideo = ext in listOf("MOV", "MP4", "M4V")

                if (isPhoto || isVideo) {
                    val remotePath = "$folderPath/$fileName"
                    val cachedThumbnailFile = File(photosCacheDir, "${folderName}_$fileName")

                    allItems.add(
                        PhotoItem(
                            fileName = fileName,
                            remotePath = remotePath,
                            extension = ext,
                            isVideo = isVideo,
                            localThumbnailPath = if (cachedThumbnailFile.exists()) cachedThumbnailFile.absolutePath else null,
                            folderName = folderName,
                            sizeFormatted = if (isVideo) "Video ($ext)" else "Photo ($ext)",
                            monthYearGroup = "Camera Roll $folderName"
                        )
                    )
                }
            }
        }

        // Sort from newest (top) to oldest (bottom)
        allItems.sortedByDescending { it.fileName }
    }

    suspend fun getTimelineGroups(): List<PhotoTimelineGroup> = withContext(Dispatchers.IO) {
        val folders = getDcimFolders()
        val latestFolder = folders.firstOrNull() ?: "100APPLE"
        val sortedItems = getPhotosForFolder(latestFolder)

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
            try {
                if (item.isVideo) {
                    val retriever = android.media.MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(cachedFile.absolutePath)
                        val frameBitmap = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        if (frameBitmap != null) {
                            java.io.FileOutputStream(cachedFile).use { fos ->
                                frameBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, fos)
                            }
                            frameBitmap.recycle()
                        }
                    } catch (_: Exception) {
                        // fallback
                    } finally {
                        retriever.release()
                    }
                } else {
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    android.graphics.BitmapFactory.decodeFile(cachedFile.absolutePath, options)
                    options.inSampleSize = calculateInSampleSize(options, 300, 300)
                    options.inJustDecodeBounds = false

                    val bitmap = android.graphics.BitmapFactory.decodeFile(cachedFile.absolutePath, options)
                    if (bitmap != null) {
                        val exif = android.media.ExifInterface(cachedFile.absolutePath)
                        val orientation = exif.getAttributeInt(
                            android.media.ExifInterface.TAG_ORIENTATION,
                            android.media.ExifInterface.ORIENTATION_UNDEFINED
                        )
                        val matrix = android.graphics.Matrix()
                        when (orientation) {
                            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                            android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                            android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                        }
                        val processed = if (!matrix.isIdentity) {
                            android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                                if (it != bitmap) bitmap.recycle()
                            }
                        } else {
                            bitmap
                        }

                        java.io.FileOutputStream(cachedFile).use { fos ->
                            processed.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, fos)
                        }
                        processed.recycle()
                    }
                }
            } catch (_: Exception) {
                // Ignore errors
            }
            cachedFile.absolutePath
        } else {
            null
        }
    }

    private fun calculateInSampleSize(options: android.graphics.BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    suspend fun exportPhotoToDownloads(item: PhotoItem): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val targetFile = File(publicPhotosDir, item.fileName)
        val result = engine.executeCommand("afcclient", listOf("get", item.remotePath, targetFile.absolutePath))
        val isSuccess = result.isSuccess && targetFile.exists()
        val msg = if (isSuccess) "Saved ${item.fileName} to Downloads/iBridge/Photos!" else result.stderr
        Pair(isSuccess, msg)
    }
}
