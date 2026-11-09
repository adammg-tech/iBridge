package com.adammg.ibridge.ui.screens.more

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adammg.ibridge.data.model.DeviceInfo
import com.adammg.ibridge.ui.screens.apps.AppsScreen
import com.adammg.ibridge.ui.screens.backup.BackupScreen
import com.adammg.ibridge.ui.screens.files.FileBrowserScreen
import com.adammg.ibridge.ui.screens.logs.LogsScreen
import com.adammg.ibridge.ui.screens.ringtones.RingtoneHubScreen

enum class MoreSubScreen {
    MAIN, DIAGNOSTICS, SETTINGS, HELP, ABOUT, RINGTONE,
    APPS, BACKUP, FILES, LOGS
}

@Composable
fun MoreScreen(
    deviceInfo: DeviceInfo,
    isPaired: Boolean,
    currentThemeMode: com.adammg.ibridge.ui.theme.ThemeMode = com.adammg.ibridge.ui.theme.ThemeMode.SYSTEM,
    onThemeModeChange: (com.adammg.ibridge.ui.theme.ThemeMode) -> Unit = {},
    onRunDiagnostics: () -> Unit = {},
    initialSubScreen: MoreSubScreen = MoreSubScreen.MAIN
) {
    var currentSubScreen by remember { mutableStateOf(initialSubScreen) }

    androidx.activity.compose.BackHandler(enabled = currentSubScreen != MoreSubScreen.MAIN) {
        currentSubScreen = MoreSubScreen.MAIN
    }

    AnimatedContent(targetState = currentSubScreen, label = "moreSubScreen") { subScreen ->
        when (subScreen) {
            MoreSubScreen.MAIN -> MoreMainView(
                onNavigate = { currentSubScreen = it }
            )
            MoreSubScreen.DIAGNOSTICS -> DiagnosticsView(
                deviceInfo = deviceInfo,
                isPaired = isPaired,
                onBack = { currentSubScreen = MoreSubScreen.MAIN },
                onRunDiagnostics = onRunDiagnostics
            )
            MoreSubScreen.SETTINGS -> SettingsView(
                currentThemeMode = currentThemeMode,
                onThemeModeChange = onThemeModeChange,
                onBack = { currentSubScreen = MoreSubScreen.MAIN }
            )
            MoreSubScreen.HELP -> HelpAndSupportView(
                onBack = { currentSubScreen = MoreSubScreen.MAIN }
            )
            MoreSubScreen.ABOUT -> AboutIBridgeView(
                onBack = { currentSubScreen = MoreSubScreen.MAIN }
            )
            MoreSubScreen.RINGTONE -> RingtoneHubScreen(
                onBack = { currentSubScreen = MoreSubScreen.MAIN }
            )
            MoreSubScreen.APPS -> ToolSubPage("Apps Manager", onBack = { currentSubScreen = MoreSubScreen.MAIN }) { AppsScreen() }
            MoreSubScreen.BACKUP -> ToolSubPage("Backup & Restore", onBack = { currentSubScreen = MoreSubScreen.MAIN }) { BackupScreen() }
            MoreSubScreen.FILES -> ToolSubPage("AFC File Explorer", onBack = { currentSubScreen = MoreSubScreen.MAIN }) { FileBrowserScreen() }
            MoreSubScreen.LOGS -> ToolSubPage("Logs & Crash Reports", onBack = { currentSubScreen = MoreSubScreen.MAIN }) { LogsScreen() }
        }
    }
}

@Composable
private fun ToolSubPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun MoreMainView(
    onNavigate: (MoreSubScreen) -> Unit
) {
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
            Text(
                text = "More",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(onClick = { onNavigate(MoreSubScreen.SETTINGS) }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Navigation Options List (Flattened utilities + More options)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MoreOptionCard(
                title = "Ringtone Management",
                subtitle = "Manage, export & import custom ringtones",
                icon = Icons.Default.MusicNote,
                iconBgColor = Color(0xFFFCE7F3),
                iconTint = Color(0xFFDB2777),
                onClick = { onNavigate(MoreSubScreen.RINGTONE) }
            )

            MoreOptionCard(
                title = "Apps Manager",
                subtitle = "Install, backup & remove apps",
                icon = Icons.Default.Apps,
                iconBgColor = Color(0xFFE0EDFF),
                iconTint = Color(0xFF1D61E7),
                onClick = { onNavigate(MoreSubScreen.APPS) }
            )

            MoreOptionCard(
                title = "Backup & Restore",
                subtitle = "Full iOS device backups",
                icon = Icons.Default.Backup,
                iconBgColor = Color(0xFFDCFCE7),
                iconTint = Color(0xFF16A34A),
                onClick = { onNavigate(MoreSubScreen.BACKUP) }
            )

            MoreOptionCard(
                title = "Logs & Crash Reports",
                subtitle = "Live syslog & crash logs",
                icon = Icons.Default.Terminal,
                iconBgColor = Color(0xFFDCFCE7),
                iconTint = Color(0xFF16A34A),
                onClick = { onNavigate(MoreSubScreen.LOGS) }
            )

            MoreOptionCard(
                title = "Diagnostics",
                subtitle = "Check connection health and run tests",
                icon = Icons.Default.DeveloperBoard,
                iconBgColor = Color(0xFFE0EDFF),
                iconTint = Color(0xFF1D61E7),
                onClick = { onNavigate(MoreSubScreen.DIAGNOSTICS) }
            )

            MoreOptionCard(
                title = "Settings",
                subtitle = "Customize iBridge experience",
                icon = Icons.Default.Settings,
                iconBgColor = Color(0xFFE0EDFF),
                iconTint = Color(0xFF1D61E7),
                onClick = { onNavigate(MoreSubScreen.SETTINGS) }
            )

            MoreOptionCard(
                title = "Help and Support",
                subtitle = "Get help, view FAQs and troubleshooting",
                icon = Icons.AutoMirrored.Filled.Help,
                iconBgColor = Color(0xFFEDE9FE),
                iconTint = Color(0xFF7C3AED),
                onClick = { onNavigate(MoreSubScreen.HELP) }
            )

            MoreOptionCard(
                title = "About iBridge",
                subtitle = "App information and credits",
                icon = Icons.Default.Info,
                iconBgColor = Color(0xFFDCFCE7),
                iconTint = Color(0xFF16A34A),
                onClick = { onNavigate(MoreSubScreen.ABOUT) }
            )
        }
    }
}

