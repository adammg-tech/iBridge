package com.adammg.ibridge.data.repository

import android.content.Context
import com.adammg.ibridge.core.engine.IBridgeEngine
import com.adammg.ibridge.data.model.BackupInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRepository(private val context: Context) {

    private val engine = IBridgeEngine(context)

    fun getBackupDirectory(): File {
        val backupFolder = File(context.filesDir, "backups")
        if (!backupFolder.exists()) backupFolder.mkdirs()
        return backupFolder
    }

    suspend fun listLocalBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        val backupDir = getBackupDirectory()
        val files = backupDir.listFiles() ?: return@withContext emptyList()

        files.filter { it.isDirectory }.map { dir ->
            val sizeMb = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } / (1024 * 1024)
            val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(dir.lastModified()))

            BackupInfo(
                backupName = dir.name,
                backupPath = dir.absolutePath,
                targetUdid = dir.name,
                creationDate = dateFormatted,
                sizeFormatted = "$sizeMb MB"
            )
        }
    }

    fun createBackup(targetUdid: String? = null): Flow<String> = flow {
        val backupDir = getBackupDirectory()
        val args = mutableListOf("backup", backupDir.absolutePath)
        targetUdid?.let { args.addAll(listOf("-u", it)) }

        emit("Starting backup to ${backupDir.absolutePath}...")
        engine.streamCommand("idevicebackup2", args).collect { line ->
            emit(line)
        }
    }.flowOn(Dispatchers.IO)

    fun restoreBackup(backupName: String, targetUdid: String? = null): Flow<String> = flow {
        val backupDir = getBackupDirectory()
        val args = mutableListOf("restore", backupDir.absolutePath)
        targetUdid?.let { args.addAll(listOf("-u", it)) }

        emit("Starting restore from $backupName...")
        engine.streamCommand("idevicebackup2", args).collect { line ->
            emit(line)
        }
    }.flowOn(Dispatchers.IO)
}
