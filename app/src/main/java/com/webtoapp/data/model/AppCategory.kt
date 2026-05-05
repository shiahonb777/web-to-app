package com.webtoapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey




@Entity(tableName = "app_categories")
data class AppCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val icon: String = "📁",
    val color: String = "#6200EE",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
