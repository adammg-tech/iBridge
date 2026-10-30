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

        val args = if (includeSystemApps) listOf("list", "--system") else listOf("list", "--user")
        val result = engine.executeCommand("ideviceinstaller", args)

        var output = result.stdout
        if (output.isBlank() || !output.contains("CFBundleIdentifier")) {
            val fallback = engine.executeCommand("ideviceinstaller", listOf("list", "--all"))
            if (fallback.stdout.isNotBlank()) {
                output = fallback.stdout
            }
        }

        if (output.isBlank()) return@withContext emptyList()

        val appsList = mutableListOf<IosApp>()
        val lines = output.lines()
        val isCsv = lines.firstOrNull()?.contains("CFBundleIdentifier") == true

        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank()) {
                if (isCsv && index == 0) return@forEachIndexed // skip header

                if (isCsv || trimmed.contains(',')) {
                    val parts = parseCsvLine(trimmed)
                    if (parts.size >= 3) {
                        val bundleId = parts[0].trim().removeSurrounding("\"")
                        val version = parts[1].trim().removeSurrounding("\"")
                        val appName = parts[2].trim().removeSurrounding("\"")
                        if (bundleId.isNotBlank() && bundleId.contains('.')) {
                            appsList.add(
                                IosApp(
                                    appName = appName.ifEmpty { bundleId.substringAfterLast('.') },
                                    bundleIdentifier = bundleId,
                                    version = version,
                                    isSystemApp = includeSystemApps
                                )
                            )
                        }
                    } else if (parts.size >= 1 && parts[0].contains('.')) {
                        val bundleId = parts[0].trim().removeSurrounding("\"")
                        appsList.add(
                            IosApp(
                                appName = bundleId.substringAfterLast('.'),
                                bundleIdentifier = bundleId,
                                version = "1.0",
                                isSystemApp = includeSystemApps
                            )
                        )
                    }
                } else {
                    val parts = trimmed.split(Regex("\\s+-\\s+|\\s{2,}"))
                    if (parts.size >= 2) {
                        val bundleId = parts[0].trim()
                        val nameAndVersion = parts[1].trim()

                        val versionIndex = nameAndVersion.lastIndexOf(' ')
                        val appName = if (versionIndex > 0) nameAndVersion.substring(0, versionIndex) else nameAndVersion
                        val version = if (versionIndex > 0) nameAndVersion.substring(versionIndex + 1) else "1.0"

                        appsList.add(
                            IosApp(
                                appName = appName.trim().removeSurrounding("\""),
                                bundleIdentifier = bundleId,
                                version = version.trim().removeSurrounding("\""),
                                isSystemApp = includeSystemApps
                            )
                        )
                    } else if (trimmed.contains('.')) {
                        appsList.add(
                            IosApp(
                                appName = trimmed.substringAfterLast('.'),
                                bundleIdentifier = trimmed,
                                version = "1.0",
                                isSystemApp = includeSystemApps
                            )
                        )
                    }
                }
            }
        }
        appsList.distinctBy { it.bundleIdentifier }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val curVal = StringBuilder()
        var inQuotes = false
        for (i in line.indices) {
            val c = line[i]
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(curVal.toString().trim())
                    curVal.clear()
                }
                else -> curVal.append(c)
            }
        }
        result.add(curVal.toString().trim())
        return result
    }

    suspend fun installIpa(ipaPath: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isInstallerAvailable()) return@withContext Pair(false, "ideviceinstaller binary not available")

        val result = engine.executeCommand("ideviceinstaller", listOf("install", ipaPath))
        val isSuccess = result.isSuccess
        val message = if (isSuccess) "App installed successfully!" else (result.stderr.ifEmpty { result.stdout })
        Pair(isSuccess, message)
    }

    suspend fun uninstallApp(bundleId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isInstallerAvailable()) return@withContext Pair(false, "ideviceinstaller binary not available")

        val result = engine.executeCommand("ideviceinstaller", listOf("uninstall", bundleId))
        val isSuccess = result.isSuccess
        val message = if (isSuccess) "App uninstalled successfully!" else (result.stderr.ifEmpty { result.stdout })
        Pair(isSuccess, message)
    }
}
