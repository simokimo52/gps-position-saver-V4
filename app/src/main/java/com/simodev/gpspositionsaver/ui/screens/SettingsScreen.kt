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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.simodev.gpspositionsaver.BuildConfig
import com.simodev.gpspositionsaver.data.model.AppSettings
import com.simodev.gpspositionsaver.data.model.CoordinateFormat
import com.simodev.gpspositionsaver.data.model.Group
import com.simodev.gpspositionsaver.data.model.UnitSystem
import com.simodev.gpspositionsaver.platform.IntentHelpers
import com.simodev.gpspositionsaver.platform.QrGenerator
import com.simodev.gpspositionsaver.ui.AppViewModel
import com.simodev.gpspositionsaver.ui.components.ColorDot
import com.simodev.gpspositionsaver.ui.components.ConfirmDialog
import com.simodev.gpspositionsaver.ui.components.NewGroupDialog
import com.simodev.gpspositionsaver.ui.components.SectionCard
import com.simodev.gpspositionsaver.ui.components.parseHexColor

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    settings: AppSettings,
    groups: List<Group>,
    placeCount: Int,
) {
    val context = LocalContext.current
    var showFormat by remember { mutableStateOf(false) }
    var showUnits by remember { mutableStateOf(false) }
    var showDefaultGroup by remember { mutableStateOf(false) }
    var showNewGroup by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showAppQr by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        SectionCard {
            SettingRow(
                title = "Coordinate Format",
                value = settings.coordinateFormat.displayName,
                onClick = { showFormat = true },
            )
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            SettingRow(
                title = "Unit System",
                value = settings.unitSystem.displayName,
                onClick = { showUnits = true },
            )
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            SettingRow(
                title = "Default Category",
                value = groups.firstOrNull { it.id == settings.defaultGroupId }?.name ?: "None",
                onClick = { showDefaultGroup = true },
            )
        }

        // Categories
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Categories", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { showNewGroup = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New category")
                }
            }
            if (groups.isEmpty()) {
                Text("No categories yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            groups.forEach { group ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                ) {
                    ColorDot(parseHexColor(group.colorHex))
                    Spacer(Modifier.width(10.dp))
                    Text(group.name, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    if (settings.defaultGroupId == group.id) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Default",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = { vm.deleteGroup(group.id) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete category",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        // Storage & privacy
        SectionCard {
            Text("Storage & Privacy", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Saved places: $placeCount", fontSize = 13.sp)
            Text("All data stays on this device.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                "Pure native Kotlin + Jetpack Compose ${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = { showAppQr = true }) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("App QR")
                }
                TextButton(onClick = { showClearConfirm = true }) {
                    Text("Clear all data", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Text(
            "© GPS Position Saver • offline-first",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showFormat) {
        ChoiceDialog(
            title = "Coordinate Format",
            options = CoordinateFormat.entries.map { it.displayName to it },
            selected = settings.coordinateFormat,
            onSelect = { vm.setCoordinateFormat(it); showFormat = false },
            onDismiss = { showFormat = false },
            subtitle = { it.example },
        )
    }

    if (showUnits) {
        ChoiceDialog(
            title = "Unit System",
            options = UnitSystem.entries.map { it.displayName to it },
            selected = settings.unitSystem,
            onSelect = { vm.setUnitSystem(it); showUnits = false },
            onDismiss = { showUnits = false },
            subtitle = { it.caption },
        )
    }

    if (showDefaultGroup) {
        AlertDialog(
            onDismissRequest = { showDefaultGroup = false },
            title = { Text("Default Category") },
            text = {
                Column {
                    TextButton(onClick = { vm.setDefaultGroup(null); showDefaultGroup = false }) {
                        Text("None")
                    }
                    groups.forEach { group ->
                        TextButton(onClick = { vm.setDefaultGroup(group.id); showDefaultGroup = false }) {
                            ColorDot(parseHexColor(group.colorHex))
                            Spacer(Modifier.width(8.dp))
                            Text(group.name)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDefaultGroup = false }) { Text("Close") } },
        )
    }

    if (showNewGroup) {
        NewGroupDialog(
            onDismiss = { showNewGroup = false },
            onCreate = { name, color ->
                vm.createGroup(name, color)
                showNewGroup = false
            },
        )
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "Clear all data?",
            message = "All saved places and live sessions will be deleted. Categories are kept.",
            confirmLabel = "Clear",
            destructive = true,
            onConfirm = { vm.clearAllData(); showClearConfirm = false },
            onDismiss = { showClearConfirm = false },
        )
    }

    if (showAppQr) {
        val qr = remember { QrGenerator.generate("GPS Position Saver\nOffline GPS bookmarking\nv${BuildConfig.VERSION_NAME}", 600) }
        AlertDialog(
            onDismissRequest = { showAppQr = false },
            title = { Text("App QR") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (qr != null) {
                        Image(
                            bitmap = qr.asImageBitmap(),
                            contentDescription = "App QR",
                            modifier = Modifier.size(220.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("GPS Position Saver v${BuildConfig.VERSION_NAME}", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    IntentHelpers.copyToClipboard(context, "app", "GPS Position Saver v${BuildConfig.VERSION_NAME}")
                    vm.showToast("Copied")
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Copy")
                }
            },
            dismissButton = { TextButton(onClick = { showAppQr = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onClick) { Text("Change") }
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    subtitle: (T) -> String,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (value == selected)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        onClick = { onSelect(value) },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    subtitle(value),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (value == selected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
