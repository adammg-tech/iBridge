package com.adammg.ibridge.data.repository

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import com.adammg.ibridge.core.engine.IBridgeEngine
import com.dd.plist.NSDictionary
import com.dd.plist.PropertyListParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

class RingtoneRepository(private val context: Context) {

    private val engine = IBridgeEngine(context)
    private val cacheDir = File(context.cacheDir, "ringtones").apply {
        if (!exists()) mkdirs()
    }
    private val backupsDir = File(context.filesDir, "ringtone-backups").apply {
        if (!exists()) mkdirs()
    }

    init {
        inspectUploadedPlist()
    }

    private fun inspectUploadedPlist() {
        try {
            val file = File("C:/Users/amgal/Downloads/Ringtones Newest - Modified.plist")
            if (file.exists()) {
                val root = PropertyListParser.parse(file) as NSDictionary
                val ringtones = root.objectForKey("Ringtones") as? NSDictionary
                ringtones?.allKeys()?.forEach { key ->
                    val valDict = ringtones.objectForKey(key) as? NSDictionary
                    val name = valDict?.objectForKey("Name")?.toString()
                    val guid = valDict?.objectForKey("GUID")?.toString()
                    val time = valDict?.objectForKey("Total Time")?.toString()
                    android.util.Log.d("PlistInspect", "Key: $key -> Name: $name, GUID: $guid, Time: $time")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PlistInspect", "Error inspecting plist", e)
        }
    }

    /**
     * Dynamically discovers the ringtone directory and Ringtones.plist path via AFC.
     * Locks in valid discovered paths like /Purchases immediately without fallback overwriting.
     */
    suspend fun discoverRingtonePaths(): Pair<String, String> = withContext(Dispatchers.IO) {
        var ringtoneDir = "/Purchases"
        var plistPath = "/iTunes_Control/iTunes/Ringtones.plist"

        val foundFolder = scanForRingtoneFolder()
        if (foundFolder != null) {
            ringtoneDir = foundFolder
        }

        val checkPlists = listOf(
            "$ringtoneDir/Ringtones.plist",
            "$ringtoneDir/../iTunes/Ringtones.plist",
            "/iTunes_Control/iTunes/Ringtones.plist",
            "/Media/iTunes_Control/iTunes/Ringtones.plist",
            "/Purchases/Ringtones.plist"
        )

        for (p in checkPlists) {
            val localTest = File(cacheDir, "test.plist")
            val res = engine.executeCommand("afcclient", listOf("get", p, localTest.absolutePath))
            if (res.isSuccess && localTest.exists() && localTest.length() > 0) {
                plistPath = p
                localTest.delete()
                break
            }
            if (localTest.exists()) localTest.delete()
        }

        Pair(ringtoneDir, plistPath)
    }

    /**
     * Scans all accessible AFC folders (excluding DCIM and Photos) to find the ringtone folder.
     */
    suspend fun scanForRingtoneFolder(): String? = withContext(Dispatchers.IO) {
        val excludeKeywords = listOf("dcim", "photos")
        val queue = java.util.LinkedList<String>()
        queue.add("/")

        val visited = mutableSetOf<String>()

        while (queue.isNotEmpty() && visited.size < 150) {
            val currentDir = queue.poll() ?: break
            if (!visited.add(currentDir)) continue

            val lowerCurrent = currentDir.lowercase()
            if (excludeKeywords.any { lowerCurrent.contains(it) }) continue

            val lsRes = engine.executeCommand("afcclient", listOf("ls", currentDir))
            if (lsRes.isSuccess && lsRes.stdout.isNotBlank()) {
                val files = lsRes.stdout.lines().map { it.trim().removeSuffix("/") }.filter { it.isNotBlank() && it != "." && it != ".." }
                if (files.any { it.endsWith(".m4r", ignoreCase = true) || it.equals("Ringtones.plist", ignoreCase = true) }) {
                    return@withContext currentDir
                }

                for (file in files) {
                    val subPath = if (currentDir == "/") "/$file" else "$currentDir/$file"
                    val lowerSub = subPath.lowercase()
                    if (excludeKeywords.none { lowerSub.contains(it) }) {
                        queue.add(subPath)
                    }
                }
            }
        }
        null
    }

    /**
     * Calculates SHA-256 checksum of a file.
     */
    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    var lastFFmpegLog: String = ""
        private set

    /**
     * Converts any input audio URI (MP3, WAV, M4A, AAC) to AAC-compatible .m4r format using FFmpeg with optional start/end trimming.
     */
    suspend fun convertToM4rWithTrim(audioUri: Uri, startTimeSec: Float, endTimeSec: Float): Pair<Boolean, File?> = withContext(Dispatchers.IO) {
        val inputTemp = File(cacheDir, "input_audio_${System.currentTimeMillis()}")
        val uuid = UUID.randomUUID().toString().uppercase()
        val outputFile = File(cacheDir, "import_$uuid.m4r")

        try {
            context.contentResolver.openInputStream(audioUri)?.use { input ->
                FileOutputStream(inputTemp).use { output ->
                    input.copyTo(output)
                }
            }

            if (!inputTemp.exists() || inputTemp.length() == 0L) {
                lastFFmpegLog = "Input audio file stream could not be opened or file was empty."
                return@withContext Pair(false, null)
            }

            val duration = endTimeSec - startTimeSec
            val cmd = if (startTimeSec > 0f || duration > 0f) {
                "-y -vn -i ${inputTemp.absolutePath} -ss $startTimeSec -t $duration -c:a aac -b:a 192k -f ipod ${outputFile.absolutePath}"
            } else {
                "-y -vn -i ${inputTemp.absolutePath} -c:a aac -b:a 192k -f ipod ${outputFile.absolutePath}"
            }

            val session = com.antonkarpenko.ffmpegkit.FFmpegKit.execute(cmd)
            lastFFmpegLog = "Command: $cmd\nReturn Code: ${session.returnCode}\nLogs:\n${session.output}"
            android.util.Log.d("FFmpegLog", lastFFmpegLog)
            inputTemp.delete()

            if (com.antonkarpenko.ffmpegkit.ReturnCode.isSuccess(session.returnCode) && outputFile.exists() && outputFile.length() > 0) {
                Pair(true, outputFile)
            } else {
                if (outputFile.exists()) outputFile.delete()
                Pair(false, null)
            }
        } catch (e: Exception) {
            lastFFmpegLog = "FFmpeg Exception: ${e.message}"
            try { inputTemp.delete() } catch (_: Exception) {}
            try { if (outputFile.exists()) outputFile.delete() } catch (_: Exception) {}
            Pair(false, null)
        }
    }

    private fun transcodeToAacM4r(
        inputFile: File,
        outputFile: File,
        startTimeSec: Float,
        endTimeSec: Float
    ): Boolean {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            extractor = MediaExtractor().apply { setDataSource(inputFile.absolutePath) }
            var trackIndex = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    inputFormat = fmt
                    break
                }
            }

            if (trackIndex == -1 || inputFormat == null) return false
            extractor.selectTrack(trackIndex)

            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: ""

            // Direct remux for native AAC
            if (mime == MediaFormat.MIMETYPE_AUDIO_AAC || mime == "audio/mp4a-latm") {
                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val writeTrackIndex = muxer.addTrack(inputFormat)
                muxer.start()

                val startUs = (startTimeSec * 1_000_000L).toLong()
                val endUs = (endTimeSec * 1_000_000L).toLong()
                if (startUs > 0) extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                val buffer = java.nio.ByteBuffer.allocate(1024 * 64)
                val bufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    if (endUs > 0 && bufferInfo.presentationTimeUs > endUs) break
                    bufferInfo.flags = 0
                    muxer.writeSampleData(writeTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }

                muxer.stop()
                muxer.release()
                extractor.release()
                return outputFile.exists() && outputFile.length() > 0
            }

            // For non-AAC audio (MP3, WAV, OGG, etc.), decode to PCM and encode to AAC
            val sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            val channelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2

            val outputFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128000)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerTrackIndex = -1
            var muxerStarted = false

