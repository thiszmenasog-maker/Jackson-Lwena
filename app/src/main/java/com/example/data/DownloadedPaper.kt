package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_papers")
data class DownloadedPaper(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String,
    val level: String, // e.g., "Ordinary Level", "Advanced Level", "Primary School"
    val year: String, // e.g., "2023", "2022"
    val fileUrl: String,
    val localFilePath: String, // Relative/absolute inside context.filesDir
    val fileSize: String, // e.g., "1.2 MB"
    val downloadTime: Long = System.currentTimeMillis()
)
