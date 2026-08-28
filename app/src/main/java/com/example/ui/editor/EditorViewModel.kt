package com.example.ui.editor

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.DocumentRepository
import com.example.pdf.PdfExporter
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID

enum class ToolbarMode {
    DEFAULT,
    TEXT,
    IMAGE,
    TABLE,
    DRAW,
    PAGE_MANAGER
}

enum class DrawToolType {
    PEN,
    HIGHLIGHTER,
    ERASER
}

data class DrawToolState(
    val type: DrawToolType = DrawToolType.PEN,
    val colorHex: String = "#111827",
    val strokeWidth: Float = 3f
)

sealed class SaveStatus {
    object Saved : SaveStatus()
    object Saving : SaveStatus()
}

@OptIn(FlowPreview::class)
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository.getInstance(application)

    private val _document = MutableStateFlow<DocumentModel?>(null)
    val document: StateFlow<DocumentModel?> = _document.asStateFlow()

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Saved)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    private val _toolbarMode = MutableStateFlow(ToolbarMode.DEFAULT)
    val toolbarMode: StateFlow<ToolbarMode> = _toolbarMode.asStateFlow()

    private val _selectedPageIndex = MutableStateFlow(0)
    val selectedPageIndex: StateFlow<Int> = _selectedPageIndex.asStateFlow()

    private val _selectedTextBlockId = MutableStateFlow<String?>(null)
    val selectedTextBlockId: StateFlow<String?> = _selectedTextBlockId.asStateFlow()

    private val _selectedImageId = MutableStateFlow<String?>(null)
    val selectedImageId: StateFlow<String?> = _selectedImageId.asStateFlow()

    private val _selectedTableId = MutableStateFlow<String?>(null)
    val selectedTableId: StateFlow<String?> = _selectedTableId.asStateFlow()

    private val _selectedTableCell = MutableStateFlow<Pair<Int, Int>?>(null)
    val selectedTableCell: StateFlow<Pair<Int, Int>?> = _selectedTableCell.asStateFlow()

    private val _zoomScale = MutableStateFlow(1.0f) // 1.0 = Fit A4
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _drawToolState = MutableStateFlow(DrawToolState())
    val drawToolState: StateFlow<DrawToolState> = _drawToolState.asStateFlow()

    private val _includePageNumbers = MutableStateFlow(false)
    val includePageNumbers: StateFlow<Boolean> = _includePageNumbers.asStateFlow()

    // Undo / Redo stacks
    private val undoStack = mutableListOf<DocumentModel>()
    private val redoStack = mutableListOf<DocumentModel>()
    private val _undoAvailable = MutableStateFlow(false)
    val undoAvailable: StateFlow<Boolean> = _undoAvailable.asStateFlow()
    private val _redoAvailable = MutableStateFlow(false)
    val redoAvailable: StateFlow<Boolean> = _redoAvailable.asStateFlow()

    private var autosaveJob: Job? = null

    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            val doc = repository.getDocument(documentId)
            if (doc != null) {
                _document.value = doc
                undoStack.clear()
                redoStack.clear()
                updateUndoRedoStates()
            }
        }
    }

    private fun pushUndoState() {
        val current = _document.value ?: return
        if (undoStack.size >= 30) {
            undoStack.removeAt(0)
        }
        undoStack.add(current)
        redoStack.clear()
        updateUndoRedoStates()
    }

    private fun updateUndoRedoStates() {
        _undoAvailable.value = undoStack.isNotEmpty()
        _redoAvailable.value = redoStack.isNotEmpty()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val current = _document.value ?: return
            redoStack.add(current)
            val prev = undoStack.removeAt(undoStack.lastIndex)
            _document.value = prev
            updateUndoRedoStates()
            scheduleAutosave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val current = _document.value ?: return
            undoStack.add(current)
            val next = redoStack.removeAt(redoStack.lastIndex)
            _document.value = next
            updateUndoRedoStates()
            scheduleAutosave()
        }
    }

    private fun updateDocument(transform: (DocumentModel) -> DocumentModel) {
        val current = _document.value ?: return
        pushUndoState()
        val updated = transform(current).copy(updatedAt = System.currentTimeMillis())
        _document.value = updated
        scheduleAutosave()
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            _saveStatus.value = SaveStatus.Saving
            delay(1000)
            val doc = _document.value
            if (doc != null) {
                repository.saveDocument(doc)
            }
            _saveStatus.value = SaveStatus.Saved
        }
    }

    fun saveImmediately() {
        viewModelScope.launch {
            val doc = _document.value ?: return@launch
            _saveStatus.value = SaveStatus.Saving
            repository.saveDocument(doc)
            _saveStatus.value = SaveStatus.Saved
        }
    }

    // --- Mode / Selection Handlers ---

    fun setToolbarMode(mode: ToolbarMode) {
        _toolbarMode.value = mode
    }

    fun selectPage(index: Int) {
        val current = _document.value ?: return
        if (index in current.pages.indices) {
            _selectedPageIndex.value = index
        }
    }

    fun selectTextBlock(textBlockId: String?) {
        _selectedTextBlockId.value = textBlockId
        _selectedImageId.value = null
        _selectedTableId.value = null
        _selectedTableCell.value = null
        if (textBlockId != null) {
            _toolbarMode.value = ToolbarMode.TEXT
        } else if (_toolbarMode.value == ToolbarMode.TEXT) {
            _toolbarMode.value = ToolbarMode.DEFAULT
        }
    }

    fun selectImage(imageId: String?) {
        _selectedImageId.value = imageId
        _selectedTextBlockId.value = null
        _selectedTableId.value = null
        _selectedTableCell.value = null
        if (imageId != null) {
            _toolbarMode.value = ToolbarMode.IMAGE
        } else if (_toolbarMode.value == ToolbarMode.IMAGE) {
            _toolbarMode.value = ToolbarMode.DEFAULT
        }
    }

    fun selectTable(tableId: String?, cell: Pair<Int, Int>? = null) {
        _selectedTableId.value = tableId
        _selectedTableCell.value = cell
        _selectedTextBlockId.value = null
        _selectedImageId.value = null
        if (tableId != null) {
            _toolbarMode.value = ToolbarMode.TABLE
        } else if (_toolbarMode.value == ToolbarMode.TABLE) {
            _toolbarMode.value = ToolbarMode.DEFAULT
        }
    }

    fun clearSelection() {
        _selectedTextBlockId.value = null
        _selectedImageId.value = null
        _selectedTableId.value = null
        _selectedTableCell.value = null
        if (_toolbarMode.value != ToolbarMode.DRAW && _toolbarMode.value != ToolbarMode.PAGE_MANAGER) {
            _toolbarMode.value = ToolbarMode.DEFAULT
        }
    }

    fun setZoomScale(scale: Float) {
        _zoomScale.value = scale.coerceIn(0.5f, 3.0f)
    }

    fun resetZoom() {
        _zoomScale.value = 1.0f
    }

    fun setDrawToolState(state: DrawToolState) {
        _drawToolState.value = state
    }

    fun togglePageNumbers() {
        _includePageNumbers.value = !_includePageNumbers.value
    }

    // --- Document Renaming ---

    fun renameDocument(newTitle: String) {
        if (newTitle.isBlank()) return
        updateDocument { it.copy(title = newTitle.trim()) }
    }

    // --- Text Editing & Formatting ---

    fun activateTextMode() {
        val currentDoc = _document.value ?: return
        val page = currentDoc.pages.getOrNull(_selectedPageIndex.value) ?: return
        if (page.textBlocks.isNotEmpty()) {
            selectTextBlock(page.textBlocks.first().id)
        } else {
            insertTextBox()
        }
    }

    fun updateTextContent(pageIndex: Int, textBlockId: String, newText: String) {
        val currentDoc = _document.value ?: return
        val page = currentDoc.pages.getOrNull(pageIndex) ?: return
        val tb = page.textBlocks.firstOrNull { it.id == textBlockId } ?: return
        if (tb.text == newText) return

        val updatedPages = currentDoc.pages.mapIndexed { idx, p ->
            if (idx == pageIndex) {
                val updatedBlocks = p.textBlocks.map { b ->
                    if (b.id == textBlockId) b.copy(text = newText) else b
                }
                p.copy(textBlocks = updatedBlocks)
            } else p
        }
        _document.value = currentDoc.copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
        scheduleAutosave()
    }

    fun updateSelectedTextFormatting(
        isBold: Boolean? = null,
        isItalic: Boolean? = null,
        isUnderline: Boolean? = null,
        fontSizeDelta: Float? = null,
        fontFamily: String? = null,
        textColorHex: String? = null,
        highlightColorHex: String? = null,
        clearHighlight: Boolean = false,
        alignment: TextAlignment? = null,
        lineSpacingMultiplier: Float? = null
    ) {
        val blockId = _selectedTextBlockId.value ?: return
        val pageIndex = _selectedPageIndex.value

        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, page ->
                if (idx == pageIndex) {
                    val updatedBlocks = page.textBlocks.map { tb ->
                        if (tb.id == blockId) {
                            var b = tb
                            if (isBold != null) b = b.copy(isBold = isBold)
                            if (isItalic != null) b = b.copy(isItalic = isItalic)
                            if (isUnderline != null) b = b.copy(isUnderline = isUnderline)
                            if (fontSizeDelta != null) b = b.copy(fontSize = (b.fontSize + fontSizeDelta).coerceIn(8f, 72f))
                            if (fontFamily != null) b = b.copy(fontFamily = fontFamily)
                            if (textColorHex != null) b = b.copy(textColorHex = textColorHex)
                            if (clearHighlight) b = b.copy(highlightColorHex = null)
                            else if (highlightColorHex != null) b = b.copy(highlightColorHex = highlightColorHex)
                            if (alignment != null) b = b.copy(alignment = alignment)
                            if (lineSpacingMultiplier != null) b = b.copy(lineSpacingMultiplier = lineSpacingMultiplier)
                            b
                        } else tb
                    }
                    page.copy(textBlocks = updatedBlocks)
                } else page
            }
            doc.copy(pages = updatedPages)
        }
    }

    // --- Page Management ---

    fun addPage(afterIndex: Int = _selectedPageIndex.value) {
        updateDocument { doc ->
            val newPage = PageModel(
                id = UUID.randomUUID().toString(),
                pageNumber = doc.pages.size + 1,
                textBlocks = listOf(
                    TextBlock(
                        text = "",
                        x = 40f,
                        y = 50f,
                        width = A4_WIDTH - 80f,
                        height = A4_HEIGHT - 100f
                    )
                )
            )
            val mutablePages = doc.pages.toMutableList()
            val insertIndex = (afterIndex + 1).coerceIn(0, mutablePages.size)
            mutablePages.add(insertIndex, newPage)
            val reindexed = mutablePages.mapIndexed { i, p -> p.copy(pageNumber = i + 1) }
            _selectedPageIndex.value = insertIndex
            doc.copy(pages = reindexed)
        }
    }

    fun duplicatePage(pageIndex: Int = _selectedPageIndex.value) {
        updateDocument { doc ->
            val target = doc.pages.getOrNull(pageIndex) ?: return@updateDocument doc
            val duplicate = target.copy(
                id = UUID.randomUUID().toString(),
                textBlocks = target.textBlocks.map { it.copy(id = UUID.randomUUID().toString()) },
                images = target.images.map { it.copy(id = UUID.randomUUID().toString()) },
                tables = target.tables.map { it.copy(id = UUID.randomUUID().toString()) },
                shapes = target.shapes.map { it.copy(id = UUID.randomUUID().toString()) },
                drawingStrokes = target.drawingStrokes.map { it.copy(id = UUID.randomUUID().toString()) }
            )
            val mutablePages = doc.pages.toMutableList()
            mutablePages.add(pageIndex + 1, duplicate)
            val reindexed = mutablePages.mapIndexed { i, p -> p.copy(pageNumber = i + 1) }
            _selectedPageIndex.value = pageIndex + 1
            doc.copy(pages = reindexed)
        }
    }

    fun deletePage(pageIndex: Int = _selectedPageIndex.value) {
        val currentDoc = _document.value ?: return
        if (currentDoc.pages.size <= 1) return // Keep at least one page

        updateDocument { doc ->
            val mutablePages = doc.pages.toMutableList()
            if (pageIndex in mutablePages.indices) {
                mutablePages.removeAt(pageIndex)
            }
            val reindexed = mutablePages.mapIndexed { i, p -> p.copy(pageNumber = i + 1) }
            _selectedPageIndex.value = (pageIndex - 1).coerceAtLeast(0)
            doc.copy(pages = reindexed)
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        updateDocument { doc ->
            val mutablePages = doc.pages.toMutableList()
            if (fromIndex in mutablePages.indices && toIndex in mutablePages.indices) {
                val item = mutablePages.removeAt(fromIndex)
                mutablePages.add(toIndex, item)
            }
            val reindexed = mutablePages.mapIndexed { i, p -> p.copy(pageNumber = i + 1) }
            _selectedPageIndex.value = toIndex
            doc.copy(pages = reindexed)
        }
    }

    fun rotatePage(pageIndex: Int = _selectedPageIndex.value) {
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    p.copy(rotationDegrees = (p.rotationDegrees + 90) % 360)
                } else p
            }
            doc.copy(pages = updatedPages)
        }
    }

    // --- Insert: Image, Table, Line, Shape ---

    fun insertImage(uri: Uri) {
        val pageIndex = _selectedPageIndex.value
        val newImg = ImageElement(
            id = UUID.randomUUID().toString(),
            uriString = uri.toString(),
            x = 50f,
            y = 120f,
            width = 240f,
            height = 180f
        )
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    p.copy(images = p.images + newImg)
                } else p
            }
            doc.copy(pages = updatedPages)
        }
        selectImage(newImg.id)
    }

    fun insertTextBox() {
        val pageIndex = _selectedPageIndex.value
        val newTextBlock = TextBlock(
            id = UUID.randomUUID().toString(),
            text = "Text Box",
            x = 50f,
            y = 200f,
            width = 200f,
            height = 50f,
            fontSize = 16f
        )
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    p.copy(textBlocks = p.textBlocks + newTextBlock)
                } else p
            }
            doc.copy(pages = updatedPages)
        }
        selectTextBlock(newTextBlock.id)
    }

    fun updateImageTransform(imageId: String, scaleFactor: Float? = null, rotationDelta: Float? = null) {
        val pageIndex = _selectedPageIndex.value
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    val updatedImages = p.images.map { img ->
                        if (img.id == imageId) {
                            var i = img
                            if (scaleFactor != null) {
                                val w = (i.width * scaleFactor).coerceIn(40f, A4_WIDTH - 20f)
                                val h = (i.height * scaleFactor).coerceIn(40f, A4_HEIGHT - 20f)
                                i = i.copy(width = w, height = h)
                            }
                            if (rotationDelta != null) {
                                i = i.copy(rotation = (i.rotation + rotationDelta) % 360f)
                            }
                            i
                        } else img
                    }
                    p.copy(images = updatedImages)
                } else p
            }
            doc.copy(pages = updatedPages)
        }
    }

    fun deleteSelectedImage() {
        val imageId = _selectedImageId.value ?: return
        val pageIndex = _selectedPageIndex.value
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    p.copy(images = p.images.filterNot { it.id == imageId })
                } else p
            }
            doc.copy(pages = updatedPages)
        }
        clearSelection()
    }

    fun insertTable(rows: Int = 3, cols: Int = 3) {
        val pageIndex = _selectedPageIndex.value
        val newTable = TableElement(
            id = UUID.randomUUID().toString(),
            x = 40f,
            y = 150f,
            width = A4_WIDTH - 80f,
            rows = rows,
            cols = cols,
            cells = List(rows) { List(cols) { "" } },
            cellAlignments = List(rows) { List(cols) { TextAlignment.LEFT } }
        )
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    p.copy(tables = p.tables + newTable)
                } else p
            }
            doc.copy(pages = updatedPages)
        }
        selectTable(newTable.id, Pair(0, 0))
    }

    fun updateTableCell(tableId: String, row: Int, col: Int, text: String) {
        val pageIndex = _selectedPageIndex.value
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    val updatedTables = p.tables.map { tbl ->
                        if (tbl.id == tableId) {
                            val newCells = tbl.cells.mapIndexed { rIdx, rList ->
                                if (rIdx == row) {
                                    rList.mapIndexed { cIdx, cVal ->
                                        if (cIdx == col) text else cVal
                                    }
                                } else rList
                            }
                            tbl.copy(cells = newCells)
                        } else tbl
                    }
                    p.copy(tables = updatedTables)
                } else p
            }
            doc.copy(pages = updatedPages)
        }
    }

    fun addTableRow(tableId: String) {
        val pageIndex = _selectedPageIndex.value
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    val updatedTables = p.tables.map { tbl ->
                        if (tbl.id == tableId) {
                            val newRows = tbl.rows + 1
                            val newCells = tbl.cells + listOf(List(tbl.cols) { "" })
                            val newAligns = tbl.cellAlignments + listOf(List(tbl.cols) { TextAlignment.LEFT })
                            tbl.copy(rows = newRows, cells = newCells, cellAlignments = newAligns)
                        } else tbl
                    }
                    p.copy(tables = updatedTables)
                } else p
            }
            doc.copy(pages = updatedPages)
        }
    }

    fun deleteTableRow(tableId: String) {
        val pageIndex = _selectedPageIndex.value
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    val updatedTables = p.tables.map { tbl ->
                        if (tbl.id == tableId && tbl.rows > 1) {
                            val newRows = tbl.rows - 1
                            val newCells = tbl.cells.dropLast(1)
                            val newAligns = tbl.cellAlignments.dropLast(1)
                            tbl.copy(rows = newRows, cells = newCells, cellAlignments = newAligns)
                        } else tbl
                    }
                    p.copy(tables = updatedTables)
                } else p
            }
            doc.copy(pages = updatedPages)
        }
    }

    fun addTableColumn(tableId: String) {
        val pageIndex = _selectedPageIndex.value
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    val updatedTables = p.tables.map { tbl ->
                        if (tbl.id == tableId) {
                            val newCols = tbl.cols + 1
                            val newCells = tbl.cells.map { it + "" }
                            val newAligns = tbl.cellAlignments.map { it + TextAlignment.LEFT }
                            tbl.copy(cols = newCols, cells = newCells, cellAlignments = newAligns)
                        } else tbl
                    }
                    p.copy(tables = updatedTables)
                } else p
            }
            doc.copy(pages = updatedPages)
        }
    }

    fun deleteTableColumn(tableId: String) {
        val pageIndex = _selectedPageIndex.value
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    val updatedTables = p.tables.map { tbl ->
                        if (tbl.id == tableId && tbl.cols > 1) {
                            val newCols = tbl.cols - 1
                            val newCells = tbl.cells.map { it.dropLast(1) }
                            val newAligns = tbl.cellAlignments.map { it.dropLast(1) }
                            tbl.copy(cols = newCols, cells = newCells, cellAlignments = newAligns)
                        } else tbl
                    }
                    p.copy(tables = updatedTables)
                } else p
            }
            doc.copy(pages = updatedPages)
        }
    }

    fun deleteSelectedTable() {
        val tableId = _selectedTableId.value ?: return
        val pageIndex = _selectedPageIndex.value
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    p.copy(tables = p.tables.filterNot { it.id == tableId })
                } else p
            }
            doc.copy(pages = updatedPages)
        }
        clearSelection()
    }

    fun insertShape(type: ShapeType) {
        val pageIndex = _selectedPageIndex.value
        val shape = when (type) {
            ShapeType.LINE -> ShapeElement(
                id = UUID.randomUUID().toString(),
                type = ShapeType.LINE,
                x = 40f,
                y = 120f,
                width = A4_WIDTH - 80f,
                height = 2f,
                strokeWidth = 2f
            )
            ShapeType.RECTANGLE -> ShapeElement(
                id = UUID.randomUUID().toString(),
                type = ShapeType.RECTANGLE,
                x = 40f,
                y = 120f,
                width = 200f,
                height = 100f,
                strokeWidth = 2f
            )
            ShapeType.CIRCLE -> ShapeElement(
                id = UUID.randomUUID().toString(),
                type = ShapeType.CIRCLE,
                x = 60f,
                y = 120f,
                width = 120f,
                height = 120f,
                strokeWidth = 2f
            )
        }
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    p.copy(shapes = p.shapes + shape)
                } else p
            }
            doc.copy(pages = updatedPages)
        }
    }

    // --- Drawing & Annotations ---

    fun addDrawingStroke(pageIndex: Int, stroke: DrawingStroke) {
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    p.copy(drawingStrokes = p.drawingStrokes + stroke)
                } else p
            }
            doc.copy(pages = updatedPages)
        }
    }

    fun clearDrawingStrokes(pageIndex: Int = _selectedPageIndex.value) {
        updateDocument { doc ->
            val updatedPages = doc.pages.mapIndexed { idx, p ->
                if (idx == pageIndex) {
                    p.copy(drawingStrokes = emptyList())
                } else p
            }
            doc.copy(pages = updatedPages)
        }
    }

    // --- PDF Export ---

    suspend fun exportPdf(context: Context, outputStream: OutputStream): Boolean {
        val doc = _document.value ?: return false
        return PdfExporter.exportToStream(
            context = context,
            document = doc,
            outputStream = outputStream,
            includePageNumbers = _includePageNumbers.value
        )
    }

    suspend fun exportToCache(context: Context): File? {
        val doc = _document.value ?: return null
        val cacheFile = File(context.cacheDir, "${doc.title.replace(" ", "_")}.pdf")
        val stream = FileOutputStream(cacheFile)
        val success = PdfExporter.exportToStream(
            context = context,
            document = doc,
            outputStream = stream,
            includePageNumbers = _includePageNumbers.value
        )
        stream.close()
        return if (success) cacheFile else null
    }
}