            val startUs = (startTimeSec * 1_000_000L).toLong()
            val endUs = (endTimeSec * 1_000_000L).toLong()
            if (startUs > 0) extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val info = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var sawDecoderEOS = false
            var sawEncoderEOS = false

            while (!sawEncoderEOS) {
                if (!sawInputEOS) {
                    val inputBufIndex = decoder.dequeueInputBuffer(10000)
                    if (inputBufIndex >= 0) {
                        val inputBuf = decoder.getInputBuffer(inputBufIndex) ?: continue
                        val sampleSize = extractor.readSampleData(inputBuf, 0)
                        val presentationTimeUs = extractor.sampleTime

                        if (sampleSize < 0 || (endUs > 0 && presentationTimeUs > endUs)) {
                            decoder.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEOS = true
                        } else {
                            decoder.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                if (!sawDecoderEOS) {
                    val decStatus = decoder.dequeueOutputBuffer(info, 10000)
                    if (decStatus >= 0) {
                        val decBuf = decoder.getOutputBuffer(decStatus) ?: continue
                        val isEOS = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0

                        val encInputBufIndex = encoder.dequeueInputBuffer(10000)
                        if (encInputBufIndex >= 0) {
                            val encInputBuf = encoder.getInputBuffer(encInputBufIndex) ?: continue
                            encInputBuf.clear()
                            encInputBuf.put(decBuf)
                            encoder.queueInputBuffer(
                                encInputBufIndex,
                                0,
                                info.size,
                                info.presentationTimeUs,
                                if (isEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                            )
                        }
                        decoder.releaseOutputBuffer(decStatus, false)
                        if (isEOS) sawDecoderEOS = true
                    }
                }

                val encStatus = encoder.dequeueOutputBuffer(info, 10000)
                if (encStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    muxerTrackIndex = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    muxerStarted = true
                } else if (encStatus >= 0) {
                    val encBuf = encoder.getOutputBuffer(encStatus) ?: continue
                    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        info.size = 0
                    }
                    if (info.size > 0 && muxerStarted) {
                        muxer.writeSampleData(muxerTrackIndex, encBuf, info)
                    }
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawEncoderEOS = true
                    }
                    encoder.releaseOutputBuffer(encStatus, false)
                }
            }

            try { decoder.stop(); decoder.release() } catch (_: Exception) {}
            try { encoder.stop(); encoder.release() } catch (_: Exception) {}
            try { if (muxerStarted) { muxer.stop(); muxer.release() } } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}

            return outputFile.exists() && outputFile.length() > 0
        } catch (e: Exception) {
            try { decoder?.release() } catch (_: Exception) {}
            try { encoder?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
            return false
        }
    }

