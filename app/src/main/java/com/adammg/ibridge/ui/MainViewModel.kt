package com.adammg.ibridge.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adammg.ibridge.core.usb.UsbConnectionState
import com.adammg.ibridge.core.usb.UsbFdBridge
import com.adammg.ibridge.data.model.DeviceInfo
import com.adammg.ibridge.data.repository.DeviceRepository
import com.adammg.ibridge.service.UsbmuxdService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val usbFdBridge = UsbFdBridge(application)
    private val deviceRepository = DeviceRepository(application)

    val usbConnectionState: StateFlow<UsbConnectionState> = usbFdBridge.connectionState

    private val _deviceInfo = MutableStateFlow(DeviceInfo())
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

    private val _isPaired = MutableStateFlow(false)
    val isPaired: StateFlow<Boolean> = _isPaired.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        clearSessionPairingCertificates()
        observeUsbState()
        usbFdBridge.registerReceivers()
    }

    private fun clearSessionPairingCertificates() {
        viewModelScope.launch {
            deviceRepository.forgetAllPairings(getApplication())
        }
    }

    private fun observeUsbState() {
        viewModelScope.launch {
            usbConnectionState.collect { state ->
                when (state) {
                    is UsbConnectionState.Connected -> {
                        stopUsbmuxdService()
                        kotlinx.coroutines.delay(300)

                        startUsbmuxdService(state.fd, state.device.deviceName)
                        kotlinx.coroutines.delay(1500)

                        val connectedDevices = deviceRepository.getConnectedDevices()
                        val activeUdid = connectedDevices.firstOrNull()?.udid

                        if (activeUdid != null && deviceRepository.hasLocalPairingCertificate(getApplication(), activeUdid)) {
                            val isAlreadyPaired = deviceRepository.validatePairing(activeUdid)
                            _isPaired.value = isAlreadyPaired
                            if (isAlreadyPaired) {
                                _deviceInfo.value = deviceRepository.getDeviceInfo()
                                _statusMessage.value = "Device Connected & Trusted!"
                            } else {
                                _statusMessage.value = "Pairing validation failed. Tap 'Trust & Pair'."
                                pairDevice()
                            }
                        } else {
                            _isPaired.value = false
                            _statusMessage.value = "New device detected! Check your iPhone for 'Trust This Computer'."
                            pairDevice()
                        }
                    }
                    else -> {
                        _isPaired.value = false
                        clearSessionPairingCertificates()
                        stopUsbmuxdService()
                    }
                }
            }
        }
    }

    private fun startUsbmuxdService(fd: Int, deviceName: String?) {
        val intent = Intent(getApplication(), UsbmuxdService::class.java).apply {
            action = UsbmuxdService.ACTION_START_USBMUXD
            putExtra(UsbmuxdService.EXTRA_USB_FD, fd)
            putExtra(UsbmuxdService.EXTRA_DEVICE_NAME, deviceName)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    private fun stopUsbmuxdService() {
        val intent = Intent(getApplication(), UsbmuxdService::class.java).apply {
            action = UsbmuxdService.ACTION_STOP_USBMUXD
        }
        getApplication<Application>().startService(intent)
    }

    fun refreshDeviceInfo() {
        viewModelScope.launch {
            _isPaired.value = deviceRepository.validatePairing()
            if (_isPaired.value) {
                _deviceInfo.value = deviceRepository.getDeviceInfo()
            }
        }
    }

    fun pairDevice() {
        val currentState = usbConnectionState.value
        if (currentState is UsbConnectionState.PermissionRequired) {
            _statusMessage.value = "Requesting USB permission for iPhone..."
            usbFdBridge.requestPermission(currentState.device)
            return
        }
        if (currentState is UsbConnectionState.DeviceFound) {
            _statusMessage.value = "Scanning and requesting USB permission..."
            usbFdBridge.scanAndConnect()
            return
        }

        viewModelScope.launch {
            _statusMessage.value = "Initiating pairing... Please tap 'Trust' on your iPhone if prompted."
            val (success, message) = deviceRepository.pairDevice()
            _isPaired.value = success
            _statusMessage.value = message
            if (success) {
                refreshDeviceInfo()
            }
        }
    }

    fun unpairDevice() {
        viewModelScope.launch {
            val success = deviceRepository.unpairDevice()
            _isPaired.value = !success
            _statusMessage.value = if (success) "Unpaired device" else "Failed to unpair"
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearSessionPairingCertificates()
        usbFdBridge.unregisterReceivers()
    }
}
