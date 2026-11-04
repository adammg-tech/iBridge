package com.adammg.ibridge.ui.screens.systeminfo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adammg.ibridge.data.model.DeviceInfo
import com.adammg.ibridge.ui.screens.overview.OverviewViewModel
import kotlinx.coroutines.launch

fun getPreciseMarketingName(productType: String): String {
    val clean = productType.lowercase().trim()
    return when {
        clean.contains("iphone1,1") -> "iPhone"
        clean.contains("iphone1,2") -> "iPhone 3G"
        clean.contains("iphone2,1") -> "iPhone 3GS"
        clean.contains("iphone3,1") || clean.contains("iphone3,2") || clean.contains("iphone3,3") -> "iPhone 4"
        clean.contains("iphone4,1") -> "iPhone 4S"
        clean.contains("iphone5,1") || clean.contains("iphone5,2") -> "iPhone 5"
        clean.contains("iphone5,3") || clean.contains("iphone5,4") -> "iPhone 5c"
        clean.contains("iphone6,1") || clean.contains("iphone6,2") -> "iPhone 5s"
        clean.contains("iphone7,2") -> "iPhone 6"
        clean.contains("iphone7,1") -> "iPhone 6 Plus"
        clean.contains("iphone8,1") -> "iPhone 6s"
        clean.contains("iphone8,2") -> "iPhone 6s Plus"
        clean.contains("iphone8,4") -> "iPhone SE (1st generation)"
        clean.contains("iphone9,1") || clean.contains("iphone9,3") -> "iPhone 7"
        clean.contains("iphone9,2") || clean.contains("iphone9,4") -> "iPhone 7 Plus"
        clean.contains("iphone10,1") || clean.contains("iphone10,4") -> "iPhone 8"
        clean.contains("iphone10,2") || clean.contains("iphone10,5") -> "iPhone 8 Plus"
        clean.contains("iphone10,3") || clean.contains("iphone10,6") -> "iPhone X"
        clean.contains("iphone11,8") -> "iPhone XR"
        clean.contains("iphone11,2") -> "iPhone XS"
        clean.contains("iphone11,4") || clean.contains("iphone11,6") -> "iPhone XS Max"
        clean.contains("iphone12,1") -> "iPhone 11"
        clean.contains("iphone12,3") -> "iPhone 11 Pro"
        clean.contains("iphone12,5") -> "iPhone 11 Pro Max"
        clean.contains("iphone12,8") -> "iPhone SE (2nd generation)"
        clean.contains("iphone13,1") -> "iPhone 12 mini"
        clean.contains("iphone13,2") -> "iPhone 12"
        clean.contains("iphone13,3") -> "iPhone 12 Pro"
        clean.contains("iphone13,4") -> "iPhone 12 Pro Max"
        clean.contains("iphone14,4") -> "iPhone 13 mini"
        clean.contains("iphone14,5") -> "iPhone 13"
        clean.contains("iphone14,2") -> "iPhone 13 Pro"
        clean.contains("iphone14,3") -> "iPhone 13 Pro Max"
        clean.contains("iphone14,6") -> "iPhone SE (3rd generation)"
        clean.contains("iphone14,7") -> "iPhone 14"
        clean.contains("iphone14,8") -> "iPhone 14 Plus"
        clean.contains("iphone15,2") -> "iPhone 14 Pro"
        clean.contains("iphone15,3") -> "iPhone 14 Pro Max"
        clean.contains("iphone15,4") -> "iPhone 15"
        clean.contains("iphone15,5") -> "iPhone 15 Plus"
        clean.contains("iphone16,1") -> "iPhone 15 Pro"
        clean.contains("iphone16,2") -> "iPhone 15 Pro Max"
        clean.contains("iphone17,3") -> "iPhone 16"
        clean.contains("iphone17,4") -> "iPhone 16 Plus"
        clean.contains("iphone17,1") -> "iPhone 16 Pro"
        clean.contains("iphone17,2") -> "iPhone 16 Pro Max"
        clean.contains("iphone17,5") -> "iPhone 16e"
        clean.contains("iphone18,4") -> "iPhone Air"
        clean.contains("iphone18,3") -> "iPhone 17"
        clean.contains("iphone18,1") -> "iPhone 17 Pro"
        clean.contains("iphone18,2") -> "iPhone 17 Pro Max"
        clean.contains("iphone18,5") -> "iPhone 17e"
        clean.contains("iphone19,2") -> "iPhone 18 Pro"
        clean.contains("iphone19,3") || clean.contains("iphone19,7") -> "iPhone 18 Pro Max"
        clean.contains("iphone19,4") -> "iPhone Duo"
        else -> productType.ifBlank { "iPhone" }
    }
}

fun formatStorageBytes(bytes: Long): String {
    if (bytes <= 0L) return "N/A"
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return "%.1f GB".format(gb)
}

