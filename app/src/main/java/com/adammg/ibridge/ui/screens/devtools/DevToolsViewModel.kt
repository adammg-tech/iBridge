package com.adammg.ibridge.ui.screens.devtools

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adammg.ibridge.data.repository.DevToolsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DevToolsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DevToolsRepository(application)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _profiles = MutableStateFlow<List<String>>(emptyList())
    val profiles: StateFlow<List<String>> = _profiles.asStateFlow()

    fun setLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            _statusMessage.value = "Setting GPS location..."
            val (success, msg) = repository.setGpsLocation(lat, lng)
            _statusMessage.value = msg
        }
    }

    fun resetLocation() {
        viewModelScope.launch {
            _statusMessage.value = "Resetting GPS location..."
            val (success, msg) = repository.resetGpsLocation()
            _statusMessage.value = msg
        }
    }

    fun loadProvisioningProfiles() {
        viewModelScope.launch {
            _profiles.value = repository.listProvisioningProfiles()
        }
    }
}
