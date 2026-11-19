package com.example.livetv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.livetv.BuildConfig

/**
 * Settings: scraping source, Acestream engine address and the update check.
 * These used to live in the header; they are rarely changed, so they now sit here.
 */
@Composable
fun SettingsDialog(
    currentUrl: String,
    currentAcestreamIp: String,
    onUrlUpdate: (String) -> Unit,
    onResetUrl: () -> Unit,
    onAcestreamIpUpdate: (String) -> Unit,
    onResetAcestreamIp: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onDismiss: () -> Unit,
) {
    var url by remember { mutableStateOf(currentUrl) }
    var ip by remember { mutableStateOf(currentAcestreamIp) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Scraping source") },
                    placeholder = { Text("https://livetv.sx/enx/allupcomingsports/1/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { onResetUrl(); onDismiss() }) { Text("Reset source to default") }
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("Acestream engine IP") },
                    placeholder = { Text("127.0.0.1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { onResetAcestreamIp(); onDismiss() }) { Text("Reset IP to default") }
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = { onDismiss(); onCheckForUpdates() }) { Text("Check for updates") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (url.isNotBlank() && url != currentUrl) onUrlUpdate(url)
                if (ip.isNotBlank() && ip != currentAcestreamIp) onAcestreamIpUpdate(ip)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
