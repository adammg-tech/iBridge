package com.adammg.ibridge.ui.screens.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adammg.ibridge.data.model.DeviceInfo
import com.adammg.ibridge.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeviceRepository(application)
    private val binaryManager = com.adammg.ibridge.core.binary.BinaryManager(application)

    private val _deviceInfo = MutableStateFlow(DeviceInfo())
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val extractedBinariesCount: Int
        get() = binaryManager.binDir.listFiles()?.size ?: 0

    val extractedLibrariesCount: Int
        get() = binaryManager.libDir.listFiles()?.size ?: 0

    val binariesPath: String
        get() = binaryManager.binDir.absolutePath

    val librariesPath: String
        get() = binaryManager.libDir.absolutePath

    val socketPath: String
        get() = binaryManager.socketFile.absolutePath

    val linkerPath: String
        get() = binaryManager.getLinkerPath()

    val extractedBinariesList: List<String>
        get() = binaryManager.binDir.listFiles()?.map { it.name }?.sorted() ?: emptyList()

    fun loadDeviceInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            _deviceInfo.value = repository.getDeviceInfo()
            _isLoading.value = false
        }
    }

    fun rebootDevice() {
        viewModelScope.launch {
            _statusMessage.value = "Sending reboot command..."
            val success = repository.rebootDevice()
            _statusMessage.value = if (success) "Device is restarting!" else "Failed to send restart command"
        }
    }

    fun shutdownDevice() {
        viewModelScope.launch {
            _statusMessage.value = "Sending shutdown command..."
            val success = repository.shutdownDevice()
            _statusMessage.value = if (success) "Device shutting down!" else "Failed to send shutdown command"
        }
    }

    fun renameDevice(newName: String) {
        viewModelScope.launch {
            _statusMessage.value = "Renaming device to '$newName'..."
            val success = repository.renameDevice(newName)
            if (success) {
                _statusMessage.value = "Device successfully renamed to '$newName'!"
                loadDeviceInfo()
            } else {
                _statusMessage.value = "Failed to rename device."
            }
        }
    }

    fun refreshOverview() {
        viewModelScope.launch {
            _statusMessage.value = "Refreshing overview & device info..."
            loadDeviceInfo()
            _statusMessage.value = "Overview refreshed successfully!"
        }
    }
}
