package com.webtoapp.core.ai.v2.llm

import okio.BufferedSource

internal class SseParser {
    fun consume(source: BufferedSource, handler: (event: String?, data: String) -> Boolean) {
        var currentEvent: String? = null
        val dataBuffer = StringBuilder()
        fun flush(): Boolean {
            val data = dataBuffer.toString().trim('\n')
            dataBuffer.setLength(0)
            val ev = currentEvent; currentEvent = null
            if (data.isEmpty()) return true
            return handler(ev, data)
        }
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            when {
                line.startsWith("event:") -> currentEvent = line.removePrefix("event:").trim()
                line.startsWith("data:") -> { if (dataBuffer.isNotEmpty()) dataBuffer.append('\n'); dataBuffer.append(line.removePrefix("data:").trimStart()) }
                line.startsWith(":") -> Unit
                line.isBlank() -> if (!flush()) return
            }
        }
        if (dataBuffer.isNotEmpty()) flush()
    }
}
