package com.adammg.ibridge.data.repository

import android.content.Context
import com.adammg.ibridge.core.engine.IBridgeEngine
import com.adammg.ibridge.data.model.CrashReport
import com.adammg.ibridge.data.model.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class LogRepository(private val context: Context) {

    private val engine = IBridgeEngine(context)

    fun streamLiveSyslog(): Flow<LogEntry> = flow {
        engine.streamCommand("idevicesyslog").collect { rawLine ->
            val entry = parseLogLine(rawLine)
            emit(entry)
        }
    }.flowOn(Dispatchers.IO)

    private fun parseLogLine(line: String): LogEntry {
        return try {
            val parts = line.split(" ", limit = 6)
            if (parts.size >= 6) {
                LogEntry(
                    timestamp = "${parts[0]} ${parts[1]} ${parts[2]}",
                    processName = parts[3],
                    processId = parts[4],
                    logLevel = "INFO",
                    message = parts[5]
                )
            } else {
                LogEntry("", "", "", "INFO", line)
            }
        } catch (e: Exception) {
            LogEntry("", "", "", "INFO", line)
        }
    }

    suspend fun getCrashReports(): List<CrashReport> = withContext(Dispatchers.IO) {
        val crashDir = File(context.filesDir, "crash_reports")
        if (!crashDir.exists()) crashDir.mkdirs()

        engine.executeCommand("idevicecrashreport", listOf(crashDir.absolutePath))

        val files = crashDir.listFiles() ?: return@withContext emptyList()
        files.filter { it.isFile }.map { file ->
            CrashReport(
                fileName = file.name,
                path = file.absolutePath,
                sizeBytes = file.length(),
                dateCreated = java.util.Date(file.lastModified()).toString()
            )
        }
    }
}