    /**
     * Imports custom ringtone using dd-plist to parse and serialize Apple binary property list (bplist00) format.
     */
    @Suppress("DEPRECATION")
    suspend fun importRingtoneWithFile(
        m4rFile: File,
        ringtoneName: String,
        selectedDirectory: String,
        onProgress: (String) -> Unit
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            onProgress("1. Discovering Ringtones.plist and ringtone directory on iPhone...")
            val plistPath = findPlistPath(selectedDirectory)
                ?: return@withContext Pair(false, "Could not locate Ringtones.plist in the selected directory.")

            onProgress("2. Downloading original Ringtones.plist and verifying SHA-256 backup...")
            val originalPlistFile = File(cacheDir, "original_Ringtones.plist")
            val downloadRes = engine.executeCommand("afcclient", listOf("get", plistPath, originalPlistFile.absolutePath))
            if (!downloadRes.isSuccess || !originalPlistFile.exists() || originalPlistFile.length() == 0L) {
                return@withContext Pair(false, "Failed to download original Ringtones.plist for backup. Aborting import.")
            }

            val originalSize = originalPlistFile.length()
            val originalHash = calculateSha256(originalPlistFile)

            val backupFile = File(backupsDir, "Ringtones_${System.currentTimeMillis()}.plist.bak")
            originalPlistFile.copyTo(backupFile, overwrite = true)

            if (!backupFile.exists() || backupFile.length() != originalSize || calculateSha256(backupFile) != originalHash) {
                originalPlistFile.delete()
                return@withContext Pair(false, "Backup verification failed (Size or SHA-256 mismatch). Aborting import.")
            }

            saveCopyToPublicDownloads(backupFile)

            onProgress("3. Checking exact audio duration...")
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(m4rFile.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val exactDurationMs = durationStr?.toLongOrNull() ?: 20282L
            retriever.release()

            val uuid = m4rFile.nameWithoutExtension.removePrefix("import_")
            val remoteM4rPath = "$selectedDirectory/${m4rFile.name}"

            onProgress("4. Transferring .m4r ringtone file to iPhone (${m4rFile.name})...")
            var m4rTransferred = false
            engine.executeCommand("afcclient", listOf("rm", remoteM4rPath))
            val transferResult = engine.executeCommand("afcclient", listOf("put", m4rFile.absolutePath, remoteM4rPath))
            if (transferResult.isSuccess) {
                m4rTransferred = true
            } else {
                originalPlistFile.delete()
                return@withContext Pair(false, "AFC file transfer failed: ${transferResult.stderr}")
            }

            onProgress("5. Parsing and updating Ringtones.plist as binary property list (bplist00)...")
            val rootDict = PropertyListParser.parse(originalPlistFile) as NSDictionary
            var ringtonesDict = rootDict.objectForKey("Ringtones") as? NSDictionary
            if (ringtonesDict == null) {
                ringtonesDict = NSDictionary()
                rootDict.put("Ringtones", ringtonesDict)
            }

            val newEntry = NSDictionary().apply {
                put("Name", ringtoneName)
                put("Purchased", false)
                put("Total Time", exactDurationMs.toInt())
                put("Protected Content", false)
                put("GUID", uuid)
            }

            ringtonesDict.put("import_$uuid.m4r", newEntry)

            PropertyListParser.saveAsBinary(rootDict, originalPlistFile)

            onProgress("6. Uploading updated Ringtones.plist to iPhone...")
            engine.executeCommand("afcclient", listOf("rm", plistPath))
            val uploadRes = engine.executeCommand("afcclient", listOf("put", originalPlistFile.absolutePath, plistPath))
            if (!uploadRes.isSuccess) {
                rollback(plistPath, backupFile, remoteM4rPath, m4rTransferred)
                return@withContext Pair(false, "Failed to upload updated Ringtones.plist. Changes rolled back: ${uploadRes.stderr}")
            }

            originalPlistFile.delete()
            onProgress("7. Import completed successfully!")
            Pair(true, "Ringtone successfully imported as binary plist (bplist00)! Note: iOS requires Settings app restart or device respring to index new custom ringtones.")
        } catch (e: Exception) {
            Pair(false, "Import failed with exception: ${e.message}")
        }
    }

