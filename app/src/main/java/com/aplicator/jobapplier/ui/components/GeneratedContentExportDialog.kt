package com.aplicator.jobapplier.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.aplicator.jobapplier.data.export.ExportFormat

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun GeneratedContentExportDialog(
    suggestedFileName: String,
    onDismiss: () -> Unit,
    onExport: (String, ExportFormat) -> Unit,
) {
    var fileName by remember(suggestedFileName) { mutableStateOf(suggestedFileName) }
    var format by remember { mutableStateOf(ExportFormat.Pdf) }
    val canSave = fileName.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export answer") },
        text = {
            androidx.compose.foundation.layout.Column {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Format",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = format == ExportFormat.Pdf,
                        onClick = { format = ExportFormat.Pdf },
                        label = { Text("PDF") },
                    )
                    FilterChip(
                        selected = format == ExportFormat.Docx,
                        onClick = { format = ExportFormat.Docx },
                        label = { Text("DOCX") },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onExport(fileName, format) },
                enabled = canSave,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
