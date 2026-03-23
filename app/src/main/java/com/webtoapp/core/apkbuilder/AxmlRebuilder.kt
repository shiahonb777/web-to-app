package com.webtoapp.core.apkbuilder

import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AXML 重构器
 * 支持修改字符串池并重建整个 AXML 文件
 * 
 * 主要用途：将相对路径类名（如 .MainActivity）展开为绝对路径（如 com.pkg.MainActivity）
 * 这样修改包名后，组件类名仍然指向原包名下的类，避免 ClassNotFoundException
 */
class AxmlRebuilder {

    companion object {
        private const val TAG = "AxmlRebuilder"
        
        // AXML Chunk Types
        private const val CHUNK_AXML_FILE = 0x0003
        private const val CHUNK_STRING_POOL = 0x0001
        private const val CHUNK_RESOURCE_MAP = 0x0180
        private const val CHUNK_START_NAMESPACE = 0x0100
        private const val CHUNK_END_NAMESPACE = 0x0101
        private const val CHUNK_START_ELEMENT = 0x0102
        private const val CHUNK_END_ELEMENT = 0x0103
        private const val CHUNK_TEXT = 0x0104
        
        // Attribute resource IDs
        private const val ATTR_NAME = 0x01010003
        private const val ATTR_VERSION_CODE = 0x0101021b
        private const val ATTR_VERSION_NAME = 0x0101021c
        private const val ATTR_TEST_ONLY = 0x01010272
        private const val ATTR_LABEL = 0x01010001
        private const val ATTR_ICON = 0x01010002
        private const val ATTR_TARGET_ACTIVITY = 0x01010202
        private const val ATTR_EXPORTED = 0x01010010
        private const val ATTR_ENABLED = 0x0101000e
    }

