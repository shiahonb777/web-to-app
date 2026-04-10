package com.webtoapp.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * 全局 Gson 单例提供者
 * 适用于无需特殊配置的 JSON 序列化/反序列化场景
 * 
 * 注意：需要自定义配置（如 lenient、enum deserializer）的场景
 * 应自行构建 Gson 实例（如 ExtensionManager）
 */
object GsonProvider {
    val gson: Gson by lazy {
        GsonBuilder()
            .enableComplexMapKeySerialization()
            .create()
    }
}
