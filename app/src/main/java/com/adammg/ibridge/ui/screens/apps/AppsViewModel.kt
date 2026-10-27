package com.adammg.ibridge.ui.screens.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adammg.ibridge.data.model.IosApp
import com.adammg.ibridge.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    private val _appList = MutableStateFlow<List<IosApp>>(emptyList())
    val appList: StateFlow<List<IosApp>> = _appList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val isBinaryAvailable: Boolean = repository.isInstallerAvailable()

    fun loadApps(includeSystemApps: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _appList.value = repository.listApps(includeSystemApps)
            _isLoading.value = false
        }
    }

    fun installIpa(ipaPath: String) {
        viewModelScope.launch {
            _statusMessage.value = "Installing IPA..."
            val (success, message) = repository.installIpa(ipaPath)
            _statusMessage.value = message
            if (success) loadApps()
        }
    }

    fun uninstallApp(bundleId: String) {
        viewModelScope.launch {
            _statusMessage.value = "Uninstalling $bundleId..."
            val (success, message) = repository.uninstallApp(bundleId)
            _statusMessage.value = message
            if (success) loadApps()
        }
    }
}
