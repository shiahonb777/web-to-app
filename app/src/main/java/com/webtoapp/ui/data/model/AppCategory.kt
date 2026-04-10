package com.webtoapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 应用分类实体类
 */
@Entity(tableName = "app_categories")
data class AppCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,                    // 分类名称
    val icon: String = "📁",             // 分类图标（emoji）
    val color: String = "#6200EE",       // 分类颜色（十六进制）
    val sortOrder: Int = 0,              // Sort顺序
    val createdAt: Long = System.currentTimeMillis()
)
