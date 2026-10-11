package com.simodev.gpspositionsaver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simodev.gpspositionsaver.data.model.Group

/** Parses a "#RRGGBB" hex string into a Compose Color (falls back to blue). */
fun parseHexColor(hex: String): Color = try {
    val clean = hex.removePrefix("#")
    val value = clean.toLong(16)
    if (clean.length == 6) Color(0xFF000000L or value) else Color(value)
} catch (e: Exception) {
    Color(0xFF1E88E5)
}

val GROUP_PALETTE = listOf(
    "#4CAF50", "#1E88E5", "#FB8C00", "#8E24AA",
    "#00ACC1", "#E53935", "#FDD835", "#3949AB",
)

/** Horizontal, scrollable row of category filter chips. */
@Composable
fun GroupChips(
    groups: List<Group>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    includeAll: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (includeAll) {
            FilterPill(
                label = "All",
                color = MaterialTheme.colorScheme.primary,
                selected = selectedId == null,
                onClick = { onSelect(null) },
            )
        }
        groups.forEach { group ->
            FilterPill(
                label = group.name,
                color = parseHexColor(group.colorHex),
                selected = selectedId == group.id,
                onClick = { onSelect(group.id) },
            )
        }
    }
}

@Composable
fun FilterPill(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) color else color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50),
            )
            .border(
                width = 1.dp,
                color = if (selected) color else color.copy(alpha = 0.4f),
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else color,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
        )
    }
}

/** Small colored dot used in lists and map legends. */
@Composable
fun ColorDot(color: Color, size: Int = 12) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(color, shape = RoundedCornerShape(50))
    )
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

/** Reusable "create a category" dialog with name + palette picker. */
@Composable
fun NewGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, colorHex: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(GROUP_PALETTE[1]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text("Color", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GROUP_PALETTE.forEach { hex ->
                        val c = parseHexColor(hex)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(c, RoundedCornerShape(50))
                                .border(
                                    width = if (color == hex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(50),
                                )
                                .clickable { color = hex },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onCreate(name.trim(), color) },
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Confirmation dialog used for destructive actions. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Numeric text field helper used by the manual-coordinate entry dialog. */
@Composable
fun CoordinateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
fun SpacerH(width: Int) = Spacer(Modifier.width(width.dp))
