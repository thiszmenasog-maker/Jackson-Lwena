package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val type: String, // "JOBS", "RESULTS", "SELECTIONS", "LOANS", "ANNOUNCEMENT"
    val url: String, // Link that opening the notification navigates to
    val authority: String, // "NECTA", "TCU", "TAMISEMI", "HESLB", "NACTE", "RITA", "AJIRAPORTAL", "ELIMUPDF"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
