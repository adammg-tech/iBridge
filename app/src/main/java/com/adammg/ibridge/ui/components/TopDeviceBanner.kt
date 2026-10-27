package com.adammg.ibridge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adammg.ibridge.core.usb.UsbConnectionState
import com.adammg.ibridge.data.model.DeviceInfo

@Composable
fun TopDeviceBanner(
    usbState: UsbConnectionState,
    deviceInfo: DeviceInfo,
    isPaired: Boolean,
    onPairClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneIphone,
                        contentDescription = "iOS Device",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when (usbState) {
                                is UsbConnectionState.Connected -> if (deviceInfo.deviceName.isNotBlank()) deviceInfo.deviceName else "iPhone Connected"
                                is UsbConnectionState.DeviceFound -> "Apple Device Detected"
                                is UsbConnectionState.PermissionRequired -> "Awaiting USB Permission"
                                is UsbConnectionState.Error -> "Connection Error"
                                UsbConnectionState.NoDeviceConnected -> "No iOS Device Connected"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (usbState) {
                                is UsbConnectionState.Connected -> "iOS ${deviceInfo.productVersion} • ${deviceInfo.productType}"
                                else -> "Connect via USB OTG cable"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = when (usbState) {
                                is UsbConnectionState.Connected -> if (isPaired) "Trusted" else "Pair Needed"
                                is UsbConnectionState.DeviceFound -> "Detected"
                                else -> "Disconnected"
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isPaired) Icons.Default.PhonelinkLock else Icons.Default.Usb,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isPaired) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            AnimatedVisibility(
                visible = usbState is UsbConnectionState.PermissionRequired || usbState is UsbConnectionState.DeviceFound,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "USB Permission required to connect iPhone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = onPairClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text("Grant USB Permission", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            AnimatedVisibility(
                visible = usbState is UsbConnectionState.Connected,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    if (!isPaired) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "👉 Please unlock your iPhone and tap 'Trust' on the screen if prompted.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (deviceInfo.batteryIsCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                contentDescription = "Battery",
                                tint = if (deviceInfo.batteryCurrentCapacity < 20) Color.Red else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Battery: ${deviceInfo.batteryCurrentCapacity}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (!isPaired) {
                            FilledTonalButton(
                                onClick = onPairClick,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Trust & Pair", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (deviceInfo.batteryCurrentCapacity / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
