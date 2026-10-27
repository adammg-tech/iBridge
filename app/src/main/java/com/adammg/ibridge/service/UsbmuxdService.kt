package com.adammg.ibridge.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.adammg.ibridge.R
import com.adammg.ibridge.core.binary.BinaryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class UsbmuxdService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var binaryManager: BinaryManager
    private var usbmuxdProcess: Process? = null

    companion object {
        private const val TAG = "UsbmuxdService"
        private const val CHANNEL_ID = "usbmuxd_service_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_USBMUXD = "com.adammg.ibridge.START_USBMUXD"
        const val ACTION_STOP_USBMUXD = "com.adammg.ibridge.STOP_USBMUXD"
        const val EXTRA_USB_FD = "extra_usb_fd"
        const val EXTRA_DEVICE_NAME = "extra_device_name"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _daemonLogs = MutableStateFlow<List<String>>(emptyList())
        val daemonLogs: StateFlow<List<String>> = _daemonLogs.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        binaryManager = BinaryManager(this)
        binaryManager.prepareEnvironment()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_USBMUXD

        when (action) {
            ACTION_START_USBMUXD -> {
                val usbFd = intent?.getIntExtra(EXTRA_USB_FD, -1)
                val deviceName = intent?.getStringExtra(EXTRA_DEVICE_NAME)
                startForegroundServiceNotification("Connecting to iOS Device...")
                if (usbFd != null && usbFd != -1) {
                    startUsbmuxdDaemon(usbFd, deviceName)
                } else {
                    Log.w(TAG, "Starting usbmuxd without USB FD")
                    startUsbmuxdDaemon(null, deviceName)
                }
            }
            ACTION_STOP_USBMUXD -> {
                stopUsbmuxdDaemon()
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startUsbmuxdDaemon(usbFd: Int?, deviceName: String?) {
        stopUsbmuxdDaemon()

        serviceScope.launch {
            try {
                // Clean up stale lockfiles and sockets from previous sessions
                val pidFile = java.io.File(binaryManager.fVarRunDir, "usbmuxd.pid")
                if (pidFile.exists()) pidFile.delete()
                if (binaryManager.socketFile.exists()) binaryManager.socketFile.delete()

                _daemonLogs.value = listOf("Starting usbmuxd daemon (FD=$usbFd, Device=$deviceName)...")
                val usbmuxdPath = binaryManager.getBinaryPath("usbmuxd")
                val socketPath = binaryManager.socketFile.absolutePath

                // Set native C process environment variables
                try {
                    usbFd?.let {
                        android.system.Os.setenv("TERMUX_USB_FD", it.toString(), true)
                        android.system.Os.setenv("LIBUSB_FD", it.toString(), true)
                    }
                    if (!deviceName.isNullOrBlank()) {
                        android.system.Os.setenv("TERMUX_USB_DEV_PATH", deviceName, true)
                        val parts = deviceName.split("/")
                        if (parts.size >= 2) {
                            val busStr = (parts[parts.size - 2].toIntOrNull() ?: 1).toString()
                            val devStr = (parts.last().toIntOrNull() ?: 1).toString()
                            android.system.Os.setenv("TERMUX_USB_BUS", busStr, true)
                            android.system.Os.setenv("TERMUX_USB_DEV", devStr, true)
                            android.system.Os.setenv("LIBUSB_BUS", busStr, true)
                            android.system.Os.setenv("LIBUSB_DEV", devStr, true)
                        }
                    }
                    android.system.Os.setenv("USBMUXD_SOCKET_ADDRESS", socketPath, true)
                    android.system.Os.setenv("USBMUXD_SOCKET_PATH", socketPath, true)
                    android.system.Os.setenv("LD_LIBRARY_PATH", binaryManager.libDir.absolutePath, true)
                    android.system.Os.setenv("HOME", binaryManager.filesDir.absolutePath, true)
                    android.system.Os.setenv("PREFIX", binaryManager.filesDir.absolutePath, true)
                } catch (e: Exception) {
                    com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Warning setenv: ${e.message}")
                }

                com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Launching usbmuxd via Pure Native C Engine (FD=$usbFd)...")
                if (usbFd != null && usbFd != -1) {
                    val nativePid = com.adammg.ibridge.core.native.IBridgeNativeEngine.startNativeUsbmuxd(
                        usbFd,
                        socketPath,
                        usbmuxdPath,
                        binaryManager.libDir.absolutePath
                    )
                    com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Native usbmuxd child PID: $nativePid")
                    _isRunning.value = true
                    updateNotification("iOS Device Connected & Active")
                    _daemonLogs.value = listOf("usbmuxd started successfully via Native C Engine (PID $nativePid)")
                } else {
                    val execCmd = "exec ${binaryManager.getLinkerPath()} $usbmuxdPath -f -v"
                    val commandList = listOf("/system/bin/sh", "-c", execCmd)

                    val pb = ProcessBuilder(commandList)
                    pb.redirectErrorStream(true)

                    val env = pb.environment()
                    env.putAll(binaryManager.getEnvironmentVariables(usbFd, deviceName))

                    com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Launching via Shell exec: $execCmd")
                    com.adammg.ibridge.core.logger.AppLogger.log(TAG, "Dynamic Environment: TERMUX_USB_FD=$usbFd, DEV=$deviceName, SOCKET=$socketPath")

                    val process = pb.start()
                    usbmuxdProcess = process
                    _isRunning.value = true

                    updateNotification("iOS Device Connected & Active")

                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { logLine ->
                            Log.d(TAG, "[usbmuxd] $logLine")
                            com.adammg.ibridge.core.logger.AppLogger.log("usbmuxd", logLine)
                            _daemonLogs.value = (_daemonLogs.value + logLine).takeLast(100)
                        }
                    }

                    val exitCode = process.waitFor()
                    com.adammg.ibridge.core.logger.AppLogger.log(TAG, "usbmuxd process exited with code $exitCode")
                    _daemonLogs.value = _daemonLogs.value + "usbmuxd exited with code $exitCode"
                }
            } catch (e: Exception) {
                com.adammg.ibridge.core.logger.AppLogger.log(TAG, "usbmuxd Error: ${e.message}")
                _daemonLogs.value = _daemonLogs.value + "usbmuxd Error: ${e.message}"
                updateNotification("usbmuxd Error: ${e.message}")
            } finally {
                _isRunning.value = false
            }
        }
    }

    private fun stopUsbmuxdDaemon() {
        try {
            usbmuxdProcess?.destroyForcibly()
        } catch (e: Exception) {
            // ignore
        }
        usbmuxdProcess = null

        // Force kill any orphaned usbmuxd processes to release sockets and file descriptors
        try {
            val p = Runtime.getRuntime().exec("pkill -9 usbmuxd")
            p.waitFor()
        } catch (e: Exception) {
            // ignore
        }

        try {
            val pidFile = java.io.File(binaryManager.fVarRunDir, "usbmuxd.pid")
            if (pidFile.exists()) pidFile.delete()
            if (binaryManager.socketFile.exists()) binaryManager.socketFile.delete()
        } catch (e: Exception) {
            // ignore
        }
        _isRunning.value = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "iOS USB Daemon Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps usbmuxd active in background for connected iOS device"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification(status: String) {
        val notification = buildNotification(status)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(status: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(status))
    }

    private fun buildNotification(status: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("iBridge iOS Connection")
            .setContentText(status)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUsbmuxdDaemon()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
