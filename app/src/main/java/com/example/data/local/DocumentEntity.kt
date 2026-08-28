package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isPdf: Boolean,
    val sourcePdfUri: String?,
    val pageCount: Int,
    val previewSnippet: String,
    val createdAt: Long,
    val updatedAt: Long,
    val jsonContent: String
)
