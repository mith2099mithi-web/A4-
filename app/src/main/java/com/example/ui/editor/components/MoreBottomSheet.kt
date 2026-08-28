package com.example.ui.editor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreBottomSheet(
    docTitle: String,
    includePageNumbers: Boolean,
    onExportPdf: () -> Unit,
    onRename: () -> Unit,
    onTogglePageNumbers: () -> Unit,
    onFitZoom: () -> Unit,
    onDeleteDocument: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Document Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Primary action: Export PDF
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onDismiss()
                        onExportPdf()
                    }
                    .testTag("more_export_pdf_button"),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Export A4 PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("True 210x297mm PDF document", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MoreMenuItem(
                icon = Icons.Default.Edit,
                title = "Rename Document",
                subtitle = docTitle,
                onClick = {
                    onDismiss()
                    onRename()
                }
            )

            MoreMenuItem(
                icon = Icons.Default.Numbers,
                title = "Page Numbers",
                subtitle = if (includePageNumbers) "Enabled at bottom of pages" else "Disabled",
                trailing = {
                    Switch(checked = includePageNumbers, onCheckedChange = { onTogglePageNumbers() })
                },
                onClick = onTogglePageNumbers
            )

            MoreMenuItem(
                icon = Icons.Default.FitScreen,
                title = "Fit A4 to Screen",
                subtitle = "Reset zoom scale to 100%",
                onClick = {
                    onDismiss()
                    onFitZoom()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            MoreMenuItem(
                icon = Icons.Default.Delete,
                title = "Delete Document",
                subtitle = "Permanently remove this document",
                isDanger = true,
                onClick = {
                    onDismiss()
                    onDeleteDocument()
                }
            )
        }
    }
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isDanger: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = color)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}
