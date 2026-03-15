package com.example.livetv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlConfigHeader(
    currentUrl: String,
    onUrlUpdate: (String) -> Unit,
    onResetUrl: () -> Unit,
    currentAcestreamIp: String,
    onAcestreamIpUpdate: (String) -> Unit,
    onResetAcestreamIp: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editedUrl by remember { mutableStateOf(currentUrl) }
    var editedAcestreamIp by remember { mutableStateOf(currentAcestreamIp) }

    LaunchedEffect(currentUrl, currentAcestreamIp) {
        editedUrl = currentUrl
        editedAcestreamIp = currentAcestreamIp
    }

    // ── Header surface ───────────────────────────────────────────────────────
    Surface(
        modifier      = modifier.fillMaxWidth(),
        color         = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape         = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            if (isCompact) {
                // ── Mobile layout ───────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(top = 2.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Language,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(12.dp),
                        )
                        Text(
                            text     = currentUrl,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .weight(1f),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Router,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(12.dp),
                        )
                        Text(
                            text     = "Acestream: $currentAcestreamIp",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            } else {
                // ── Tablet / TV layout ──────────────────────────────────────
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment     = Alignment.Top,
                    ) {
                        // Scraping source
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier          = Modifier.padding(bottom = 2.dp),
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Language,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.primary,
                                    modifier           = Modifier.size(16.dp),
                                )
                                Text(
                                    text     = "Scraping Source:",
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                            Text(
                                text     = currentUrl,
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 22.dp),
                            )
                        }

                        // Acestream IP
                        Column(modifier = Modifier.weight(0.6f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier          = Modifier.padding(bottom = 2.dp),
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Router,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.primary,
                                    modifier           = Modifier.size(16.dp),
                                )
                                Text(
                                    text     = "Acestream IP:",
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                            Text(
                                text     = currentAcestreamIp,
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 22.dp),
                            )
                        }
                    }
                }
            }

            // ── Action buttons ──────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Edit — primary action
                FocusableButton(
                    onClick          = { showEditDialog = true },
                    backgroundColor  = MaterialTheme.colorScheme.primary,
                    contentColor     = MaterialTheme.colorScheme.onPrimary,
                    focusColor       = MaterialTheme.colorScheme.tertiary,
                ) {
                    Icon(
                        imageVector        = Icons.Default.Edit,
                        contentDescription = "Edit Configuration",
                        tint               = MaterialTheme.colorScheme.onPrimary,
                        modifier           = Modifier.size(20.dp),
                    )
                }

                // Reset — secondary / destructive action (no alpha hacks)
                FocusableButton(
                    onClick          = onResetUrl,
                    backgroundColor  = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor     = MaterialTheme.colorScheme.onSecondaryContainer,
                    focusColor       = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector        = Icons.Default.Check,
                        contentDescription = "Reset to Default",
                        tint               = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier           = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    // ── Edit dialog ──────────────────────────────────────────────────────────
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog    = false
                editedUrl         = currentUrl
                editedAcestreamIp = currentAcestreamIp
            },
            title = { Text("Configuration") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            "Base URL for scraping match data:",
                            style    = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        OutlinedTextField(
                            value         = editedUrl,
                            onValueChange = { editedUrl = it },
                            label         = { Text("Base URL") },
                            placeholder   = { Text("https://livetv.sx/enx/allupcomingsports/1/") },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                        )
                    }
                    Column {
                        Text(
                            "Acestream engine IP address:",
                            style    = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        OutlinedTextField(
                            value         = editedAcestreamIp,
                            onValueChange = { editedAcestreamIp = it },
                            label         = { Text("Acestream IP") },
                            placeholder   = { Text("127.0.0.1") },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedUrl.isNotBlank() && editedUrl != currentUrl)
                            onUrlUpdate(editedUrl)
                        if (editedAcestreamIp.isNotBlank() && editedAcestreamIp != currentAcestreamIp)
                            onAcestreamIpUpdate(editedAcestreamIp)
                        showEditDialog = false
                    },
                ) { Text("Update") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog    = false
                        editedUrl         = currentUrl
                        editedAcestreamIp = currentAcestreamIp
                    },
                ) { Text("Cancel") }
            },
        )
    }
}
