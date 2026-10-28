package com.adammg.ibridge.ui.screens.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.adammg.ibridge.ui.components.LiveTerminalConsole

@Composable
fun LogsScreen(
    viewModel: LogsViewModel = viewModel()
) {
    val logs by viewModel.logs.collectAsState()
    val crashReports by viewModel.crashReports.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            viewModel.fetchCrashReports()
        }
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
            Text(
                text = "Logs & Diagnostics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) {
                Text("Live Syslog")
            }
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) {
                Text("Crash Reports")
            }
            SegmentedButton(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) {
                Text("Termux Log")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        OutlinedButton(onClick = {
                            if (isStreaming) viewModel.stopLogStreaming() else viewModel.startLogStreaming()
                        }) {
                            Icon(
                                imageVector = if (isStreaming) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isStreaming) "Pause" else "Start Stream")
                        }
                    }

                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E1E),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (logs.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isStreaming) "Listening for iOS system logs..." else "Tap 'Start Stream' to view live console logs",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.padding(12.dp)) {
                            items(logs) { entry ->
                                Text(
                                    text = "${entry.timestamp} [${entry.processName}]: ${entry.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00FF66),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
            1 -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Retrieved Crash Reports",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.fetchCrashReports() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (crashReports.isEmpty()) {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No crash reports found on device",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(crashReports) { report ->
                            ElevatedCard(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = report.fileName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Date: ${report.dateCreated} • Size: ${report.sizeBytes} bytes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                LiveTerminalConsole(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
