package com.example.ui.home

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DocumentModel
import com.example.data.repository.DocumentRepository
import com.example.data.repository.DocumentSummary
import com.example.pdf.PdfRendererHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository.getInstance(application)

    init {
        viewModelScope.launch {
            repository.ensureSampleDocument()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val documents: StateFlow<List<DocumentSummary>> = _searchQuery
        .debounce(200)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allDocuments
            } else {
                repository.searchDocuments(query.trim())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun createNewDocument(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newDoc = repository.createNewDocument("Untitled Document")
            onCreated(newDoc.id)
        }
    }

    fun openPdfUri(context: Context, uri: Uri, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val pageCount = PdfRendererHelper.getPdfPageCount(context, uri).coerceAtLeast(1)
            val title = getFileName(context, uri) ?: "Imported PDF"
            val doc = repository.createPdfAnnotatorDocument(title, uri.toString(), pageCount)
            onCreated(doc.id)
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            repository.deleteDocument(id)
        }
    }

    fun renameDocument(id: String, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) {
                repository.updateTitle(id, newTitle.trim())
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result?.removeSuffix(".pdf")
    }
}
