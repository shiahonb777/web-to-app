package com.webtoapp.core.extension

import com.webtoapp.core.extension.snippets.SnippetRegistry

typealias CodeSnippetCategory = com.webtoapp.core.extension.snippets.CodeSnippetCategory
typealias CodeSnippet = com.webtoapp.core.extension.snippets.CodeSnippet

object CodeSnippets {
    fun getAll(): List<CodeSnippetCategory> = SnippetRegistry.getAll()

    fun getByCategory(categoryId: String): CodeSnippetCategory? {
        return SnippetRegistry.getByCategory(categoryId)
    }

    fun search(query: String): List<CodeSnippet> {
        return SnippetRegistry.search(query)
    }

    fun getPopular(): List<CodeSnippet> = SnippetRegistry.getPopular()
}
