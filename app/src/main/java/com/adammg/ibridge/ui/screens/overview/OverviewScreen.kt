package com.adammg.ibridge.ui.screens.overview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel = viewModel()
) {
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDeviceInfo()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Device Specs & Hardware",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.loadDeviceInfo() }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        statusMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "System Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow("Device Name", deviceInfo.deviceName)
                InfoRow("Product Type", deviceInfo.productType)
                InfoRow("iOS Version", "${deviceInfo.productVersion} (${deviceInfo.buildVersion})")
                InfoRow("Serial Number", deviceInfo.serialNumber)
                InfoRow("UDID", deviceInfo.udid)
                InfoRow("ECID", deviceInfo.ecid)
                InfoRow("Activation State", deviceInfo.activationState)
            }
        }

        if (deviceInfo.totalStorageBytes > 0L) {
            val totalGb = deviceInfo.totalStorageBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            val usedGb = deviceInfo.usedStorageBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            val freeGb = deviceInfo.freeStorageBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            val percentUsed = if (totalGb > 0) (usedGb / totalGb).toFloat() else 0f

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Storage & Capacity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow("Total Storage", String.format(java.util.Locale.getDefault(), "%.1f GB", totalGb))
                    InfoRow("Used Storage", String.format(java.util.Locale.getDefault(), "%.1f GB", usedGb))
                    InfoRow("Free Available", String.format(java.util.Locale.getDefault(), "%.1f GB", freeGb))
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { percentUsed.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeveloperBoard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hardware & Network",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow("Hardware Model", deviceInfo.hardwareModel)
                InfoRow("CPU Architecture", deviceInfo.cpuArchitecture)
                InfoRow("Wi-Fi MAC Address", deviceInfo.wifiAddress)
                InfoRow("Bluetooth MAC Address", deviceInfo.bluetoothAddress)
            }
        }

        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Diagnostics & Power Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.rebootDevice() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reboot")
                    }

                    Button(
                        onClick = { viewModel.shutdownDevice() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Power Off")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    val context = LocalContext.current
    val displayValue = value.ifEmpty { "N/A" }
    val renderedValue = if (label == "UDID" && displayValue.length > 16) {
        displayValue.take(8) + "..." + displayValue.takeLast(6)
    } else {
        displayValue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = displayValue != "N/A") {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(label, displayValue)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
            }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = renderedValue,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            if (displayValue != "N/A") {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
