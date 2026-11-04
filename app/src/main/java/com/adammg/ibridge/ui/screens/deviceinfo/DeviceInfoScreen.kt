package com.adammg.ibridge.ui.screens.deviceinfo

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adammg.ibridge.data.model.DeviceInfo

@Composable
fun DeviceInfoScreen(
    deviceInfo: DeviceInfo,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val isActivated = deviceInfo.activationState.equals("Activated", ignoreCase = true)

    val gridItems = listOf(
        Triple("Device Name", deviceInfo.deviceName.ifEmpty { "N/A" }, Icons.Default.Info),
        Triple("Product Type", deviceInfo.productType.ifEmpty { "N/A" }, Icons.Default.Info),
        Triple("iOS Version", "${deviceInfo.productVersion} (${deviceInfo.buildVersion})", Icons.Default.Info),
        Triple("Activation State", deviceInfo.activationState.ifEmpty { "Unknown" }, if (isActivated) Icons.Default.CheckCircle else Icons.Default.Cancel),
        Triple("Hardware Model", deviceInfo.hardwareModel.ifEmpty { "N/A" }, Icons.Default.Info),
        Triple("CPU Architecture", deviceInfo.cpuArchitecture.ifEmpty { "arm64" }, Icons.Default.Info)
    )

    val singleLineItems = listOf(
        "Serial Number" to deviceInfo.serialNumber,
        "UDID" to deviceInfo.udid,
        "ECID" to deviceInfo.ecid,
        "Wi-Fi MAC" to deviceInfo.wifiAddress,
        "Bluetooth MAC" to deviceInfo.bluetoothAddress,
        "Time Zone" to deviceInfo.timeZone
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "iPhone Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Grid of Same-Sized Cards
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(340.dp)
        ) {
            items(gridItems) { (label, value, icon) ->
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(label, value)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (label == "Activation State") {
                                    if (isActivated) Color(0xFF22C55E) else Color(0xFFEF4444)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (label == "Activation State") {
                                if (isActivated) Color(0xFF15803D) else Color(0xFFB91C1C)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 2
                        )
                    }
                }
            }
        }

        Text(
            text = "Hardware & Identifiers",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Single Line Cards for Long Information
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            singleLineItems.forEach { (label, value) ->
                if (value.isNotBlank() && value != "N/A") {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(label, value)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy $label",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