data class SystemInfoItem(
    val label: String,
    val value: String,
    val icon: ImageVector? = null,
    val drawableResId: Int = 0,
    val isActivatedState: Boolean = false,
    val isActivated: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemInfoScreen(
    deviceInfo: DeviceInfo,
    onBack: () -> Unit,
    overviewViewModel: OverviewViewModel = viewModel()
) {
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isActivated = deviceInfo.activationState.equals("Activated", ignoreCase = true)
    val preciseName = getPreciseMarketingName(deviceInfo.productType)
    val customDeviceName = if (deviceInfo.deviceName.isNotBlank() && deviceInfo.deviceName != "Unknown Device" && deviceInfo.deviceName != "iOS Device") {
        deviceInfo.deviceName
    } else {
        preciseName
    }

    // Resolve device image drawable
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

    // Resolve iOS version logo drawable (e.g. ios17, ios18)
    val iosDrawableId = remember(deviceInfo.productVersion) {
        val major = deviceInfo.productVersion.substringBefore(".").toIntOrNull() ?: 17
        val name = "ios$major"
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId != 0) resId else 0
    }

    val batteryValue = "Level: ${deviceInfo.batteryCurrentCapacity}%\nStatus: ${if (deviceInfo.batteryIsCharging) "Charging" else "Discharging / Battery"}"

    val storageValue = if (deviceInfo.totalStorageBytes > 0L) {
        "${formatStorageBytes(deviceInfo.usedStorageBytes)} Used / ${formatStorageBytes(deviceInfo.totalStorageBytes)}\n(${formatStorageBytes(deviceInfo.freeStorageBytes)} Free)"
    } else {
        "System & App Data\nConnected via AFC Storage"
    }

    val items = listOf(
        SystemInfoItem(
            label = "Model & Name",
            value = "$preciseName\nModel: ${deviceInfo.productType}",
            icon = Icons.Default.PhoneIphone
        ),
        SystemInfoItem(
            label = "iOS Version",
            value = "iOS ${deviceInfo.productVersion}\nBuild: ${deviceInfo.buildVersion}",
            drawableResId = iosDrawableId
        ),
        SystemInfoItem(
            label = "Battery Status",
            value = batteryValue,
            icon = Icons.Default.BatteryChargingFull
        ),
        SystemInfoItem(
            label = "Storage Consumed",
            value = storageValue,
            icon = Icons.Default.Storage
        ),
        SystemInfoItem(
            label = "Serial Number",
            value = deviceInfo.serialNumber.ifEmpty { "N/A" },
            icon = Icons.Default.Info
        ),
        SystemInfoItem(
            label = "UDID",
            value = deviceInfo.udid.ifEmpty { "N/A" },
            icon = Icons.Default.Info
        ),
        SystemInfoItem(
            label = "Activation State",
            value = if (isActivated) "Activated\nStatus: OK" else "Not Activated",
            isActivatedState = true,
            isActivated = isActivated
        ),
        SystemInfoItem(
            label = "Hardware Model",
            value = "${deviceInfo.hardwareModel}\nArchitecture: ${deviceInfo.cpuArchitecture}",
            icon = Icons.Default.Info
        ),
        SystemInfoItem(
            label = "Identifiers (ECID)",
            value = deviceInfo.ecid.ifEmpty { "N/A" },
            icon = Icons.Default.Info
        ),
        SystemInfoItem(
            label = "Time Zone",
            value = deviceInfo.timeZone.ifEmpty { "N/A" },
            icon = Icons.Default.Info
        )
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                overviewViewModel.loadDeviceInfo()
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "System Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Top Connected iPhone Image & Custom Device Name
            if (deviceDrawableId != 0) {
                Image(
                    painter = painterResource(id = deviceDrawableId),
                    contentDescription = customDeviceName,
                    modifier = Modifier
                        .height(140.dp)
                        .widthIn(max = 140.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PhoneIphone,
                    contentDescription = "iPhone",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(96.dp)
                )
            }

            Text(
                text = customDeviceName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Connected via USB • Serial: ${deviceInfo.serialNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 2-Column Grid of System Info Cards
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(680.dp)
            ) {
                items(items) { item ->
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(item.label, item.value)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied ${item.label}", Toast.LENGTH_SHORT).show()
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
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                when {
                                    item.drawableResId != 0 -> {
                                        Image(
                                            painter = painterResource(id = item.drawableResId),
                                            contentDescription = "iOS Version Logo",
                                            modifier = Modifier.size(24.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    item.isActivatedState -> {
                                        Icon(
                                            imageVector = if (item.isActivated) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (item.isActivated) Color(0xFF22C55E) else Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    item.icon != null -> {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = item.value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isActivatedState) {
                                    if (item.isActivated) Color(0xFF15803D) else Color(0xFFB91C1C)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 3
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SystemInfoScreenPreview() {
    MaterialTheme {
        SystemInfoScreen(
            deviceInfo = DeviceInfo(deviceName = "Ahmad's iPhone", productType = "iPhone14,3", productVersion = "17.5.1", serialNumber = "C78GHJ4NPLXX"),
            onBack = {}
        )
    }
}
