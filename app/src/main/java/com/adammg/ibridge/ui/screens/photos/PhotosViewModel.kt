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

    private var allMasterPhotos = listOf<PhotoItem>()
    private var visibleCount = 100

    private val _photoGroups = MutableStateFlow<List<PhotoTimelineGroup>>(emptyList())
    val photoGroups: StateFlow<List<PhotoTimelineGroup>> = _photoGroups.asStateFlow()

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
            _statusMessage.value = "Scanning iPhone /DCIM/ photo library..."
            val groups = repository.getTimelineGroups()
            allMasterPhotos = groups.flatMap { it.photos }
            visibleCount = 100

            updateVisiblePhotos()
            _isLoading.value = false

            val totalCount = allMasterPhotos.size
            _statusMessage.value = "Showing ${minOf(visibleCount, totalCount)} of $totalCount media items"
        }
    }

    fun loadMorePhotos() {
        visibleCount += 100
        updateVisiblePhotos()
        val totalCount = allMasterPhotos.size
        _statusMessage.value = "Showing ${minOf(visibleCount, totalCount)} of $totalCount media items"
    }

    private fun updateVisiblePhotos() {
        val sliced = allMasterPhotos.take(visibleCount)
        val groups = sliced.chunked(45).mapIndexed { index, chunk ->
            PhotoTimelineGroup(
                monthYearTitle = "Photos Timeline Group ${index + 1} (${chunk.first().folderName})",
                photos = chunk
            )
        }
        _photoGroups.value = groups
        _hasMorePhotos.value = visibleCount < allMasterPhotos.size

        val missingThumbs = sliced.filter { it.localThumbnailPath == null && !it.isVideo }.take(30)
        if (missingThumbs.isNotEmpty()) {
            loadThumbnails(missingThumbs)
        }
    }

    private fun loadThumbnails(items: List<PhotoItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            var updated = false
            items.forEach { item ->
                if (item.localThumbnailPath == null && !item.isVideo) {
                    val localPath = repository.downloadThumbnail(item)
                    if (localPath != null) {
                        allMasterPhotos = allMasterPhotos.map {
                            if (it.remotePath == item.remotePath) it.copy(localThumbnailPath = localPath) else it
                        }
                        updated = true
                    }
                    kotlinx.coroutines.delay(100)
                }
            }
            if (updated) {
                val sliced = allMasterPhotos.take(visibleCount)
                val groups = sliced.chunked(45).mapIndexed { index, chunk ->
                    PhotoTimelineGroup(
                        monthYearTitle = "Photos Timeline Group ${index + 1} (${chunk.first().folderName})",
                        photos = chunk
                    )
                }
                _photoGroups.value = groups
            }
        }
    }

    fun uploadPhotoFromUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Preparing photo for upload..."
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = java.io.File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                _statusMessage.value = "Uploading photo to iPhone DCIM..."
                val (success, msg) = repository.uploadPhotoToIphone(tempFile)
                _statusMessage.value = msg
                if (success) {
                    loadPhotos()
                }
            } catch (e: Exception) {
                _statusMessage.value = "Upload failed: ${e.message}"
            } finally {
                _isLoading.value = false
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
