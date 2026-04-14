package com.webtoapp.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Shared Gson singleton for standard JSON serialization.
 *
 * Build a dedicated Gson instance when a feature needs custom settings.
 */
object GsonProvider {
    val gson: Gson by lazy {
        GsonBuilder()
            .enableComplexMapKeySerialization()
            .create()
    }
}
