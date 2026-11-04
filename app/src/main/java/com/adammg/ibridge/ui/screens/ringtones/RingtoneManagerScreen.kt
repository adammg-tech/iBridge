package com.adammg.ibridge.ui.screens.ringtones

import android.app.Application
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

class RingtoneManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RingtoneRepository(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _ringtones = MutableStateFlow<List<RingtoneRepository.ExistingRingtone>>(emptyList())
    val ringtones: StateFlow<List<RingtoneRepository.ExistingRingtone>> = _ringtones.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _selectedDirectory = MutableStateFlow("/iTunes_Control/Ringtones")
    val selectedDirectory: StateFlow<String> = _selectedDirectory.asStateFlow()

    init {
        discoverDefaultDirectory()
    }

    private fun discoverDefaultDirectory() {
        viewModelScope.launch {
            try {
                val paths = repository.discoverRingtonePaths()
                _selectedDirectory.value = paths.first
                loadRingtones()
            } catch (_: Exception) {
                loadRingtones()
            }
        }
    }

    fun setSelectedDirectory(dir: String) {
        _selectedDirectory.value = dir
    }

    fun scanIPhoneFolder(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Scanning iPhone folders (excluding DCIM & Photos)..."
            val foundDir = repository.scanForRingtoneFolder()
            if (foundDir != null) {
                _selectedDirectory.value = foundDir
                _statusMessage.value = "Found ringtone folder: $foundDir"
                loadRingtones()
            } else {
                _statusMessage.value = "Scan complete. No ringtone folder found."
            }
            _isLoading.value = false
            onResult(foundDir)
        }
    }

    fun loadRingtones() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Reading existing ringtones from directory: ${_selectedDirectory.value}..."
            _ringtones.value = repository.loadExistingRingtonesForDirectory(_selectedDirectory.value)
            _isLoading.value = false
            _statusMessage.value = "Found ${_ringtones.value.size} ringtones."
        }
    }

    fun exportRingtone(ringtone: RingtoneRepository.ExistingRingtone, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Saving ${ringtone.name} to Downloads..."
            val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "iBridge/Ringtones")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val sanitizedName = ringtone.name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            val finalFilename = if (sanitizedName.isNotEmpty()) "$sanitizedName.m4r" else ringtone.filename
            val destFile = File(downloadsDir, finalFilename)

            val (success, msg) = repository.exportRingtoneFile(ringtone.filename, _selectedDirectory.value, destFile)
            _statusMessage.value = msg
            _isLoading.value = false
            onComplete(success, msg)
        }
    }

    fun removeRingtone(ringtone: RingtoneRepository.ExistingRingtone, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Removing ${ringtone.name}..."
            val (success, msg) = repository.removeRingtone(ringtone.filename, _selectedDirectory.value)
            _statusMessage.value = msg
            _isLoading.value = false
            loadRingtones()
            onComplete(success, msg)
        }
    }

    fun renameRingtone(ringtone: RingtoneRepository.ExistingRingtone, newName: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Renaming ${ringtone.name} to $newName..."
            val (success, msg) = repository.renameRingtone(ringtone.filename, newName, _selectedDirectory.value)
            _statusMessage.value = msg
            _isLoading.value = false
            loadRingtones()
            onComplete(success, msg)
        }
    }
}

@Composable
fun RingtoneManagerScreen(
    onBack: () -> Unit,
    viewModel: RingtoneManagerViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val ringtones by viewModel.ringtones.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val selectedDirectory by viewModel.selectedDirectory.collectAsState()

    var ringtoneToDelete by remember { mutableStateOf<RingtoneRepository.ExistingRingtone?>(null) }
    var ringtoneToRename by remember { mutableStateOf<Pair<RingtoneRepository.ExistingRingtone, String>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadRingtones()
    }

    // Delete Confirmation Dialog
    ringtoneToDelete?.let { ringtone ->
        AlertDialog(
            onDismissRequest = { ringtoneToDelete = null },
            title = { Text("Delete Ringtone") },
            text = { Text("Are you sure you want to delete '${ringtone.name}'? This will remove the ringtone file and update Ringtones.plist with rollback protection.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = ringtone
                        ringtoneToDelete = null
                        viewModel.removeRingtone(target) { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { ringtoneToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    ringtoneToRename?.let { (ringtone, currentName) ->
        var tempName by remember { mutableStateOf(currentName) }
        AlertDialog(
            onDismissRequest = { ringtoneToRename = null },
            title = { Text("Rename Ringtone") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter new name for ringtone:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = ringtone
                        val newN = tempName
                        ringtoneToRename = null
                        viewModel.renameRingtone(target, newN) { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { ringtoneToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Manage iPhone Ringtones",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = { viewModel.loadRingtones() }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.loadRingtones() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reload Path")
                    }

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
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan iPhone")
                    }
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        statusMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (ringtones.isEmpty() && !isLoading) {
            Text(
                text = "No ringtones found on iPhone or Ringtones.plist is inaccessible.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(ringtones) { ringtone ->
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = ringtone.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "File: ${ringtone.filename} • Duration: ${ringtone.totalTime}ms",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.exportRingtone(ringtone) { _, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = "Save", modifier = Modifier.size(20.dp))
                                }

                                OutlinedButton(
                                    onClick = { ringtoneToRename = ringtone to ringtone.name },
                                    enabled = !isLoading,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(20.dp))
                                }

                                OutlinedButton(
                                    onClick = { ringtoneToDelete = ringtone },
                                    enabled = !isLoading,
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
