package com.example.data.local

import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object DocumentJsonAdapter {

    fun toJson(doc: DocumentModel): String {
        val root = JSONObject()
        root.put("id", doc.id)
        root.put("title", doc.title)
        root.put("mode", doc.mode.name)
        root.put("sourcePdfUri", doc.sourcePdfUri ?: "")
        root.put("createdAt", doc.createdAt)
        root.put("updatedAt", doc.updatedAt)

        val pagesArr = JSONArray()
        for (page in doc.pages) {
            val pageObj = JSONObject()
            pageObj.put("id", page.id)
            pageObj.put("pageNumber", page.pageNumber)
            pageObj.put("rotationDegrees", page.rotationDegrees)
            if (page.pdfPageIndex != null) {
                pageObj.put("pdfPageIndex", page.pdfPageIndex)
            }

            // Text blocks
            val tbArr = JSONArray()
            for (tb in page.textBlocks) {
                val tbObj = JSONObject()
                tbObj.put("id", tb.id)
                tbObj.put("text", tb.text)
                tbObj.put("x", tb.x.toDouble())
                tbObj.put("y", tb.y.toDouble())
                tbObj.put("width", tb.width.toDouble())
                tbObj.put("height", tb.height.toDouble())
                tbObj.put("isBold", tb.isBold)
                tbObj.put("isItalic", tb.isItalic)
                tbObj.put("isUnderline", tb.isUnderline)
                tbObj.put("fontSize", tb.fontSize.toDouble())
                tbObj.put("fontFamily", tb.fontFamily)
                tbObj.put("textColorHex", tb.textColorHex)
                if (tb.highlightColorHex != null) {
                    tbObj.put("highlightColorHex", tb.highlightColorHex)
                }
                tbObj.put("alignment", tb.alignment.name)
                tbObj.put("lineSpacingMultiplier", tb.lineSpacingMultiplier.toDouble())
                tbArr.put(tbObj)
            }
            pageObj.put("textBlocks", tbArr)

            // Images
            val imgArr = JSONArray()
            for (img in page.images) {
                val imgObj = JSONObject()
                imgObj.put("id", img.id)
                imgObj.put("uriString", img.uriString)
                imgObj.put("x", img.x.toDouble())
                imgObj.put("y", img.y.toDouble())
                imgObj.put("width", img.width.toDouble())
                imgObj.put("height", img.height.toDouble())
                imgObj.put("rotation", img.rotation.toDouble())
                imgArr.put(imgObj)
            }
            pageObj.put("images", imgArr)

            // Tables
            val tblArr = JSONArray()
            for (tbl in page.tables) {
                val tblObj = JSONObject()
                tblObj.put("id", tbl.id)
                tblObj.put("x", tbl.x.toDouble())
                tblObj.put("y", tbl.y.toDouble())
                tblObj.put("width", tbl.width.toDouble())
                tblObj.put("rows", tbl.rows)
                tblObj.put("cols", tbl.cols)

                val cellsArr = JSONArray()
                for (row in tbl.cells) {
                    val rArr = JSONArray()
                    for (cell in row) {
                        rArr.put(cell)
                    }
                    cellsArr.put(rArr)
                }
                tblObj.put("cells", cellsArr)

                val alignArr = JSONArray()
                for (row in tbl.cellAlignments) {
                    val rArr = JSONArray()
                    for (a in row) {
                        rArr.put(a.name)
                    }
                    alignArr.put(rArr)
                }
                tblObj.put("cellAlignments", alignArr)
                tblArr.put(tblObj)
            }
            pageObj.put("tables", tblArr)

            // Shapes
            val shapeArr = JSONArray()
            for (shape in page.shapes) {
                val sObj = JSONObject()
                sObj.put("id", shape.id)
                sObj.put("type", shape.type.name)
                sObj.put("x", shape.x.toDouble())
                sObj.put("y", shape.y.toDouble())
                sObj.put("width", shape.width.toDouble())
                sObj.put("height", shape.height.toDouble())
                sObj.put("strokeWidth", shape.strokeWidth.toDouble())
                sObj.put("colorHex", shape.colorHex)
                shapeArr.put(sObj)
            }
            pageObj.put("shapes", shapeArr)

            // Drawing strokes
            val strokeArr = JSONArray()
            for (stroke in page.drawingStrokes) {
                val strkObj = JSONObject()
                strkObj.put("id", stroke.id)
                strkObj.put("colorHex", stroke.colorHex)
                strkObj.put("strokeWidth", stroke.strokeWidth.toDouble())
                strkObj.put("isHighlighter", stroke.isHighlighter)

                val ptsArr = JSONArray()
                for (pt in stroke.points) {
                    val ptObj = JSONObject()
                    ptObj.put("x", pt.x.toDouble())
                    ptObj.put("y", pt.y.toDouble())
                    ptsArr.put(ptObj)
                }
                strkObj.put("points", ptsArr)
                strokeArr.put(strkObj)
            }
            pageObj.put("drawingStrokes", strokeArr)

            pagesArr.put(pageObj)
        }
        root.put("pages", pagesArr)

        return root.toString()
    }

    fun fromJson(jsonStr: String): DocumentModel {
        val root = JSONObject(jsonStr)
        val id = root.optString("id", UUID.randomUUID().toString())
        val title = root.optString("title", "Untitled Document")
        val modeStr = root.optString("mode", DocumentMode.DOC_EDITOR.name)
        val mode = try { DocumentMode.valueOf(modeStr) } catch (e: Exception) { DocumentMode.DOC_EDITOR }
        val sourcePdfUriRaw = root.optString("sourcePdfUri", "")
        val sourcePdfUri = if (sourcePdfUriRaw.isEmpty()) null else sourcePdfUriRaw
        val createdAt = root.optLong("createdAt", System.currentTimeMillis())
        val updatedAt = root.optLong("updatedAt", System.currentTimeMillis())

        val pages = mutableListOf<PageModel>()
        val pagesArr = root.optJSONArray("pages") ?: JSONArray()
        for (i in 0 until pagesArr.length()) {
            val pageObj = pagesArr.getJSONObject(i)
            val pId = pageObj.optString("id", UUID.randomUUID().toString())
            val pNum = pageObj.optInt("pageNumber", i + 1)
            val pRot = pageObj.optInt("rotationDegrees", 0)
            val pdfIndex = if (pageObj.has("pdfPageIndex")) pageObj.optInt("pdfPageIndex") else null

            // Text blocks
            val textBlocks = mutableListOf<TextBlock>()
            val tbArr = pageObj.optJSONArray("textBlocks") ?: JSONArray()
            for (j in 0 until tbArr.length()) {
                val tbObj = tbArr.getJSONObject(j)
                val tbAlignStr = tbObj.optString("alignment", TextAlignment.LEFT.name)
                val tbAlign = try { TextAlignment.valueOf(tbAlignStr) } catch (e: Exception) { TextAlignment.LEFT }

                textBlocks.add(
                    TextBlock(
                        id = tbObj.optString("id", UUID.randomUUID().toString()),
                        text = tbObj.optString("text", ""),
                        x = tbObj.optDouble("x", 40.0).toFloat(),
                        y = tbObj.optDouble("y", 50.0).toFloat(),
                        width = tbObj.optDouble("width", (A4_WIDTH - 80).toDouble()).toFloat(),
                        height = tbObj.optDouble("height", (A4_HEIGHT - 100).toDouble()).toFloat(),
                        isBold = tbObj.optBoolean("isBold", false),
                        isItalic = tbObj.optBoolean("isItalic", false),
                        isUnderline = tbObj.optBoolean("isUnderline", false),
                        fontSize = tbObj.optDouble("fontSize", 14.0).toFloat(),
                        fontFamily = tbObj.optString("fontFamily", "Inter"),
                        textColorHex = tbObj.optString("textColorHex", "#111827"),
                        highlightColorHex = if (tbObj.has("highlightColorHex")) tbObj.optString("highlightColorHex") else null,
                        alignment = tbAlign,
                        lineSpacingMultiplier = tbObj.optDouble("lineSpacingMultiplier", 1.3).toFloat()
                    )
                )
            }
            if (textBlocks.isEmpty()) {
                textBlocks.add(TextBlock())
            }

            // Images
            val images = mutableListOf<ImageElement>()
            val imgArr = pageObj.optJSONArray("images") ?: JSONArray()
            for (j in 0 until imgArr.length()) {
                val imgObj = imgArr.getJSONObject(j)
                images.add(
                    ImageElement(
                        id = imgObj.optString("id", UUID.randomUUID().toString()),
                        uriString = imgObj.optString("uriString", ""),
                        x = imgObj.optDouble("x", 50.0).toFloat(),
                        y = imgObj.optDouble("y", 100.0).toFloat(),
                        width = imgObj.optDouble("width", 200.0).toFloat(),
                        height = imgObj.optDouble("height", 150.0).toFloat(),
                        rotation = imgObj.optDouble("rotation", 0.0).toFloat()
                    )
                )
            }

            // Tables
            val tables = mutableListOf<TableElement>()
            val tblArr = pageObj.optJSONArray("tables") ?: JSONArray()
            for (j in 0 until tblArr.length()) {
                val tblObj = tblArr.getJSONObject(j)
                val rows = tblObj.optInt("rows", 3)
                val cols = tblObj.optInt("cols", 3)

                val cells = mutableListOf<List<String>>()
                val cellsArr = tblObj.optJSONArray("cells")
                if (cellsArr != null) {
                    for (r in 0 until cellsArr.length()) {
                        val rowArr = cellsArr.getJSONArray(r)
                        val rList = mutableListOf<String>()
                        for (c in 0 until rowArr.length()) {
                            rList.add(rowArr.optString(c, ""))
                        }
                        cells.add(rList)
                    }
                }
                val defaultCells = if (cells.isEmpty()) List(rows) { List(cols) { "" } } else cells

                val aligns = mutableListOf<List<TextAlignment>>()
                val alignArr = tblObj.optJSONArray("cellAlignments")
                if (alignArr != null) {
                    for (r in 0 until alignArr.length()) {
                        val rowArr = alignArr.getJSONArray(r)
                        val rList = mutableListOf<TextAlignment>()
                        for (c in 0 until rowArr.length()) {
                            val str = rowArr.optString(c, TextAlignment.LEFT.name)
                            rList.add(try { TextAlignment.valueOf(str) } catch (e: Exception) { TextAlignment.LEFT })
                        }
                        aligns.add(rList)
                    }
                }
                val defaultAligns = if (aligns.isEmpty()) List(rows) { List(cols) { TextAlignment.LEFT } } else aligns

                tables.add(
                    TableElement(
                        id = tblObj.optString("id", UUID.randomUUID().toString()),
                        x = tblObj.optDouble("x", 40.0).toFloat(),
                        y = tblObj.optDouble("y", 150.0).toFloat(),
                        width = tblObj.optDouble("width", (A4_WIDTH - 80).toDouble()).toFloat(),
                        rows = rows,
                        cols = cols,
                        cells = defaultCells,
                        cellAlignments = defaultAligns
                    )
                )
            }

            // Shapes
            val shapes = mutableListOf<ShapeElement>()
            val shapeArr = pageObj.optJSONArray("shapes") ?: JSONArray()
            for (j in 0 until shapeArr.length()) {
                val sObj = shapeArr.getJSONObject(j)
                val sTypeStr = sObj.optString("type", ShapeType.LINE.name)
                val sType = try { ShapeType.valueOf(sTypeStr) } catch (e: Exception) { ShapeType.LINE }
                shapes.add(
                    ShapeElement(
                        id = sObj.optString("id", UUID.randomUUID().toString()),
                        type = sType,
                        x = sObj.optDouble("x", 40.0).toFloat(),
                        y = sObj.optDouble("y", 100.0).toFloat(),
                        width = sObj.optDouble("width", (A4_WIDTH - 80).toDouble()).toFloat(),
                        height = sObj.optDouble("height", 2.0).toFloat(),
                        strokeWidth = sObj.optDouble("strokeWidth", 2.0).toFloat(),
                        colorHex = sObj.optString("colorHex", "#111827")
                    )
                )
            }

            // Drawing strokes
            val strokes = mutableListOf<DrawingStroke>()
            val strokeArr = pageObj.optJSONArray("drawingStrokes") ?: JSONArray()
            for (j in 0 until strokeArr.length()) {
                val strkObj = strokeArr.getJSONObject(j)
                val pts = mutableListOf<PointF>()
                val ptsArr = strkObj.optJSONArray("points") ?: JSONArray()
                for (p in 0 until ptsArr.length()) {
                    val pObj = ptsArr.getJSONObject(p)
                    pts.add(PointF(pObj.optDouble("x", 0.0).toFloat(), pObj.optDouble("y", 0.0).toFloat()))
                }
                strokes.add(
                    DrawingStroke(
                        id = strkObj.optString("id", UUID.randomUUID().toString()),
                        points = pts,
                        colorHex = strkObj.optString("colorHex", "#111827"),
                        strokeWidth = strkObj.optDouble("strokeWidth", 3.0).toFloat(),
                        isHighlighter = strkObj.optBoolean("isHighlighter", false)
                    )
                )
            }

            pages.add(
                PageModel(
                    id = pId,
                    pageNumber = pNum,
                    rotationDegrees = pRot,
                    textBlocks = textBlocks,
                    images = images,
                    tables = tables,
                    shapes = shapes,
                    drawingStrokes = strokes,
                    pdfPageIndex = pdfIndex
                )
            )
        }

        if (pages.isEmpty()) {
            pages.add(PageModel())
        }

        return DocumentModel(
            id = id,
            title = title,
            mode = mode,
            sourcePdfUri = sourcePdfUri,
            pages = pages,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
