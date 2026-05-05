package com.webtoapp.core.apkbuilder

import com.webtoapp.core.logging.AppLogger
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder








class AxmlRebuilder {

    companion object {
        private const val TAG = "AxmlRebuilder"


        private const val CHUNK_AXML_FILE = 0x0003
        private const val CHUNK_STRING_POOL = 0x0001
        private const val CHUNK_RESOURCE_MAP = 0x0180
        private const val CHUNK_START_NAMESPACE = 0x0100
        private const val CHUNK_END_NAMESPACE = 0x0101
        private const val CHUNK_START_ELEMENT = 0x0102
        private const val CHUNK_END_ELEMENT = 0x0103
        private const val CHUNK_TEXT = 0x0104


        private const val ATTR_NAME = 0x01010003
        private const val ATTR_VERSION_CODE = 0x0101021b
        private const val ATTR_VERSION_NAME = 0x0101021c
        private const val ATTR_TEST_ONLY = 0x01010272
        private const val ATTR_LABEL = 0x01010001
        private const val ATTR_ICON = 0x01010002
        private const val ATTR_TARGET_ACTIVITY = 0x01010202
        private const val ATTR_EXPORTED = 0x01010010
        private const val ATTR_ENABLED = 0x0101000e
        private const val ATTR_SCHEME = 0x01010027
        private const val ATTR_HOST = 0x01010028


        private val CLASS_NAME_REGEX = Regex("^[A-Z][a-zA-Z0-9]*$")

        private val BASELINE_RUNTIME_PERMISSIONS = listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE"
        )
    }











    fun expandAndModifyWithAliases(
        axmlData: ByteArray,
        originalPackage: String,
        newPackage: String,
        aliasCount: Int = 0,
        appName: String = "",
        permissions: List<String> = BASELINE_RUNTIME_PERMISSIONS
    ): ByteArray {
        return try {
            val parsed = parseAxml(axmlData)
            if (parsed == null) {
                AppLogger.e(TAG, "Failed to parse AXML for aliases")
                return axmlData
            }


            val expansions = findRelativeClassNames(parsed, originalPackage)
            AppLogger.d(TAG, "Found ${expansions.size} relative class names to expand")


            if (expansions.isNotEmpty()) {
                expandClassNames(parsed, expansions)
            }


            replacePackageString(parsed, originalPackage, newPackage)


            ensureUsesPermissions(parsed, permissions)


            if (aliasCount > 0) {
                addActivityAliases(parsed, newPackage, aliasCount, appName)
                AppLogger.d(TAG, "Added $aliasCount activity-alias entries")
            }


            val result = rebuildAxml(parsed)

            AppLogger.d(TAG, "AXML rebuild with aliases complete: original=${axmlData.size}, new=${result.size}")
            result

        } catch (e: Exception) {
            AppLogger.e(TAG, "AXML rebuild with aliases failed", e)
            axmlData
        }
    }




    private fun ensureUsesPermissions(parsed: ParsedAxml, permissions: List<String>) {
        val resourceMap = parsed.resourceMap ?: return
        val nameAttrIndex = resourceMap.indexOf(ATTR_NAME)
        if (nameAttrIndex < 0) {
            AppLogger.w(TAG, "android:name attribute not found in resource map, cannot add permissions")
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
            AppLogger.d(TAG, "Injected uses-permission: $perm")
        }

        parsed.chunks.addAll(insertIndex, newChunks)
    }




    private fun hasUsesPermission(parsed: ParsedAxml, permission: String, nameAttrIndex: Int): Boolean {
        for (chunk in parsed.chunks) {
            if (chunk.type != CHUNK_START_ELEMENT) continue
            val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(16)
            buffer.int
            val elementName = buffer.int
            buffer.short
            val attrSize = buffer.short.toInt() and 0xFFFF
            val attrCount = buffer.short.toInt() and 0xFFFF

            if (attrSize == 0 || attrCount == 0) continue
            val elementNameStr = parsed.stringPool.strings.getOrNull(elementName) ?: continue
            if (elementNameStr != "uses-permission") continue

            for (i in 0 until attrCount) {
                val attrOffset = 36 + i * attrSize
                if (attrOffset + 20 > chunk.data.size) break
                buffer.position(attrOffset)
                buffer.int
                val attrName = buffer.int
                buffer.int
                buffer.short
                buffer.get()
                val attrValueType = buffer.get().toInt() and 0xFF
                val attrValueData = buffer.int

                if (attrName == nameAttrIndex && attrValueType == 0x03) {
                    val valueStr = parsed.stringPool.strings.getOrNull(attrValueData)
                    if (valueStr == permission) {
                        return true
                    }
                }
            }
        }
        return false
    }




    private fun findApplicationStartIndex(parsed: ParsedAxml): Int {
        val applicationStrIndex = parsed.stringPool.strings.indexOf("application")
        if (applicationStrIndex < 0) return -1

        for (i in parsed.chunks.indices) {
            val chunk = parsed.chunks[i]
            if (chunk.type != CHUNK_START_ELEMENT) continue
            val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(16)
            buffer.int
            val name = buffer.int
            if (name == applicationStrIndex) {
                return i
            }
        }
        return -1
    }







    private fun addActivityAliases(
        parsed: ParsedAxml,
        packageName: String,
        aliasCount: Int,
        appName: String
    ) {

        val applicationEndIndex = findApplicationEndIndex(parsed)
        if (applicationEndIndex < 0) {
            AppLogger.e(TAG, "Cannot find </application> element")
            return
        }

        val resourceMap = parsed.resourceMap
        if (resourceMap == null) {
            AppLogger.e(TAG, "No resource map found, cannot add activity-alias")
            return
        }


        val nameAttrIndex = resourceMap.indexOf(ATTR_NAME)
        val labelAttrIndex = resourceMap.indexOf(ATTR_LABEL)
        val iconAttrIndex = resourceMap.indexOf(ATTR_ICON)
        val exportedAttrIndex = resourceMap.indexOf(ATTR_EXPORTED)


        var targetActivityAttrIndex = resourceMap.indexOf(ATTR_TARGET_ACTIVITY)
        if (targetActivityAttrIndex < 0) {


            targetActivityAttrIndex = resourceMap.size


            parsed.stringPool.strings.add(targetActivityAttrIndex, "targetActivity")


            updateStringIndicesAfterInsert(parsed, targetActivityAttrIndex)


            val newResourceMap = resourceMap.copyOf(resourceMap.size + 1)
            newResourceMap[targetActivityAttrIndex] = ATTR_TARGET_ACTIVITY
            parsed.resourceMap = newResourceMap
            AppLogger.d(TAG, "Added targetActivity to string pool and resource map at index $targetActivityAttrIndex")
        }

        if (nameAttrIndex < 0 || labelAttrIndex < 0 || iconAttrIndex < 0 || exportedAttrIndex < 0) {
            AppLogger.e(TAG, "Missing required attribute indices: name=$nameAttrIndex, label=$labelAttrIndex, icon=$iconAttrIndex, exported=$exportedAttrIndex")
            return
        }

        AppLogger.d(TAG, "Attribute indices: name=$nameAttrIndex, targetActivity=$targetActivityAttrIndex, label=$labelAttrIndex, icon=$iconAttrIndex, exported=$exportedAttrIndex")


        val androidNsIndex = getOrAddString(parsed.stringPool, "http://schemas.android.com/apk/res/android")


        val activityAliasNameIndex = getOrAddString(parsed.stringPool, "activity-alias")
        val intentFilterNameIndex = getOrAddString(parsed.stringPool, "intent-filter")
        val actionNameIndex = getOrAddString(parsed.stringPool, "action")
        val categoryNameIndex = getOrAddString(parsed.stringPool, "category")


        val mainActionIndex = getOrAddString(parsed.stringPool, "android.intent.action.MAIN")
        val launcherCategoryIndex = getOrAddString(parsed.stringPool, "android.intent.category.LAUNCHER")




        val targetActivityValue = "com.webtoapp.ui.shell.ShellActivity"
        val targetActivityValueIndex = getOrAddString(parsed.stringPool, targetActivityValue)
        AppLogger.d(TAG, "targetActivity value: $targetActivityValue (using original class name)")






        val newChunks = mutableListOf<Chunk>()


        val intentFilterStartTemplate = buildSimpleStartElement(androidNsIndex, intentFilterNameIndex, 0)
        val actionStartTemplate = buildActionOrCategoryElement(
            androidNsIndex = androidNsIndex,
            elementNameIndex = actionNameIndex,
            nameAttrIndex = nameAttrIndex,
            nameValueIndex = mainActionIndex
        )
        val actionEndTemplate = buildEndElement(androidNsIndex, actionNameIndex)
        val categoryStartTemplate = buildActionOrCategoryElement(
            androidNsIndex = androidNsIndex,
            elementNameIndex = categoryNameIndex,
            nameAttrIndex = nameAttrIndex,
            nameValueIndex = launcherCategoryIndex
        )
        val categoryEndTemplate = buildEndElement(androidNsIndex, categoryNameIndex)
        val intentFilterEndTemplate = buildEndElement(androidNsIndex, intentFilterNameIndex)
        val aliasEndTemplate = buildEndElement(androidNsIndex, activityAliasNameIndex)


        if (aliasCount > 100) {
            AppLogger.d(TAG, "Icon Storm: generating $aliasCount aliases (pre-allocated buffer for ${aliasCount * 8} chunks)")
        }

        for (i in 1..aliasCount) {

            val aliasName = ".LauncherAlias$i"
            val aliasNameValueIndex = getOrAddString(parsed.stringPool, aliasName)


            val aliasLabel = appName
            val aliasLabelIndex = getOrAddString(parsed.stringPool, aliasLabel)


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
            newChunks.add(intentFilterStartTemplate)
            newChunks.add(actionStartTemplate)
            newChunks.add(actionEndTemplate)
            newChunks.add(categoryStartTemplate)
            newChunks.add(categoryEndTemplate)
            newChunks.add(intentFilterEndTemplate)
            newChunks.add(aliasEndTemplate)


            if (aliasCount >= 500 && i % 500 == 0) {
                AppLogger.d(TAG, "Icon Storm progress: $i / $aliasCount aliases generated")
            }
        }


        parsed.chunks.addAll(applicationEndIndex, newChunks)
        AppLogger.d(TAG, "Inserted ${newChunks.size} chunks for $aliasCount activity-alias entries" +
            if (aliasCount >= 100) " (Icon Storm mode)" else ""
        )
    }




    private fun findApplicationEndIndex(parsed: ParsedAxml): Int {
        val applicationStrIndex = parsed.stringPool.strings.indexOf("application")
        if (applicationStrIndex < 0) return -1

        for (i in parsed.chunks.indices) {
            val chunk = parsed.chunks[i]
            if (chunk.type == CHUNK_END_ELEMENT) {
                val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
                buffer.position(16)
                val namespaceUri = buffer.int
                val name = buffer.int
                if (name == applicationStrIndex) {
                    return i
                }
            }
        }
        return -1
    }





    private fun addDeepLinkIntentFilter(parsed: ParsedAxml, hosts: List<String>) {
        if (hosts.isEmpty()) return

        val resourceMap = parsed.resourceMap
        if (resourceMap == null) {
            AppLogger.e(TAG, "No resource map found, cannot add deep link intent-filter")
            return
        }

        val nameAttrIndex = resourceMap.indexOf(ATTR_NAME)
        if (nameAttrIndex < 0) {
            AppLogger.e(TAG, "android:name not in resource map, cannot add deep link")
            return
        }


        val shellActivityEndIndex = findActivityEndIndex(parsed, "com.webtoapp.ui.shell.ShellActivity")
        if (shellActivityEndIndex < 0) {
            AppLogger.e(TAG, "Cannot find ShellActivity </activity> element for deep link")
            return
        }


        var schemeAttrIndex = resourceMap.indexOf(ATTR_SCHEME)
        if (schemeAttrIndex < 0) {
            schemeAttrIndex = resourceMap.size
            parsed.stringPool.strings.add(schemeAttrIndex, "scheme")
            updateStringIndicesAfterInsert(parsed, schemeAttrIndex)
            val newResourceMap = resourceMap.copyOf(resourceMap.size + 1)
            newResourceMap[schemeAttrIndex] = ATTR_SCHEME
            parsed.resourceMap = newResourceMap
            AppLogger.d(TAG, "Added scheme to string pool and resource map at index $schemeAttrIndex")
        }

        var hostAttrIndex = parsed.resourceMap!!.indexOf(ATTR_HOST)
        if (hostAttrIndex < 0) {
            hostAttrIndex = parsed.resourceMap!!.size
            parsed.stringPool.strings.add(hostAttrIndex, "host")
            updateStringIndicesAfterInsert(parsed, hostAttrIndex)
            val newResourceMap2 = parsed.resourceMap!!.copyOf(parsed.resourceMap!!.size + 1)
            newResourceMap2[hostAttrIndex] = ATTR_HOST
            parsed.resourceMap = newResourceMap2
            AppLogger.d(TAG, "Added host to string pool and resource map at index $hostAttrIndex")
        }


        val currentNameAttrIndex = parsed.resourceMap!!.indexOf(ATTR_NAME)
        val currentSchemeAttrIndex = parsed.resourceMap!!.indexOf(ATTR_SCHEME)
        val currentHostAttrIndex = parsed.resourceMap!!.indexOf(ATTR_HOST)

        val androidNsIndex = getOrAddString(parsed.stringPool, "http://schemas.android.com/apk/res/android")
        val intentFilterNameIndex = getOrAddString(parsed.stringPool, "intent-filter")
        val actionNameIndex = getOrAddString(parsed.stringPool, "action")
        val categoryNameIndex = getOrAddString(parsed.stringPool, "category")
        val dataNameIndex = getOrAddString(parsed.stringPool, "data")

        val viewActionIndex = getOrAddString(parsed.stringPool, "android.intent.action.VIEW")
        val defaultCategoryIndex = getOrAddString(parsed.stringPool, "android.intent.category.DEFAULT")
        val browsableCategoryIndex = getOrAddString(parsed.stringPool, "android.intent.category.BROWSABLE")
        val httpsSchemeIndex = getOrAddString(parsed.stringPool, "https")
        val httpSchemeIndex = getOrAddString(parsed.stringPool, "http")

        val newChunks = mutableListOf<Chunk>()


        newChunks.add(buildSimpleStartElement(androidNsIndex, intentFilterNameIndex, 0))


        newChunks.add(buildActionOrCategoryElement(androidNsIndex, actionNameIndex, currentNameAttrIndex, viewActionIndex))
        newChunks.add(buildEndElement(androidNsIndex, actionNameIndex))


        newChunks.add(buildActionOrCategoryElement(androidNsIndex, categoryNameIndex, currentNameAttrIndex, defaultCategoryIndex))
        newChunks.add(buildEndElement(androidNsIndex, categoryNameIndex))


        newChunks.add(buildActionOrCategoryElement(androidNsIndex, categoryNameIndex, currentNameAttrIndex, browsableCategoryIndex))
        newChunks.add(buildEndElement(androidNsIndex, categoryNameIndex))


        for (host in hosts) {
            val hostValueIndex = getOrAddString(parsed.stringPool, host)

            newChunks.add(buildDataElement(androidNsIndex, dataNameIndex, currentSchemeAttrIndex, httpsSchemeIndex, currentHostAttrIndex, hostValueIndex))
            newChunks.add(buildEndElement(androidNsIndex, dataNameIndex))

            newChunks.add(buildDataElement(androidNsIndex, dataNameIndex, currentSchemeAttrIndex, httpSchemeIndex, currentHostAttrIndex, hostValueIndex))
            newChunks.add(buildEndElement(androidNsIndex, dataNameIndex))
        }


        newChunks.add(buildEndElement(androidNsIndex, intentFilterNameIndex))



        val currentEndIndex = findActivityEndIndex(parsed, "com.webtoapp.ui.shell.ShellActivity")
        if (currentEndIndex >= 0) {
            parsed.chunks.addAll(currentEndIndex, newChunks)
            AppLogger.d(TAG, "Inserted ${newChunks.size} chunks for deep link intent-filter")
        }
    }














    private fun rewireLauncherToShellActivity(parsed: ParsedAxml, addDirectLauncherToShell: Boolean) {
        val resourceMap = parsed.resourceMap ?: run {
            AppLogger.e(TAG, "No resource map found, cannot rewire launcher")
            return
        }

        val exportedAttrIndex = resourceMap.indexOf(ATTR_EXPORTED)
        if (exportedAttrIndex < 0) {
            AppLogger.e(TAG, "android:exported not found in resource map, cannot rewire launcher")
            return
        }

        setExistingBooleanAttributeOnActivity(
            parsed = parsed,
            activityClassName = "com.webtoapp.ui.MainActivity",
            attrIndex = exportedAttrIndex,
            value = false
        )

        setExistingBooleanAttributeOnActivity(
            parsed = parsed,
            activityClassName = "com.webtoapp.ui.shell.ShellActivity",
            attrIndex = exportedAttrIndex,
            value = true
        )

        if (addDirectLauncherToShell) {
            addLauncherIntentFilterToShellActivity(parsed)
        }
    }




    private fun setExistingBooleanAttributeOnActivity(
        parsed: ParsedAxml,
        activityClassName: String,
        attrIndex: Int,
        value: Boolean
    ) {
        val activityStartIndex = findActivityStartIndex(parsed, activityClassName)
        if (activityStartIndex < 0) {
            AppLogger.w(TAG, "Cannot find <$activityClassName> START_ELEMENT for boolean attribute patch")
            return
        }

        val chunk = parsed.chunks[activityStartIndex]
        val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(16)
        buffer.int
        buffer.int
        val attrStart = buffer.short.toInt() and 0xFFFF
        val attrSize = buffer.short.toInt() and 0xFFFF
        val attrCount = buffer.short.toInt() and 0xFFFF

        for (i in 0 until attrCount) {
            val attrOffset = 36 + i * attrSize
            if (attrOffset + 20 > chunk.data.size) break
            buffer.position(attrOffset)
            buffer.int
            val attrName = buffer.int
            if (attrName != attrIndex) continue


            buffer.putInt(attrOffset + 8, -1)
            buffer.putShort(attrOffset + 12, 8)
            buffer.put(attrOffset + 14, 0)
            buffer.put(attrOffset + 15, 0x12.toByte())
            buffer.putInt(attrOffset + 16, if (value) -1 else 0)
            AppLogger.d(TAG, "Patched boolean attr index=$attrIndex on $activityClassName to $value")
            return
        }

        AppLogger.w(TAG, "Attribute index=$attrIndex not found on $activityClassName, skip patch")
    }





    private fun addLauncherIntentFilterToShellActivity(parsed: ParsedAxml) {
        val resourceMap = parsed.resourceMap ?: run {
            AppLogger.e(TAG, "No resource map found, cannot add launcher intent-filter")
            return
        }

        val nameAttrIndex = resourceMap.indexOf(ATTR_NAME)
        if (nameAttrIndex < 0) {
            AppLogger.e(TAG, "android:name not in resource map, cannot add launcher intent-filter")
            return
        }

        val shellActivityEndIndex = findActivityEndIndex(parsed, "com.webtoapp.ui.shell.ShellActivity")
        if (shellActivityEndIndex < 0) {
            AppLogger.e(TAG, "Cannot find ShellActivity </activity> element for launcher intent-filter")
            return
        }

        val androidNsIndex = getOrAddString(parsed.stringPool, "http://schemas.android.com/apk/res/android")
        val intentFilterNameIndex = getOrAddString(parsed.stringPool, "intent-filter")
        val actionNameIndex = getOrAddString(parsed.stringPool, "action")
        val categoryNameIndex = getOrAddString(parsed.stringPool, "category")
        val mainActionIndex = getOrAddString(parsed.stringPool, "android.intent.action.MAIN")
        val launcherCategoryIndex = getOrAddString(parsed.stringPool, "android.intent.category.LAUNCHER")

        val newChunks = mutableListOf<Chunk>()
        newChunks.add(buildSimpleStartElement(androidNsIndex, intentFilterNameIndex, 0))
        newChunks.add(buildActionOrCategoryElement(androidNsIndex, actionNameIndex, nameAttrIndex, mainActionIndex))
        newChunks.add(buildEndElement(androidNsIndex, actionNameIndex))
        newChunks.add(buildActionOrCategoryElement(androidNsIndex, categoryNameIndex, nameAttrIndex, launcherCategoryIndex))
        newChunks.add(buildEndElement(androidNsIndex, categoryNameIndex))
        newChunks.add(buildEndElement(androidNsIndex, intentFilterNameIndex))

        val currentEndIndex = findActivityEndIndex(parsed, "com.webtoapp.ui.shell.ShellActivity")
        if (currentEndIndex >= 0) {
            parsed.chunks.addAll(currentEndIndex, newChunks)
            AppLogger.d(TAG, "Inserted launcher intent-filter into ShellActivity")
        }
    }




    private fun findActivityStartIndex(parsed: ParsedAxml, activityClassName: String): Int {
        val nameAttrIndex = parsed.resourceMap?.indexOf(ATTR_NAME) ?: return -1

        for (i in parsed.chunks.indices) {
            val chunk = parsed.chunks[i]
            if (chunk.type != CHUNK_START_ELEMENT) continue

            val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(16)
            buffer.int
            val elementName = buffer.int
            val attrStart = buffer.short.toInt() and 0xFFFF
            val attrSize = buffer.short.toInt() and 0xFFFF
            val attrCount = buffer.short.toInt() and 0xFFFF

            val elementNameStr = parsed.stringPool.strings.getOrNull(elementName) ?: continue
            if (elementNameStr != "activity") continue

            for (j in 0 until attrCount) {
                val attrOffset = 36 + j * attrSize
                if (attrOffset + 20 > chunk.data.size) break
                buffer.position(attrOffset)
                buffer.int
                val attrName = buffer.int
                buffer.int
                buffer.short
                buffer.get()
                val attrValueType = buffer.get().toInt() and 0xFF
                val attrValueData = buffer.int

                if (attrName == nameAttrIndex && attrValueType == 0x03) {
                    val valueStr = parsed.stringPool.strings.getOrNull(attrValueData)
                    if (valueStr == activityClassName) {
                        return i
                    }
                }
            }
        }

        return -1
    }




    private fun findActivityEndIndex(parsed: ParsedAxml, activityClassName: String): Int {
        val activityStrIndex = parsed.stringPool.strings.indexOf("activity")
        if (activityStrIndex < 0) return -1

        val nameAttrIndex = parsed.resourceMap?.indexOf(ATTR_NAME) ?: return -1
        var insideTargetActivity = false
        var depth = 0

        for (i in parsed.chunks.indices) {
            val chunk = parsed.chunks[i]
            if (chunk.type == CHUNK_START_ELEMENT) {
                val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
                buffer.position(16)
                buffer.int
                val elementName = buffer.int
                val attrStart = buffer.short.toInt() and 0xFFFF
                val attrSize = buffer.short.toInt() and 0xFFFF
                val attrCount = buffer.short.toInt() and 0xFFFF

                val elementNameStr = parsed.stringPool.strings.getOrNull(elementName)

                if (elementNameStr == "activity" && !insideTargetActivity) {

                    for (j in 0 until attrCount) {
                        val attrOffset = 36 + j * attrSize
                        if (attrOffset + 20 > chunk.data.size) break
                        buffer.position(attrOffset)
                        buffer.int
                        val attrName = buffer.int
                        buffer.int
                        buffer.short
                        buffer.get()
                        val attrValueType = buffer.get().toInt() and 0xFF
                        val attrValueData = buffer.int

                        if (attrName == nameAttrIndex && attrValueType == 0x03) {
                            val valueStr = parsed.stringPool.strings.getOrNull(attrValueData)
                            if (valueStr == activityClassName) {
                                insideTargetActivity = true
                                depth = 1
                                break
                            }
                        }
                    }
                } else if (insideTargetActivity) {
                    depth++
                }
            } else if (chunk.type == CHUNK_END_ELEMENT && insideTargetActivity) {
                depth--
                if (depth == 0) {

                    return i
                }
            }
        }
        return -1
    }






    private fun buildDataElement(
        androidNsIndex: Int,
        elementNameIndex: Int,
        schemeAttrIndex: Int,
        schemeValueIndex: Int,
        hostAttrIndex: Int,
        hostValueIndex: Int
    ): Chunk {
        val attrCount = 2
        val attrSize = 20
        val headerSize = 16
        val chunkSize = 36 + attrCount * attrSize

        val buffer = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)

        buffer.putShort(CHUNK_START_ELEMENT.toShort())
        buffer.putShort(headerSize.toShort())
        buffer.putInt(chunkSize)
        buffer.putInt(0)
        buffer.putInt(-1)
        buffer.putInt(-1)
        buffer.putInt(elementNameIndex)
        buffer.putShort(20)
        buffer.putShort(attrSize.toShort())
        buffer.putShort(attrCount.toShort())
        buffer.putShort(0)
        buffer.putShort(0)
        buffer.putShort(0)


        buffer.putInt(androidNsIndex)
        buffer.putInt(schemeAttrIndex)
        buffer.putInt(schemeValueIndex)
        buffer.putShort(8)
        buffer.put(0)
        buffer.put(0x03)
        buffer.putInt(schemeValueIndex)


        buffer.putInt(androidNsIndex)
        buffer.putInt(hostAttrIndex)
        buffer.putInt(hostValueIndex)
        buffer.putShort(8)
        buffer.put(0)
        buffer.put(0x03)
        buffer.putInt(hostValueIndex)

        return Chunk(CHUNK_START_ELEMENT, 0, chunkSize, buffer.array())
    }




    private fun getOrAddString(pool: StringPool, str: String): Int {
        val index = pool.strings.indexOf(str)
        if (index >= 0) return index
        pool.strings.add(str)
        return pool.strings.size - 1
    }








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

        val attrCount = 5
        val attrSize = 20
        val headerSize = 16
        val attrStart = 20
        val chunkSize = 36 + attrCount * attrSize

        val buffer = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)


        buffer.putShort(CHUNK_START_ELEMENT.toShort())
        buffer.putShort(headerSize.toShort())
        buffer.putInt(chunkSize)


        buffer.putInt(0)
        buffer.putInt(-1)


        buffer.putInt(-1)
        buffer.putInt(elementNameIndex)
        buffer.putShort(attrStart.toShort())
        buffer.putShort(attrSize.toShort())
        buffer.putShort(attrCount.toShort())
        buffer.putShort(0)
        buffer.putShort(0)
        buffer.putShort(0)





        buffer.putInt(androidNsIndex)
        buffer.putInt(labelAttrIndex)
        buffer.putInt(labelValueIndex)
        buffer.putShort(8)
        buffer.put(0)
        buffer.put(0x03)
        buffer.putInt(labelValueIndex)


        buffer.putInt(androidNsIndex)
        buffer.putInt(iconAttrIndex)
        buffer.putInt(-1)
        buffer.putShort(8)
        buffer.put(0)
        buffer.put(0x01)
        buffer.putInt(0x7f0d0000)


        buffer.putInt(androidNsIndex)
        buffer.putInt(nameAttrIndex)
        buffer.putInt(nameValueIndex)
        buffer.putShort(8)
        buffer.put(0)
        buffer.put(0x03)
        buffer.putInt(nameValueIndex)


        buffer.putInt(androidNsIndex)
        buffer.putInt(exportedAttrIndex)
        buffer.putInt(-1)
        buffer.putShort(8)
        buffer.put(0)
        buffer.put(0x12)
        buffer.putInt(-1)


        buffer.putInt(androidNsIndex)
        buffer.putInt(targetActivityAttrIndex)
        buffer.putInt(targetActivityValueIndex)
        buffer.putShort(8)
        buffer.put(0)
        buffer.put(0x03)
        buffer.putInt(targetActivityValueIndex)

        return Chunk(CHUNK_START_ELEMENT, 0, chunkSize, buffer.array())
    }




    private fun buildSimpleStartElement(androidNsIndex: Int, elementNameIndex: Int, attrCount: Int): Chunk {
        val attrSize = 20
        val headerSize = 16
        val chunkSize = 36 + attrCount * attrSize

        val buffer = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)

        buffer.putShort(CHUNK_START_ELEMENT.toShort())
        buffer.putShort(headerSize.toShort())
        buffer.putInt(chunkSize)
        buffer.putInt(0)
        buffer.putInt(-1)
        buffer.putInt(-1)
        buffer.putInt(elementNameIndex)
        buffer.putShort(20)
        buffer.putShort(attrSize.toShort())
        buffer.putShort(attrCount.toShort())
        buffer.putShort(0)
        buffer.putShort(0)
        buffer.putShort(0)

        return Chunk(CHUNK_START_ELEMENT, 0, chunkSize, buffer.array())
    }




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
        buffer.putInt(-1)
        buffer.putInt(elementNameIndex)
        buffer.putShort(20)
        buffer.putShort(attrSize.toShort())
        buffer.putShort(attrCount.toShort())
        buffer.putShort(0)
        buffer.putShort(0)
        buffer.putShort(0)


        buffer.putInt(androidNsIndex)
        buffer.putInt(nameAttrIndex)
        buffer.putInt(nameValueIndex)
        buffer.putShort(8)
        buffer.put(0)
        buffer.put(0x03)
        buffer.putInt(nameValueIndex)

        return Chunk(CHUNK_START_ELEMENT, 0, chunkSize, buffer.array())
    }




    private fun buildEndElement(androidNsIndex: Int, elementNameIndex: Int): Chunk {
        val headerSize = 16
        val chunkSize = 24

        val buffer = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)

        buffer.putShort(CHUNK_END_ELEMENT.toShort())
        buffer.putShort(headerSize.toShort())
        buffer.putInt(chunkSize)
        buffer.putInt(0)
        buffer.putInt(-1)
        buffer.putInt(-1)
        buffer.putInt(elementNameIndex)

        return Chunk(CHUNK_END_ELEMENT, 0, chunkSize, buffer.array())
    }





    private fun updateStringIndicesAfterInsert(parsed: ParsedAxml, insertIndex: Int) {
        for (chunk in parsed.chunks) {
            when (chunk.type) {
                CHUNK_START_ELEMENT -> updateStartElementIndices(chunk, insertIndex)
                CHUNK_END_ELEMENT -> updateEndElementIndices(chunk, insertIndex)
                CHUNK_START_NAMESPACE, CHUNK_END_NAMESPACE -> updateNamespaceIndices(chunk, insertIndex)
            }
        }
    }




    private fun updateStartElementIndices(chunk: Chunk, insertIndex: Int) {
        val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)


        val nsOffset = 16
        val ns = buffer.getInt(nsOffset)
        if (ns >= insertIndex) {
            buffer.putInt(nsOffset, ns + 1)
        }


        val nameOffset = 20
        val name = buffer.getInt(nameOffset)
        if (name >= insertIndex) {
            buffer.putInt(nameOffset, name + 1)
        }


        val attrStart = buffer.getShort(24).toInt() and 0xFFFF
        val attrSize = buffer.getShort(26).toInt() and 0xFFFF
        val attrCount = buffer.getShort(28).toInt() and 0xFFFF


        for (i in 0 until attrCount) {
            val attrOffset = 16 + attrStart + i * attrSize


            val attrNs = buffer.getInt(attrOffset)
            if (attrNs >= insertIndex) {
                buffer.putInt(attrOffset, attrNs + 1)
            }



            val attrName = buffer.getInt(attrOffset + 4)
            if (attrName >= insertIndex) {
                buffer.putInt(attrOffset + 4, attrName + 1)
            }


            val rawValue = buffer.getInt(attrOffset + 8)
            if (rawValue >= 0 && rawValue >= insertIndex) {
                buffer.putInt(attrOffset + 8, rawValue + 1)
            }


            val valueType = buffer.get(attrOffset + 15).toInt() and 0xFF


            if (valueType == 0x03) {
                val valueData = buffer.getInt(attrOffset + 16)
                if (valueData >= insertIndex) {
                    buffer.putInt(attrOffset + 16, valueData + 1)
                }
            }
        }
    }




    private fun updateEndElementIndices(chunk: Chunk, insertIndex: Int) {
        val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)


        val ns = buffer.getInt(16)
        if (ns >= insertIndex) {
            buffer.putInt(16, ns + 1)
        }


        val name = buffer.getInt(20)
        if (name >= insertIndex) {
            buffer.putInt(20, name + 1)
        }
    }




    private fun updateNamespaceIndices(chunk: Chunk, insertIndex: Int) {
        val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)


        val prefix = buffer.getInt(16)
        if (prefix >= insertIndex) {
            buffer.putInt(16, prefix + 1)
        }


        val uri = buffer.getInt(20)
        if (uri >= insertIndex) {
            buffer.putInt(20, uri + 1)
        }
    }









    fun expandAndModify(axmlData: ByteArray, originalPackage: String, newPackage: String): ByteArray {
        return try {
            val parsed = parseAxml(axmlData)
            if (parsed == null) {
                AppLogger.e(TAG, "Failed to parse AXML")
                return axmlData
            }


            val expansions = findRelativeClassNames(parsed, originalPackage)
            AppLogger.d(TAG, "Found ${expansions.size} relative class names to expand")


            if (expansions.isNotEmpty()) {
                expandClassNames(parsed, expansions)
            }



            replacePackageString(parsed, originalPackage, newPackage)


            val result = rebuildAxml(parsed)

            AppLogger.d(TAG, "AXML rebuild complete: original=${axmlData.size}, new=${result.size}")
            result

        } catch (e: Exception) {
            AppLogger.e(TAG, "AXML rebuild failed", e)
            axmlData
        }
    }













    fun expandAndModifyFull(
        axmlData: ByteArray,
        originalPackage: String,
        newPackage: String,
        versionCode: Int,
        versionName: String,
        aliasCount: Int = 0,
        appName: String = "",
        deepLinkHosts: List<String> = emptyList(),
        permissions: List<String> = BASELINE_RUNTIME_PERMISSIONS
    ): ByteArray {
        return try {
            val parsed = parseAxml(axmlData)
            if (parsed == null) {
                AppLogger.e(TAG, "Failed to parse AXML for full modification")
                return axmlData
            }


            val expansions = findRelativeClassNames(parsed, originalPackage)
            AppLogger.d(TAG, "Found ${expansions.size} relative class names to expand")


            if (expansions.isNotEmpty()) {
                expandClassNames(parsed, expansions)
            }


            replacePackageString(parsed, originalPackage, newPackage)


            modifyVersionInfo(parsed, versionCode, versionName)


            stripTestOnlyFlag(parsed)


            ensureUsesPermissions(parsed, permissions)


            if (aliasCount > 0 && appName.isNotEmpty()) {
                addActivityAliases(parsed, newPackage, aliasCount, appName)
                AppLogger.d(TAG, "Added $aliasCount activity-alias entries for multi-launcher-icons")
            }



            rewireLauncherToShellActivity(parsed, addDirectLauncherToShell = aliasCount == 0)


            if (deepLinkHosts.isNotEmpty()) {
                addDeepLinkIntentFilter(parsed, deepLinkHosts)
                AppLogger.d(TAG, "Added deep link intent-filter for hosts: $deepLinkHosts")
            }


            val result = rebuildAxml(parsed)

            AppLogger.d(TAG, "AXML full rebuild complete: original=${axmlData.size}, new=${result.size}, aliases=$aliasCount, deepLinkHosts=${deepLinkHosts.size}")
            result

        } catch (e: Exception) {
            AppLogger.e(TAG, "AXML full rebuild failed", e)
            axmlData
        }
    }











    fun expandAndModifyWithVersion(
        axmlData: ByteArray,
        originalPackage: String,
        newPackage: String,
        versionCode: Int,
        versionName: String,
        permissions: List<String> = BASELINE_RUNTIME_PERMISSIONS
    ): ByteArray {
        return try {
            val parsed = parseAxml(axmlData)
            if (parsed == null) {
                AppLogger.e(TAG, "Failed to parse AXML for version modification")
                return axmlData
            }


            val expansions = findRelativeClassNames(parsed, originalPackage)
            AppLogger.d(TAG, "Found ${expansions.size} relative class names to expand")


            if (expansions.isNotEmpty()) {
                expandClassNames(parsed, expansions)
            }


            replacePackageString(parsed, originalPackage, newPackage)


            modifyVersionInfo(parsed, versionCode, versionName)


            stripTestOnlyFlag(parsed)


            ensureUsesPermissions(parsed, permissions)


            val result = rebuildAxml(parsed)

            AppLogger.d(TAG, "AXML rebuild with version complete: original=${axmlData.size}, new=${result.size}")
            result

        } catch (e: Exception) {
            AppLogger.e(TAG, "AXML rebuild with version failed", e)
            axmlData
        }
    }




    private fun modifyVersionInfo(parsed: ParsedAxml, versionCode: Int, versionName: String) {
        val resourceMap = parsed.resourceMap ?: return


        val versionCodeAttrIndex = resourceMap.indexOf(ATTR_VERSION_CODE)
        val versionNameAttrIndex = resourceMap.indexOf(ATTR_VERSION_NAME)

        AppLogger.d(TAG, "Version attr indices: versionCode=$versionCodeAttrIndex, versionName=$versionNameAttrIndex")


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


            val elementNameStr = parsed.stringPool.strings.getOrNull(elementName) ?: continue
            if (elementNameStr != "manifest") continue

            AppLogger.d(TAG, "Found manifest element with $attrCount attributes")


            for (i in 0 until attrCount) {
                val attrOffset = 36 + i * attrSize
                if (attrOffset + 20 > chunk.data.size) break

                buffer.position(attrOffset)
                val attrNs = buffer.int
                val attrName = buffer.int
                val attrRawValue = buffer.int
                val attrValueSize = buffer.short.toInt() and 0xFFFF
                buffer.get()
                val attrValueType = buffer.get().toInt() and 0xFF
                val attrValueData = buffer.int

                when (attrName) {
                    versionCodeAttrIndex -> {

                        if (attrValueType == 0x10) {
                            buffer.position(attrOffset + 16)
                            buffer.putInt(versionCode)
                            AppLogger.d(TAG, "Modified versionCode to $versionCode")
                        }
                    }
                    versionNameAttrIndex -> {

                        if (attrValueType == 0x03) {

                            var newIndex = parsed.stringPool.strings.indexOf(versionName)
                            if (newIndex < 0) {
                                newIndex = parsed.stringPool.strings.size
                                parsed.stringPool.strings.add(versionName)
                                AppLogger.d(TAG, "Added versionName string at index $newIndex: '$versionName'")
                            }


                            buffer.position(attrOffset + 8)
                            buffer.putInt(newIndex)


                            buffer.position(attrOffset + 16)
                            buffer.putInt(newIndex)

                            AppLogger.d(TAG, "Modified versionName to '$versionName' (index $newIndex)")
                        }
                    }
                }
            }

            break
        }
    }







    private fun stripTestOnlyFlag(parsed: ParsedAxml) {
        val resourceMap = parsed.resourceMap ?: return


        val testOnlyAttrIndex = resourceMap.indexOf(ATTR_TEST_ONLY)
        if (testOnlyAttrIndex < 0) {
            AppLogger.d(TAG, "testOnly attribute not found in resource map, skipping")
            return
        }

        AppLogger.d(TAG, "Found testOnly attr index: $testOnlyAttrIndex")


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


            val elementNameStr = parsed.stringPool.strings.getOrNull(elementName) ?: continue
            if (elementNameStr != "application") continue

            AppLogger.d(TAG, "Found application element with $attrCount attributes, checking for testOnly")


            for (i in 0 until attrCount) {
                val attrOffset = 36 + i * attrSize
                if (attrOffset + 20 > chunk.data.size) break

                buffer.position(attrOffset)
                val attrNs = buffer.int
                val attrName = buffer.int
                val attrRawValue = buffer.int
                val attrValueSize = buffer.short.toInt() and 0xFFFF
                buffer.get()
                val attrValueType = buffer.get().toInt() and 0xFF
                val attrValueData = buffer.int

                if (attrName == testOnlyAttrIndex) {
                    AppLogger.d(TAG, "Found testOnly attribute at index $i, type=0x${attrValueType.toString(16)}, value=$attrValueData")



                    if (attrValueType == 0x12 || attrValueType == 0x10) {
                        buffer.position(attrOffset + 16)
                        buffer.putInt(0)
                        AppLogger.d(TAG, "Set testOnly to false")
                    }
                }
            }
        }
    }




    private fun parseAxml(data: ByteArray): ParsedAxml? {
        if (data.size < 8) return null

        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        val fileType = buffer.short.toInt() and 0xFFFF
        val fileHeaderSize = buffer.short.toInt() and 0xFFFF
        val fileSize = buffer.int

        if (fileType != CHUNK_AXML_FILE) {
            AppLogger.e(TAG, "Not a valid AXML file: type=0x${fileType.toString(16)}")
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
            AppLogger.e(TAG, "String pool not found")
            return null
        }

        return ParsedAxml(fileHeaderSize, stringPool, resourceMap, chunks)
    }




    private fun parseStringPool(data: ByteArray, offset: Int): StringPool {
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(offset)

        buffer.short
        val headerSize = buffer.short.toInt() and 0xFFFF
        val chunkSize = buffer.int
        val stringCount = buffer.int
        val styleCount = buffer.int
        val flags = buffer.int
        val stringsStart = buffer.int
        val stylesStart = buffer.int

        val isUtf8 = (flags and 0x100) != 0


        val stringOffsets = IntArray(stringCount) { buffer.int }


        val styleOffsets = IntArray(styleCount) { buffer.int }


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


        val stylesData = if (styleCount > 0 && stylesStart > 0) {
            val stylesDataStart = offset + stylesStart
            val stylesDataEnd = offset + chunkSize
            data.copyOfRange(stylesDataStart, stylesDataEnd)
        } else {
            null
        }


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




    private fun parseResourceMap(data: ByteArray, offset: Int, size: Int): IntArray {
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(offset + 8)
        val count = (size - 8) / 4
        return IntArray(count) { buffer.int }
    }




    private fun findRelativeClassNames(parsed: ParsedAxml, originalPackage: String): List<ClassNameExpansion> {
        val expansions = mutableListOf<ClassNameExpansion>()
        val resourceMap = parsed.resourceMap ?: return expansions


        val nameAttrIndex = resourceMap.indexOf(ATTR_NAME)
        if (nameAttrIndex < 0) {
            AppLogger.d(TAG, "android:name attribute not found in resource map")
            return expansions
        }


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


            val elementNameStr = parsed.stringPool.strings.getOrNull(elementName) ?: continue
            if (elementNameStr !in listOf("activity", "service", "receiver", "provider", "application", "activity-alias")) {
                continue
            }


            for (i in 0 until attrCount) {
                val attrOffset = 36 + i * attrSize
                if (attrOffset + 20 > chunk.data.size) break

                buffer.position(attrOffset)
                val attrNs = buffer.int
                val attrName = buffer.int
                val attrRawValue = buffer.int
                val attrValueSize = buffer.short.toInt() and 0xFFFF
                buffer.get()
                val attrValueType = buffer.get().toInt() and 0xFF
                val attrValueData = buffer.int


                if (attrName != nameAttrIndex) continue


                if (attrValueType != 3) continue


                val stringIndex = attrValueData
                val stringValue = parsed.stringPool.strings.getOrNull(stringIndex) ?: continue


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

                    AppLogger.d(TAG, "Found relative class name: '$stringValue' -> '$absoluteName' in <$elementNameStr>")
                }
            }
        }

        return expansions
    }




    private fun expandClassNames(parsed: ParsedAxml, expansions: List<ClassNameExpansion>): ParsedAxml {
        val stringPool = parsed.stringPool

        for (expansion in expansions) {

            var newIndex = stringPool.strings.indexOf(expansion.expandedValue)

            if (newIndex < 0) {

                newIndex = stringPool.strings.size
                stringPool.strings.add(expansion.expandedValue)
                AppLogger.d(TAG, "Added new string at index $newIndex: '${expansion.expandedValue}'")
            }


            val chunk = parsed.chunks[expansion.chunkIndex]
            val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)


            buffer.position(expansion.attrOffset + 8)
            buffer.putInt(newIndex)


            buffer.position(expansion.attrOffset + 16)
            buffer.putInt(newIndex)
        }

        return parsed
    }






    private fun replacePackageString(parsed: ParsedAxml, oldPackage: String, newPackage: String) {
        val stringPool = parsed.stringPool

        for (i in stringPool.strings.indices) {
            val str = stringPool.strings[i]

            when {

                str == oldPackage -> {
                    stringPool.strings[i] = newPackage
                    AppLogger.d(TAG, "Replaced package at index $i: '$oldPackage' -> '$newPackage'")
                }



                str.startsWith("$oldPackage.") -> {

                    val suffix = str.substring(oldPackage.length + 1)
                    val isComponentClassName = isLikelyClassName(suffix)

                    if (isComponentClassName) {

                        AppLogger.d(TAG, "Skipped component class at index $i: '$str'")
                    } else {

                        val newStr = newPackage + str.substring(oldPackage.length)
                        stringPool.strings[i] = newStr
                        AppLogger.d(TAG, "Replaced prefixed string at index $i: '$str' -> '$newStr'")
                    }
                }
            }
        }
    }





    private fun isLikelyClassName(suffix: String): Boolean {

        val lastDotIndex = suffix.lastIndexOf('.')
        val className = if (lastDotIndex >= 0) {
            suffix.substring(lastDotIndex + 1)
        } else {
            suffix
        }


        if (className.isNotEmpty() && className[0].isUpperCase()) {

            val componentSuffixes = listOf(
                "Activity", "Service", "Provider", "Receiver",
                "Application", "Fragment", "Adapter", "View",
                "Manager", "Helper", "Listener", "Callback"
            )

            return componentSuffixes.any { className.endsWith(it) } ||
                   className.matches(CLASS_NAME_REGEX)
        }

        return false
    }




    private fun rebuildAxml(parsed: ParsedAxml): ByteArray {
        val output = ByteArrayOutputStream()


        val stringPoolData = rebuildStringPool(parsed.stringPool)


        val resourceMap = parsed.resourceMap
        val resourceMapData = if (resourceMap != null) {
            rebuildResourceMap(resourceMap)
        } else {
            ByteArray(0)
        }


        val chunksData = ByteArrayOutputStream()
        for (chunk in parsed.chunks) {
            chunksData.write(chunk.data)
        }


        val totalSize = parsed.fileHeaderSize + stringPoolData.size + resourceMapData.size + chunksData.size()


        val header = ByteBuffer.allocate(parsed.fileHeaderSize).order(ByteOrder.LITTLE_ENDIAN)
        header.putShort(CHUNK_AXML_FILE.toShort())
        header.putShort(parsed.fileHeaderSize.toShort())
        header.putInt(totalSize)
        output.write(header.array())


        output.write(stringPoolData)


        output.write(resourceMapData)


        chunksData.writeTo(output)

        return output.toByteArray()
    }




    private fun rebuildStringPool(pool: StringPool): ByteArray {
        val isUtf8 = pool.isUtf8
        val stringCount = pool.strings.size
        val styleCount = pool.styleOffsets.size


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


        while (stringsBuffer.size() % 4 != 0) {
            stringsBuffer.write(0)
        }

        val stringsData = stringsBuffer.toByteArray()


        val stringsDataSizeDelta = stringsData.size - pool.originalStringsDataSize


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


        val result = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)


        result.putShort(CHUNK_STRING_POOL.toShort())
        result.putShort(headerSize.toShort())
        result.putInt(chunkSize)
        result.putInt(stringCount)
        result.putInt(styleCount)

        val newFlags = pool.flags and 0x01.inv()
        result.putInt(newFlags)
        result.putInt(stringsStart)
        result.putInt(stylesStart)


        for (offset in stringOffsets) {
            result.putInt(offset)
        }


        for (offset in pool.styleOffsets) {
            result.putInt(offset + stringsDataSizeDelta)
        }


        result.put(stringsData)


        pool.stylesData?.let { result.put(it) }

        return result.array()
    }




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




    private fun simplePackageReplace(data: ByteArray, oldPackage: String, newPackage: String): ByteArray {
        val result = data.copyOf()


        replacePackageBytes(result, oldPackage, newPackage, Charsets.UTF_8)


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
            newBytes + ByteArray(oldBytes.size - newBytes.size)
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



    private fun readUtf8String(data: ByteArray, offset: Int): String {
        if (offset >= data.size) return ""
        var o = offset


        var charLen = data[o].toInt() and 0x7F
        if (data[o].toInt() and 0x80 != 0) {
            if (o + 1 >= data.size) return ""
            charLen = ((data[o].toInt() and 0x7F) shl 8) or (data[o + 1].toInt() and 0xFF)
            o += 2
        } else {
            o += 1
        }


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


        if (charLen > 0x7F) {
            output.write(0x80 or ((charLen shr 8) and 0x7F))
            output.write(charLen and 0xFF)
        } else {
            output.write(charLen)
        }


        if (byteLen > 0x7F) {
            output.write(0x80 or ((byteLen shr 8) and 0x7F))
            output.write(byteLen and 0xFF)
        } else {
            output.write(byteLen)
        }


        output.write(bytes)


        output.write(0)
    }

    private fun writeUtf16String(output: ByteArrayOutputStream, str: String) {
        val length = str.length


        if (length > 0x7FFF) {
            output.write(0x80 or ((length shr 24) and 0x7F))
            output.write((length shr 16) and 0xFF)
            output.write((length shr 8) and 0xFF)
            output.write(length and 0xFF)
        } else {
            output.write(length and 0xFF)
            output.write((length shr 8) and 0xFF)
        }


        val bytes = str.toByteArray(Charsets.UTF_16LE)
        output.write(bytes)


        output.write(0)
        output.write(0)
    }



    private data class ParsedAxml(
        val fileHeaderSize: Int,
        val stringPool: StringPool,
        var resourceMap: IntArray?,
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
