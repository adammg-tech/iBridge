package com.adammg.ibridge.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adammg.ibridge.core.logger.AppLogger
import com.adammg.ibridge.core.engine.IBridgeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun LiveTerminalConsole(
    modifier: Modifier = Modifier
) {
    val logs by AppLogger.logFlow.collectAsState()
    var isExpanded by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var commandInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val engine = remember { IBridgeEngine(context) }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && isExpanded) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(if (isExpanded) "Hide Terminal Log" else "Show Terminal Log (${logs.size})")
                    }
                }

                if (isExpanded) {
                    Row {
                        IconButton(onClick = {
                            if (logs.isNotEmpty()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("iBridge Logs", logs.joinToString("\n"))
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Full log copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Full Log")
                        }
                        IconButton(onClick = { AppLogger.clear() }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Console")
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0D0D0D),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        if (logs.isEmpty()) {
                            BoxContent("Awaiting system & binary events...")
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                items(logs) { line ->
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                        color = if (line.contains("ERROR", ignoreCase = true) || line.contains("exited with code", ignoreCase = true)) {
                                            Color(0xFFFF5555)
                                        } else if (line.contains("GRANTED", ignoreCase = true) || line.contains("SUCCESS", ignoreCase = true)) {
                                            Color(0xFF55FF55)
                                        } else {
                                            Color(0xFF00E5FF)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commandInput,
                            onValueChange = { commandInput = it },
                            placeholder = { Text("Type libimobiledevice command (e.g. ideviceinfo)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (commandInput.isNotBlank()) {
                                    val cmd = commandInput.trim()
                                    commandInput = ""
                                    scope.launch(Dispatchers.IO) {
                                        AppLogger.log("Terminal", "$ $cmd")
                                        val parts = cmd.split("\\s+".toRegex())
                                        val binary = parts[0]
                                        val args = parts.drop(1)
                                        val result = engine.executeCommand(binary, args)
                                        if (result.stdout.isNotBlank()) {
                                            result.stdout.lines().forEach { AppLogger.log(binary, it) }
                                        }
                                        if (result.stderr.isNotBlank()) {
                                            result.stderr.lines().forEach { AppLogger.log("$binary [err]", it) }
                                        }
                                        if (result.stdout.isBlank() && result.stderr.isBlank()) {
                                            AppLogger.log(binary, "Exited with code ${result.exitCode}")
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Run")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxContent(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LiveTerminalConsolePreview() {
    MaterialTheme {
        LiveTerminalConsole()
    }
}
