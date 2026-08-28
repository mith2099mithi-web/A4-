package com.example.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.pdf.PdfRendererHelper
import com.example.ui.theme.BorderLight
import com.example.ui.theme.PaperWhite
import kotlinx.coroutines.launch

@Composable
fun A4CanvasView(
    document: DocumentModel,
    zoomScale: Float,
    toolbarMode: ToolbarMode,
    selectedTextBlockId: String?,
    selectedImageId: String?,
    selectedTableId: String?,
    selectedTableCell: Pair<Int, Int>?,
    drawToolState: DrawToolState,
    includePageNumbers: Boolean,
    onZoomChange: (Float) -> Unit,
    onSelectPage: (Int) -> Unit,
    onSelectTextBlock: (String) -> Unit,
    onUpdateText: (pageIndex: Int, textBlockId: String, newText: String) -> Unit,
    onSelectImage: (String) -> Unit,
    onSelectTable: (tableId: String, cell: Pair<Int, Int>) -> Unit,
    onUpdateTableCell: (tableId: String, row: Int, col: Int, text: String) -> Unit,
    onAddDrawingStroke: (pageIndex: Int, stroke: DrawingStroke) -> Unit,
    onAddPage: () -> Unit,
    onClearSelection: () -> Unit,
    onOpenPageManager: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val firstVisiblePage by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex.coerceIn(0, (document.pages.size - 1).coerceAtLeast(0))
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (toolbarMode != ToolbarMode.DRAW) {
                        onZoomChange(zoomScale * zoom)
                    }
                }
            }
    ) {
        val availableWidth = maxWidth - 32.dp
        val basePageWidthDp = availableWidth.coerceIn(280.dp, 620.dp)
        val scaledPageWidthDp = basePageWidthDp * zoomScale
        val scaledPageHeightDp = scaledPageWidthDp * (A4_HEIGHT / A4_WIDTH)

        // Scale ratio from internal 595 pt to display dp
        val coordinateScale = scaledPageWidthDp.value / A4_WIDTH

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("a4_canvas_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            itemsIndexed(document.pages, key = { _, page -> page.id }) { pageIndex, pageModel ->
                PageContainer(
                    context = context,
                    document = document,
                    pageIndex = pageIndex,
                    pageModel = pageModel,
                    totalPages = document.pages.size,
                    pageWidthDp = scaledPageWidthDp,
                    pageHeightDp = scaledPageHeightDp,
                    coordinateScale = coordinateScale,
                    toolbarMode = toolbarMode,
                    selectedTextBlockId = selectedTextBlockId,
                    selectedImageId = selectedImageId,
                    selectedTableId = selectedTableId,
                    selectedTableCell = selectedTableCell,
                    drawToolState = drawToolState,
                    includePageNumbers = includePageNumbers,
                    onSelectPage = { onSelectPage(pageIndex) },
                    onSelectTextBlock = onSelectTextBlock,
                    onUpdateText = onUpdateText,
                    onSelectImage = onSelectImage,
                    onSelectTable = onSelectTable,
                    onUpdateTableCell = onUpdateTableCell,
                    onAddDrawingStroke = onAddDrawingStroke,
                    onClearSelection = onClearSelection
                )
            }

            // Quick "+ Add Page" button at the bottom of document
            item {
                OutlinedButton(
                    onClick = onAddPage,
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 40.dp)
                        .testTag("bottom_add_page_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Add Page", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Floating Page Indicator & Grid overview button matching mockup
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Page X of Y badge pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "Page ${firstVisiblePage + 1} of ${document.pages.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            // Grid / Page overview button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onOpenPageManager)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                    Canvas(modifier = Modifier.size(16.dp)) {
                        val s = size.width
                        val gap = 2.dp.toPx()
                        val cell = (s - gap) / 2
                        val r = 1.5.dp.toPx()
                        drawRoundRect(color = onSurfaceColor, topLeft = Offset(0f, 0f), size = Size(cell, cell), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))
                        drawRoundRect(color = onSurfaceColor, topLeft = Offset(cell + gap, 0f), size = Size(cell, cell), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))
                        drawRoundRect(color = onSurfaceColor, topLeft = Offset(0f, cell + gap), size = Size(cell, cell), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))
                        drawRoundRect(color = onSurfaceColor, topLeft = Offset(cell + gap, cell + gap), size = Size(cell, cell), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))
                    }
                }
            }
        }
    }
}

