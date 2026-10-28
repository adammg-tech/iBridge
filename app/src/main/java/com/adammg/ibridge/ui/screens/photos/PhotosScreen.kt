package com.adammg.ibridge.ui.screens.photos

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adammg.ibridge.data.model.PhotoItem
import java.io.File

@Composable
fun PhotosScreen(
    viewModel: PhotosViewModel = viewModel()
) {
    val photoGroups by viewModel.photoGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val selectedPhoto by viewModel.selectedPhoto.collectAsState()
    val hasMorePhotos by viewModel.hasMorePhotos.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val uploadLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadPhotoFromUri(context, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadPhotos()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "iPhone Photos & Videos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Row {
                IconButton(onClick = { uploadLauncher.launch("image/*") }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Upload Photo")
                }
                IconButton(onClick = { viewModel.loadPhotos() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        statusMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (photoGroups.isEmpty() && !isLoading) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Photos Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ensure your iPhone is connected and trusted via USB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photoGroups) { group ->
                    Column {
                        Text(
                            text = "🗓️ ${group.monthYearTitle} (${group.photos.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Render 3-column rows manually or using chunked items for clean LazyColumn nested lists
                        val chunkedPhotos = group.photos.chunked(3)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            chunkedPhotos.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            PhotoGridCard(
                                                item = item,
                                                onClick = { viewModel.selectPhoto(item) }
                                            )
                                        }
                                    }
                                    // Fill empty space in row if rowItems.size < 3
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                if (hasMorePhotos) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.loadMorePhotos() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Text("Load +100 Photos")
                        }
                    }
                }
            }
        }
    }

    selectedPhoto?.let { photo ->
        PhotoDetailDialog(
            photo = photo,
            onDismiss = { viewModel.selectPhoto(null) },
            onExport = { viewModel.exportSelectedPhotoToGallery(photo) }
        )
    }
}

@Composable
fun PhotoGridCard(
    item: PhotoItem,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.localThumbnailPath != null && File(item.localThumbnailPath).exists()) {
                val bitmap = remember(item.localThumbnailPath) {
                    loadCorrectlyOrientedBitmap(item.localThumbnailPath)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = item.fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = item.extension,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Text(
                    text = item.extension,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun PhotoDetailDialog(
    photo: PhotoItem,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = photo.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(Color.Black, shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (photo.localThumbnailPath != null && File(photo.localThumbnailPath).exists()) {
                        val bitmap = remember(photo.localThumbnailPath) {
                            loadCorrectlyOrientedBitmap(photo.localThumbnailPath)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = photo.fileName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (photo.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Previewing ${photo.fileName}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Folder: ${photo.folderName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Remote Path: ${photo.remotePath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to Downloads/iBridge/Photos")
                }
            }
        }
    }
}

private val bitmapCache = object : android.util.LruCache<String, android.graphics.Bitmap>(40 * 1024 * 1024) {
    override fun sizeOf(key: String, value: android.graphics.Bitmap): Int {
        return value.byteCount
    }
}

private fun loadCorrectlyOrientedBitmap(filePath: String): android.graphics.Bitmap? {
    bitmapCache.get(filePath)?.let { return it }
    val bitmap = BitmapFactory.decodeFile(filePath) ?: return null
    val rotated = try {
        val exif = android.media.ExifInterface(filePath)
        val orientation = exif.getAttributeInt(
            android.media.ExifInterface.TAG_ORIENTATION,
            android.media.ExifInterface.ORIENTATION_UNDEFINED
        )
        val matrix = android.graphics.Matrix()
        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        if (matrix.isIdentity) bitmap else android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        bitmap
    }
    bitmapCache.put(filePath, rotated)
    return rotated
}
