package com.adammg.ibridge.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adammg.ibridge.ui.screens.files.FileBrowserScreen
import com.adammg.ibridge.ui.screens.home.HomeScreen
import com.adammg.ibridge.ui.screens.more.MoreScreen
import com.adammg.ibridge.ui.screens.more.MoreSubScreen
import com.adammg.ibridge.ui.screens.onboarding.OnboardingWizardScreen
import com.adammg.ibridge.ui.screens.photos.PhotosScreen
import com.adammg.ibridge.ui.theme.IBridgeTheme
import com.adammg.ibridge.ui.theme.ThemeMode

data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun MainScreen(
    mainViewModel: MainViewModel = viewModel()
) {
    val usbState by mainViewModel.usbConnectionState.collectAsState()
    val deviceInfo by mainViewModel.deviceInfo.collectAsState()
    val isPaired by mainViewModel.isPaired.collectAsState()
    val statusMessage by mainViewModel.statusMessage.collectAsState()
    val themeMode by mainViewModel.themeMode.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var moreInitialSubScreen by remember { mutableStateOf(MoreSubScreen.MAIN) }
    var hasStartedBridge by remember { mutableStateOf(false) }

    val navItems = listOf(
        NavItem("Home", Icons.Default.Home),
        NavItem("Files", Icons.Default.Folder),
        NavItem("Photos", Icons.Default.PhotoLibrary),
        NavItem("More", Icons.Default.MoreHoriz)
    )

    IBridgeTheme(themeMode = themeMode) {
        if (!hasStartedBridge || !isPaired) {
            OnboardingWizardScreen(
                usbState = usbState,
                isPaired = isPaired,
                deviceInfo = deviceInfo,
                statusMessage = statusMessage,
                onAllowUsbClick = { mainViewModel.pairDevice() },
                onStartUsingBridge = { hasStartedBridge = true }
            )
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        navItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = selectedTabIndex == index,
                                onClick = {
                                    if (index == 3) {
                                        moreInitialSubScreen = MoreSubScreen.MAIN
                                    }
                                    selectedTabIndex = index
                                },
                                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTabIndex) {
                            0 -> HomeScreen(
                                deviceInfo = deviceInfo,
                                onNavigateToFiles = { selectedTabIndex = 1 },
                                onNavigateToPhotos = { selectedTabIndex = 2 },
                                onNavigateToSettings = {
                                    moreInitialSubScreen = MoreSubScreen.SETTINGS
                                    selectedTabIndex = 3
                                },
                                onNavigateToBackups = {
                                    moreInitialSubScreen = MoreSubScreen.BACKUP
                                    selectedTabIndex = 3
                                }
                            )
                            1 -> FileBrowserScreen()
                            2 -> PhotosScreen()
                            3 -> MoreScreen(
                                deviceInfo = deviceInfo,
                                isPaired = isPaired,
                                currentThemeMode = themeMode,
                                onThemeModeChange = { mainViewModel.setThemeMode(it) },
                                initialSubScreen = moreInitialSubScreen
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
fun MainScreenPreview() {
    MainScreen()
}
