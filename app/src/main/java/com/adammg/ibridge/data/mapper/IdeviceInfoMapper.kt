package com.adammg.ibridge.data.mapper

import com.adammg.ibridge.data.model.DeviceInfo

object IdeviceInfoMapper {

    fun parseIdeviceInfoOutput(rawOutput: String): DeviceInfo {
        val properties = mutableMapOf<String, String>()

        rawOutput.lines().forEach { line ->
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim()
                val value = line.substring(colonIndex + 1).trim()
                properties[key] = value
            }
        }

        return DeviceInfo(
            deviceName = properties["DeviceName"] ?: properties["MarketName"] ?: "iOS Device",
            productType = properties["ProductType"] ?: "Unknown",
            productVersion = properties["ProductVersion"] ?: "Unknown",
            buildVersion = properties["BuildVersion"] ?: "",
            serialNumber = properties["SerialNumber"] ?: "",
            udid = properties["UniqueDeviceID"] ?: properties["UniqueChipID"] ?: "",
            ecid = properties["UniqueChipID"] ?: "",
            batteryCurrentCapacity = properties["BatteryCurrentCapacity"]?.toIntOrNull() ?: 100,
            batteryIsCharging = properties["BatteryIsCharging"]?.lowercase() == "true",
            hardwareModel = properties["HardwareModel"] ?: properties["ModelNumber"] ?: "",
            cpuArchitecture = properties["CPUArchitecture"] ?: "arm64",
            wifiAddress = properties["WiFiAddress"] ?: "",
            bluetoothAddress = properties["BluetoothAddress"] ?: "",
            activationState = properties["ActivationState"] ?: "Activated",
            timeZone = properties["TimeZone"] ?: "",
            rawProperties = properties
        )
    }
}
