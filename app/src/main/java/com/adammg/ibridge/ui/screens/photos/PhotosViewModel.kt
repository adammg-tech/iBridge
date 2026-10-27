package com.adammg.ibridge.ui.screens.photos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adammg.ibridge.data.model.PhotoItem
import com.adammg.ibridge.data.model.PhotoTimelineGroup
import com.adammg.ibridge.data.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhotosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PhotoRepository(application)

    private val _photoGroups = MutableStateFlow<List<PhotoTimelineGroup>>(emptyList())
    val photoGroups: StateFlow<List<PhotoTimelineGroup>> = _photoGroups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _selectedPhoto = MutableStateFlow<PhotoItem?>(null)
    val selectedPhoto: StateFlow<PhotoItem?> = _selectedPhoto.asStateFlow()

    fun loadPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Scanning iPhone /DCIM/ photo library..."
            val groups = repository.getTimelineGroups()
            _photoGroups.value = groups
            _isLoading.value = false
            val totalCount = groups.sumOf { it.photos.size }
            _statusMessage.value = "Found $totalCount media items across ${groups.size} albums"

            // Pre-download thumbnails in background for first 30 photos
            val firstItems = groups.flatMap { it.photos }.take(30)
            loadThumbnails(firstItems)
        }
    }

    private fun loadThumbnails(items: List<PhotoItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            items.forEach { item ->
                if (item.localThumbnailPath == null && !item.isVideo) {
                    val localPath = repository.downloadThumbnail(item)
                    if (localPath != null) {
                        _photoGroups.value = _photoGroups.value.map { group ->
                            group.copy(
                                photos = group.photos.map {
                                    if (it.remotePath == item.remotePath) it.copy(localThumbnailPath = localPath) else it
                                }
                            )
                        }
                    }
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