@Composable
private fun DiagnosticsView(
    deviceInfo: DeviceInfo,
    isPaired: Boolean,
    onBack: () -> Unit,
    onRunDiagnostics: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Diagnostics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Health Status Card
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isPaired) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPaired) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isPaired) Color(0xFF16A34A) else Color(0xFFDC2626),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isPaired) "Everything looks good!" else "Connection / Pairing Issue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isPaired) "Your iPhone is connected and trusted." else "Please ensure USB OTG is connected and 'Trust' was accepted on the iPhone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Connection Details Card
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Connection Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                DetailRow("Device", if (deviceInfo.deviceName.isNotBlank()) deviceInfo.deviceName else deviceInfo.marketingName)
                DetailRow("iOS Version", deviceInfo.productVersion)
                DetailRow("Connection Type", "USB")
            }
        }

        Button(
            onClick = onRunDiagnostics,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Run Diagnostics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsView(
    currentThemeMode: com.adammg.ibridge.ui.theme.ThemeMode,
    onThemeModeChange: (com.adammg.ibridge.ui.theme.ThemeMode) -> Unit,
    onBack: () -> Unit
) {
    var showThemeMenu by remember { mutableStateOf(false) }

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
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Box {
            MoreOptionCard(
                title = "Appearance",
                subtitle = "Theme: ${currentThemeMode.name.lowercase().replaceFirstChar { it.uppercase() }}",
                icon = Icons.Default.Palette,
                iconBgColor = Color(0xFFE0EDFF),
                iconTint = Color(0xFF1D61E7),
                onClick = { showThemeMenu = true }
            )

            DropdownMenu(
                expanded = showThemeMenu,
                onDismissRequest = { showThemeMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("System Default") },
                    onClick = {
                        onThemeModeChange(com.adammg.ibridge.ui.theme.ThemeMode.SYSTEM)
                        showThemeMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Light Theme") },
                    onClick = {
                        onThemeModeChange(com.adammg.ibridge.ui.theme.ThemeMode.LIGHT)
                        showThemeMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Dark Theme") },
                    onClick = {
                        onThemeModeChange(com.adammg.ibridge.ui.theme.ThemeMode.DARK)
                        showThemeMenu = false
                    }
                )
            }
        }

        MoreOptionCard(title = "Connection", subtitle = "USB behavior and preferences", icon = Icons.Default.Usb, iconBgColor = Color(0xFFE0EDFF), iconTint = Color(0xFF1D61E7), onClick = {})
        MoreOptionCard(title = "Transfers", subtitle = "File transfer options and behavior", icon = Icons.Default.SwapHoriz, iconBgColor = Color(0xFFE0EDFF), iconTint = Color(0xFF1D61E7), onClick = {})
        MoreOptionCard(title = "Privacy", subtitle = "Manage data and permissions", icon = Icons.Default.Lock, iconBgColor = Color(0xFFEDE9FE), iconTint = Color(0xFF7C3AED), onClick = {})
    }
}

@Composable
private fun HelpAndSupportView(onBack: () -> Unit) {
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
                text = "Help & Support",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Troubleshooting Connection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Text("1. Ensure your USB OTG cable is firmly connected.\n2. Tap 'Allow' if Android asks for USB permission.\n3. Unlock your iPhone and tap 'Trust' on the screen prompt.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AboutIBridgeView(onBack: () -> Unit) {
    val context = LocalContext.current

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
                text = "About iBridge",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                com.adammg.ibridge.ui.components.IBridgeLogo(heightDp = 48)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Version 3.1",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "iBridge is a powerful and easy-to-use app that lets you connect your iPhone and securely access its features on Android.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        MoreOptionCard(
            title = "Project on GitHub",
            subtitle = "View source code and contribute",
            icon = Icons.Default.Code,
            iconBgColor = Color(0xFFF3F4F6),
            iconTint = Color(0xFF1F2937),
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"))
                context.startActivity(intent)
            }
        )

        MoreOptionCard(
            title = "License",
            subtitle = "Apache License 2.0",
            icon = Icons.Default.Info,
            iconBgColor = Color(0xFFF3F4F6),
            iconTint = Color(0xFF1F2937),
            onClick = {}
        )

        MoreOptionCard(
            title = "Credits",
            subtitle = "Special thanks to libimobiledevice & open source community",
            icon = Icons.Default.Person,
            iconBgColor = Color(0xFFF3F4F6),
            iconTint = Color(0xFF1F2937),
            onClick = {}
        )
    }
}

@Composable
private fun MoreOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val effectiveBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else iconBgColor
    val effectiveTint = if (isDark) MaterialTheme.colorScheme.primary else iconTint

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = effectiveBg,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = effectiveTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview(showBackground = true)
@Composable
fun MoreScreenPreview() {
    MaterialTheme {
        MoreScreen(
            deviceInfo = DeviceInfo(deviceName = "iPhone 15 Pro", productVersion = "17.6.1"),
            isPaired = true
        )
    }
}
