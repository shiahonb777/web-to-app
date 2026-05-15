package com.webtoapp.core.ai.v2.tool

import com.google.gson.JsonArray
import com.google.gson.JsonObject

internal class SchemaBuilder {
    private val properties = JsonObject()
    private val required = mutableListOf<String>()
    fun property(name: String, type: String, desc: String, required: Boolean = false) { properties.add(name, JsonObject().apply { addProperty("type",type);addProperty("description",desc) }); if(required) this.required.add(name) }
    fun enumProperty(name: String, values: List<String>, desc: String, required: Boolean = false) { properties.add(name, JsonObject().apply { addProperty("type","string");addProperty("description",desc);add("enum",JsonArray().apply{values.forEach{add(it)}}) }); if(required) this.required.add(name) }
    fun booleanProperty(name: String, desc: String, required: Boolean = false) { properties.add(name, JsonObject().apply { addProperty("type","boolean");addProperty("description",desc) }); if(required) this.required.add(name) }
    fun build(): JsonObject = JsonObject().apply { addProperty("type","object");add("properties",properties);add("required",JsonArray().apply{this@SchemaBuilder.required.forEach{add(it)}}) }
}
internal fun schema(init: SchemaBuilder.() -> Unit): JsonObject = SchemaBuilder().apply(init).build()
