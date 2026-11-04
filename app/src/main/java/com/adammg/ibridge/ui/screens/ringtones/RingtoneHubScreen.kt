package com.adammg.ibridge.ui.screens.ringtones

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RingtoneHubScreen(
    onBack: () -> Unit
) {
    var currentTab by remember { mutableStateOf<RingtoneHubTab>(RingtoneHubTab.HUB) }

    when (currentTab) {
        RingtoneHubTab.HUB -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ringtone Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                RingtoneHubCard(
                    title = "Manage & Export Ringtones",
                    subtitle = "View existing iPhone ringtones and save them to phone",
                    icon = Icons.Default.Audiotrack,
                    iconBgColor = androidx.compose.ui.graphics.Color(0xFFE0EDFF),
                    iconTint = androidx.compose.ui.graphics.Color(0xFF1D61E7),
                    onClick = { currentTab = RingtoneHubTab.MANAGE }
                )

                RingtoneHubCard(
                    title = "Import Custom Ringtone",
                    subtitle = "Convert and transfer custom .m4r ringtones",
                    icon = Icons.Default.MusicNote,
                    iconBgColor = androidx.compose.ui.graphics.Color(0xFFFCE7F3),
                    iconTint = androidx.compose.ui.graphics.Color(0xFFDB2777),
                    onClick = { currentTab = RingtoneHubTab.IMPORT }
                )
            }
        }
        RingtoneHubTab.MANAGE -> {
            RingtoneManagerScreen(
                onBack = { currentTab = RingtoneHubTab.HUB }
            )
        }
        RingtoneHubTab.IMPORT -> {
            RingtoneImportScreen(
                onBack = { currentTab = RingtoneHubTab.HUB }
            )
        }
    }
}

enum class RingtoneHubTab {
    HUB, MANAGE, IMPORT
}

@Composable
private fun RingtoneHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = iconBgColor,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
