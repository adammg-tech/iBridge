package com.adammg.ibridge.core.engine

import android.content.Context
import android.util.Log
import com.adammg.ibridge.core.binary.BinaryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val isSuccess: Boolean = exitCode == 0
)

class IBridgeEngine(context: Context) {

    private val binaryManager = BinaryManager(context)

    companion object {
        private const val TAG = "IBridgeEngine"
    }

    suspend fun executeCommand(
        binaryName: String,
        args: List<String> = emptyList()
    ): CommandResult = withContext(Dispatchers.IO) {
        var binaryFile = java.io.File(binaryManager.getBinaryPath(binaryName))
        if (!binaryFile.exists() || binaryFile.length() == 0L) {
            com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Binary $binaryName missing or empty, calling prepareEnvironment()...")
            binaryManager.prepareEnvironment()
            binaryFile = java.io.File(binaryManager.getBinaryPath(binaryName))
        }

        val linkerPath = binaryManager.getLinkerPath()
        val commandList = mutableListOf(linkerPath, binaryFile.absolutePath).apply { addAll(args) }

        val pb = ProcessBuilder(commandList)
        val env = pb.environment()
        env.putAll(binaryManager.getEnvironmentVariables())

        try {
            val cmdStr = commandList.joinToString(" ")
            com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Executing: $cmdStr (binarySize=${binaryFile.length()} bytes)")
            val process = pb.start()

            val stdoutDeferred = async(Dispatchers.IO) {
                val builder = StringBuilder()
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        builder.append(line).append("\n")
                    }
                }
                builder.toString().trim()
            }

            val stderrDeferred = async(Dispatchers.IO) {
                val builder = StringBuilder()
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        builder.append(line).append("\n")
                    }
                }
                builder.toString().trim()
            }

            val exitCode = process.waitFor()
            val stdoutText = stdoutDeferred.await()
            val stderrText = stderrDeferred.await()

            com.adammg.ibridge.core.logger.AppLogger.log(
                TAG,
                "Result $binaryName (exitCode=$exitCode)\nSTDOUT: ${stdoutText.ifEmpty { "(empty)" }}\nSTDERR: ${stderrText.ifEmpty { "(empty)" }}"
            )
            CommandResult(exitCode, stdoutText, stderrText)
        } catch (e: Exception) {
            com.adammg.ibridge.core.logger.AppLogger.log(TAG, "ERROR executing $binaryName: ${e.message}")
            CommandResult(-1, "", e.message ?: "Execution failed")
        }
    }

    fun streamCommand(
        binaryName: String,
        args: List<String> = emptyList()
    ): Flow<String> = flow {
        val linkerPath = binaryManager.getLinkerPath()
        val binaryPath = binaryManager.getBinaryPath(binaryName)
        val commandList = mutableListOf(linkerPath, binaryPath).apply { addAll(args) }

        try {
            val pb = ProcessBuilder(commandList)
            val env = pb.environment()
            env.putAll(binaryManager.getEnvironmentVariables())

            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            try {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { emit(it) }
                }
            } finally {
                process.destroyForcibly()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in streamCommand for $binaryName", e)
            emit("Error starting $binaryName stream: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    fun isBinaryInstalled(binaryName: String): Boolean {
        return binaryManager.isBinaryAvailable(binaryName)
    }
}
