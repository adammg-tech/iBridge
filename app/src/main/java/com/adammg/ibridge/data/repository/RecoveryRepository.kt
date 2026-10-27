package com.adammg.ibridge.data.repository

import android.content.Context
import com.adammg.ibridge.core.engine.IBridgeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class RecoveryRepository(context: Context) {

    private val engine = IBridgeEngine(context)

    suspend fun enterRecoveryMode(udid: String? = null): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val args = if (udid != null) listOf(udid) else emptyList()
        val result = engine.executeCommand("ideviceenterrecovery", args)
        val isSuccess = result.isSuccess
        val message = if (isSuccess) "Device commanded to enter Recovery Mode" else result.stderr
        Pair(isSuccess, message)
    }

    suspend fun exitRecoveryMode(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!engine.isBinaryInstalled("irecovery")) {
            return@withContext Pair(false, "irecovery binary not available")
        }
        val result = engine.executeCommand("irecovery", listOf("-n"))
        val isSuccess = result.isSuccess
        val message = if (isSuccess) "Commanded exit from Recovery Mode" else result.stderr
        Pair(isSuccess, message)
    }

    fun flashIpswFirmware(ipswPath: String): Flow<String> = flow {
        if (!engine.isBinaryInstalled("idevicerestore")) {
            emit("Error: idevicerestore binary is not available")
            return@flow
        }
        emit("Initiating IPSW firmware flash for: $ipswPath")
        engine.streamCommand("idevicerestore", listOf("-e", ipswPath)).collect { line ->
            emit(line)
        }
    }.flowOn(Dispatchers.IO)
}
