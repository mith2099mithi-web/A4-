package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.DocumentDao
import com.example.data.local.DocumentEntity
import com.example.data.local.DocumentJsonAdapter
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class DocumentRepository(
    private val documentDao: DocumentDao
) {
    val allDocuments: Flow<List<DocumentSummary>> = documentDao.getAllDocuments().map { list ->
        list.map { it.toSummary() }
    }

    fun searchDocuments(query: String): Flow<List<DocumentSummary>> {
        return documentDao.searchDocuments(query).map { list ->
            list.map { it.toSummary() }
        }
    }

    suspend fun getDocument(id: String): DocumentModel? = withContext(Dispatchers.IO) {
        val entity = documentDao.getDocumentById(id) ?: return@withContext null
        try {
            DocumentJsonAdapter.fromJson(entity.jsonContent)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveDocument(doc: DocumentModel): Unit = withContext(Dispatchers.IO) {
        val json = DocumentJsonAdapter.toJson(doc)
        val snippet = doc.pages.firstOrNull()?.textBlocks?.firstOrNull()?.text?.take(150)?.trim() ?: ""
        val entity = DocumentEntity(
            id = doc.id,
            title = doc.title.ifBlank { "Untitled Document" },
            isPdf = doc.mode == DocumentMode.PDF_VIEWER_ANNOTATOR,
            sourcePdfUri = doc.sourcePdfUri,
            pageCount = doc.pages.size,
            previewSnippet = snippet,
            createdAt = doc.createdAt,
            updatedAt = System.currentTimeMillis(),
            jsonContent = json
        )
        documentDao.insertOrUpdate(entity)
    }

    suspend fun updateTitle(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        documentDao.updateTitle(id, newTitle)
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        documentDao.deleteById(id)
    }

    suspend fun ensureSampleDocument() = withContext(Dispatchers.IO) {
        val count = documentDao.getDocumentCount()
        if (count == 0) {
            val sampleDoc = DocumentModel(
                id = UUID.randomUUID().toString(),
                title = "My Document",
                mode = DocumentMode.DOC_EDITOR,
                pages = listOf(
                    PageModel(
                        id = UUID.randomUUID().toString(),
                        pageNumber = 1,
                        textBlocks = listOf(
                            TextBlock(
                                text = "A4 Document\n\nThis is a simple A4 document.\n\nYou can write anything you want. Add pages,\nimages, tables and export it as PDF.\n\nFeatures\n• Easy writing\n• Add pages\n• Insert images\n• Simple tables\n• Export as A4 PDF",
                                x = 40f,
                                y = 45f,
                                width = A4_WIDTH - 80f,
                                height = 310f,
                                fontSize = 15f,
                                lineSpacingMultiplier = 1.35f
                            ),
                            TextBlock(
                                text = "Hello World",
                                x = 40f,
                                y = 370f,
                                width = 200f,
                                height = 45f,
                                fontSize = 18f,
                                isBold = true
                            )
                        )
                    ),
                    PageModel(
                        id = UUID.randomUUID().toString(),
                        pageNumber = 2,
                        textBlocks = listOf(
                            TextBlock(
                                text = "Page 2 Notes\n\nAdd shapes, tables, or annotate with stylus pen.",
                                x = 40f,
                                y = 50f,
                                width = A4_WIDTH - 80f,
                                height = 150f,
                                fontSize = 14f
                            )
                        )
                    ),
                    PageModel(
                        id = UUID.randomUUID().toString(),
                        pageNumber = 3,
                        textBlocks = listOf(
                            TextBlock(
                                text = "Page 3 Summary\n\nReady to export as a clean A4 PDF file.",
                                x = 40f,
                                y = 50f,
                                width = A4_WIDTH - 80f,
                                height = 150f,
                                fontSize = 14f
                            )
                        )
                    )
                )
            )
            saveDocument(sampleDoc)
        }
    }

    suspend fun createNewDocument(title: String = "Untitled Document"): DocumentModel {
        val newDoc = DocumentModel(
            id = UUID.randomUUID().toString(),
            title = title,
            mode = DocumentMode.DOC_EDITOR,
            pages = listOf(
                PageModel(
                    pageNumber = 1,
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
            )
        )
        saveDocument(newDoc)
        return newDoc
    }

    suspend fun createPdfAnnotatorDocument(title: String, pdfUri: String, pageCount: Int): DocumentModel {
        val pages = (1..pageCount).map { pageNum ->
            PageModel(
                id = UUID.randomUUID().toString(),
                pageNumber = pageNum,
                pdfPageIndex = pageNum - 1,
                textBlocks = emptyList()
            )
        }
        val doc = DocumentModel(
            id = UUID.randomUUID().toString(),
            title = title,
            mode = DocumentMode.PDF_VIEWER_ANNOTATOR,
            sourcePdfUri = pdfUri,
            pages = pages
        )
        saveDocument(doc)
        return doc
    }

    companion object {
        @Volatile
        private var INSTANCE: DocumentRepository? = null

        fun getInstance(context: Context): DocumentRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val repo = DocumentRepository(db.documentDao())
                INSTANCE = repo
                repo
            }
        }
    }
}

data class DocumentSummary(
    val id: String,
    val title: String,
    val isPdf: Boolean,
    val sourcePdfUri: String?,
    val pageCount: Int,
    val previewSnippet: String,
    val createdAt: Long,
    val updatedAt: Long
)

private fun DocumentEntity.toSummary() = DocumentSummary(
    id = id,
    title = title,
    isPdf = isPdf,
    sourcePdfUri = sourcePdfUri,
    pageCount = pageCount,
    previewSnippet = previewSnippet,
    createdAt = createdAt,
    updatedAt = updatedAt
)
