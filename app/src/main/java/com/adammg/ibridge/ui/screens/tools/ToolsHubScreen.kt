package com.adammg.ibridge.ui.screens.tools

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adammg.ibridge.ui.screens.apps.AppsScreen
import com.adammg.ibridge.ui.screens.backup.BackupScreen
import com.adammg.ibridge.ui.screens.devtools.DevToolsScreen
import com.adammg.ibridge.ui.screens.files.FileBrowserScreen
import com.adammg.ibridge.ui.screens.logs.LogsScreen
import com.adammg.ibridge.ui.screens.recovery.RecoveryScreen

enum class ToolType(val title: String, val subtitle: String, val icon: ImageVector) {
    APPS("Apps Manager", "Install, backup & remove apps", Icons.Default.Apps),
    BACKUP("Backup & Restore", "Full iOS device backups", Icons.Default.Backup),
    FILES("AFC File Explorer", "Two-way file sharing & browsing", Icons.Default.Folder),
    LOGS("Logs & Crash Reports", "Live syslog & crash logs", Icons.Default.Terminal),
    DEV("GPS & Dev Tools", "Location spoofer & developer tools", Icons.Default.Build),
    RECOVERY("Recovery & Restore", "Recovery mode & DFU utilities", Icons.Default.SystemUpdate)
}

@Composable
fun ToolsHubScreen() {
    var activeTool by remember { mutableStateOf<ToolType?>(null) }

    if (activeTool == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "iOS Developer Tools",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select a utility to manage your connected iPhone",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(ToolType.entries) { tool ->
                    ToolCard(
                        tool = tool,
                        onClick = { activeTool = tool }
                    )
                }
            }
        }
    } else {
        ToolDetailPage(
            tool = activeTool!!,
            onBack = { activeTool = null }
        )
    }
}

@Composable
fun ToolCard(
    tool: ToolType,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tool.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolDetailPage(
    tool: ToolType,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tool.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (tool) {
                ToolType.APPS -> AppsScreen()
                ToolType.BACKUP -> BackupScreen()
                ToolType.FILES -> FileBrowserScreen()
                ToolType.LOGS -> LogsScreen()
                ToolType.DEV -> DevToolsScreen()
                ToolType.RECOVERY -> RecoveryScreen()
            }
        }
    }
}
