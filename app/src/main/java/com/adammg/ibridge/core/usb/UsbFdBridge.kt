package com.adammg.ibridge.core.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class UsbConnectionState {
    object NoDeviceConnected : UsbConnectionState()
    data class DeviceFound(val deviceName: String, val vendorId: Int, val productId: Int) : UsbConnectionState()
    data class PermissionRequired(val device: UsbDevice) : UsbConnectionState()
    data class Connected(val device: UsbDevice, val fd: Int, val connection: UsbDeviceConnection) : UsbConnectionState()
    data class Error(val message: String) : UsbConnectionState()
}

class UsbFdBridge(private val context: Context) {

    private val usbManager: UsbManager =
        context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _connectionState = MutableStateFlow<UsbConnectionState>(UsbConnectionState.NoDeviceConnected)
    val connectionState: StateFlow<UsbConnectionState> = _connectionState.asStateFlow()

    private var currentConnection: UsbDeviceConnection? = null

    companion object {
        private const val TAG = "UsbFdBridge"
        const val ACTION_USB_PERMISSION = "com.adammg.ibridge.USB_PERMISSION"
        const val APPLE_VENDOR_ID = 1452 // 0x05ac (Apple Inc.)
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }

                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            com.adammg.ibridge.core.logger.AppLogger.log(TAG, "USB Permission GRANTED for Apple Device: ${device?.deviceName}")
                            device?.let { openUsbDevice(it) }
                        } else {
                            com.adammg.ibridge.core.logger.AppLogger.log(TAG, "USB Permission DENIED for device: $device")
                            _connectionState.value = UsbConnectionState.Error("USB permission denied by user")
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    com.adammg.ibridge.core.logger.AppLogger.log(TAG, "USB Device Attached")
                    scanAndConnect()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    com.adammg.ibridge.core.logger.AppLogger.log(TAG, "USB Device Detached")
                    closeConnection()
                }
            }
        }
    }

    fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
        scanAndConnect()
    }

    fun unregisterReceivers() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering USB receiver", e)
        }
        closeConnection()
    }

    fun scanAndConnect(): Boolean {
        val deviceList = usbManager.deviceList
        com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Scanning USB bus... Total devices found: ${deviceList.size}")

        for (dev in deviceList.values) {
            com.adammg.ibridge.core.logger.AppLogger.log(
                TAG,
                "Detected USB: name=${dev.deviceName}, vendorId=${dev.vendorId} (0x${Integer.toHexString(dev.vendorId)}), productId=0x${Integer.toHexString(dev.productId)}"
            )
        }

        val appleDevice = deviceList.values.firstOrNull { dev ->
            dev.vendorId == APPLE_VENDOR_ID || dev.vendorId == 0x05ac ||
                    (dev.manufacturerName?.contains("Apple", ignoreCase = true) == true)
        }

        if (appleDevice == null) {
            com.adammg.ibridge.core.logger.AppLogger.log(TAG, "No Apple USB device matched on bus")
            _connectionState.value = UsbConnectionState.NoDeviceConnected
            return false
        }

        com.adammg.ibridge.core.logger.AppLogger.log(
            TAG,
            "Apple Device Matched! Path: ${appleDevice.deviceName}, VendorID: 0x${Integer.toHexString(appleDevice.vendorId)}, ProductID: 0x${Integer.toHexString(appleDevice.productId)}"
        )

        _connectionState.value = UsbConnectionState.DeviceFound(
            deviceName = appleDevice.deviceName ?: "Apple USB Device",
            vendorId = appleDevice.vendorId,
            productId = appleDevice.productId
        )

        if (usbManager.hasPermission(appleDevice)) {
            com.adammg.ibridge.core.logger.AppLogger.log(TAG, "USB permission already granted")
            return openUsbDevice(appleDevice)
        } else {
            com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Requesting Android USB permission...")
            _connectionState.value = UsbConnectionState.PermissionRequired(appleDevice)
            requestPermission(appleDevice)
            return false
        }
    }

    fun requestPermission(device: UsbDevice) {
        com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Triggering Android OS USB Permission dialog for ${device.deviceName}...")
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val intent = Intent(ACTION_USB_PERMISSION).apply {
            setPackage(context.packageName)
        }
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            flags
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun openUsbDevice(device: UsbDevice): Boolean {
        val connection = usbManager.openDevice(device)
        if (connection == null) {
            com.adammg.ibridge.core.logger.AppLogger.log(TAG, "ERROR: UsbManager.openDevice() returned null")
            _connectionState.value = UsbConnectionState.Error("Unable to open USB device connection")
            return false
        }

        val fd = connection.fileDescriptor
        com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Opened UsbDeviceConnection successfully! Raw FD = $fd")

        var activeFd = fd
        try {
            val oldFdObj = java.io.FileDescriptor()
            val fdField = java.io.FileDescriptor::class.java.getDeclaredField("descriptor")
            fdField.isAccessible = true
            fdField.setInt(oldFdObj, fd)

            // Duplicate to FD 3 (first open non-standard file descriptor for ProcessBuilder child inheritance)
            val targetFd = 3
            try {
                android.system.Os.dup2(oldFdObj, targetFd)
                val newFdObj = java.io.FileDescriptor()
                fdField.setInt(newFdObj, targetFd)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.system.Os.fcntlInt(newFdObj, android.system.OsConstants.F_SETFD, 0)
                }
                activeFd = targetFd
                com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Duplicated USB FD $fd -> FD $targetFd and cleared FD_CLOEXEC!")
            } catch (e: Exception) {
                com.adammg.ibridge.core.logger.AppLogger.log(TAG, "dup2 to FD 3 failed, falling back to original FD $fd: ${e.message}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.system.Os.fcntlInt(oldFdObj, android.system.OsConstants.F_SETFD, 0)
                }
            }
        } catch (e: Exception) {
            com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Warning: Failed configuring FD: ${e.message}")
        }

        currentConnection = connection
        _connectionState.value = UsbConnectionState.Connected(
            device = device,
            fd = activeFd,
            connection = connection
        )
        return true
    }

    fun closeConnection() {
        try {
            currentConnection?.close()
        } catch (e: Exception) {
            // ignore
        }
        currentConnection = null

        // Force kill usbmuxd on disconnect so reconnect starts 100% fresh like a force stop!
        try {
            val p = Runtime.getRuntime().exec("pkill -9 usbmuxd")
            p.waitFor()
        } catch (e: Exception) {
            // ignore
        }

        try {
            val binaryManager = com.adammg.ibridge.core.binary.BinaryManager(context)
            if (binaryManager.socketFile.exists()) binaryManager.socketFile.delete()
            val pidFile = java.io.File(binaryManager.fVarRunDir, "usbmuxd.pid")
            if (pidFile.exists()) pidFile.delete()
        } catch (e: Exception) {
            // ignore
        }

        _connectionState.value = UsbConnectionState.NoDeviceConnected
    }
}
