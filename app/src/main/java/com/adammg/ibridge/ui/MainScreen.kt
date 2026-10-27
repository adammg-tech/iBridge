package com.adammg.ibridge.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adammg.ibridge.ui.components.LiveTerminalConsole
import com.adammg.ibridge.ui.components.TopDeviceBanner
import com.adammg.ibridge.ui.screens.overview.OverviewScreen
import com.adammg.ibridge.ui.screens.photos.PhotosScreen
import com.adammg.ibridge.ui.screens.tools.ToolsHubScreen

data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun MainScreen(
    mainViewModel: MainViewModel = viewModel()
) {
    val usbState by mainViewModel.usbConnectionState.collectAsState()
    val deviceInfo by mainViewModel.deviceInfo.collectAsState()
    val isPaired by mainViewModel.isPaired.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val navItems = listOf(
        NavItem("Overview", Icons.Default.Info),
        NavItem("Photos", Icons.Default.PhotoLibrary),
        NavItem("Tools", Icons.Default.Build)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
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
            TopDeviceBanner(
                usbState = usbState,
                deviceInfo = deviceInfo,
                isPaired = isPaired,
                onPairClick = { mainViewModel.pairDevice() }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabIndex) {
                    0 -> OverviewScreen()
                    1 -> PhotosScreen()
                    2 -> ToolsHubScreen()
                }
            }
        }
    }
}
