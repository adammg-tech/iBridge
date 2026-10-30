package com.adammg.ibridge.ui.screens.photos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adammg.ibridge.data.model.PhotoItem
import com.adammg.ibridge.data.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhotosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PhotoRepository(application)

    private val _dcimFolders = MutableStateFlow<List<String>>(emptyList())
    val dcimFolders: StateFlow<List<String>> = _dcimFolders.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    private var allFolderPhotos = listOf<PhotoItem>()
    private var visibleCount = 50

    private val _folderPhotos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val folderPhotos: StateFlow<List<PhotoItem>> = _folderPhotos.asStateFlow()

    private val _hasMorePhotos = MutableStateFlow(false)
    val hasMorePhotos: StateFlow<Boolean> = _hasMorePhotos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _selectedPhoto = MutableStateFlow<PhotoItem?>(null)
    val selectedPhoto: StateFlow<PhotoItem?> = _selectedPhoto.asStateFlow()

    fun loadPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Scanning DCIM folders..."
            val folders = repository.getDcimFolders()
            _dcimFolders.value = folders
            _selectedFolder.value = null
            _isLoading.value = false
            _statusMessage.value = "Found ${folders.size} camera roll folders"
        }
    }

    fun selectFolder(folderName: String?) {
        _selectedFolder.value = folderName
        if (folderName != null) {
            viewModelScope.launch {
                _isLoading.value = true
                _statusMessage.value = "Loading $folderName..."
                allFolderPhotos = repository.getPhotosForFolder(folderName)
                visibleCount = 50
                updateFolderPhotosSlice()
                _isLoading.value = false
                _statusMessage.value = "Loaded ${allFolderPhotos.size} items in $folderName"
            }
        }
    }

    fun loadMorePhotos() {
        visibleCount += 50
        updateFolderPhotosSlice()
    }

    private fun updateFolderPhotosSlice() {
        val sliced = allFolderPhotos.take(visibleCount)
        _folderPhotos.value = sliced
        _hasMorePhotos.value = visibleCount < allFolderPhotos.size

        val missingThumbs = sliced.filter { it.localThumbnailPath == null }.take(50)
        if (missingThumbs.isNotEmpty()) {
            loadThumbnails(missingThumbs)
        }
    }

    private fun loadThumbnails(items: List<PhotoItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            val missing = items.filter { it.localThumbnailPath == null }
            if (missing.isEmpty()) return@launch

            missing.chunked(8).forEach { batch ->
                val deferredResults = batch.map { item ->
                    async(Dispatchers.IO) {
                        val localPath = repository.downloadThumbnail(item)
                        Pair(item.remotePath, localPath)
                    }
                }
                val results = deferredResults.awaitAll()

                var batchUpdated = false
                results.forEach { (remotePath, localPath) ->
                    if (localPath != null) {
                        allFolderPhotos = allFolderPhotos.map {
                            if (it.remotePath == remotePath) it.copy(localThumbnailPath = localPath) else it
                        }
                        batchUpdated = true
                    }
                }

                if (batchUpdated) {
                    _folderPhotos.value = allFolderPhotos.take(visibleCount)
                }
            }
        }
    }

    fun selectPhoto(item: PhotoItem?) {
        _selectedPhoto.value = item
        if (item != null && item.localThumbnailPath == null) {
            viewModelScope.launch {
                val localPath = repository.downloadThumbnail(item)
                if (localPath != null) {
                    _selectedPhoto.value = item.copy(localThumbnailPath = localPath)
                }
            }
        }
    }

    fun exportSelectedPhotoToGallery(item: PhotoItem) {
        viewModelScope.launch {
            _statusMessage.value = "Saving ${item.fileName} to Downloads..."
            val (_, message) = repository.exportPhotoToDownloads(item)
            _statusMessage.value = message
        }
    }
}
