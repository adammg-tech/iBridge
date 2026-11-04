package com.adammg.ibridge.ui.screens.ringtones

import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

fun formatMmSsMs(seconds: Float): String {
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    val millis = ((seconds % 1) * 10).toInt()
    return "%02d:%02d.%d".format(mins, secs, millis)
}

@Composable
fun AudioTrimScreen(
    audioUri: Uri,
    initialRingName: String,
    onBack: () -> Unit,
    onConfirmTrim: (Float, Float, String) -> Unit
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    var totalDurationSec by remember { mutableFloatStateOf(30f) }
    var startTimeSec by remember { mutableFloatStateOf(0f) }
    var endTimeSec by remember { mutableFloatStateOf(30f) }
    var playheadSec by remember { mutableFloatStateOf(0f) }
    var ringtoneName by remember { mutableStateOf(initialRingName) }

    DisposableEffect(audioUri) {
        val player = MediaPlayer.create(context, audioUri)?.apply {
            totalDurationSec = (duration / 1000f).coerceAtLeast(1f)
            endTimeSec = if (totalDurationSec > 30f) 30f else totalDurationSec
            setOnCompletionListener {
                isPlaying = false
                playheadSec = startTimeSec
            }
        }
        mediaPlayer = player
        onDispose {
            try {
                player?.stop()
                player?.release()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isPlaying, startTimeSec, endTimeSec) {
        if (isPlaying) {
            while (isPlaying && mediaPlayer != null) {
                val currentMs = try { mediaPlayer?.currentPosition ?: 0 } catch (_: Exception) { 0 }
                playheadSec = currentMs / 1000f

                if (currentMs >= (endTimeSec * 1000).toInt()) {
                    try {
                        mediaPlayer?.pause()
                        mediaPlayer?.seekTo((startTimeSec * 1000).toInt())
                    } catch (_: Exception) {}
                    isPlaying = false
                    playheadSec = startTimeSec
                    break
                }
                kotlinx.coroutines.delay(50)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                try {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                } catch (_: Exception) {}
                onBack()
            }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Audio Trimmer & Editor",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = ringtoneName,
                    onValueChange = { ringtoneName = it },
                    label = { Text("Ringtone Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Precision +/- Controls (0.1s step) for Start and End Times matching reference image
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start Time Adjuster
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    startTimeSec = (startTimeSec - 0.1f).coerceAtLeast(0f)
                                    try {
                                        mediaPlayer?.seekTo((startTimeSec * 1000).toInt())
                                        mediaPlayer?.start()
                                        isPlaying = true
                                    } catch (_: Exception) {}
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease Start", modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatMmSsMs(startTimeSec),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    startTimeSec = (startTimeSec + 0.1f).coerceAtMost(endTimeSec - 0.5f)
                                    try {
                                        mediaPlayer?.seekTo((startTimeSec * 1000).toInt())
                                        mediaPlayer?.start()
                                        isPlaying = true
                                    } catch (_: Exception) {}
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase Start", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // End Time Adjuster
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    endTimeSec = (endTimeSec - 0.1f).coerceAtLeast(startTimeSec + 0.5f)
                                    try {
                                        val previewStart = (endTimeSec - 2.0f).coerceAtLeast(startTimeSec)
                                        mediaPlayer?.seekTo((previewStart * 1000).toInt())
                                        mediaPlayer?.start()
                                        isPlaying = true
                                    } catch (_: Exception) {}
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease End", modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatMmSsMs(endTimeSec),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    endTimeSec = (endTimeSec + 0.1f).coerceAtMost(totalDurationSec)
                                    try {
                                        val previewStart = (endTimeSec - 2.0f).coerceAtLeast(startTimeSec)
                                        mediaPlayer?.seekTo((previewStart * 1000).toInt())
                                        mediaPlayer?.start()
                                        isPlaying = true
                                    } catch (_: Exception) {}
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase End", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Interactive Draggable Audio Waveform Component with Playhead
                InteractiveWaveformView(
                    startTimeSec = startTimeSec,
                    endTimeSec = endTimeSec,
                    playheadSec = if (isPlaying) playheadSec else startTimeSec,
                    totalDurationSec = totalDurationSec,
                    onStartChange = { newStart ->
                        startTimeSec = newStart
                        try {
                            mediaPlayer?.seekTo((startTimeSec * 1000).toInt())
                        } catch (_: Exception) {}
                    },
                    onEndChange = { newEnd ->
                        endTimeSec = newEnd
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected: %.1fs".format(endTimeSec - startTimeSec),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "%s / %s".format(formatMmSsMs(playheadSec), formatMmSsMs(totalDurationSec)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        val player = mediaPlayer ?: return@Button
                        if (isPlaying) {
                            try {
                                player.pause()
                            } catch (_: Exception) {}
                            isPlaying = false
                        } else {
                            try {
                                player.seekTo((startTimeSec * 1000).toInt())
                                player.start()
                            } catch (_: Exception) {}
                            isPlaying = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPlaying) "Pause Preview" else "Play Full Selection")
                }
            }
        }

        // Save Trim & Return Button with strict <= 30s validation
        Button(
            onClick = {
                val duration = endTimeSec - startTimeSec
                if (duration > 30.0f) {
                    Toast.makeText(context, "Ringtone duration must be 30 seconds or less!", Toast.LENGTH_LONG).show()
                    return@Button
                }
                try {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                } catch (_: Exception) {}
                onConfirmTrim(startTimeSec, endTimeSec, ringtoneName)
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Save Trim & Return", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InteractiveWaveformView(
    startTimeSec: Float,
    endTimeSec: Float,
    playheadSec: Float,
    totalDurationSec: Float,
    onStartChange: (Float) -> Unit,
    onEndChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val dimmedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val handleColor = MaterialTheme.colorScheme.tertiary
    val playheadColor = Color(0xFFEF4444)

    var draggingHandle by remember { mutableStateOf<Int?>(null) } // 0 = start, 1 = end

    Canvas(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .pointerInput(totalDurationSec) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val startX = (startTimeSec / totalDurationSec) * size.width
                        val endX = (endTimeSec / totalDurationSec) * size.width
                        val distStart = kotlin.math.abs(offset.x - startX)
                        val distEnd = kotlin.math.abs(offset.x - endX)
                        draggingHandle = if (distStart <= distEnd) 0 else 1
                    },
                    onDragEnd = { draggingHandle = null },
                    onDragCancel = { draggingHandle = null },
                    onDrag = { change, _ ->
                        val newRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                        val newTime = newRatio * totalDurationSec
                        if (draggingHandle == 0) {
                            val clampedStart = newTime.coerceIn(0f, endTimeSec - 0.5f)
                            onStartChange(clampedStart)
                        } else if (draggingHandle == 1) {
                            val clampedEnd = newTime.coerceIn(startTimeSec + 0.5f, totalDurationSec)
                            onEndChange(clampedEnd)
                        }
                    }
                )
            }
    ) {
        val barCount = 45
        val barGap = 3.dp.toPx()
        val totalGaps = (barCount - 1) * barGap
        val barWidth = (size.width - totalGaps) / barCount

        val startRatio = (startTimeSec / totalDurationSec).coerceIn(0f, 1f)
        val endRatio = (endTimeSec / totalDurationSec).coerceIn(0f, 1f)
        val playheadRatio = (playheadSec / totalDurationSec).coerceIn(0f, 1f)

        val waveformHeights = listOf(
            0.3f, 0.5f, 0.8f, 0.4f, 0.9f, 0.6f, 0.3f, 0.7f, 0.5f, 0.9f,
            0.4f, 0.2f, 0.6f, 0.8f, 0.5f, 0.7f, 0.9f, 0.3f, 0.6f, 0.8f,
            0.4f, 0.7f, 0.9f, 0.5f, 0.3f, 0.8f, 0.6f, 0.4f, 0.9f, 0.7f,
            0.5f, 0.3f, 0.8f, 0.6f, 0.4f, 0.7f, 0.9f, 0.5f, 0.3f, 0.6f,
            0.8f, 0.5f, 0.7f, 0.4f, 0.2f
        )

        // Draw Waveform Bars
        for (i in 0 until barCount) {
            val x = i * (barWidth + barGap)
            val barRatio = i.toFloat() / barCount
            val isSelected = barRatio in startRatio..endRatio

            val heightFactor = waveformHeights[i % waveformHeights.size]
            val barHeight = size.height * heightFactor
            val y = (size.height - barHeight) / 2f

            drawRoundRect(
                color = if (isSelected) primaryColor else dimmedColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }

        // Draw Start Handle line & knob
        val startX = size.width * startRatio
        drawLine(
            color = handleColor,
            start = Offset(startX, 0f),
            end = Offset(startX, size.height),
            strokeWidth = 4.dp.toPx()
        )
        drawCircle(
            color = handleColor,
            radius = 7.dp.toPx(),
            center = Offset(startX, 0f)
        )
        drawCircle(
            color = handleColor,
            radius = 7.dp.toPx(),
            center = Offset(startX, size.height)
        )

        // Draw End Handle line & knob
        val endX = size.width * endRatio
        drawLine(
            color = handleColor,
            start = Offset(endX, 0f),
            end = Offset(endX, size.height),
            strokeWidth = 4.dp.toPx()
        )
        drawCircle(
            color = handleColor,
            radius = 7.dp.toPx(),
            center = Offset(endX, 0f)
        )
        drawCircle(
            color = handleColor,
            radius = 7.dp.toPx(),
            center = Offset(endX, size.height)
        )

        // Draw Animated Red Playhead Line
        if (playheadRatio in startRatio..endRatio) {
            val playheadX = size.width * playheadRatio
            drawLine(
                color = playheadColor,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, size.height),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
