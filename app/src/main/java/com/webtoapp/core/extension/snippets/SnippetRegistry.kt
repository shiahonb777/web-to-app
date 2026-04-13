package com.webtoapp.core.extension.snippets

object SnippetRegistry {
    fun getAll(): List<CodeSnippetCategory> = listOf(
        nativeBridgeOperations(),
        domOperations(),
        styleOperations(),
        eventListeners(),
        storageOperations(),
        networkOperations(),
        dataProcessing(),
        uiComponents(),
        floatingWidgets(),
        notifications(),
        scrollOperations(),
        formOperations(),
        mediaOperations(),
        pageEnhance(),
        contentFilter(),
        adBlocker(),
        utilityFunctions(),
        textProcessing(),
        interceptors(),
        automation(),
        debugging()
    )

    fun getByCategory(categoryId: String): CodeSnippetCategory? {
        return getAll().find { it.id == categoryId }
    }

    fun search(query: String): List<CodeSnippet> {
        val lowerQuery = query.lowercase()
        return getAll().flatMap { it.snippets }.filter { snippet ->
            snippet.name.lowercase().contains(lowerQuery) ||
                snippet.description.lowercase().contains(lowerQuery) ||
                snippet.tags.any { it.lowercase().contains(lowerQuery) }
        }
    }

    fun getPopular(): List<CodeSnippet> = listOf(
        getByCategory("native")?.snippets?.find { it.id == "native-save-image" },
        getByCategory("native")?.snippets?.find { it.id == "native-share" },
        getByCategory("dom")?.snippets?.find { it.id == "dom-hide-element" },
        getByCategory("style")?.snippets?.find { it.id == "style-inject-css" },
        getByCategory("ui")?.snippets?.find { it.id == "ui-floating-button" },
        getByCategory("scroll")?.snippets?.find { it.id == "scroll-to-top" },
        getByCategory("adblocker")?.snippets?.find { it.id == "ad-hide-common" },
        getByCategory("events")?.snippets?.find { it.id == "event-mutation" }
    ).filterNotNull()
}
