package com.adammg.ibridge.ui.screens.recovery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adammg.ibridge.data.repository.RecoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecoveryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecoveryRepository(application)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _flashLog = MutableStateFlow<List<String>>(emptyList())
    val flashLog: StateFlow<List<String>> = _flashLog.asStateFlow()

    fun enterRecovery() {
        viewModelScope.launch {
            _statusMessage.value = "Sending enter recovery command..."
            val (success, msg) = repository.enterRecoveryMode()
            _statusMessage.value = msg
        }
    }

    fun exitRecovery() {
        viewModelScope.launch {
            _statusMessage.value = "Sending exit recovery command..."
            val (success, msg) = repository.exitRecoveryMode()
            _statusMessage.value = msg
        }
    }

    fun flashIpsw(ipswPath: String) {
        viewModelScope.launch {
            _flashLog.value = emptyList()
            repository.flashIpswFirmware(ipswPath).collect { line ->
                _flashLog.value = _flashLog.value + line
            }
        }
    }
}
