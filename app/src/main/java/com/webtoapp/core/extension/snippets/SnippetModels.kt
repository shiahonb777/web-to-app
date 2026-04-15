package com.webtoapp.core.extension.snippets

/**
 * 代码块分类
 */
data class CodeSnippetCategory(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val snippets: List<CodeSnippet>
)

/**
 * 代码块
 */
data class CodeSnippet(
    val id: String,
    val name: String,
    val description: String,
    val code: String,
    val tags: List<String> = emptyList()
)
