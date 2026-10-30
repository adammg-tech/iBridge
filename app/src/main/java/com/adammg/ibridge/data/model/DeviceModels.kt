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
) {
    val marketingName: String
        get() = getMarketingDeviceName(productType)
}

fun getMarketingDeviceName(productType: String): String {
    if (productType.isBlank() || productType == "Unknown") return "iOS Device"
    return when (productType.trim()) {
        "iPhone1,1" -> "iPhone (1st gen)"
        "iPhone1,2" -> "iPhone 3G"
        "iPhone2,1" -> "iPhone 3GS"
        "iPhone3,1", "iPhone3,2", "iPhone3,3" -> "iPhone 4"
        "iPhone4,1" -> "iPhone 4S"
        "iPhone5,1", "iPhone5,2" -> "iPhone 5"
        "iPhone5,3", "iPhone5,4" -> "iPhone 5c"
        "iPhone6,1", "iPhone6,2" -> "iPhone 5s"
        "iPhone7,2" -> "iPhone 6"
        "iPhone7,1" -> "iPhone 6 Plus"
        "iPhone8,1" -> "iPhone 6s"
        "iPhone8,2" -> "iPhone 6s Plus"
        "iPhone8,4" -> "iPhone SE (1st gen)"
        "iPhone9,1", "iPhone9,3" -> "iPhone 7"
        "iPhone9,2", "iPhone9,4" -> "iPhone 7 Plus"
        "iPhone10,1", "iPhone10,4" -> "iPhone 8"
        "iPhone10,2", "iPhone10,5" -> "iPhone 8 Plus"
        "iPhone10,3", "iPhone10,6" -> "iPhone X"
        "iPhone11,2" -> "iPhone XS"
        "iPhone11,4", "iPhone11,6" -> "iPhone XS Max"
        "iPhone11,8" -> "iPhone XR"
        "iPhone12,1" -> "iPhone 11"
        "iPhone12,3" -> "iPhone 11 Pro"
        "iPhone12,5" -> "iPhone 11 Pro Max"
        "iPhone12,8" -> "iPhone SE (2nd gen)"
        "iPhone13,1" -> "iPhone 12 mini"
        "iPhone13,2" -> "iPhone 12"
        "iPhone13,3" -> "iPhone 12 Pro"
        "iPhone13,4" -> "iPhone 12 Pro Max"
        "iPhone14,4" -> "iPhone 13 mini"
        "iPhone14,5" -> "iPhone 13"
        "iPhone14,2" -> "iPhone 13 Pro"
        "iPhone14,3" -> "iPhone 13 Pro Max"
        "iPhone14,6" -> "iPhone SE (3rd gen)"
        "iPhone14,7" -> "iPhone 14"
        "iPhone14,8" -> "iPhone 14 Plus"
        "iPhone15,2" -> "iPhone 14 Pro"
        "iPhone15,3" -> "iPhone 14 Pro Max"
        "iPhone15,4" -> "iPhone 15"
        "iPhone15,5" -> "iPhone 15 Plus"
        "iPhone16,1" -> "iPhone 15 Pro"
        "iPhone16,2" -> "iPhone 15 Pro Max"
        "iPhone17,3" -> "iPhone 16"
        "iPhone17,4" -> "iPhone 16 Plus"
        "iPhone17,1" -> "iPhone 16 Pro"
        "iPhone17,2" -> "iPhone 16 Pro Max"
        "iPhone17,5" -> "iPhone 16e"
        "iPhone18,1" -> "iPhone 17 Pro"
        "iPhone18,2" -> "iPhone 17 Pro Max"
        "iPhone18,3" -> "iPhone 17"
        "iPhone18,4" -> "iPhone Air"
        "iPhone18,5" -> "iPhone 17e"
        "iPhone19,2" -> "iPhone 18 Pro"
        "iPhone19,3" -> "iPhone 18 Pro Max"
        "iPhone19,4" -> "iPhone Duo"
        "iPhone19,7" -> "iPhone 18 Pro Max (Regional)"
        else -> if (productType.startsWith("iPhone")) productType else "iPhone ($productType)"
    }
}

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
