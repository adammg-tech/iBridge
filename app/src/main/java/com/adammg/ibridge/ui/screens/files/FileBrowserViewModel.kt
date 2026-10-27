package com.adammg.ibridge.ui.screens.files

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adammg.ibridge.data.model.AfcFile
import com.adammg.ibridge.data.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FileBrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRepository(application)

    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _fileList = MutableStateFlow<List<AfcFile>>(emptyList())
    val fileList: StateFlow<List<AfcFile>> = _fileList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun loadDirectory(path: String = "/") {
        viewModelScope.launch {
            _isLoading.value = true
            _currentPath.value = path
            _fileList.value = repository.listFiles(path)
            _isLoading.value = false
        }
    }

    fun navigateUp() {
        val path = _currentPath.value
        if (path == "/" || path.isBlank()) return
        val lastSlash = path.lastIndexOf('/')
        val parentPath = if (lastSlash <= 0) "/" else path.substring(0, lastSlash)
        loadDirectory(parentPath)
    }

    fun downloadFile(file: AfcFile, localTargetDir: String) {
        viewModelScope.launch {
            _statusMessage.value = "Downloading ${file.name}..."
            val localPath = "$localTargetDir/${file.name}"
            val (success, message) = repository.downloadFile(file.path, localPath)
            _statusMessage.value = message
        }
    }

    fun deleteFile(file: AfcFile) {
        viewModelScope.launch {
            _statusMessage.value = "Deleting ${file.name}..."
            val (success, message) = repository.deleteFile(file.path)
            _statusMessage.value = message
            if (success) loadDirectory(_currentPath.value)
        }
    }

    fun uploadFile(localPath: String, fileName: String) {
        viewModelScope.launch {
            _statusMessage.value = "Uploading $fileName to iPhone..."
            val remotePath = if (_currentPath.value == "/") "/$fileName" else "${_currentPath.value}/$fileName"
            val (success, message) = repository.uploadFile(localPath, remotePath)
            _statusMessage.value = message
            if (success) loadDirectory(_currentPath.value)
        }
    }

    fun downloadFileUsingUri(file: AfcFile, uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            _statusMessage.value = "Extracting ${file.name} to selected location..."
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    val (_, message) = repository.downloadFileToOutputStream(file.path, outputStream, context)
                    _statusMessage.value = message
                } else {
                    _statusMessage.value = "Failed to open file output stream"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error extracting file: ${e.message}"
            }
        }
    }
}
