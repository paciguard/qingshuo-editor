package com.qingshuo.editor.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.qingshuo.editor.data.ClipFilter

@Composable
fun AddTextDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                if (text.isNotBlank()) onConfirm(text.trim())
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add text overlay") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Type your text…") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
fun FilterPickerDialog(
    current: ClipFilter,
    onPick: (ClipFilter) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Choose filter") },
        text = {
            LazyColumn {
                items(ClipFilter.values().toList()) { f ->
                    TextButton(
                        onClick = { onPick(f); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (f == current) "● ${f.displayName}" else f.displayName,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun TransitionPickerDialog(
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(0L to "Cut", 300L to "Fast crossfade", 600L to "Crossfade", 1200L to "Slow crossfade")
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Transition to next clip") },
        text = {
            Column {
                options.forEach { (ms, label) ->
                    TextButton(
                        onClick = { onPick(ms); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(label, modifier = Modifier.fillMaxWidth()) }
                }
            }
        }
    )
}

@Composable
fun ExportProgressDialog(progress: Float) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text("Exporting…") },
        text = {
            Column {
                Text("%.0f%%".format(progress * 100))
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
fun ExportDoneDialog(path: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text("OK") } },
        title = { Text("Export complete") },
        text = { Text("Saved to:\n$path") }
    )
}

@Composable
fun ExportErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text("OK") } },
        title = { Text("Export failed") },
        text = { Text(message) }
    )
}
