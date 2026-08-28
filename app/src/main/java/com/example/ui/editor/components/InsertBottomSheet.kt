package com.example.ui.editor.components

import androidx.compose.foundation.BorderStroke
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
import com.example.data.model.ShapeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsertBottomSheet(
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    onInsertTable: (rows: Int, cols: Int) -> Unit,
    onInsertShape: (ShapeType) -> Unit,
    onInsertTextBox: () -> Unit = {},
    onInsertPageBreak: () -> Unit = {}
) {
    var showTableDialog by remember { mutableStateOf(false) }
    var showShapeDialog by remember { mutableStateOf(false) }

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
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Insert",
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

            // 3-Column x 2-Row Grid matching mockup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InsertCardItem(
                    icon = Icons.Default.Image,
                    label = "Image",
                    testTag = "insert_image_button",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onPickImage()
                    }
                )

                InsertCardItem(
                    icon = Icons.Default.TableChart,
                    label = "Table",
                    testTag = "insert_table_button",
                    modifier = Modifier.weight(1f),
                    onClick = { showTableDialog = true }
                )

                InsertCardItem(
                    icon = Icons.Default.HorizontalRule,
                    label = "Line",
                    testTag = "insert_line_button",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onInsertShape(ShapeType.LINE)
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                InsertCardItem(
                    icon = Icons.Default.ChangeHistory,
                    label = "Shape",
                    testTag = "insert_shape_button",
                    modifier = Modifier.weight(1f),
                    onClick = { showShapeDialog = true }
                )

                InsertCardItem(
                    icon = Icons.Default.CropFree,
                    label = "Text Box",
                    testTag = "insert_textbox_button",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onInsertTextBox()
                    }
                )

                InsertCardItem(
                    icon = Icons.Default.VerticalSplit,
                    label = "Page Break",
                    testTag = "insert_pagebreak_button",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        onInsertPageBreak()
                    }
                )
            }
        }
    }

    // Table creation dialog
    if (showTableDialog) {
        var rowsText by remember { mutableStateOf("3") }
        var colsText by remember { mutableStateOf("3") }

        AlertDialog(
            onDismissRequest = { showTableDialog = false },
            title = { Text("Insert Table", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = rowsText,
                        onValueChange = { rowsText = it },
                        label = { Text("Number of Rows (1-10)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = colsText,
                        onValueChange = { colsText = it },
                        label = { Text("Number of Columns (1-6)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val r = rowsText.toIntOrNull()?.coerceIn(1, 10) ?: 3
                        val c = colsText.toIntOrNull()?.coerceIn(1, 6) ?: 3
                        showTableDialog = false
                        onDismiss()
                        onInsertTable(r, c)
                    }
                ) {
                    Text("Insert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTableDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Shape selection dialog
    if (showShapeDialog) {
        AlertDialog(
            onDismissRequest = { showShapeDialog = false },
            title = { Text("Select Shape", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListItem(
                        headlineContent = { Text("Rectangle Frame") },
                        leadingContent = { Icon(Icons.Default.CropSquare, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showShapeDialog = false
                            onDismiss()
                            onInsertShape(ShapeType.RECTANGLE)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Circle / Ellipse") },
                        leadingContent = { Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showShapeDialog = false
                            onDismiss()
                            onInsertShape(ShapeType.CIRCLE)
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Divider Line") },
                        leadingContent = { Icon(Icons.Default.HorizontalRule, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showShapeDialog = false
                            onDismiss()
                            onInsertShape(ShapeType.LINE)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showShapeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InsertCardItem(
    icon: ImageVector,
    label: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier
            .aspectRatio(1.05f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
