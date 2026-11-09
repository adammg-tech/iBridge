package com.adammg.ibridge.ui.screens.ringtones

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adammg.ibridge.data.repository.RingtoneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class RingtoneViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RingtoneRepository(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _selectedDirectory = MutableStateFlow("/iTunes_Control/Ringtones")
    val selectedDirectory: StateFlow<String> = _selectedDirectory.asStateFlow()

    private val _backups = MutableStateFlow<List<File>>(emptyList())
    val backups: StateFlow<List<File>> = _backups.asStateFlow()

    val ffmpegLog: String
        get() = repository.lastFFmpegLog

    init {
        loadBackups()
        discoverDefaultDirectory()
    }

    private fun discoverDefaultDirectory() {
        viewModelScope.launch {
            try {
                val paths = repository.discoverRingtonePaths()
                _selectedDirectory.value = paths.first
            } catch (_: Exception) {
                // fallback
            }
        }
    }

    fun setSelectedDirectory(dir: String) {
        _selectedDirectory.value = dir
    }

    fun loadBackups() {
        viewModelScope.launch {
            _backups.value = repository.getAvailableBackups()
        }
    }

    fun scanIPhoneFolder(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Scanning iPhone folders (excluding DCIM & Photos)..."
            val foundDir = repository.scanForRingtoneFolder()
            if (foundDir != null) {
                _selectedDirectory.value = foundDir
                _statusMessage.value = "Found ringtone folder: $foundDir"
            } else {
                _statusMessage.value = "Scan complete. No ringtone folder found, using default."
            }
            _isLoading.value = false
            onResult(foundDir)
        }
    }

    fun convertAndImportWithTrim(
        audioUri: Uri,
        ringtoneName: String,
        startTimeSec: Float,
        endTimeSec: Float,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _statusMessage.value = "Converting and processing audio..."
                val (convSuccess, m4rFile) = repository.convertToM4rWithTrim(audioUri, startTimeSec, endTimeSec)
                if (!convSuccess || m4rFile == null || !m4rFile.exists()) {
                    _statusMessage.value = "Audio conversion failed. See FFmpeg log below."
                    _isLoading.value = false
                    onComplete(false, "Audio conversion failed.")
                    return@launch
                }

                _statusMessage.value = "Transferring .m4r file and updating Ringtones.plist..."
                val (success, message) = repository.importRingtoneWithFile(m4rFile, ringtoneName, _selectedDirectory.value) { stepMsg ->
                    _statusMessage.value = stepMsg
                }
                _statusMessage.value = message
                _isLoading.value = false
                loadBackups()
                onComplete(success, message)
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.message}"
                _isLoading.value = false
                onComplete(false, e.message ?: "Unknown error")
            }
        }
    }

    fun restoreBackupFile(backupFile: File, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Restoring Ringtones.plist from backup..."
            try {
                val (success, message) = repository.restoreBackup(backupFile)
                _statusMessage.value = message
                _isLoading.value = false
                onComplete(success, message)
            } catch (e: Exception) {
                _statusMessage.value = "Restore error: ${e.message}"
                _isLoading.value = false
                onComplete(false, e.message ?: "Unknown error")
            }
        }
    }
}

@Composable
fun RingtoneImportScreen(
    onBack: () -> Unit,
    viewModel: RingtoneViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val selectedDirectory by viewModel.selectedDirectory.collectAsState()
    val backups by viewModel.backups.collectAsState()

    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var ringtoneName by remember { mutableStateOf("My Ringtone") }
    var trimStartSec by remember { mutableFloatStateOf(0f) }
    var trimEndSec by remember { mutableFloatStateOf(30f) }

    var showTrimScreen by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = showTrimScreen) {
        showTrimScreen = false
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedAudioUri = uri
            showTrimScreen = true
        }
    }

    if (showTrimScreen && selectedAudioUri != null) {
        AudioTrimScreen(
            audioUri = selectedAudioUri!!,
            initialRingName = ringtoneName,
            onBack = { showTrimScreen = false },
            onConfirmTrim = { start, end, name ->
                trimStartSec = start
                trimEndSec = end
                ringtoneName = name
                showTrimScreen = false
            }
        )
        return
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Ringtones.plist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a verified backup to restore:", style = MaterialTheme.typography.bodyMedium)
                    if (backups.isEmpty()) {
                        Text("No backups found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    } else {
                        backups.forEach { backup ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.restoreBackupFile(backup) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            if (success) showRestoreDialog = false
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = backup.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(text = "Size: ${backup.length()} bytes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Close") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onBack() }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Custom Ringtone Importer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = { showRestoreDialog = true }) {
                Icon(imageVector = Icons.Default.Restore, contentDescription = "Restore Backup", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // iPhone Directory Reference Card
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Target iPhone Directory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = selectedDirectory,
                    onValueChange = { viewModel.setSelectedDirectory(it) },
                    label = { Text("iPhone Ringtone Directory") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.scanIPhoneFolder { found ->
                            if (found != null) {
                                Toast.makeText(context, "Found: $found", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No ringtone folder discovered", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan iPhone for Ringtone Folder")
                }
            }
        }

        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Select Audio File",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.AudioFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedAudioUri == null) "Choose Audio File (MP3, WAV, M4A)" else "Choose Different File")
                }
            }
        }

        selectedAudioUri?.let { uri ->
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ringtone: $ringtoneName",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Trim Range: %.2fs - %.2fs (Duration: %.2fs)".format(trimStartSec, trimEndSec, trimEndSec - trimStartSec),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = { showTrimScreen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edit Trim / Name")
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.convertAndImportWithTrim(uri, ringtoneName, trimStartSec, trimEndSec) { _, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                },
                enabled = !isLoading && ringtoneName.isNotBlank(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(imageVector = Icons.Default.MusicNote, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Ringtone to iPhone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        statusMessage?.let { msg ->
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Copyable FFmpeg Log Card
        if (viewModel.ffmpegLog.isNotBlank()) {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FFmpeg Execution Log",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("FFmpeg Log", viewModel.ffmpegLog)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied FFmpeg log to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Log", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = viewModel.ffmpegLog,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 10
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RingtoneImportScreenPreview() {
    MaterialTheme {
        RingtoneImportScreen(onBack = {})
    }
}
