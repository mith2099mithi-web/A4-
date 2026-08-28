package com.example.data.model

import java.util.UUID

/**
 * Exact standard A4 dimensions in points/units (72 dpi standard: 595 x 842).
 * Width: 210mm -> 595.28 pt
 * Height: 297mm -> 841.89 pt
 */
const val A4_WIDTH = 595f
const val A4_HEIGHT = 842f
const val A4_ASPECT_RATIO = A4_WIDTH / A4_HEIGHT // approx 0.707

enum class DocumentMode {
    DOC_EDITOR,
    PDF_VIEWER_ANNOTATOR
}

data class DocumentModel(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Document",
    val mode: DocumentMode = DocumentMode.DOC_EDITOR,
    val sourcePdfUri: String? = null,
    val pages: List<PageModel> = listOf(PageModel()),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class PageModel(
    val id: String = UUID.randomUUID().toString(),
    val pageNumber: Int = 1,
    val rotationDegrees: Int = 0,
    val textBlocks: List<TextBlock> = listOf(TextBlock()),
    val images: List<ImageElement> = emptyList(),
    val tables: List<TableElement> = emptyList(),
    val shapes: List<ShapeElement> = emptyList(),
    val drawingStrokes: List<DrawingStroke> = emptyList(),
    val pdfPageIndex: Int? = null // For imported PDF pages
)

data class TextBlock(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val x: Float = 40f, // Margin 40 pt
    val y: Float = 50f,
    val width: Float = A4_WIDTH - 80f,
    val height: Float = A4_HEIGHT - 100f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val fontSize: Float = 14f,
    val fontFamily: String = "Inter",
    val textColorHex: String = "#111827",
    val highlightColorHex: String? = null,
    val alignment: TextAlignment = TextAlignment.LEFT,
    val lineSpacingMultiplier: Float = 1.3f
)

enum class TextAlignment {
    LEFT, CENTER, RIGHT, JUSTIFY
}

data class ImageElement(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val x: Float = 50f,
    val y: Float = 100f,
    val width: Float = 200f,
    val height: Float = 150f,
    val rotation: Float = 0f
)

data class TableElement(
    val id: String = UUID.randomUUID().toString(),
    val x: Float = 40f,
    val y: Float = 150f,
    val width: Float = A4_WIDTH - 80f,
    val rows: Int = 3,
    val cols: Int = 3,
    val cells: List<List<String>> = List(3) { List(3) { "" } },
    val cellAlignments: List<List<TextAlignment>> = List(3) { List(3) { TextAlignment.LEFT } }
)

enum class ShapeType {
    LINE, RECTANGLE, CIRCLE
}

data class ShapeElement(
    val id: String = UUID.randomUUID().toString(),
    val type: ShapeType = ShapeType.LINE,
    val x: Float = 40f,
    val y: Float = 100f,
    val width: Float = A4_WIDTH - 80f,
    val height: Float = 2f,
    val strokeWidth: Float = 2f,
    val colorHex: String = "#111827"
)

data class DrawingStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<PointF>,
    val colorHex: String = "#111827",
    val strokeWidth: Float = 3f,
    val isHighlighter: Boolean = false
)

data class PointF(
    val x: Float,
    val y: Float
)
