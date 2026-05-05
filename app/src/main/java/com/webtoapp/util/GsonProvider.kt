package com.webtoapp.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder








object GsonProvider {
    val gson: Gson by lazy {
        GsonBuilder()
            .enableComplexMapKeySerialization()
            .create()
    }
}
