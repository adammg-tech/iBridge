package com.adammg.ibridge.ui.screens.backup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adammg.ibridge.data.model.BackupInfo
import com.adammg.ibridge.data.repository.BackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BackupRepository(application)

    private val _backups = MutableStateFlow<List<BackupInfo>>(emptyList())
    val backups: StateFlow<List<BackupInfo>> = _backups.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _logStream = MutableStateFlow<List<String>>(emptyList())
    val logStream: StateFlow<List<String>> = _logStream.asStateFlow()

    fun loadBackups() {
        viewModelScope.launch {
            _backups.value = repository.listLocalBackups()
        }
    }

    fun startBackup(udid: String? = null) {
        viewModelScope.launch {
            _isBackingUp.value = true
            _logStream.value = emptyList()

            repository.createBackup(udid).collect { line ->
                _logStream.value = _logStream.value + line
            }

            _isBackingUp.value = false
            loadBackups()
        }
    }

    fun restoreBackup(backupName: String, udid: String? = null) {
        viewModelScope.launch {
            _isBackingUp.value = true
            _logStream.value = emptyList()

            repository.restoreBackup(backupName, udid).collect { line ->
                _logStream.value = _logStream.value + line
            }

            _isBackingUp.value = false
        }
    }
}
