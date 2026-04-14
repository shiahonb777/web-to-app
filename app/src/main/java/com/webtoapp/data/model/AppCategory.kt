package com.webtoapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * App category entity.
 */
@Entity(tableName = "app_categories")
data class AppCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,                    // Category name
    val icon: String = "📁",             // Category icon (emoji)
    val color: String = "#6200EE",       // Category color (hex)
    val sortOrder: Int = 0,              // Sort order
    val createdAt: Long = System.currentTimeMillis()
)
