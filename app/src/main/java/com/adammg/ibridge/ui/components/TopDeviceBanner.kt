package com.adammg.ibridge.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import com.adammg.ibridge.ui.components.WavyProgressIndicator
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.remember
import com.adammg.ibridge.R
import com.adammg.ibridge.core.usb.UsbConnectionState
import com.adammg.ibridge.data.model.DeviceInfo

private fun getIosDrawableResId(majorVersion: Int): Int {
    return when (majorVersion) {
        1 -> R.drawable.ios1
        2 -> R.drawable.ios2
        3 -> R.drawable.ios3
        4 -> R.drawable.ios4
        5 -> R.drawable.ios5
        6 -> R.drawable.ios6
        7 -> R.drawable.ios7
        8 -> R.drawable.ios8
        9 -> R.drawable.ios9
        10 -> R.drawable.ios10
        11 -> R.drawable.ios11
        12 -> R.drawable.ios12
        13 -> R.drawable.ios13
        14 -> R.drawable.ios14
        15 -> R.drawable.ios15
        16 -> R.drawable.ios16
        17 -> R.drawable.ios17
        18 -> R.drawable.ios18
        26 -> R.drawable.ios26
        27 -> R.drawable.ios27
        else -> 0
    }
}

@Composable
fun TopDeviceBanner(
    usbState: UsbConnectionState,
    deviceInfo: DeviceInfo,
    isPaired: Boolean,
    onPairClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val majorVersion = remember(deviceInfo.productVersion) {
        deviceInfo.productVersion.split(".").firstOrNull()?.toIntOrNull() ?: 0
    }

    val iosDrawableId = remember(majorVersion) {
        getIosDrawableResId(majorVersion)
    }

    val deviceDrawableId = remember(deviceInfo.productType) {
        if (deviceInfo.productType.isNotBlank() && deviceInfo.productType != "Unknown") {
            var cleanName = deviceInfo.productType.lowercase().trim().replace(",", "_")
            cleanName = when (cleanName) {
                "iphone10_4" -> "iphone10_1"
                "iphone10_5" -> "iphone10_2"
                "iphone10_6" -> "iphone10_3"
                "iphone9_3" -> "iphone9_1"
                "iphone9_4" -> "iphone9_2"
                "iphone11_4" -> "iphone11_6"
                "iphone5_4" -> "iphone5_3"
                "iphone6_2" -> "iphone6_1"
                "iphone19_7" -> "iphone19_3"
                else -> cleanName
            }
            val resId = context.resources.getIdentifier(cleanName, "drawable", context.packageName)
            if (resId != 0) resId else 0
        } else 0
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(enabled = usbState is UsbConnectionState.Connected) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val deviceSummary = "Device: ${deviceInfo.deviceName}\nVersion: iOS ${deviceInfo.productVersion} (${deviceInfo.buildVersion})\nModel: ${deviceInfo.productType}\nUDID: ${deviceInfo.udid}\nSerial: ${deviceInfo.serialNumber}"
                            val clip = ClipData.newPlainText("iOS Device Info", deviceSummary)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied iOS device details to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    if (usbState is UsbConnectionState.Connected) {
                        val displayDrawableId = if (deviceDrawableId != 0) deviceDrawableId else iosDrawableId
                        if (displayDrawableId != 0) {
                            Image(
                                painter = painterResource(id = displayDrawableId),
                                contentDescription = deviceInfo.marketingName,
                                modifier = Modifier
                                    .height(48.dp)
                                    .widthIn(max = 64.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PhoneIphone,
                                contentDescription = "iOS Device",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhoneIphone,
                            contentDescription = "iOS Device",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            if (usbState is UsbConnectionState.Connected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Details",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = when (usbState) {
                                is UsbConnectionState.Connected -> deviceInfo.marketingName
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
                                else -> "○"
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (usbState) {
                                is UsbConnectionState.Connected -> if (isPaired) Icons.Default.PhonelinkLock else Icons.Default.Usb
                                else -> Icons.Default.Usb
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (usbState) {
                            is UsbConnectionState.Connected -> if (isPaired) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
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
                    WavyProgressIndicator(
                        progress = (deviceInfo.batteryCurrentCapacity / 100f).coerceIn(0f, 1f),
                        color = if (deviceInfo.batteryCurrentCapacity < 20) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
