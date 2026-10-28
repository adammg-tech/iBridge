package com.adammg.ibridge.data.model

data class IosDevice(
    val udid: String,
    val connectionType: ConnectionType = ConnectionType.USB,
    val deviceName: String = "iOS Device",
    val productType: String = "Unknown",
    val isPaired: Boolean = false
) {
    enum class ConnectionType { USB, WIFI }
}

data class DeviceInfo(
    val deviceName: String = "Unknown Device",
    val productType: String = "Unknown",
    val productVersion: String = "Unknown",
    val buildVersion: String = "Unknown",
    val serialNumber: String = "Unknown",
    val udid: String = "",
    val ecid: String = "",
    val batteryCurrentCapacity: Int = 0,
    val batteryIsCharging: Boolean = false,
    val hardwareModel: String = "",
    val cpuArchitecture: String = "",
    val wifiAddress: String = "",
    val bluetoothAddress: String = "",
    val activationState: String = "Unactivated",
    val timeZone: String = "",
    val totalStorageBytes: Long = 0L,
    val freeStorageBytes: Long = 0L,
    val usedStorageBytes: Long = 0L,
    val rawProperties: Map<String, String> = emptyMap()
)

data class IosApp(
    val appName: String,
    val bundleIdentifier: String,
    val version: String,
    val isSystemApp: Boolean = false,
    val signerIdentity: String = ""
)

data class BackupInfo(
    val backupName: String,
    val backupPath: String,
    val targetUdid: String,
    val creationDate: String,
    val sizeFormatted: String,
    val isEncrypted: Boolean = false
)

data class AfcFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0,
    val lastModified: String = ""
)

data class CrashReport(
    val fileName: String,
    val path: String,
    val sizeBytes: Long,
    val dateCreated: String
)

data class LogEntry(
    val timestamp: String,
    val processName: String,
    val processId: String,
    val logLevel: String,
    val message: String
)

data class PhotoItem(
    val fileName: String,
    val remotePath: String,
    val extension: String,
    val isVideo: Boolean = false,
    val localThumbnailPath: String? = null,
    val folderName: String = "DCIM",
    val sizeFormatted: String = "",
    val monthYearGroup: String = "Recent Photos",
    val lastModifiedMs: Long = 0L
)

data class PhotoTimelineGroup(
    val monthYearTitle: String,
    val photos: List<PhotoItem>
)
