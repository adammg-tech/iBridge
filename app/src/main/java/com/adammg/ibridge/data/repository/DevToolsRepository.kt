package com.adammg.ibridge.data.repository

import android.content.Context
import com.adammg.ibridge.core.engine.IBridgeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DevToolsRepository(context: Context) {

    private val engine = IBridgeEngine(context)

    suspend fun setGpsLocation(latitude: Double, longitude: Double): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!engine.isBinaryInstalled("idevicelocation")) {
            return@withContext Pair(false, "idevicelocation binary not available")
        }
        val result = engine.executeCommand("idevicelocation", listOf(latitude.toString(), longitude.toString()))
        val isSuccess = result.isSuccess
        val message = if (isSuccess) "Location set to ($latitude, $longitude)" else result.stderr
        Pair(isSuccess, message)
    }

    suspend fun resetGpsLocation(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!engine.isBinaryInstalled("idevicelocation")) {
            return@withContext Pair(false, "idevicelocation binary not available")
        }
        val result = engine.executeCommand("idevicelocation", listOf("reset"))
        val isSuccess = result.isSuccess
        val message = if (isSuccess) "GPS location reset to real position" else result.stderr
        Pair(isSuccess, message)
    }

    suspend fun mountDeveloperImage(imagePath: String, signaturePath: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val result = engine.executeCommand("ideviceimagemounter", listOf(imagePath, signaturePath))
        val isSuccess = result.isSuccess
        val message = if (isSuccess) "Developer Disk Image mounted successfully!" else result.stderr
        Pair(isSuccess, message)
    }

    suspend fun listProvisioningProfiles(): List<String> = withContext(Dispatchers.IO) {
        val result = engine.executeCommand("ideviceprovision", listOf("list"))
        if (!result.isSuccess || result.stdout.isBlank()) return@withContext emptyList()
        result.stdout.lines().filter { it.isNotBlank() }
    }
}
