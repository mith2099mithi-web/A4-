package com.example.ui.editor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PageModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageManagerBottomSheet(
    pages: List<PageModel>,
    selectedIndex: Int,
    onSelectPage: (Int) -> Unit,
    onAddPage: () -> Unit,
    onDuplicatePage: (Int) -> Unit,
    onDeletePage: (Int) -> Unit,
    onMovePage: (from: Int, to: Int) -> Unit,
    onRotatePage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header with Title & Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // List matching Page sheet in mockup
            PageActionRow(
                icon = Icons.Default.AddBox,
                label = "Add Page",
                testTag = "page_action_add",
                onClick = {
                    onAddPage()
                    onDismiss()
                }
            )

            PageActionRow(
                icon = Icons.Default.ContentCopy,
                label = "Duplicate Page",
                testTag = "page_action_duplicate",
                onClick = {
                    onDuplicatePage(selectedIndex)
                    onDismiss()
                }
            )

            PageActionRow(
                icon = Icons.Default.DeleteOutline,
                label = "Delete Page",
                testTag = "page_action_delete",
                isDestructive = true,
                enabled = pages.size > 1,
                onClick = {
                    onDeletePage(selectedIndex)
                    onDismiss()
                }
            )

            PageActionRow(
                icon = Icons.Default.SwapVert,
                label = "Move Page",
                testTag = "page_action_move",
                onClick = {
                    if (selectedIndex < pages.size - 1) {
                        onMovePage(selectedIndex, selectedIndex + 1)
                    } else if (selectedIndex > 0) {
                        onMovePage(selectedIndex, selectedIndex - 1)
                    }
                    onDismiss()
                }
            )

            PageActionRow(
                icon = Icons.Default.RotateRight,
                label = "Rotate Page",
                testTag = "page_action_rotate",
                onClick = {
                    onRotatePage(selectedIndex)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun PageActionRow(
    icon: ImageVector,
    label: String,
    testTag: String,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                isDestructive -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                isDestructive -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
