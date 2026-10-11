package com.simodev.gpspositionsaver.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simodev.gpspositionsaver.data.model.LocationState
import com.simodev.gpspositionsaver.data.model.ShareSession
import com.simodev.gpspositionsaver.geo.GeoUtils
import com.simodev.gpspositionsaver.platform.IntentHelpers
import com.simodev.gpspositionsaver.platform.QrGenerator
import com.simodev.gpspositionsaver.ui.AppViewModel
import com.simodev.gpspositionsaver.ui.components.SectionCard
import kotlinx.coroutines.delay

private val DURATIONS = listOf(15, 30, 60, 240, 480)

@Composable
fun LiveShareScreen(
    vm: AppViewModel,
    location: LocationState,
    activeSession: ShareSession?,
    sessions: List<ShareSession>,
) {
    val context = LocalContext.current
    var duration by remember { mutableStateOf(30) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(activeSession?.sessionId) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Live Location Sharing", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        if (activeSession == null) {
            SectionCard {
                Text("Share your location for a limited time", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "A viewer link with a self-terminating token is generated. " +
                        "No account or third-party server required.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Text("Duration", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DURATIONS.forEach { minutes ->
                        FilterChip(
                            selected = duration == minutes,
                            onClick = { duration = minutes },
                            label = { Text(formatDuration(minutes)) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { vm.startLiveSession(duration) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Radio, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Live Sharing")
                }
            }
        } else {
            val remaining = ((activeSession.expiresAt - now) / 1000).coerceAtLeast(0)
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF2E7D32), RoundedCornerShape(50)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("LIVE", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatCountdown(remaining),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Current position: " +
                        GeoUtils.formatCoordinates(location.latitude, location.longitude, com.simodev.gpspositionsaver.data.model.CoordinateFormat.DECIMAL_DEGREES),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))
                val qr = remember(activeSession.shareUrl) {
                    QrGenerator.generate(activeSession.shareUrl, 600)
                }
                if (qr != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = qr.asImageBitmap(),
                            contentDescription = "Live share QR code",
                            modifier = Modifier.size(220.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    activeSession.shareUrl,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            IntentHelpers.copyToClipboard(context, "live link", activeSession.shareUrl)
                            vm.showToast("Link copied")
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Copy")
                    }
                    OutlinedButton(
                        onClick = {
                            IntentHelpers.shareText(context, activeSession.shareUrl, "My live location")
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Send")
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { vm.stopLiveSession() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop Sharing")
                }
            }
        }

        if (sessions.isNotEmpty()) {
            Text("History", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            sessions.take(10).forEach { session ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            formatDuration(session.durationMinutes),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (session.isActive && session.expiresAt > now) "Active" else "Ended",
                            fontSize = 11.sp,
                            color = if (session.isActive && session.expiresAt > now)
                                Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        session.shareUrl,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun formatDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    else -> "${minutes / 60} h"
}

private fun formatCountdown(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