    /**
     * 展开相对路径类名、修改包名，并添加 activity-alias（多桌面图标）
     * 
     * @param axmlData 原始 AXML 数据
     * @param originalPackage 原始包名
     * @param newPackage 新包名
     * @param aliasCount 要添加的 activity-alias 数量（0 表示不添加）
     * @param appName 应用名称（用于 alias 的 label）
     * @return 修改后的 AXML 数据
     */
    fun expandAndModifyWithAliases(
        axmlData: ByteArray,
        originalPackage: String,
        newPackage: String,
        aliasCount: Int = 0,
        appName: String = ""
    ): ByteArray {
        return try {
            val parsed = parseAxml(axmlData)
            if (parsed == null) {
                Log.e(TAG, "Failed to parse AXML for aliases")
                return axmlData
            }
            
            // 步骤1：找到需要展开的相对路径类名
            val expansions = findRelativeClassNames(parsed, originalPackage)
            Log.d(TAG, "Found ${expansions.size} relative class names to expand")
            
            // 步骤2：添加新字符串并更新引用
            if (expansions.isNotEmpty()) {
                expandClassNames(parsed, expansions)
            }
            
            // 步骤3：修改包名和所有包名前缀的字符串
            replacePackageString(parsed, originalPackage, newPackage)
            
            // 步骤4：添加 activity-alias（多桌面图标）
            if (aliasCount > 0) {
                addActivityAliases(parsed, newPackage, aliasCount, appName)
                Log.d(TAG, "Added $aliasCount activity-alias entries")
            }
            
            // 步骤5：重建 AXML
            val result = rebuildAxml(parsed)
            
            Log.d(TAG, "AXML rebuild with aliases complete: original=${axmlData.size}, new=${result.size}")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "AXML rebuild with aliases failed", e)
            axmlData
        }
    }
    
    /**
     * Ensure required uses-permission entries exist in manifest (add if missing)
     */
    private fun ensureUsesPermissions(parsed: ParsedAxml, permissions: List<String>) {
        val resourceMap = parsed.resourceMap ?: return
        val nameAttrIndex = resourceMap.indexOf(ATTR_NAME)
        if (nameAttrIndex < 0) {
            Log.w(TAG, "android:name attribute not found in resource map, cannot add permissions")
            return
        }
        
        val androidNsIndex = getOrAddString(parsed.stringPool, "http://schemas.android.com/apk/res/android")
        val usesPermissionNameIndex = getOrAddString(parsed.stringPool, "uses-permission")
        
        val missing = permissions.filterNot { hasUsesPermission(parsed, it, nameAttrIndex) }
        if (missing.isEmpty()) return
        
        val insertIndex = findApplicationStartIndex(parsed).let { if (it >= 0) it else parsed.chunks.size }
        val newChunks = mutableListOf<Chunk>()
        
        for (perm in missing) {
            val permValueIndex = getOrAddString(parsed.stringPool, perm)
            val start = buildActionOrCategoryElement(androidNsIndex, usesPermissionNameIndex, nameAttrIndex, permValueIndex)
            val end = buildEndElement(androidNsIndex, usesPermissionNameIndex)
            newChunks.add(start)
            newChunks.add(end)
            Log.d(TAG, "Injected uses-permission: $perm")
        }
        
        parsed.chunks.addAll(insertIndex, newChunks)
    }
    
    /**
     * Check if manifest already declares the given uses-permission
     */
    private fun hasUsesPermission(parsed: ParsedAxml, permission: String, nameAttrIndex: Int): Boolean {
        for (chunk in parsed.chunks) {
            if (chunk.type != CHUNK_START_ELEMENT) continue
            val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(16)
            buffer.int // namespaceUri
            val elementName = buffer.int
            buffer.short // attrStart
            val attrSize = buffer.short.toInt() and 0xFFFF
            val attrCount = buffer.short.toInt() and 0xFFFF
            
            if (attrSize == 0 || attrCount == 0) continue
            val elementNameStr = parsed.stringPool.strings.getOrNull(elementName) ?: continue
            if (elementNameStr != "uses-permission") continue
            
            for (i in 0 until attrCount) {
                val attrOffset = 36 + i * attrSize
                if (attrOffset + 20 > chunk.data.size) break
                buffer.position(attrOffset)
                buffer.int // attrNs
                val attrName = buffer.int
                buffer.int // attrRawValue
                buffer.short // valueSize
                buffer.get() // res0
                val attrValueType = buffer.get().toInt() and 0xFF
                val attrValueData = buffer.int
                
                if (attrName == nameAttrIndex && attrValueType == 0x03) { // TYPE_STRING
                    val valueStr = parsed.stringPool.strings.getOrNull(attrValueData)
                    if (valueStr == permission) {
                        return true
                    }
                }
            }
        }
        return false
    }
    
    /**
     * Find the index of <application> START_ELEMENT to insert permissions before it
     */
    private fun findApplicationStartIndex(parsed: ParsedAxml): Int {
        val applicationStrIndex = parsed.stringPool.strings.indexOf("application")
        if (applicationStrIndex < 0) return -1
        
        for (i in parsed.chunks.indices) {
            val chunk = parsed.chunks[i]
            if (chunk.type != CHUNK_START_ELEMENT) continue
            val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(16)
            buffer.int // namespaceUri
            val name = buffer.int
            if (name == applicationStrIndex) {
                return i
            }
        }
        return -1
    }
    
    /**
     * 添加 activity-alias 到 manifest
     * 每个 alias 都指向 ShellActivity，并带有 MAIN/LAUNCHER intent-filter
     * 
     * 关键：属性名必须使用 Resource Map 中已有的索引，或者同时更新 Resource Map
     */
    private fun addActivityAliases(
        parsed: ParsedAxml,
        packageName: String,
        aliasCount: Int,
        appName: String
    ) {
        // 找到 </application> 的位置（END_ELEMENT with name="application"）
        val applicationEndIndex = findApplicationEndIndex(parsed)
        if (applicationEndIndex < 0) {
            Log.e(TAG, "Cannot find </application> element")
            return
        }
        
        val resourceMap = parsed.resourceMap
        if (resourceMap == null) {
            Log.e(TAG, "No resource map found, cannot add activity-alias")
            return
        }
        
        // 从 Resource Map 中查找属性名索引（这些属性在原始 manifest 中应该已存在）
        val nameAttrIndex = resourceMap.indexOf(ATTR_NAME)
        val labelAttrIndex = resourceMap.indexOf(ATTR_LABEL)
        val iconAttrIndex = resourceMap.indexOf(ATTR_ICON)
        val exportedAttrIndex = resourceMap.indexOf(ATTR_EXPORTED)
        
        // targetActivity 可能不在原始 manifest 中，需要添加到 Resource Map
        var targetActivityAttrIndex = resourceMap.indexOf(ATTR_TARGET_ACTIVITY)
        if (targetActivityAttrIndex < 0) {
            // 关键：Resource Map 中的索引必须与字符串池索引对应
            // 新的 targetActivity 索引 = 当前 Resource Map 大小
            targetActivityAttrIndex = resourceMap.size
            
            // 在字符串池的 targetActivityAttrIndex 位置插入（而不是 add 到末尾）
            parsed.stringPool.strings.add(targetActivityAttrIndex, "targetActivity")
            
            // Update所有现有 chunk 中 >= targetActivityAttrIndex 的字符串索引
            updateStringIndicesAfterInsert(parsed, targetActivityAttrIndex)
            
            // 扩展 Resource Map
            val newResourceMap = resourceMap.copyOf(resourceMap.size + 1)
            newResourceMap[targetActivityAttrIndex] = ATTR_TARGET_ACTIVITY
            parsed.resourceMap = newResourceMap
            Log.d(TAG, "Added targetActivity to string pool and resource map at index $targetActivityAttrIndex")
        }
        
        if (nameAttrIndex < 0 || labelAttrIndex < 0 || iconAttrIndex < 0 || exportedAttrIndex < 0) {
            Log.e(TAG, "Missing required attribute indices: name=$nameAttrIndex, label=$labelAttrIndex, icon=$iconAttrIndex, exported=$exportedAttrIndex")
            return
        }
        
        Log.d(TAG, "Attribute indices: name=$nameAttrIndex, targetActivity=$targetActivityAttrIndex, label=$labelAttrIndex, icon=$iconAttrIndex, exported=$exportedAttrIndex")
        
        // 找到 android namespace 字符串索引
        val androidNsIndex = getOrAddString(parsed.stringPool, "http://schemas.android.com/apk/res/android")
        
        // 添加元素名字符串（元素名不需要 Resource Map 映射）
        val activityAliasNameIndex = getOrAddString(parsed.stringPool, "activity-alias")
        val intentFilterNameIndex = getOrAddString(parsed.stringPool, "intent-filter")
        val actionNameIndex = getOrAddString(parsed.stringPool, "action")
        val categoryNameIndex = getOrAddString(parsed.stringPool, "category")
        
        // 添加值字符串（值字符串也不需要 Resource Map 映射）
        val mainActionIndex = getOrAddString(parsed.stringPool, "android.intent.action.MAIN")
        val launcherCategoryIndex = getOrAddString(parsed.stringPool, "android.intent.category.LAUNCHER")
        
        // ShellActivity 的完整类名
        // 重要：必须使用原始类名，因为 DEX 文件中的类名没有被替换
        // manifest 中的组件类名也没有被替换（参见 Skipped component class 日志）
        val targetActivityValue = "com.webtoapp.ui.shell.ShellActivity"
        val targetActivityValueIndex = getOrAddString(parsed.stringPool, targetActivityValue)
        Log.d(TAG, "targetActivity value: $targetActivityValue (using original class name)")
        
        // 为每个 alias 创建 chunks
        val newChunks = mutableListOf<Chunk>()
        
        for (i in 1..aliasCount) {
            // Generate alias 名称，如 ".LauncherAlias1"
            val aliasName = ".LauncherAlias$i"
            val aliasNameValueIndex = getOrAddString(parsed.stringPool, aliasName)
            
            // Generate label
            val aliasLabel = appName
            val aliasLabelIndex = getOrAddString(parsed.stringPool, aliasLabel)
            
            // Build <activity-alias> START_ELEMENT
            val aliasStartChunk = buildActivityAliasStartElement(
                androidNsIndex = androidNsIndex,
                elementNameIndex = activityAliasNameIndex,
                nameAttrIndex = nameAttrIndex,
                nameValueIndex = aliasNameValueIndex,
                targetActivityAttrIndex = targetActivityAttrIndex,
                targetActivityValueIndex = targetActivityValueIndex,
                labelAttrIndex = labelAttrIndex,
                labelValueIndex = aliasLabelIndex,
                iconAttrIndex = iconAttrIndex,
                exportedAttrIndex = exportedAttrIndex
            )
            newChunks.add(aliasStartChunk)
            
            // Build <intent-filter> START_ELEMENT
            val intentFilterStart = buildSimpleStartElement(androidNsIndex, intentFilterNameIndex, 0)
            newChunks.add(intentFilterStart)
            
            // Build <action android:name="android.intent.action.MAIN" />
            val actionStart = buildActionOrCategoryElement(
                androidNsIndex = androidNsIndex,
                elementNameIndex = actionNameIndex,
                nameAttrIndex = nameAttrIndex,
                nameValueIndex = mainActionIndex
            )
            newChunks.add(actionStart)
            val actionEnd = buildEndElement(androidNsIndex, actionNameIndex)
            newChunks.add(actionEnd)
            
            // Build <category android:name="android.intent.category.LAUNCHER" />
            val categoryStart = buildActionOrCategoryElement(
                androidNsIndex = androidNsIndex,
                elementNameIndex = categoryNameIndex,
                nameAttrIndex = nameAttrIndex,
                nameValueIndex = launcherCategoryIndex
            )
            newChunks.add(categoryStart)
            val categoryEnd = buildEndElement(androidNsIndex, categoryNameIndex)
            newChunks.add(categoryEnd)
            
            // Build </intent-filter> END_ELEMENT
            val intentFilterEnd = buildEndElement(androidNsIndex, intentFilterNameIndex)
            newChunks.add(intentFilterEnd)
            
            // Build </activity-alias> END_ELEMENT
            val aliasEndChunk = buildEndElement(androidNsIndex, activityAliasNameIndex)
            newChunks.add(aliasEndChunk)
        }
        
        // 在 </application> 之前插入新的 chunks
        parsed.chunks.addAll(applicationEndIndex, newChunks)
        Log.d(TAG, "Inserted ${newChunks.size} chunks for $aliasCount activity-alias entries")
    }
    
    /**
     * 找到 </application> END_ELEMENT 的索引
     */
    private fun findApplicationEndIndex(parsed: ParsedAxml): Int {
        val applicationStrIndex = parsed.stringPool.strings.indexOf("application")
        if (applicationStrIndex < 0) return -1
        
        for (i in parsed.chunks.indices) {
            val chunk = parsed.chunks[i]
            if (chunk.type == CHUNK_END_ELEMENT) {
                val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
                buffer.position(16) // 跳过 header (8) + lineNumber (4) + comment (4)
                val namespaceUri = buffer.int
                val name = buffer.int
                if (name == applicationStrIndex) {
                    return i
                }
            }
        }
        return -1
    }
    
    /**
     * 获取或添加字符串到字符串池
     */
    private fun getOrAddString(pool: StringPool, str: String): Int {
        val index = pool.strings.indexOf(str)
        if (index >= 0) return index
        pool.strings.add(str)
        return pool.strings.size - 1
    }
    
    /**
     * 构建 activity-alias 的 START_ELEMENT chunk
     * 属性: android:label, android:icon, android:name, android:exported, android:targetActivity
     * 
     * 重要：属性必须按资源 ID 升序排列，否则 Android 框架解析时可能找不到某些属性
     * 资源 ID 顺序：label(0x01010001) < icon(0x01010002) < name(0x01010003) < exported(0x01010010) < targetActivity(0x01010202)
     */
    private fun buildActivityAliasStartElement(
        androidNsIndex: Int,
        elementNameIndex: Int,
        nameAttrIndex: Int,
        nameValueIndex: Int,
        targetActivityAttrIndex: Int,
        targetActivityValueIndex: Int,
        labelAttrIndex: Int,
        labelValueIndex: Int,
        iconAttrIndex: Int,
        exportedAttrIndex: Int
    ): Chunk {
        // 5 个属性，每个 20 字节
        val attrCount = 5
        val attrSize = 20
        val headerSize = 16
        val attrStart = 20 // 从 chunk 开始的偏移
        val chunkSize = 36 + attrCount * attrSize  // 36 = header(16) + element info(20)
        
        val buffer = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
        
        // Chunk header
        buffer.putShort(CHUNK_START_ELEMENT.toShort())
        buffer.putShort(headerSize.toShort())
        buffer.putInt(chunkSize)
        
        // Line number, comment
        buffer.putInt(0)  // lineNumber
        buffer.putInt(-1) // comment (0xFFFFFFFF)
        
        // Element info
        buffer.putInt(-1) // namespaceUri = -1（元素本身无命名空间）
        buffer.putInt(elementNameIndex) // name
        buffer.putShort(attrStart.toShort()) // attributeStart
        buffer.putShort(attrSize.toShort()) // attributeSize
        buffer.putShort(attrCount.toShort()) // attributeCount
        buffer.putShort(0) // idIndex
        buffer.putShort(0) // classIndex
        buffer.putShort(0) // styleIndex
        
        // Properties必须按资源 ID 升序排列！
        // Android 框架使用二分搜索查找属性，不排序会导致找不到某些属性
        
        // Attribute 1: android:label (0x01010001) - 字符串
        buffer.putInt(androidNsIndex)
        buffer.putInt(labelAttrIndex)
        buffer.putInt(labelValueIndex)
        buffer.putShort(8)
        buffer.put(0)
        buffer.put(0x03) // TYPE_STRING
        buffer.putInt(labelValueIndex)
        
        // Attribute 2: android:icon (0x01010002) - 引用
        buffer.putInt(androidNsIndex)
        buffer.putInt(iconAttrIndex)
        buffer.putInt(-1) // rawValue = -1 for reference
        buffer.putShort(8)
        buffer.put(0)
        buffer.put(0x01) // TYPE_REFERENCE
        buffer.putInt(0x7f0d0000) // ic_launcher 资源 ID
        
        // Attribute 3: android:name (0x01010003) - 字符串
        buffer.putInt(androidNsIndex) // namespace
        buffer.putInt(nameAttrIndex) // name (Resource Map 索引)
        buffer.putInt(nameValueIndex) // rawValue
        buffer.putShort(8) // valueSize
        buffer.put(0) // res0
        buffer.put(0x03) // valueType = TYPE_STRING
        buffer.putInt(nameValueIndex) // valueData
        
        // Attribute 4: android:exported (0x01010010) - 布尔值 true
        buffer.putInt(androidNsIndex)
        buffer.putInt(exportedAttrIndex)
        buffer.putInt(-1) // rawValue
        buffer.putShort(8)
        buffer.put(0)
        buffer.put(0x12) // TYPE_INT_BOOLEAN
        buffer.putInt(-1) // true = 0xFFFFFFFF
        
        // Attribute 5: android:targetActivity (0x01010202) - 字符串
        buffer.putInt(androidNsIndex)
        buffer.putInt(targetActivityAttrIndex)
        buffer.putInt(targetActivityValueIndex)
        buffer.putShort(8)
        buffer.put(0)
        buffer.put(0x03) // TYPE_STRING
        buffer.putInt(targetActivityValueIndex)
        
        return Chunk(CHUNK_START_ELEMENT, 0, chunkSize, buffer.array())
    }
    
    /**
     * 构建简单的 START_ELEMENT (无属性或少量属性)
     */
    private fun buildSimpleStartElement(androidNsIndex: Int, elementNameIndex: Int, attrCount: Int): Chunk {
        val attrSize = 20
        val headerSize = 16
        val chunkSize = 36 + attrCount * attrSize
        
        val buffer = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
        
        buffer.putShort(CHUNK_START_ELEMENT.toShort())
        buffer.putShort(headerSize.toShort())
        buffer.putInt(chunkSize)
        buffer.putInt(0)  // lineNumber
        buffer.putInt(-1) // comment
        buffer.putInt(-1) // namespaceUri = -1 表示无命名空间
        buffer.putInt(elementNameIndex)
        buffer.putShort(20) // attributeStart
        buffer.putShort(attrSize.toShort())
        buffer.putShort(attrCount.toShort())
        buffer.putShort(0) // idIndex
        buffer.putShort(0) // classIndex
        buffer.putShort(0) // styleIndex
        
        return Chunk(CHUNK_START_ELEMENT, 0, chunkSize, buffer.array())
    }
    
    /**
     * 构建 action 或 category 元素 (带 android:name 属性)
     */
    private fun buildActionOrCategoryElement(
        androidNsIndex: Int,
        elementNameIndex: Int,
        nameAttrIndex: Int,
        nameValueIndex: Int
    ): Chunk {
        val attrCount = 1
        val attrSize = 20
        val headerSize = 16
        val chunkSize = 36 + attrCount * attrSize
        
        val buffer = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
        
        buffer.putShort(CHUNK_START_ELEMENT.toShort())
        buffer.putShort(headerSize.toShort())
        buffer.putInt(chunkSize)
        buffer.putInt(0)
        buffer.putInt(-1)
        buffer.putInt(-1) // namespaceUri
        buffer.putInt(elementNameIndex)
        buffer.putShort(20)
        buffer.putShort(attrSize.toShort())
        buffer.putShort(attrCount.toShort())
        buffer.putShort(0)
        buffer.putShort(0)
        buffer.putShort(0)
        
        // android:name 属性
        buffer.putInt(androidNsIndex)
        buffer.putInt(nameAttrIndex)
        buffer.putInt(nameValueIndex)
        buffer.putShort(8)
        buffer.put(0)
        buffer.put(0x03) // TYPE_STRING
        buffer.putInt(nameValueIndex)
        
        return Chunk(CHUNK_START_ELEMENT, 0, chunkSize, buffer.array())
    }
    
    /**
     * 构建 END_ELEMENT chunk
     */
    private fun buildEndElement(androidNsIndex: Int, elementNameIndex: Int): Chunk {
        val headerSize = 16
        val chunkSize = 24 // header(16) + namespaceUri(4) + name(4)
        
        val buffer = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
        
        buffer.putShort(CHUNK_END_ELEMENT.toShort())
        buffer.putShort(headerSize.toShort())
        buffer.putInt(chunkSize)
        buffer.putInt(0)  // lineNumber
        buffer.putInt(-1) // comment
        buffer.putInt(-1) // namespaceUri (使用 -1，与原始 manifest 保持一致)
        buffer.putInt(elementNameIndex)
        
        return Chunk(CHUNK_END_ELEMENT, 0, chunkSize, buffer.array())
    }
    
    /**
     * 在字符串池中插入字符串后，更新所有现有 chunk 中 >= insertIndex 的字符串索引
     * 这是因为插入会导致后续字符串索引全部 +1
     */
    private fun updateStringIndicesAfterInsert(parsed: ParsedAxml, insertIndex: Int) {
        for (chunk in parsed.chunks) {
            when (chunk.type) {
                CHUNK_START_ELEMENT -> updateStartElementIndices(chunk, insertIndex)
                CHUNK_END_ELEMENT -> updateEndElementIndices(chunk, insertIndex)
                CHUNK_START_NAMESPACE, CHUNK_END_NAMESPACE -> updateNamespaceIndices(chunk, insertIndex)
            }
        }
    }
    
    /**
     * 更新 START_ELEMENT chunk 中的字符串索引
     */
    private fun updateStartElementIndices(chunk: Chunk, insertIndex: Int) {
        val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
        
        // namespaceUri at offset 16
        val nsOffset = 16
        val ns = buffer.getInt(nsOffset)
        if (ns >= insertIndex) {
            buffer.putInt(nsOffset, ns + 1)
        }
        
        // name at offset 20
        val nameOffset = 20
        val name = buffer.getInt(nameOffset)
        if (name >= insertIndex) {
            buffer.putInt(nameOffset, name + 1)
        }
        
        // 读取属性数量和起始位置
        val attrStart = buffer.getShort(24).toInt() and 0xFFFF
        val attrSize = buffer.getShort(26).toInt() and 0xFFFF
        val attrCount = buffer.getShort(28).toInt() and 0xFFFF
        
        // Update每个属性中的字符串索引
        for (i in 0 until attrCount) {
            val attrOffset = 16 + attrStart + i * attrSize
            
            // namespace at attrOffset + 0
            val attrNs = buffer.getInt(attrOffset)
            if (attrNs >= insertIndex) {
                buffer.putInt(attrOffset, attrNs + 1)
            }
            
            // name at attrOffset + 4 (这是属性名，通常在 Resource Map 范围内，不需要更新)
            // 但如果 insertIndex 小于这个索引，也需要更新
            val attrName = buffer.getInt(attrOffset + 4)
            if (attrName >= insertIndex) {
                buffer.putInt(attrOffset + 4, attrName + 1)
            }
            
            // rawValue at attrOffset + 8
            val rawValue = buffer.getInt(attrOffset + 8)
            if (rawValue >= 0 && rawValue >= insertIndex) {
                buffer.putInt(attrOffset + 8, rawValue + 1)
            }
            
            // valueType at attrOffset + 15
            val valueType = buffer.get(attrOffset + 15).toInt() and 0xFF
            
            // valueData at attrOffset + 16 (如果是字符串类型)
            if (valueType == 0x03) { // TYPE_STRING
                val valueData = buffer.getInt(attrOffset + 16)
                if (valueData >= insertIndex) {
                    buffer.putInt(attrOffset + 16, valueData + 1)
                }
            }
        }
    }
    
    /**
     * 更新 END_ELEMENT chunk 中的字符串索引
     */
    private fun updateEndElementIndices(chunk: Chunk, insertIndex: Int) {
        val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
        
        // namespaceUri at offset 16
        val ns = buffer.getInt(16)
        if (ns >= insertIndex) {
            buffer.putInt(16, ns + 1)
        }
        
        // name at offset 20
        val name = buffer.getInt(20)
        if (name >= insertIndex) {
            buffer.putInt(20, name + 1)
        }
    }
    
    /**
     * 更新 NAMESPACE chunk 中的字符串索引
     */
    private fun updateNamespaceIndices(chunk: Chunk, insertIndex: Int) {
        val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
        
        // prefix at offset 16
        val prefix = buffer.getInt(16)
        if (prefix >= insertIndex) {
            buffer.putInt(16, prefix + 1)
        }
        
        // uri at offset 20
        val uri = buffer.getInt(20)
        if (uri >= insertIndex) {
            buffer.putInt(20, uri + 1)
        }
    }

    /**
     * 展开相对路径类名并修改包名
     * 
     * @param axmlData 原始 AXML 数据
     * @param originalPackage 原始包名
     * @param newPackage 新包名
     * @return 修改后的 AXML 数据
     */
    fun expandAndModify(axmlData: ByteArray, originalPackage: String, newPackage: String): ByteArray {
        return try {
            val parsed = parseAxml(axmlData)
            if (parsed == null) {
                Log.e(TAG, "Failed to parse AXML")
                return axmlData
            }
            
            // 步骤1：找到需要展开的相对路径类名
            val expansions = findRelativeClassNames(parsed, originalPackage)
            Log.d(TAG, "Found ${expansions.size} relative class names to expand")
            
            // 步骤2：添加新字符串并更新引用（如果有相对路径）
            if (expansions.isNotEmpty()) {
                expandClassNames(parsed, expansions)
            }
            
            // 步骤3：修改包名和所有包名前缀的字符串（权限、authorities 等）
            // 即使没有相对路径类名，也需要处理权限冲突
            replacePackageString(parsed, originalPackage, newPackage)
            
            // 步骤4：重建 AXML
            val result = rebuildAxml(parsed)
            
            Log.d(TAG, "AXML rebuild complete: original=${axmlData.size}, new=${result.size}")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "AXML rebuild failed", e)
            axmlData
        }
    }

    /**
     * 完整的 AXML 修改方法：展开类名、修改包名、修改版本号、添加 activity-alias
     * 
     * @param axmlData 原始 AXML 数据
     * @param originalPackage 原始包名
     * @param newPackage 新包名
     * @param versionCode 新版本号
     * @param versionName 新版本名称
     * @param aliasCount 要添加的 activity-alias 数量（0 表示不添加）
     * @param appName 应用名称（用于 alias 的 label）
     * @return 修改后的 AXML 数据
     */
    fun expandAndModifyFull(
        axmlData: ByteArray, 
        originalPackage: String, 
        newPackage: String,
        versionCode: Int,
        versionName: String,
        aliasCount: Int = 0,
        appName: String = ""
    ): ByteArray {
        return try {
            val parsed = parseAxml(axmlData)
            if (parsed == null) {
                Log.e(TAG, "Failed to parse AXML for full modification")
                return axmlData
            }
            
            // 步骤1：找到需要展开的相对路径类名
            val expansions = findRelativeClassNames(parsed, originalPackage)
            Log.d(TAG, "Found ${expansions.size} relative class names to expand")
            
            // 步骤2：添加新字符串并更新引用
            if (expansions.isNotEmpty()) {
                expandClassNames(parsed, expansions)
            }
            
            // 步骤3：修改包名和所有包名前缀的字符串
            replacePackageString(parsed, originalPackage, newPackage)
            
            // 步骤4：修改版本号
            modifyVersionInfo(parsed, versionCode, versionName)
            
            // 步骤5：移除 testOnly 标记
            stripTestOnlyFlag(parsed)
            
            // 步骤5.5：确保关键权限存在（避免模板缺失导致功能不可用）
            ensureUsesPermissions(parsed, listOf(
                "android.permission.RECORD_AUDIO",
                "android.permission.CAMERA"
            ))
            
            // 步骤6：添加 activity-alias（多桌面图标）
            if (aliasCount > 0 && appName.isNotEmpty()) {
                addActivityAliases(parsed, newPackage, aliasCount, appName)
                Log.d(TAG, "Added $aliasCount activity-alias entries for multi-launcher-icons")
            }
            
            // 步骤7：重建 AXML
            val result = rebuildAxml(parsed)
            
            Log.d(TAG, "AXML full rebuild complete: original=${axmlData.size}, new=${result.size}, aliases=$aliasCount")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "AXML full rebuild failed", e)
            axmlData
        }
    }
    
    /**
     * 展开相对路径类名、修改包名，并修改版本号
     * 
     * @param axmlData 原始 AXML 数据
     * @param originalPackage 原始包名
     * @param newPackage 新包名
     * @param versionCode 新版本号
     * @param versionName 新版本名称
     * @return 修改后的 AXML 数据
     */
    fun expandAndModifyWithVersion(
        axmlData: ByteArray, 
        originalPackage: String, 
        newPackage: String,
        versionCode: Int,
        versionName: String
    ): ByteArray {
        return try {
            val parsed = parseAxml(axmlData)
            if (parsed == null) {
                Log.e(TAG, "Failed to parse AXML for version modification")
                return axmlData
            }
            
            // 步骤1：找到需要展开的相对路径类名
            val expansions = findRelativeClassNames(parsed, originalPackage)
            Log.d(TAG, "Found ${expansions.size} relative class names to expand")
            
            // 步骤2：添加新字符串并更新引用（如果有相对路径）
            if (expansions.isNotEmpty()) {
                expandClassNames(parsed, expansions)
            }
            
            // 步骤3：修改包名和所有包名前缀的字符串（权限、authorities 等）
            replacePackageString(parsed, originalPackage, newPackage)
            
            // 步骤4：修改版本号
            modifyVersionInfo(parsed, versionCode, versionName)
            
            // 步骤5：移除 testOnly 标记，避免 INSTALL_FAILED_TEST_ONLY
            stripTestOnlyFlag(parsed)
            
            // 步骤6：重建 AXML
            val result = rebuildAxml(parsed)
            
            Log.d(TAG, "AXML rebuild with version complete: original=${axmlData.size}, new=${result.size}")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "AXML rebuild with version failed", e)
            axmlData
        }
    }

    /**
     * 修改版本信息（versionCode 和 versionName）
     */
    private fun modifyVersionInfo(parsed: ParsedAxml, versionCode: Int, versionName: String) {
        val resourceMap = parsed.resourceMap ?: return
        
        // 找到 versionCode 和 versionName 属性的资源 ID 索引
        val versionCodeAttrIndex = resourceMap.indexOf(ATTR_VERSION_CODE)
        val versionNameAttrIndex = resourceMap.indexOf(ATTR_VERSION_NAME)
        
        Log.d(TAG, "Version attr indices: versionCode=$versionCodeAttrIndex, versionName=$versionNameAttrIndex")
        
        // Find manifest 元素（第一个 START_ELEMENT）
        for (chunk in parsed.chunks) {
            if (chunk.type != CHUNK_START_ELEMENT) continue
            
            val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(16)
            
            val namespaceUri = buffer.int
            val elementName = buffer.int
            val attrStart = buffer.short.toInt() and 0xFFFF
            val attrSize = buffer.short.toInt() and 0xFFFF
            val attrCount = buffer.short.toInt() and 0xFFFF
            
            if (attrSize == 0 || attrCount == 0) continue
            
            // Check是否是 manifest 元素
            val elementNameStr = parsed.stringPool.strings.getOrNull(elementName) ?: continue
            if (elementNameStr != "manifest") continue
            
            Log.d(TAG, "Found manifest element with $attrCount attributes")
            
            // 扫描属性
            for (i in 0 until attrCount) {
                val attrOffset = 36 + i * attrSize
                if (attrOffset + 20 > chunk.data.size) break
                
                buffer.position(attrOffset)
                val attrNs = buffer.int
                val attrName = buffer.int
                val attrRawValue = buffer.int
                val attrValueSize = buffer.short.toInt() and 0xFFFF
                buffer.get() // res0
                val attrValueType = buffer.get().toInt() and 0xFF
                val attrValueData = buffer.int
                
                when (attrName) {
                    versionCodeAttrIndex -> {
                        // versionCode 是整数类型 (type 0x10)
                        if (attrValueType == 0x10) {
                            buffer.position(attrOffset + 16)
                            buffer.putInt(versionCode)
                            Log.d(TAG, "Modified versionCode to $versionCode")
                        }
                    }
                    versionNameAttrIndex -> {
                        // versionName 是字符串类型 (type 0x03)
                        if (attrValueType == 0x03) {
                            // 添加或更新 versionName 字符串
                            var newIndex = parsed.stringPool.strings.indexOf(versionName)
                            if (newIndex < 0) {
                                newIndex = parsed.stringPool.strings.size
                                parsed.stringPool.strings.add(versionName)
                                Log.d(TAG, "Added versionName string at index $newIndex: '$versionName'")
                            }
                            
                            // Update rawValue
                            buffer.position(attrOffset + 8)
                            buffer.putInt(newIndex)
                            
                            // Update valueData
                            buffer.position(attrOffset + 16)
                            buffer.putInt(newIndex)
                            
                            Log.d(TAG, "Modified versionName to '$versionName' (index $newIndex)")
                        }
                    }
                }
            }
            
            break // manifest 只有一个
        }
    }

    /**
     * 移除 android:testOnly 标记，避免 INSTALL_FAILED_TEST_ONLY 错误
     * 
     * testOnly 属性通常出现在 <application> 元素中，值类型为布尔型 (0x12)
     * 我们将其值设置为 false (0x00000000)
     */
    private fun stripTestOnlyFlag(parsed: ParsedAxml) {
        val resourceMap = parsed.resourceMap ?: return
        
        // 找到 testOnly 属性的资源 ID 索引
        val testOnlyAttrIndex = resourceMap.indexOf(ATTR_TEST_ONLY)
        if (testOnlyAttrIndex < 0) {
            Log.d(TAG, "testOnly attribute not found in resource map, skipping")
            return
        }
        
        Log.d(TAG, "Found testOnly attr index: $testOnlyAttrIndex")
        
        // 扫描所有 START_ELEMENT chunk，查找 application 元素
        for (chunk in parsed.chunks) {
            if (chunk.type != CHUNK_START_ELEMENT) continue
            
            val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(16)
            
            val namespaceUri = buffer.int
            val elementName = buffer.int
            val attrStart = buffer.short.toInt() and 0xFFFF
            val attrSize = buffer.short.toInt() and 0xFFFF
            val attrCount = buffer.short.toInt() and 0xFFFF
            
            if (attrSize == 0 || attrCount == 0) continue
            
            // Check是否是 application 元素
            val elementNameStr = parsed.stringPool.strings.getOrNull(elementName) ?: continue
            if (elementNameStr != "application") continue
            
            Log.d(TAG, "Found application element with $attrCount attributes, checking for testOnly")
            
            // 扫描属性，查找 testOnly
            for (i in 0 until attrCount) {
                val attrOffset = 36 + i * attrSize
                if (attrOffset + 20 > chunk.data.size) break
                
                buffer.position(attrOffset)
                val attrNs = buffer.int
                val attrName = buffer.int
                val attrRawValue = buffer.int
                val attrValueSize = buffer.short.toInt() and 0xFFFF
                buffer.get() // res0
                val attrValueType = buffer.get().toInt() and 0xFF
                val attrValueData = buffer.int
                
                if (attrName == testOnlyAttrIndex) {
                    Log.d(TAG, "Found testOnly attribute at index $i, type=0x${attrValueType.toString(16)}, value=$attrValueData")
                    
                    // testOnly 是布尔类型 (type 0x12)，将值设置为 false (0)
                    // 也可能是整数类型 (type 0x10)
                    if (attrValueType == 0x12 || attrValueType == 0x10) {
                        buffer.position(attrOffset + 16)
                        buffer.putInt(0) // false
                        Log.d(TAG, "Set testOnly to false")
                    }
                }
            }
        }
    }

    /**
     * 解析 AXML 文件结构
     */
    private fun parseAxml(data: ByteArray): ParsedAxml? {
        if (data.size < 8) return null
        
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        
        val fileType = buffer.short.toInt() and 0xFFFF
        val fileHeaderSize = buffer.short.toInt() and 0xFFFF
        val fileSize = buffer.int
        
        if (fileType != CHUNK_AXML_FILE) {
            Log.e(TAG, "Not a valid AXML file: type=0x${fileType.toString(16)}")
            return null
        }
        
        val chunks = mutableListOf<Chunk>()
        var stringPool: StringPool? = null
        var resourceMap: IntArray? = null
        
        var offset = fileHeaderSize
        while (offset + 8 <= data.size) {
            buffer.position(offset)
            val chunkType = buffer.short.toInt() and 0xFFFF
            val chunkHeaderSize = buffer.short.toInt() and 0xFFFF
            val chunkSize = buffer.int
            
            if (chunkSize <= 0 || offset + chunkSize > data.size) break
            
            when (chunkType) {
                CHUNK_STRING_POOL -> {
                    stringPool = parseStringPool(data, offset)
                }
                CHUNK_RESOURCE_MAP -> {
                    resourceMap = parseResourceMap(data, offset, chunkSize)
                }
                else -> {
                    chunks.add(Chunk(chunkType, offset, chunkSize, data.copyOfRange(offset, offset + chunkSize)))
                }
            }
            
            offset += chunkSize
        }
        
        if (stringPool == null) {
            Log.e(TAG, "String pool not found")
            return null
        }
        
        return ParsedAxml(fileHeaderSize, stringPool, resourceMap, chunks)
    }

    /**
     * 解析字符串池
     */
    private fun parseStringPool(data: ByteArray, offset: Int): StringPool {
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(offset)
        
        buffer.short // type
        val headerSize = buffer.short.toInt() and 0xFFFF
        val chunkSize = buffer.int
        val stringCount = buffer.int
        val styleCount = buffer.int
        val flags = buffer.int
        val stringsStart = buffer.int
        val stylesStart = buffer.int
        
        val isUtf8 = (flags and 0x100) != 0
        
        // 读取字符串偏移
        val stringOffsets = IntArray(stringCount) { buffer.int }
        
        // 读取样式偏移
        val styleOffsets = IntArray(styleCount) { buffer.int }
        
        // 读取字符串
        val stringsDataStart = offset + stringsStart
        val strings = mutableListOf<String>()
        
        for (i in 0 until stringCount) {
            val strOffset = stringsDataStart + stringOffsets[i]
            val str = if (isUtf8) {
                readUtf8String(data, strOffset)
            } else {
                readUtf16String(data, strOffset)
            }
            strings.add(str)
        }
        
        // 读取样式数据（如果有）
        val stylesData = if (styleCount > 0 && stylesStart > 0) {
            val stylesDataStart = offset + stylesStart
            val stylesDataEnd = offset + chunkSize
            data.copyOfRange(stylesDataStart, stylesDataEnd)
        } else {
            null
        }
        
        // 计算原始字符串数据大小
        val originalStringsDataSize = if (stylesStart > 0) {
            stylesStart - stringsStart
        } else {
            chunkSize - stringsStart
        }
        
        return StringPool(
            flags = flags,
            isUtf8 = isUtf8,
            strings = strings.toMutableList(),
            styleOffsets = styleOffsets,
            stylesData = stylesData,
            originalStringsDataSize = originalStringsDataSize
        )
    }

    /**
     * 解析资源 ID 映射
     */
    private fun parseResourceMap(data: ByteArray, offset: Int, size: Int): IntArray {
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(offset + 8) // 跳过 chunk header
        val count = (size - 8) / 4
        return IntArray(count) { buffer.int }
    }

    /**
     * 查找需要展开的相对路径类名
     */
    private fun findRelativeClassNames(parsed: ParsedAxml, originalPackage: String): List<ClassNameExpansion> {
        val expansions = mutableListOf<ClassNameExpansion>()
        val resourceMap = parsed.resourceMap ?: return expansions
        
        // 找到 android:name 属性的资源 ID 索引
        val nameAttrIndex = resourceMap.indexOf(ATTR_NAME)
        if (nameAttrIndex < 0) {
            Log.d(TAG, "android:name attribute not found in resource map")
            return expansions
        }
        
        // 扫描所有 START_ELEMENT chunk，查找 android:name 属性
        for (chunk in parsed.chunks) {
            if (chunk.type != CHUNK_START_ELEMENT) continue
            
            val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(16) // 跳过 header 和 line number, comment
            
            val namespaceUri = buffer.int
            val elementName = buffer.int
            val attrStart = buffer.short.toInt() and 0xFFFF
            val attrSize = buffer.short.toInt() and 0xFFFF
            val attrCount = buffer.short.toInt() and 0xFFFF
            
            if (attrSize == 0 || attrCount == 0) continue
            
            // Check是否是组件元素（activity, service, receiver, provider, application）
            val elementNameStr = parsed.stringPool.strings.getOrNull(elementName) ?: continue
            if (elementNameStr !in listOf("activity", "service", "receiver", "provider", "application", "activity-alias")) {
                continue
            }
            
            // 扫描属性
            for (i in 0 until attrCount) {
                val attrOffset = 36 + i * attrSize
                if (attrOffset + 20 > chunk.data.size) break
                
                buffer.position(attrOffset)
                val attrNs = buffer.int
                val attrName = buffer.int
                val attrRawValue = buffer.int
                val attrValueSize = buffer.short.toInt() and 0xFFFF
                buffer.get() // res0
                val attrValueType = buffer.get().toInt() and 0xFF
                val attrValueData = buffer.int
                
                // Check是否是 android:name 属性
                if (attrName != nameAttrIndex) continue
                
                // Check属性值类型是否是字符串（type 3）
                if (attrValueType != 3) continue
                
                // Get字符串值
                val stringIndex = attrValueData
                val stringValue = parsed.stringPool.strings.getOrNull(stringIndex) ?: continue
                
                // Check是否是相对路径类名
                if (stringValue.startsWith(".") || (!stringValue.contains(".") && stringValue.isNotEmpty())) {
                    val absoluteName = if (stringValue.startsWith(".")) {
                        originalPackage + stringValue
                    } else {
                        "$originalPackage.$stringValue"
                    }
                    
                    expansions.add(ClassNameExpansion(
                        chunkIndex = parsed.chunks.indexOf(chunk),
                        attrIndex = i,
                        attrOffset = attrOffset,
                        originalStringIndex = stringIndex,
                        originalValue = stringValue,
                        expandedValue = absoluteName
                    ))
                    
                    Log.d(TAG, "Found relative class name: '$stringValue' -> '$absoluteName' in <$elementNameStr>")
                }
            }
        }
        
        return expansions
    }

    /**
     * 展开类名并更新引用
     */
    private fun expandClassNames(parsed: ParsedAxml, expansions: List<ClassNameExpansion>): ParsedAxml {
        val stringPool = parsed.stringPool
        
        for (expansion in expansions) {
            // Check展开后的字符串是否已存在
            var newIndex = stringPool.strings.indexOf(expansion.expandedValue)
            
            if (newIndex < 0) {
                // 添加新字符串
                newIndex = stringPool.strings.size
                stringPool.strings.add(expansion.expandedValue)
                Log.d(TAG, "Added new string at index $newIndex: '${expansion.expandedValue}'")
            }
            
            // Update chunk 中的属性引用
            val chunk = parsed.chunks[expansion.chunkIndex]
            val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
            
            // Update rawValue（字符串索引）
            buffer.position(expansion.attrOffset + 8)
            buffer.putInt(newIndex)
            
            // Update valueData（字符串索引）
            buffer.position(expansion.attrOffset + 16)
            buffer.putInt(newIndex)
        }
        
        return parsed
    }

    /**
     * 替换包名相关的字符串
     * 包括：package 属性、权限名称、provider authorities 等
     * 注意：不能替换组件类名，因为实际类仍在原包名下
     */
    private fun replacePackageString(parsed: ParsedAxml, oldPackage: String, newPackage: String) {
        val stringPool = parsed.stringPool
        
        for (i in stringPool.strings.indices) {
            val str = stringPool.strings[i]
            
            when {
                // 1. 完全匹配的包名（package 属性）
                str == oldPackage -> {
                    stringPool.strings[i] = newPackage
                    Log.d(TAG, "Replaced package at index $i: '$oldPackage' -> '$newPackage'")
                }
                
                // 2. 以包名开头的字符串（权限、authorities 等）
                // 但排除组件类名（Activity、Service、Provider、Receiver、Application等）
                str.startsWith("$oldPackage.") -> {
                    // Check是否是组件类名（通常以大写字母开头的类名结尾）
                    val suffix = str.substring(oldPackage.length + 1)
                    val isComponentClassName = isLikelyClassName(suffix)
                    
                    if (isComponentClassName) {
                        // 组件类名不替换，保持原样
                        Log.d(TAG, "Skipped component class at index $i: '$str'")
                    } else {
                        // Permission、authorities等需要替换
                        val newStr = newPackage + str.substring(oldPackage.length)
                        stringPool.strings[i] = newStr
                        Log.d(TAG, "Replaced prefixed string at index $i: '$str' -> '$newStr'")
                    }
                }
            }
        }
    }
    
    /**
     * 判断字符串是否看起来像组件类名
     * 组件类名特征：包含子包路径，以大写字母开头的类名结尾
     */
    private fun isLikelyClassName(suffix: String): Boolean {
        // Check最后一个点后面的部分是否以大写字母开头（类名特征）
        val lastDotIndex = suffix.lastIndexOf('.')
        val className = if (lastDotIndex >= 0) {
            suffix.substring(lastDotIndex + 1)
        } else {
            suffix
        }
        
        // Class名通常以大写字母开头
        if (className.isNotEmpty() && className[0].isUpperCase()) {
            // 进一步检查是否是常见的组件类型
            val componentSuffixes = listOf(
                "Activity", "Service", "Provider", "Receiver", 
                "Application", "Fragment", "Adapter", "View",
                "Manager", "Helper", "Listener", "Callback"
            )
            // 如果以常见组件后缀结尾，或者整体看起来像类名（大写开头+驼峰命名）
            return componentSuffixes.any { className.endsWith(it) } ||
                   className.matches(Regex("^[A-Z][a-zA-Z0-9]*$"))
        }
        
        return false
    }

    /**
     * 重建 AXML 文件
     */
    private fun rebuildAxml(parsed: ParsedAxml): ByteArray {
        val output = ByteArrayOutputStream()
        
        // 1. 重建字符串池
        val stringPoolData = rebuildStringPool(parsed.stringPool)
        
        // 2. 重建资源映射
        val resourceMap = parsed.resourceMap
        val resourceMapData = if (resourceMap != null) {
            rebuildResourceMap(resourceMap)
        } else {
            ByteArray(0)
        }
        
        // 3. 收集所有其他 chunks
        val chunksData = ByteArrayOutputStream()
        for (chunk in parsed.chunks) {
            chunksData.write(chunk.data)
        }
        
        // 4. 计算总大小
        val totalSize = parsed.fileHeaderSize + stringPoolData.size + resourceMapData.size + chunksData.size()
        
        // 5. 写入文件头
        val header = ByteBuffer.allocate(parsed.fileHeaderSize).order(ByteOrder.LITTLE_ENDIAN)
        header.putShort(CHUNK_AXML_FILE.toShort())
        header.putShort(parsed.fileHeaderSize.toShort())
        header.putInt(totalSize)
        output.write(header.array())
        
        // 6. 写入字符串池
        output.write(stringPoolData)
        
        // 7. 写入资源映射
        output.write(resourceMapData)
        
        // 8. 写入其他 chunks
        chunksData.writeTo(output)
        
        return output.toByteArray()
    }

    /**
     * 重建字符串池
     */
    private fun rebuildStringPool(pool: StringPool): ByteArray {
        val isUtf8 = pool.isUtf8
        val stringCount = pool.strings.size
        val styleCount = pool.styleOffsets.size
        
        // 序列化字符串数据
        val stringsBuffer = ByteArrayOutputStream()
        val stringOffsets = IntArray(stringCount)
        
        for (i in 0 until stringCount) {
            stringOffsets[i] = stringsBuffer.size()
            val str = pool.strings[i]
            
            if (isUtf8) {
                writeUtf8String(stringsBuffer, str)
            } else {
                writeUtf16String(stringsBuffer, str)
            }
        }
        
        // 对齐到 4 字节
        while (stringsBuffer.size() % 4 != 0) {
            stringsBuffer.write(0)
        }
        
        val stringsData = stringsBuffer.toByteArray()
        
        // 计算字符串数据大小变化量，用于调整样式偏移
        val stringsDataSizeDelta = stringsData.size - pool.originalStringsDataSize
        
        // 计算各部分大小
        val headerSize = 28
        val offsetsSize = (stringCount + styleCount) * 4
        val stringsStart = headerSize + offsetsSize
        val stylesStart = if (styleCount > 0 && pool.stylesData != null) {
            stringsStart + stringsData.size
        } else {
            0
        }
        val stylesDataSize = pool.stylesData?.size ?: 0
        val chunkSize = stringsStart + stringsData.size + stylesDataSize
        
        // Build字符串池 chunk
        val result = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
        
        // Chunk header
        result.putShort(CHUNK_STRING_POOL.toShort())
        result.putShort(headerSize.toShort())
        result.putInt(chunkSize)
        result.putInt(stringCount)
        result.putInt(styleCount)
        // 保留原始flags，但清除SORTED_FLAG（因为我们可能添加了新字符串，顺序可能改变）
        val newFlags = pool.flags and 0x01.inv()
        result.putInt(newFlags)
        result.putInt(stringsStart)
        result.putInt(stylesStart)
        
        // String offsets
        for (offset in stringOffsets) {
            result.putInt(offset)
        }
        
        // Style offsets - 需要根据字符串数据大小变化进行调整
        for (offset in pool.styleOffsets) {
            result.putInt(offset + stringsDataSizeDelta)
        }
        
        // String data
        result.put(stringsData)
        
        // Styles data
        pool.stylesData?.let { result.put(it) }
        
        return result.array()
    }

    /**
     * 重建资源映射
     */
    private fun rebuildResourceMap(resourceMap: IntArray): ByteArray {
        val size = 8 + resourceMap.size * 4
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(CHUNK_RESOURCE_MAP.toShort())
        buffer.putShort(8.toShort())
        buffer.putInt(size)
        for (id in resourceMap) {
            buffer.putInt(id)
        }
        return buffer.array()
    }

    /**
     * 简单的包名替换（当没有相对路径类名时使用）
     */
    private fun simplePackageReplace(data: ByteArray, oldPackage: String, newPackage: String): ByteArray {
        val result = data.copyOf()
        
        // UTF-8 替换
        replacePackageBytes(result, oldPackage, newPackage, Charsets.UTF_8)
        
        // UTF-16LE 替换
        replacePackageBytes(result, oldPackage, newPackage, Charsets.UTF_16LE)
        
        return result
    }

    private fun replacePackageBytes(data: ByteArray, oldPkg: String, newPkg: String, charset: java.nio.charset.Charset) {
        val oldBytes = oldPkg.toByteArray(charset)
        val newBytes = newPkg.toByteArray(charset)
        
        if (newBytes.size > oldBytes.size) return
        
        val replacement = if (newBytes.size == oldBytes.size) {
            newBytes
        } else {
            newBytes + ByteArray(oldBytes.size - newBytes.size) { 0 }
        }
        
        val isUtf16 = charset == Charsets.UTF_16LE
        var i = 0
        
        while (i <= data.size - oldBytes.size) {
            var match = true
            for (j in oldBytes.indices) {
                if (data[i + j] != oldBytes[j]) {
                    match = false
                    break
                }
            }
            
            if (match) {
                // Check是否是独立字符串
                val nextIndex = i + oldBytes.size
                var isIndependent = nextIndex >= data.size
                
                if (!isIndependent) {
                    if (isUtf16) {
                        if (nextIndex + 1 < data.size) {
                            isIndependent = data[nextIndex] == 0.toByte() && data[nextIndex + 1] == 0.toByte()
                        }
                    } else {
                        isIndependent = data[nextIndex] == 0.toByte()
                    }
                }
                
                if (isIndependent) {
                    System.arraycopy(replacement, 0, data, i, replacement.size)
                    i += oldBytes.size
                } else {
                    i++
                }
            } else {
                i++
            }
        }
    }

    // ========== 辅助方法 ==========

    private fun readUtf8String(data: ByteArray, offset: Int): String {
        if (offset >= data.size) return ""
        var o = offset
        
        // 读取字符长度（可能是 1 或 2 字节）
        var charLen = data[o].toInt() and 0x7F
        if (data[o].toInt() and 0x80 != 0) {
            if (o + 1 >= data.size) return ""
            charLen = ((data[o].toInt() and 0x7F) shl 8) or (data[o + 1].toInt() and 0xFF)
            o += 2
        } else {
            o += 1
        }
        
        // 读取字节长度
        var byteLen = data[o].toInt() and 0x7F
        if (data[o].toInt() and 0x80 != 0) {
            if (o + 1 >= data.size) return ""
            byteLen = ((data[o].toInt() and 0x7F) shl 8) or (data[o + 1].toInt() and 0xFF)
            o += 2
        } else {
            o += 1
        }
        
        if (o + byteLen > data.size) return ""
        return String(data, o, byteLen, Charsets.UTF_8)
    }

    private fun readUtf16String(data: ByteArray, offset: Int): String {
        if (offset + 2 > data.size) return ""
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(offset)
        
        var length = buffer.short.toInt() and 0xFFFF
        if (length and 0x8000 != 0) {
            if (offset + 4 > data.size) return ""
            length = ((length and 0x7FFF) shl 16) or (buffer.short.toInt() and 0xFFFF)
        }
        
        val byteLen = length * 2
        if (buffer.position() + byteLen > data.size) return ""
        
        val strBytes = ByteArray(byteLen)
        buffer.get(strBytes)
        return String(strBytes, Charsets.UTF_16LE)
    }

    private fun writeUtf8String(output: ByteArrayOutputStream, str: String) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        val charLen = str.length
        val byteLen = bytes.size
        
        // 写入字符长度
        if (charLen > 0x7F) {
            output.write(0x80 or ((charLen shr 8) and 0x7F))
            output.write(charLen and 0xFF)
        } else {
            output.write(charLen)
        }
        
        // 写入字节长度
        if (byteLen > 0x7F) {
            output.write(0x80 or ((byteLen shr 8) and 0x7F))
            output.write(byteLen and 0xFF)
        } else {
            output.write(byteLen)
        }
        
        // 写入字符串数据
        output.write(bytes)
        
        // 写入 null 终止符
        output.write(0)
    }

    private fun writeUtf16String(output: ByteArrayOutputStream, str: String) {
        val length = str.length
        
        // 写入长度
        if (length > 0x7FFF) {
            output.write(0x80 or ((length shr 24) and 0x7F))
            output.write((length shr 16) and 0xFF)
            output.write((length shr 8) and 0xFF)
            output.write(length and 0xFF)
        } else {
            output.write(length and 0xFF)
            output.write((length shr 8) and 0xFF)
        }
        
        // 写入字符串数据
        val bytes = str.toByteArray(Charsets.UTF_16LE)
        output.write(bytes)
        
        // 写入 null 终止符（2 字节）
        output.write(0)
        output.write(0)
    }

    // ========== 数据类 ==========

    private data class ParsedAxml(
        val fileHeaderSize: Int,
        val stringPool: StringPool,
        var resourceMap: IntArray?,  // var 以便在添加 activity-alias 时扩展
        val chunks: MutableList<Chunk>
    )

    private data class StringPool(
        val flags: Int,
        val isUtf8: Boolean,
        val strings: MutableList<String>,
        val styleOffsets: IntArray,
        val stylesData: ByteArray?,
        val originalStringsDataSize: Int
    )

    private data class Chunk(
        val type: Int,
        val offset: Int,
        val size: Int,
        val data: ByteArray
    )

    private data class ClassNameExpansion(
        val chunkIndex: Int,
        val attrIndex: Int,
        val attrOffset: Int,
        val originalStringIndex: Int,
        val originalValue: String,
        val expandedValue: String
    )
}
