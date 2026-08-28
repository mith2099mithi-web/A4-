package com.example.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.*
import com.example.ui.theme.DocumentFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PdfExporter {

    suspend fun exportToStream(
        context: Context,
        document: DocumentModel,
        outputStream: OutputStream,
        includePageNumbers: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val pdfDoc = PdfDocument()

        var sourcePdfRenderer: PdfRenderer? = null
        var sourcePfd: ParcelFileDescriptor? = null

        if (document.mode == DocumentMode.PDF_VIEWER_ANNOTATOR && document.sourcePdfUri != null) {
            try {
                val uri = Uri.parse(document.sourcePdfUri)
                sourcePfd = if (uri.scheme == "content") {
                    context.contentResolver.openFileDescriptor(uri, "r")
                } else {
                    ParcelFileDescriptor.open(File(uri.path ?: ""), ParcelFileDescriptor.MODE_READ_ONLY)
                }
                if (sourcePfd != null) {
                    sourcePdfRenderer = PdfRenderer(sourcePfd)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            val totalPages = document.pages.size

            document.pages.forEachIndexed { index, pageModel ->
                val pageInfo = PdfDocument.PageInfo.Builder(
                    A4_WIDTH.toInt(),
                    A4_HEIGHT.toInt(),
                    index + 1
                ).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                // 1. Draw Page Background
                val bgPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, A4_WIDTH, A4_HEIGHT, bgPaint)

                // If imported PDF page, render PDF background
                if (sourcePdfRenderer != null && pageModel.pdfPageIndex != null) {
                    val pdfIndex = pageModel.pdfPageIndex
                    if (pdfIndex >= 0 && pdfIndex < sourcePdfRenderer.pageCount) {
                        try {
                            val pdfPage = sourcePdfRenderer.openPage(pdfIndex)
                            val pageBmp = Bitmap.createBitmap(
                                A4_WIDTH.toInt(),
                                A4_HEIGHT.toInt(),
                                Bitmap.Config.ARGB_8888
                            )
                            pdfPage.render(pageBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            canvas.drawBitmap(pageBmp, 0f, 0f, null)
                            pdfPage.close()
                            pageBmp.recycle()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // 2. Draw Shapes
                pageModel.shapes.forEach { shape ->
                    drawShape(canvas, shape)
                }

                // 3. Draw Tables
                pageModel.tables.forEach { table ->
                    drawTable(context, canvas, table)
                }

                // 4. Draw Images
                pageModel.images.forEach { img ->
                    drawImage(context, canvas, img)
                }

                // 5. Draw Text Blocks
                pageModel.textBlocks.forEach { textBlock ->
                    drawTextBlock(context, canvas, textBlock)
                }

                // 6. Draw Drawing Strokes (Highlighter first, then Pen)
                val highlighters = pageModel.drawingStrokes.filter { it.isHighlighter }
                val pens = pageModel.drawingStrokes.filter { !it.isHighlighter }

                highlighters.forEach { stroke ->
                    drawStroke(canvas, stroke)
                }
                pens.forEach { stroke ->
                    drawStroke(canvas, stroke)
                }

                // 7. Draw Page Number if requested
                if (includePageNumbers) {
                    val pageNumberPaint = Paint().apply {
                        color = Color.GRAY
                        textSize = 10f
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(
                        "${index + 1} / $totalPages",
                        A4_WIDTH / 2f,
                        A4_HEIGHT - 20f,
                        pageNumberPaint
                    )
                }

                pdfDoc.finishPage(page)
            }

            pdfDoc.writeTo(outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                pdfDoc.close()
                sourcePdfRenderer?.close()
                sourcePfd?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun drawTextBlock(context: Context, canvas: Canvas, tb: TextBlock) {
        if (tb.text.isBlank()) return

        val textPaint = TextPaint().apply {
            color = parseColorSafely(tb.textColorHex, Color.BLACK)
            textSize = tb.fontSize
            isAntiAlias = true
            isUnderlineText = tb.isUnderline

            // Load the exact bundled TTF so exports match the editor canvas
            typeface = DocumentFonts.typeface(context, tb.fontFamily, tb.isBold, tb.isItalic)
        }

        // Draw highlight background if any
        if (tb.highlightColorHex != null) {
            val hlPaint = Paint().apply {
                color = parseColorSafely(tb.highlightColorHex, Color.YELLOW)
                alpha = 80
                style = Paint.Style.FILL
            }
            canvas.drawRect(tb.x, tb.y, tb.x + tb.width, tb.y + tb.height, hlPaint)
        }

        val alignment = when (tb.alignment) {
            TextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }

        val width = tb.width.toInt().coerceAtLeast(50)
        val staticLayout = StaticLayout.Builder.obtain(tb.text, 0, tb.text.length, textPaint, width)
            .setAlignment(alignment)
            .setLineSpacing(0f, tb.lineSpacingMultiplier)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(tb.x, tb.y)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawImage(context: Context, canvas: Canvas, img: ImageElement) {
        if (img.uriString.isBlank()) return
        try {
            val uri = Uri.parse(img.uriString)
            val stream = if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)
            } else {
                File(uri.path ?: "").inputStream()
            }
            stream?.use {
                val bitmap = BitmapFactory.decodeStream(it)
                if (bitmap != null) {
                    canvas.save()
                    val destRect = RectF(img.x, img.y, img.x + img.width, img.y + img.height)
                    if (img.rotation != 0f) {
                        canvas.rotate(img.rotation, destRect.centerX(), destRect.centerY())
                    }
                    canvas.drawBitmap(bitmap, null, destRect, null)
                    canvas.restore()
                    bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drawTable(context: Context, canvas: Canvas, table: TableElement) {
        val borderPaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 11f
            isAntiAlias = true
            typeface = DocumentFonts.typeface(context, DocumentFonts.Inter.id, false, false)
        }

        val cellWidth = table.width / table.cols.toFloat()
        val cellHeight = 24f // Standard row height

        val tableHeight = cellHeight * table.rows.toFloat()

        // Outer border
        canvas.drawRect(table.x, table.y, table.x + table.width, table.y + tableHeight, borderPaint)

        // Internal lines & text
        for (r in 0 until table.rows) {
            val cellY = table.y + r * cellHeight
            // Horizontal divider
            if (r > 0) {
                canvas.drawLine(table.x, cellY, table.x + table.width, cellY, borderPaint)
            }

            for (c in 0 until table.cols) {
                val cellX = table.x + c * cellWidth
                // Vertical divider
                if (c > 0) {
                    canvas.drawLine(cellX, table.y, cellX, table.y + tableHeight, borderPaint)
                }

                val text = table.cells.getOrNull(r)?.getOrNull(c) ?: ""
                if (text.isNotBlank()) {
                    val align = table.cellAlignments.getOrNull(r)?.getOrNull(c) ?: TextAlignment.LEFT
                    val textX = when (align) {
                        TextAlignment.CENTER -> cellX + cellWidth / 2f
                        TextAlignment.RIGHT -> cellX + cellWidth - 6f
                        else -> cellX + 6f
                    }
                    val textY = cellY + 16f
                    val oldAlign = textPaint.textAlign
                    textPaint.textAlign = when (align) {
                        TextAlignment.CENTER -> Paint.Align.CENTER
                        TextAlignment.RIGHT -> Paint.Align.RIGHT
                        else -> Paint.Align.LEFT
                    }
                    canvas.drawText(text, textX, textY, textPaint)
                    textPaint.textAlign = oldAlign
                }
            }
        }
    }

    private fun drawShape(canvas: Canvas, shape: ShapeElement) {
        val paint = Paint().apply {
            color = parseColorSafely(shape.colorHex, Color.BLACK)
            strokeWidth = shape.strokeWidth
            style = if (shape.type == ShapeType.LINE) Paint.Style.STROKE else Paint.Style.STROKE
            isAntiAlias = true
        }

        when (shape.type) {
            ShapeType.LINE -> {
                canvas.drawLine(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height, paint)
            }
            ShapeType.RECTANGLE -> {
                canvas.drawRect(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height, paint)
            }
            ShapeType.CIRCLE -> {
                val radius = (shape.width.coerceAtMost(shape.height)) / 2f
                canvas.drawCircle(shape.x + radius, shape.y + radius, radius, paint)
            }
        }
    }

    private fun drawStroke(canvas: Canvas, stroke: DrawingStroke) {
        if (stroke.points.size < 2) return

        val paint = Paint().apply {
            color = parseColorSafely(stroke.colorHex, Color.BLACK)
            strokeWidth = stroke.strokeWidth
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            if (stroke.isHighlighter) {
                alpha = 90
            }
        }

        val path = Path()
        path.moveTo(stroke.points[0].x, stroke.points[0].y)
        for (i in 1 until stroke.points.size) {
            val p = stroke.points[i]
            path.lineTo(p.x, p.y)
        }
        canvas.drawPath(path, paint)
    }

    private fun parseColorSafely(hex: String?, fallback: Int): Int {
        if (hex.isNullOrBlank()) return fallback
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            fallback
        }
    }
}
