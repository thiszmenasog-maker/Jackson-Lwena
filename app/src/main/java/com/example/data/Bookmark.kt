package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val category: String, // NECTA, Jobs, Selections, Pastpapers, Other
    val isCustom: Boolean = false, // True if added by user, false if it's a built-in pre-suggested item bookmark
    val timestamp: Long = System.currentTimeMillis()
)
