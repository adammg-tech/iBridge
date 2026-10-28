package com.adammg.ibridge.data.repository

import android.content.Context
import com.adammg.ibridge.core.engine.IBridgeEngine
import com.adammg.ibridge.data.mapper.IdeviceInfoMapper
import com.adammg.ibridge.data.model.DeviceInfo
import com.adammg.ibridge.data.model.IosDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceRepository(context: Context) {

    private val engine = IBridgeEngine(context)

    suspend fun getConnectedDevices(): List<IosDevice> = withContext(Dispatchers.IO) {
        val result = engine.executeCommand("idevice_id", listOf("-l"))
        if (!result.isSuccess || result.stdout.isBlank()) return@withContext emptyList()

        result.stdout.lines().filter { it.isNotBlank() }.map { udid ->
            val pairCheck = validatePairing(udid)
            IosDevice(
                udid = udid,
                connectionType = IosDevice.ConnectionType.USB,
                isPaired = pairCheck
            )
        }
    }

    suspend fun validatePairing(udid: String? = null): Boolean = withContext(Dispatchers.IO) {
        val args = if (udid != null) listOf("validate", "-u", udid) else listOf("validate")
        val result = engine.executeCommand("idevicepair", args)
        result.isSuccess && result.stdout.contains("SUCCESS", ignoreCase = true)
    }

    suspend fun pairDevice(udid: String? = null): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val args = if (udid != null) listOf("pair", "-u", udid) else listOf("pair")
        val result = engine.executeCommand("idevicepair", args)
        val success = result.isSuccess && result.stdout.contains("SUCCESS", ignoreCase = true)
        val message = if (success) "Device paired successfully!" else (result.stderr.ifEmpty { result.stdout })
        Pair(success, message)
    }

    suspend fun unpairDevice(udid: String? = null): Boolean = withContext(Dispatchers.IO) {
        val args = if (udid != null) listOf("unpair", "-u", udid) else listOf("unpair")
        val result = engine.executeCommand("idevicepair", args)
        result.isSuccess
    }

    suspend fun getDeviceInfo(): DeviceInfo = withContext(Dispatchers.IO) {
        val rootResult = engine.executeCommand("ideviceinfo")
        val batteryResult = engine.executeCommand("ideviceinfo", listOf("-q", "com.apple.mobile.battery"))
        val diskResult = engine.executeCommand("ideviceinfo", listOf("-q", "com.apple.disk_usage"))

        val combinedOutput = buildString {
            if (rootResult.isSuccess) append(rootResult.stdout).append("\n")
            if (batteryResult.isSuccess) append(batteryResult.stdout).append("\n")
            if (diskResult.isSuccess) append(diskResult.stdout)
        }

        if (combinedOutput.isNotBlank()) {
            IdeviceInfoMapper.parseIdeviceInfoOutput(combinedOutput)
        } else {
            DeviceInfo()
        }
    }

    suspend fun renameDevice(newName: String): Boolean = withContext(Dispatchers.IO) {
        val result = engine.executeCommand("idevicename", listOf(newName))
        result.isSuccess
    }

    suspend fun rebootDevice(): Boolean = withContext(Dispatchers.IO) {
        val result = engine.executeCommand("idevicediagnostics", listOf("restart"))
        result.isSuccess
    }

    suspend fun shutdownDevice(): Boolean = withContext(Dispatchers.IO) {
        val result = engine.executeCommand("idevicediagnostics", listOf("shutdown"))
        result.isSuccess
    }

    suspend fun forgetAllPairings(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            engine.executeCommand("idevicepair", listOf("unpair"))
            val binaryManager = com.adammg.ibridge.core.binary.BinaryManager(context)
            if (binaryManager.lockdownDir.exists()) {
                binaryManager.lockdownDir.listFiles()?.forEach { it.delete() }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun hasLocalPairingCertificate(context: Context, udid: String? = null): Boolean {
        val binaryManager = com.adammg.ibridge.core.binary.BinaryManager(context)
        val lockdownDir = binaryManager.lockdownDir
        if (!lockdownDir.exists()) return false

        val files = lockdownDir.listFiles() ?: return false
        if (files.isEmpty()) return false

        if (udid != null) {
            return java.io.File(lockdownDir, "$udid.plist").exists()
        }
        return files.any { it.name.endsWith(".plist") && it.length() > 0 }
    }
}