    private suspend fun rollback(plistPath: String, backupFile: File, remoteM4rPath: String, m4rTransferred: Boolean) {
        try {
            engine.executeCommand("afcclient", listOf("rm", plistPath))
            engine.executeCommand("afcclient", listOf("put", backupFile.absolutePath, plistPath))
            if (m4rTransferred) {
                engine.executeCommand("afcclient", listOf("rm", remoteM4rPath))
            }
        } catch (_: Exception) {
            // ignore rollback failures
        }
    }

    private fun saveCopyToPublicDownloads(sourceFile: File) {
        try {
            val downloadsDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "iBridge")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = File(downloadsDir, "Ringtones_original_${System.currentTimeMillis()}.plist")
            sourceFile.copyTo(destFile, overwrite = true)
        } catch (_: Exception) {}
    }

    private suspend fun findPlistPath(selectedDir: String): String? {
        val candidates = listOf(
            "$selectedDir/Ringtones.plist",
            "$selectedDir/../iTunes/Ringtones.plist",
            "/iTunes_Control/iTunes/Ringtones.plist",
            "/Media/iTunes_Control/iTunes/Ringtones.plist"
        )
        for (p in candidates) {
            val test = File(cacheDir, "test_check.plist")
            val res = engine.executeCommand("afcclient", listOf("get", p, test.absolutePath))
            if (res.isSuccess && test.exists() && test.length() > 0) {
                test.delete()
                return p
            }
            if (test.exists()) test.delete()
        }
        return null
    }

    suspend fun getAvailableBackups(): List<File> = withContext(Dispatchers.IO) {
        backupsDir.listFiles()?.filter { it.name.endsWith(".bak") }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    suspend fun restoreBackup(backupFile: File): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val paths = discoverRingtonePaths()
        val plistPath = paths.second
        engine.executeCommand("afcclient", listOf("rm", plistPath))
        val res = engine.executeCommand("afcclient", listOf("put", backupFile.absolutePath, plistPath))
        if (res.isSuccess) {
            Pair(true, "Successfully restored Ringtones.plist from backup!")
        } else {
            Pair(false, "Failed to restore backup: ${res.stderr}")
        }
    }

    data class ExistingRingtone(val filename: String, val name: String, val guid: String, val totalTime: Int)

    suspend fun loadExistingRingtonesForDirectory(selectedDir: String): List<ExistingRingtone> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ExistingRingtone>()
        try {
            val plistPath = findPlistPath(selectedDir) ?: "/iTunes_Control/iTunes/Ringtones.plist"
            val localPlist = File(cacheDir, "read_ringtones_${System.currentTimeMillis()}.plist")
            val res = engine.executeCommand("afcclient", listOf("get", plistPath, localPlist.absolutePath))
            if (res.isSuccess && localPlist.exists()) {
                val rootDict = PropertyListParser.parse(localPlist) as NSDictionary
                val ringtonesDict = rootDict.objectForKey("Ringtones") as? NSDictionary
                ringtonesDict?.allKeys()?.forEach { filename ->
                    val valDict = ringtonesDict.objectForKey(filename) as? NSDictionary

                    val nameObj = valDict?.objectForKey("Name")
                    val name = when (nameObj) {
                        is com.dd.plist.NSString -> nameObj.content
                        else -> nameObj?.toString() ?: "Unknown"
                    }

                    val guidObj = valDict?.objectForKey("GUID")
                    val guid = when (guidObj) {
                        is com.dd.plist.NSString -> guidObj.content
                        else -> guidObj?.toString() ?: ""
                    }

                    val timeObj = valDict?.objectForKey("Total Time")
                    val totalTime = when (timeObj) {
                        is com.dd.plist.NSNumber -> timeObj.intValue()
                        else -> timeObj?.toString()?.toIntOrNull() ?: 0
                    }

                    list.add(ExistingRingtone(filename, name, guid, totalTime))
                }
                localPlist.delete()
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun exportRingtoneFile(filename: String, selectedDir: String, destinationFile: File): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val remotePath = "$selectedDir/$filename"
            val res = engine.executeCommand("afcclient", listOf("get", remotePath, destinationFile.absolutePath))
            if (res.isSuccess && destinationFile.exists() && destinationFile.length() > 0) {
                Pair(true, "Ringtone successfully saved to ${destinationFile.absolutePath}")
            } else {
                Pair(false, "Failed to download ringtone file: ${res.stderr}")
            }
        } catch (e: Exception) {
            Pair(false, "Export failed: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    suspend fun removeRingtone(filename: String, selectedDirectory: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val plistPath = findPlistPath(selectedDirectory)
                ?: return@withContext Pair(false, "Could not locate Ringtones.plist.")

            val originalPlistFile = File(cacheDir, "original_remove.plist")
            val downloadRes = engine.executeCommand("afcclient", listOf("get", plistPath, originalPlistFile.absolutePath))
            if (!downloadRes.isSuccess || !originalPlistFile.exists()) {
                return@withContext Pair(false, "Failed to download Ringtones.plist for removal backup.")
            }

            val backupFile = File(backupsDir, "Ringtones_${System.currentTimeMillis()}.plist.bak")
            originalPlistFile.copyTo(backupFile, overwrite = true)
            saveCopyToPublicDownloads(backupFile)

            val rootDict = PropertyListParser.parse(originalPlistFile) as NSDictionary
            val ringtonesDict = rootDict.objectForKey("Ringtones") as? NSDictionary
            if (ringtonesDict != null) {
                ringtonesDict.remove(filename)
                PropertyListParser.saveAsBinary(rootDict, originalPlistFile)
            }

            engine.executeCommand("afcclient", listOf("rm", plistPath))
            val uploadRes = engine.executeCommand("afcclient", listOf("put", originalPlistFile.absolutePath, plistPath))
            if (!uploadRes.isSuccess) {
                engine.executeCommand("afcclient", listOf("put", backupFile.absolutePath, plistPath))
                return@withContext Pair(false, "Failed to update plist. Restored backup.")
            }

            val remoteM4rPath = "$selectedDirectory/$filename"
            engine.executeCommand("afcclient", listOf("rm", remoteM4rPath))

            originalPlistFile.delete()
            Pair(true, "Ringtone successfully removed!")
        } catch (e: Exception) {
            Pair(false, "Removal failed: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    suspend fun renameRingtone(filename: String, newName: String, selectedDirectory: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val plistPath = findPlistPath(selectedDirectory)
                ?: return@withContext Pair(false, "Could not locate Ringtones.plist.")

            val originalPlistFile = File(cacheDir, "original_rename.plist")
            val downloadRes = engine.executeCommand("afcclient", listOf("get", plistPath, originalPlistFile.absolutePath))
            if (!downloadRes.isSuccess || !originalPlistFile.exists()) {
                return@withContext Pair(false, "Failed to download Ringtones.plist for rename backup.")
            }

            val backupFile = File(backupsDir, "Ringtones_${System.currentTimeMillis()}.plist.bak")
            originalPlistFile.copyTo(backupFile, overwrite = true)
            saveCopyToPublicDownloads(backupFile)

            val rootDict = PropertyListParser.parse(originalPlistFile) as NSDictionary
            val ringtonesDict = rootDict.objectForKey("Ringtones") as? NSDictionary
            val valDict = ringtonesDict?.objectForKey(filename) as? NSDictionary
            if (valDict != null) {
                valDict.put("Name", newName)
                PropertyListParser.saveAsBinary(rootDict, originalPlistFile)
            } else {
                return@withContext Pair(false, "Ringtone entry not found in plist.")
            }

            engine.executeCommand("afcclient", listOf("rm", plistPath))
            val uploadRes = engine.executeCommand("afcclient", listOf("put", originalPlistFile.absolutePath, plistPath))
            if (!uploadRes.isSuccess) {
                engine.executeCommand("afcclient", listOf("put", backupFile.absolutePath, plistPath))
                return@withContext Pair(false, "Failed to update plist. Restored backup.")
            }

            originalPlistFile.delete()
            Pair(true, "Ringtone successfully renamed!")
        } catch (e: Exception) {
            Pair(false, "Rename failed: ${e.message}")
        }
    }
}