@Composable
private fun PageContainer(
    context: Context,
    document: DocumentModel,
    pageIndex: Int,
    pageModel: PageModel,
    totalPages: Int,
    pageWidthDp: androidx.compose.ui.unit.Dp,
    pageHeightDp: androidx.compose.ui.unit.Dp,
    coordinateScale: Float,
    toolbarMode: ToolbarMode,
    selectedTextBlockId: String?,
    selectedImageId: String?,
    selectedTableId: String?,
    selectedTableCell: Pair<Int, Int>?,
    drawToolState: DrawToolState,
    includePageNumbers: Boolean,
    onSelectPage: () -> Unit,
    onSelectTextBlock: (String) -> Unit,
    onUpdateText: (pageIndex: Int, textBlockId: String, newText: String) -> Unit,
    onSelectImage: (String) -> Unit,
    onSelectTable: (tableId: String, cell: Pair<Int, Int>) -> Unit,
    onUpdateTableCell: (tableId: String, row: Int, col: Int, text: String) -> Unit,
    onAddDrawingStroke: (pageIndex: Int, stroke: DrawingStroke) -> Unit,
    onClearSelection: () -> Unit
) {
    var pdfBitmap by remember(document.sourcePdfUri, pageModel.pdfPageIndex) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(document.sourcePdfUri, pageModel.pdfPageIndex) {
        if (document.mode == DocumentMode.PDF_VIEWER_ANNOTATOR && document.sourcePdfUri != null && pageModel.pdfPageIndex != null) {
            try {
                val uri = Uri.parse(document.sourcePdfUri)
                pdfBitmap = PdfRendererHelper.renderPdfPage(context, uri, pageModel.pdfPageIndex)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Page Header indicator
        Row(
            modifier = Modifier
                .width(pageWidthDp)
                .padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ) {
                Text(
                    text = "Page ${pageIndex + 1} of $totalPages",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // The A4 Paper Sheet
        Box(
            modifier = Modifier
                .size(width = pageWidthDp, height = pageHeightDp)
                .rotate(pageModel.rotationDegrees.toFloat())
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(2.dp), clip = true)
                .background(PaperWhite)
                .border(width = 0.8.dp, color = BorderLight, shape = RoundedCornerShape(2.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onSelectPage()
                    if (toolbarMode != ToolbarMode.DRAW) {
                        onClearSelection()
                    }
                }
                .testTag("a4_page_${pageIndex + 1}")
        ) {
            // 1. PDF Page background if imported
            if (pdfBitmap != null) {
                Image(
                    bitmap = pdfBitmap!!.asImageBitmap(),
                    contentDescription = "PDF Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }

            // 2. Shapes
            pageModel.shapes.forEach { shape ->
                RenderShape(shape = shape, coordinateScale = coordinateScale)
            }

            // 3. Tables
            pageModel.tables.forEach { table ->
                RenderTable(
                    table = table,
                    coordinateScale = coordinateScale,
                    isSelected = selectedTableId == table.id,
                    selectedCell = if (selectedTableId == table.id) selectedTableCell else null,
                    onSelectCell = { cell ->
                        onSelectPage()
                        onSelectTable(table.id, cell)
                    },
                    onUpdateCellText = { r, c, txt ->
                        onUpdateTableCell(table.id, r, c, txt)
                    }
                )
            }

            // 4. Images
            pageModel.images.forEach { img ->
                RenderImage(
                    img = img,
                    coordinateScale = coordinateScale,
                    isSelected = selectedImageId == img.id,
                    onClick = {
                        onSelectPage()
                        onSelectImage(img.id)
                    }
                )
            }

            // 5. Text Blocks
            pageModel.textBlocks.forEach { textBlock ->
                RenderTextBlock(
                    textBlock = textBlock,
                    pageIndex = pageIndex,
                    coordinateScale = coordinateScale,
                    isSelected = selectedTextBlockId == textBlock.id,
                    onFocus = {
                        onSelectPage()
                        onSelectTextBlock(textBlock.id)
                    },
                    onTextChange = { newText ->
                        onUpdateText(pageIndex, textBlock.id, newText)
                    }
                )
            }

            // 6. Drawing Layer
            DrawingCanvasLayer(
                pageIndex = pageIndex,
                strokes = pageModel.drawingStrokes,
                coordinateScale = coordinateScale,
                isDrawModeActive = toolbarMode == ToolbarMode.DRAW,
                drawToolState = drawToolState,
                onAddStroke = { stroke ->
                    onAddDrawingStroke(pageIndex, stroke)
                }
            )

            // 7. Page number footer if enabled
            if (includePageNumbers) {
                Text(
                    text = "${pageIndex + 1} / $totalPages",
                    fontSize = (10 * coordinateScale).sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = (16 * coordinateScale).dp)
                )
            }
        }
    }
}

@Composable
private fun RenderTextBlock(
    textBlock: TextBlock,
    pageIndex: Int,
    coordinateScale: Float,
    isSelected: Boolean,
    onFocus: () -> Unit,
    onTextChange: (String) -> Unit
) {
    val xDp = (textBlock.x * coordinateScale).dp
    val yDp = (textBlock.y * coordinateScale).dp
    val widthDp = (textBlock.width * coordinateScale).dp
    val minHeightDp = (textBlock.height * coordinateScale).dp

    val textStyle = TextStyle(
        color = parseColorSafely(textBlock.textColorHex, Color.Black),
        fontSize = (textBlock.fontSize * coordinateScale).sp,
        fontWeight = if (textBlock.isBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (textBlock.isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (textBlock.isUnderline) TextDecoration.Underline else TextDecoration.None,
        fontFamily = when (textBlock.fontFamily.lowercase()) {
            "serif" -> FontFamily.Serif
            "monospace" -> FontFamily.Monospace
            else -> FontFamily.SansSerif
        },
        textAlign = when (textBlock.alignment) {
            TextAlignment.CENTER -> TextAlign.Center
            TextAlignment.RIGHT -> TextAlign.Right
            TextAlignment.JUSTIFY -> TextAlign.Justify
            else -> TextAlign.Left
        },
        lineHeight = (textBlock.fontSize * textBlock.lineSpacingMultiplier * coordinateScale).sp
    )

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .offset(x = xDp, y = yDp)
            .width(widthDp)
            .heightIn(min = minHeightDp)
            .background(
                if (textBlock.highlightColorHex != null)
                    parseColorSafely(textBlock.highlightColorHex, Color.Yellow).copy(alpha = 0.35f)
                else Color.Transparent
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(2.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onFocus()
            }
            .padding(4.dp)
    ) {
        BasicTextField(
            value = textBlock.text,
            onValueChange = onTextChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrect = true
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onFocus()
                    }
                }
                .testTag("text_block_input_${pageIndex}_${textBlock.id}"),
            decorationBox = { innerTextField ->
                if (textBlock.text.isEmpty() && !isSelected) {
                    Text(
                        text = "Tap to write...",
                        style = textStyle.copy(color = Color.LightGray)
                    )
                }
                innerTextField()
            }
        )

        // Corner blue circular handles when selected matching mockup
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-8).dp, y = (-8).dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun RenderImage(
    img: ImageElement,
    coordinateScale: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val xDp = (img.x * coordinateScale).dp
    val yDp = (img.y * coordinateScale).dp
    val widthDp = (img.width * coordinateScale).dp
    val heightDp = (img.height * coordinateScale).dp

    Box(
        modifier = Modifier
            .offset(x = xDp, y = yDp)
            .size(width = widthDp, height = heightDp)
            .rotate(img.rotation)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = img.uriString,
            contentDescription = "Document Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun RenderTable(
    table: TableElement,
    coordinateScale: Float,
    isSelected: Boolean,
    selectedCell: Pair<Int, Int>?,
    onSelectCell: (Pair<Int, Int>) -> Unit,
    onUpdateCellText: (Int, Int, String) -> Unit
) {
    val xDp = (table.x * coordinateScale).dp
    val yDp = (table.y * coordinateScale).dp
    val widthDp = (table.width * coordinateScale).dp
    val cellHeightDp = (28f * coordinateScale).dp

    Column(
        modifier = Modifier
            .offset(x = xDp, y = yDp)
            .width(widthDp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray
            )
    ) {
        for (r in 0 until table.rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cellHeightDp)
            ) {
                for (c in 0 until table.cols) {
                    val isCellActive = selectedCell?.first == r && selectedCell?.second == c
                    val cellText = table.cells.getOrNull(r)?.getOrNull(c) ?: ""

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(width = 0.5.dp, color = Color.Gray)
                            .background(if (isCellActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                            .clickable { onSelectCell(Pair(r, c)) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = cellText,
                            onValueChange = { onUpdateCellText(r, c, it) },
                            textStyle = TextStyle(
                                fontSize = (11 * coordinateScale).sp,
                                color = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderShape(
    shape: ShapeElement,
    coordinateScale: Float
) {
    val xDp = (shape.x * coordinateScale).dp
    val yDp = (shape.y * coordinateScale).dp
    val widthDp = (shape.width * coordinateScale).dp
    val heightDp = (shape.height * coordinateScale).dp
    val color = parseColorSafely(shape.colorHex, Color.Black)
    val strokeWidthDp = (shape.strokeWidth * coordinateScale).dp

    when (shape.type) {
        ShapeType.LINE -> {
            Box(
                modifier = Modifier
                    .offset(x = xDp, y = yDp)
                    .width(widthDp)
                    .height(strokeWidthDp)
                    .background(color)
            )
        }
        ShapeType.RECTANGLE -> {
            Box(
                modifier = Modifier
                    .offset(x = xDp, y = yDp)
                    .size(width = widthDp, height = heightDp)
                    .border(width = strokeWidthDp, color = color)
            )
        }
        ShapeType.CIRCLE -> {
            Box(
                modifier = Modifier
                    .offset(x = xDp, y = yDp)
                    .size(width = widthDp, height = heightDp)
                    .border(width = strokeWidthDp, color = color, shape = CircleShape)
            )
        }
    }
}

@Composable
private fun DrawingCanvasLayer(
    pageIndex: Int,
    strokes: List<DrawingStroke>,
    coordinateScale: Float,
    isDrawModeActive: Boolean,
    drawToolState: DrawToolState,
    onAddStroke: (DrawingStroke) -> Unit
) {
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isDrawModeActive) {
                    Modifier.pointerInput(drawToolState) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                currentPoints = listOf(startOffset)
                            },
                            onDrag = { change, _ ->
                                currentPoints = currentPoints + change.position
                            },
                            onDragEnd = {
                                if (currentPoints.size > 1 && coordinateScale > 0) {
                                    val pts = currentPoints.map {
                                        PointF(it.x / coordinateScale, it.y / coordinateScale)
                                    }
                                    val newStroke = DrawingStroke(
                                        points = pts,
                                        colorHex = drawToolState.colorHex,
                                        strokeWidth = drawToolState.strokeWidth,
                                        isHighlighter = drawToolState.type == DrawToolType.HIGHLIGHTER
                                    )
                                    onAddStroke(newStroke)
                                }
                                currentPoints = emptyList()
                            },
                            onDragCancel = {
                                currentPoints = emptyList()
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        // Draw committed strokes
        strokes.forEach { stroke ->
            if (stroke.points.size > 1) {
                val path = Path()
                val start = stroke.points.first()
                path.moveTo(start.x * coordinateScale, start.y * coordinateScale)
                for (i in 1 until stroke.points.size) {
                    val p = stroke.points[i]
                    path.lineTo(p.x * coordinateScale, p.y * coordinateScale)
                }
                drawPath(
                    path = path,
                    color = parseColorSafely(stroke.colorHex, Color.Black).copy(
                        alpha = if (stroke.isHighlighter) 0.35f else 1.0f
                    ),
                    style = Stroke(
                        width = stroke.strokeWidth * coordinateScale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // Draw current in-progress stroke
        if (currentPoints.size > 1) {
            val livePath = Path()
            livePath.moveTo(currentPoints.first().x, currentPoints.first().y)
            for (i in 1 until currentPoints.size) {
                livePath.lineTo(currentPoints[i].x, currentPoints[i].y)
            }
            drawPath(
                path = livePath,
                color = parseColorSafely(drawToolState.colorHex, Color.Black).copy(
                    alpha = if (drawToolState.type == DrawToolType.HIGHLIGHTER) 0.35f else 1.0f
                ),
                style = Stroke(
                    width = drawToolState.strokeWidth * coordinateScale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

private fun parseColorSafely(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}
