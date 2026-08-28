package com.example.ui.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.DocumentMode
import com.example.data.model.TextBlock
import com.example.ui.editor.components.CapCutToolbar
import com.example.ui.editor.components.InsertBottomSheet
import com.example.ui.editor.components.MoreBottomSheet
import com.example.ui.editor.components.PageManagerBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    documentId: String,
    onNavigateBack: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val document by viewModel.document.collectAsStateWithLifecycle()
    val saveStatus by viewModel.saveStatus.collectAsStateWithLifecycle()
    val toolbarMode by viewModel.toolbarMode.collectAsStateWithLifecycle()
    val selectedPageIndex by viewModel.selectedPageIndex.collectAsStateWithLifecycle()
    val selectedTextBlockId by viewModel.selectedTextBlockId.collectAsStateWithLifecycle()
    val selectedImageId by viewModel.selectedImageId.collectAsStateWithLifecycle()
    val selectedTableId by viewModel.selectedTableId.collectAsStateWithLifecycle()
    val selectedTableCell by viewModel.selectedTableCell.collectAsStateWithLifecycle()
    val zoomScale by viewModel.zoomScale.collectAsStateWithLifecycle()
    val drawToolState by viewModel.drawToolState.collectAsStateWithLifecycle()
    val includePageNumbers by viewModel.includePageNumbers.collectAsStateWithLifecycle()
    val undoAvailable by viewModel.undoAvailable.collectAsStateWithLifecycle()
    val redoAvailable by viewModel.redoAvailable.collectAsStateWithLifecycle()

    var showInsertSheet by remember { mutableStateOf(false) }
    var showPageManagerSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.insertImage(uri)
        }
    }

    // PDF Export SAF Launcher
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val stream = context.contentResolver.openOutputStream(uri)
                    if (stream != null) {
                        val success = viewModel.exportPdf(context, stream)
                        stream.close()
                        if (success) {
                            Toast.makeText(context, "PDF Exported Successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    val currentDoc = document

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showRenameDialog = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = currentDoc?.title ?: "Document",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (saveStatus) {
                                SaveStatus.Saving -> "Saving..."
                                SaveStatus.Saved -> "Saved"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.saveImmediately()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = undoAvailable,
                        modifier = Modifier.testTag("editor_undo_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (undoAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = redoAvailable,
                        modifier = Modifier.testTag("editor_redo_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (redoAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // More Menu
                    IconButton(
                        onClick = { showMoreSheet = true },
                        modifier = Modifier.testTag("editor_more_options_button")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            val selectedTextBlock = currentDoc?.pages?.getOrNull(selectedPageIndex)?.textBlocks?.firstOrNull { it.id == selectedTextBlockId }
            CapCutToolbar(
                mode = toolbarMode,
                selectedTextBlock = selectedTextBlock,
                drawToolState = drawToolState,
                onModeChange = { newMode ->
                    if (newMode == ToolbarMode.TEXT && selectedTextBlockId == null) {
                        viewModel.activateTextMode()
                    } else {
                        viewModel.setToolbarMode(newMode)
                    }
                },
                onOpenInsertSheet = { showInsertSheet = true },
                onOpenPageManager = { showPageManagerSheet = true },
                onOpenMoreMenu = { showMoreSheet = true },
                onFormatText = { b, i, u, fs, font, color, hl, clearHl, align, sp ->
                    viewModel.updateSelectedTextFormatting(
                        isBold = b,
                        isItalic = i,
                        isUnderline = u,
                        fontSizeDelta = fs,
                        fontFamily = font,
                        textColorHex = color,
                        highlightColorHex = hl,
                        clearHighlight = clearHl,
                        alignment = align,
                        lineSpacingMultiplier = sp
                    )
                },
                onImageScale = { scale ->
                    if (selectedImageId != null) {
                        viewModel.updateImageTransform(selectedImageId!!, scaleFactor = scale)
                    }
                },
                onImageRotate = { deg ->
                    if (selectedImageId != null) {
                        viewModel.updateImageTransform(selectedImageId!!, rotationDelta = deg)
                    }
                },
                onDeleteImage = { viewModel.deleteSelectedImage() },
                onAddTableRow = {
                    if (selectedTableId != null) viewModel.addTableRow(selectedTableId!!)
                },
                onDeleteTableRow = {
                    if (selectedTableId != null) viewModel.deleteTableRow(selectedTableId!!)
                },
                onAddTableCol = {
                    if (selectedTableId != null) viewModel.addTableColumn(selectedTableId!!)
                },
                onDeleteTableCol = {
                    if (selectedTableId != null) viewModel.deleteTableColumn(selectedTableId!!)
                },
                onDeleteTable = { viewModel.deleteSelectedTable() },
                onUpdateDrawTool = { viewModel.setDrawToolState(it) },
                onClearDrawStrokes = { viewModel.clearDrawingStrokes(selectedPageIndex) },
                onDoneSelection = { viewModel.clearSelection() }
            )
        }
    ) { innerPadding ->
        if (currentDoc != null) {
            A4CanvasView(
                document = currentDoc,
                zoomScale = zoomScale,
                toolbarMode = toolbarMode,
                selectedTextBlockId = selectedTextBlockId,
                selectedImageId = selectedImageId,
                selectedTableId = selectedTableId,
                selectedTableCell = selectedTableCell,
                drawToolState = drawToolState,
                includePageNumbers = includePageNumbers,
                onZoomChange = { viewModel.setZoomScale(it) },
                onSelectPage = { viewModel.selectPage(it) },
                onSelectTextBlock = { viewModel.selectTextBlock(it) },
                onUpdateText = { pageIdx, blockId, txt ->
                    viewModel.updateTextContent(pageIdx, blockId, txt)
                },
                onSelectImage = { viewModel.selectImage(it) },
                onSelectTable = { tblId, cell ->
                    viewModel.selectTable(tblId, cell)
                },
                onUpdateTableCell = { tblId, r, c, txt ->
                    viewModel.updateTableCell(tblId, r, c, txt)
                },
                onAddDrawingStroke = { pageIdx, stroke ->
                    viewModel.addDrawingStroke(pageIdx, stroke)
                },
                onAddPage = { viewModel.addPage() },
                onClearSelection = { viewModel.clearSelection() },
                onOpenPageManager = { showPageManagerSheet = true },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    // Bottom Sheets
    if (showInsertSheet) {
        InsertBottomSheet(
            onDismiss = { showInsertSheet = false },
            onPickImage = { imagePickerLauncher.launch("image/*") },
            onInsertTable = { rows, cols -> viewModel.insertTable(rows, cols) },
            onInsertShape = { type -> viewModel.insertShape(type) },
            onInsertTextBox = { viewModel.insertTextBox() },
            onInsertPageBreak = { viewModel.addPage() }
        )
    }

    if (showPageManagerSheet && currentDoc != null) {
        PageManagerBottomSheet(
            pages = currentDoc.pages,
            selectedIndex = selectedPageIndex,
            onSelectPage = { idx ->
                viewModel.selectPage(idx)
                showPageManagerSheet = false
            },
            onAddPage = { viewModel.addPage() },
            onDuplicatePage = { viewModel.duplicatePage(it) },
            onDeletePage = { viewModel.deletePage(it) },
            onMovePage = { from, to -> viewModel.movePage(from, to) },
            onRotatePage = { viewModel.rotatePage(it) },
            onDismiss = { showPageManagerSheet = false }
        )
    }

    if (showMoreSheet && currentDoc != null) {
        MoreBottomSheet(
            docTitle = currentDoc.title,
            includePageNumbers = includePageNumbers,
            onExportPdf = {
                val fileName = "${currentDoc.title.replace(" ", "_")}.pdf"
                exportPdfLauncher.launch(fileName)
            },
            onRename = { showRenameDialog = true },
            onTogglePageNumbers = { viewModel.togglePageNumbers() },
            onFitZoom = { viewModel.resetZoom() },
            onDeleteDocument = { showDeleteConfirmDialog = true },
            onDismiss = { showMoreSheet = false }
        )
    }

    // Rename Dialog
    if (showRenameDialog && currentDoc != null) {
        var renameText by remember { mutableStateOf(currentDoc.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_text_field")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameDocument(renameText)
                        showRenameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && currentDoc != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Document?") },
            text = { Text("This will permanently delete '${currentDoc.title}'. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
