package com.adammg.ibridge.data.repository

import android.content.Context
import com.adammg.ibridge.core.engine.IBridgeEngine
import com.adammg.ibridge.data.model.IosApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(context: Context) {

    private val engine = IBridgeEngine(context)

    fun isInstallerAvailable(): Boolean {
        return engine.isBinaryInstalled("ideviceinstaller")
    }

    suspend fun listApps(includeSystemApps: Boolean = false): List<IosApp> = withContext(Dispatchers.IO) {
        if (!isInstallerAvailable()) return@withContext emptyList()

        val args = if (includeSystemApps) listOf("-l", "-o", "list_system") else listOf("-l")
        val result = engine.executeCommand("ideviceinstaller", args)

        if (!result.isSuccess || result.stdout.isBlank()) return@withContext emptyList()

        val appsList = mutableListOf<IosApp>()
        result.stdout.lines().forEach { line ->
            val parts = line.split(" - ")
            if (parts.size >= 2) {
                val bundleId = parts[0].trim()
                val nameAndVersion = parts[1].trim()

                val versionIndex = nameAndVersion.lastIndexOf(' ')
                val appName = if (versionIndex > 0) nameAndVersion.substring(0, versionIndex) else nameAndVersion
                val version = if (versionIndex > 0) nameAndVersion.substring(versionIndex + 1) else "1.0"

                appsList.add(
                    IosApp(
                        appName = appName,
                        bundleIdentifier = bundleId,
                        version = version,
                        isSystemApp = includeSystemApps
                    )
                )
            }
        }
        appsList
    }

    suspend fun installIpa(ipaPath: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isInstallerAvailable()) return@withContext Pair(false, "ideviceinstaller binary not available")

        val result = engine.executeCommand("ideviceinstaller", listOf("-i", ipaPath))
        val isSuccess = result.isSuccess && result.stdout.contains("Complete", ignoreCase = true)
        val message = if (isSuccess) "App installed successfully!" else (result.stderr.ifEmpty { result.stdout })
        Pair(isSuccess, message)
    }

    suspend fun uninstallApp(bundleId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isInstallerAvailable()) return@withContext Pair(false, "ideviceinstaller binary not available")

        val result = engine.executeCommand("ideviceinstaller", listOf("-U", bundleId))
        val isSuccess = result.isSuccess && result.stdout.contains("Complete", ignoreCase = true)
        val message = if (isSuccess) "App uninstalled successfully!" else (result.stderr.ifEmpty { result.stdout })
        Pair(isSuccess, message)
    }
}
