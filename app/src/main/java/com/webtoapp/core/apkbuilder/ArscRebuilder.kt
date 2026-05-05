package com.webtoapp.core.apkbuilder

import com.webtoapp.core.logging.AppLogger
import java.nio.ByteBuffer
import java.nio.ByteOrder










class ArscRebuilder {

    companion object {
        private const val TAG = "ArscRebuilder"


        private const val RES_TABLE_TYPE: Short = 0x0002
        private const val RES_STRING_POOL_TYPE: Short = 0x0001
        private const val RES_TABLE_PACKAGE_TYPE: Short = 0x0200


        private const val UTF8_FLAG = 0x00000100
    }








    fun rebuildWithNewAppName(arscData: ByteArray, targetAppName: String): ByteArray {
        return rebuildWithNewAppNameAndIcons(arscData, targetAppName, replaceIcons = false)
    }

















    fun rebuildWithNewAppNameAndIcons(
        arscData: ByteArray,
        targetAppName: String,
        replaceIcons: Boolean = false
    ): ByteArray {
        AppLogger.d(TAG, "rebuildWithNewAppName: target='$targetAppName', replaceIcons=$replaceIcons")

        try {
            val buffer = ByteBuffer.wrap(arscData).order(ByteOrder.LITTLE_ENDIAN)


            val tableType = buffer.short
            val tableHeaderSize = buffer.short
            val tableSize = buffer.int
            val packageCount = buffer.int

            if (tableType != RES_TABLE_TYPE) {
                AppLogger.e(TAG, "Invalid ARSC file: type=$tableType")
                return arscData
            }

            AppLogger.d(TAG, "Table header: size=$tableSize, packages=$packageCount")


            val stringPoolStart = tableHeaderSize.toInt()
            buffer.position(stringPoolStart)

            val poolType = buffer.short
            val poolHeaderSize = buffer.short
            val poolSize = buffer.int
            val stringCount = buffer.int
            val styleCount = buffer.int
            val poolFlags = buffer.int
            val stringsStart = buffer.int
            val stylesStart = buffer.int

            if (poolType != RES_STRING_POOL_TYPE) {
                AppLogger.e(TAG, "Invalid string pool: type=$poolType")
                return arscData
            }

            val isUtf8 = (poolFlags and UTF8_FLAG) != 0
            AppLogger.d(TAG, "String pool: count=$stringCount, utf8=$isUtf8, size=$poolSize")


            val stringOffsets = IntArray(stringCount)
            for (i in 0 until stringCount) {
                stringOffsets[i] = buffer.int
            }


            buffer.position(buffer.position() + styleCount * 4)

            val stringsDataStart = stringPoolStart + stringsStart
            val strings = mutableListOf<String>()

            for (i in 0 until stringCount) {
                val str = readStringAt(arscData, stringsDataStart + stringOffsets[i], isUtf8)
                strings.add(str)
            }

            val afterStringPool = stringPoolStart + poolSize
            val remainingData = arscData.copyOfRange(afterStringPool, arscData.size)




            if (replaceIcons) {
                val iconPaths = findIconPathIndices(remainingData, strings)
                _lastDiscoveredIconPaths = iconPaths.map { (idx, _) -> strings[idx] }.toSet()
                AppLogger.d(TAG, "Discovered old icon paths (for ZIP replacement): $_lastDiscoveredIconPaths")
            }


            var modified = false
            val appNamePatterns = listOf(
                "WebToApp - Convert Any Website to Android App",
                "WebToApp"
            )

            for (i in strings.indices) {
                val str = strings[i]

                for (pattern in appNamePatterns) {
                    if (str.startsWith(pattern) && !str.contains("/") && !str.contains(".")) {
                        AppLogger.d(TAG, "Found app_name at index $i: '$str' -> '$targetAppName'")
                        strings[i] = targetAppName
                        modified = true
                        break
                    }
                }
                if (modified) break
            }

            if (!modified) {
                AppLogger.w(TAG, "app_name not found in string pool, trying pattern matching...")

                for (i in strings.indices) {
                    val str = strings[i]
                    if (str.trim().startsWith("WebToApp") &&
                        str.length < 100 &&
                        !str.contains("Theme") &&
                        !str.contains("http") &&
                        !str.contains(".")) {
                        AppLogger.d(TAG, "Found potential app_name at index $i: '$str'")
                        strings[i] = targetAppName
                        modified = true
                        break
                    }
                }
            }

            if (!modified) {
                AppLogger.e(TAG, "Could not find app_name to replace")
                if (!replaceIcons) return arscData

            }


            val newStringPool = buildStringPool(strings, isUtf8, styleCount, poolFlags)


            val newArsc = ByteBuffer.allocate(tableHeaderSize + newStringPool.size + remainingData.size)
                .order(ByteOrder.LITTLE_ENDIAN)


            newArsc.putShort(tableType)
            newArsc.putShort(tableHeaderSize)
            newArsc.putInt(tableHeaderSize + newStringPool.size + remainingData.size)
            newArsc.putInt(packageCount)


            newArsc.put(newStringPool)


            newArsc.put(remainingData)

            AppLogger.d(TAG, "Rebuild complete: old size=${arscData.size}, new size=${newArsc.array().size}")
            return newArsc.array()

        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to rebuild ARSC", e)
            return arscData
        }
    }


    private var _lastDiscoveredIconPaths = emptySet<String>()
    fun getLastDiscoveredIconPaths(): Set<String> = _lastDiscoveredIconPaths














    private fun findIconPathIndices(
        packageData: ByteArray,
        globalStrings: List<String>
    ): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()

        try {
            val buf = ByteBuffer.wrap(packageData).order(ByteOrder.LITTLE_ENDIAN)


            val pkgType = buf.short
            val pkgHeaderSize = buf.short.toInt() and 0xFFFF
            val pkgSize = buf.int
            val pkgId = buf.int

            if (pkgType != RES_TABLE_PACKAGE_TYPE) {
                AppLogger.w(TAG, "Not a package chunk: type=0x${pkgType.toString(16)}")
                return result
            }


            buf.position(buf.position() + 256)

            val typeStringsOffset = buf.int
            val lastPublicType = buf.int
            val keyStringsOffset = buf.int
            val lastPublicKey = buf.int


            val typeStrPoolPos = typeStringsOffset
            buf.position(typeStrPoolPos)
            val typeStrings = readStringPool(buf, packageData)


            buf.position(keyStringsOffset)
            val keyStrings = readStringPool(buf, packageData)

            AppLogger.d(TAG, "Package types: ${typeStrings.joinToString()}")



            val mipmapTypeId = typeStrings.indexOfFirst { it == "mipmap" } + 1
            val drawableTypeId = typeStrings.indexOfFirst { it == "drawable" } + 1


            val icLauncherKeyIdx = keyStrings.indexOf("ic_launcher")
            val icLauncherRoundKeyIdx = keyStrings.indexOf("ic_launcher_round")
            val icLauncherFgKeyIdx = keyStrings.indexOf("ic_launcher_foreground")

            AppLogger.d(TAG, "mipmapTypeId=$mipmapTypeId, drawableTypeId=$drawableTypeId")
            AppLogger.d(TAG, "keyIndices: ic_launcher=$icLauncherKeyIdx, round=$icLauncherRoundKeyIdx, foreground=$icLauncherFgKeyIdx")

            if (mipmapTypeId <= 0 && drawableTypeId <= 0) {
                AppLogger.w(TAG, "mipmap/drawable types not found in package")
                return result
            }




            var pos = pkgHeaderSize
            var typeChunkCount = 0
            var mipmapTypeChunkCount = 0
            var drawableTypeChunkCount = 0
            AppLogger.d(TAG, "Scanning from pos=$pos, packageData.size=${packageData.size}, pkgHeaderSize=$pkgHeaderSize")
            while (pos + 8 <= packageData.size) {
                buf.position(pos)
                val chunkType = buf.short.toInt() and 0xFFFF
                val chunkHeaderSize = buf.short.toInt() and 0xFFFF
                val chunkSize = buf.int

                if (chunkSize <= 0 || pos + chunkSize > packageData.size) {
                    AppLogger.d(TAG, "Chunk scan ended at pos=$pos: chunkSize=$chunkSize, remaining=${packageData.size - pos}")
                    break
                }


                if (chunkType == 0x0201) {
                    typeChunkCount++
                    val typeId = buf.get().toInt() and 0xFF
                    buf.get()
                    buf.short
                    val entryCount = buf.int
                    val entriesStart = buf.int

                    val isInteresting = typeId == mipmapTypeId || typeId == drawableTypeId
                    if (isInteresting) {
                        if (typeId == mipmapTypeId) mipmapTypeChunkCount++
                        if (typeId == drawableTypeId) drawableTypeChunkCount++
                        AppLogger.d(TAG, "Type chunk #$typeChunkCount: typeId=$typeId, entryCount=$entryCount, entriesStart=$entriesStart, chunkHeaderSize=$chunkHeaderSize, pos=$pos")
                    }



                    buf.position(pos + chunkHeaderSize)
                    val entryOffsets = IntArray(entryCount)
                    for (i in 0 until entryCount) {
                        entryOffsets[i] = buf.int
                    }


                    if (isInteresting) {

                        for (targetIdx in listOf(icLauncherKeyIdx, icLauncherRoundKeyIdx, icLauncherFgKeyIdx)) {
                            if (targetIdx >= 0 && targetIdx < entryCount) {
                                val offset = entryOffsets[targetIdx]
                                AppLogger.d(TAG, "  Target key $targetIdx offset in entry table: $offset (entryIdx maps to keyIdx)")
                            }
                        }
                    }

                    for (entryIdx in 0 until entryCount) {
                        if (entryOffsets[entryIdx] == -1) continue

                        val entryPos = pos + entriesStart + entryOffsets[entryIdx]
                        if (entryPos + 8 > packageData.size) continue

                        buf.position(entryPos)
                        val entrySize = buf.short.toInt() and 0xFFFF
                        val entryFlags = buf.short.toInt() and 0xFFFF
                        val entryKeyIndex = buf.int

                        val isComplex = (entryFlags and 0x0001) != 0
                        if (isComplex) {
                            if (isInteresting && (entryKeyIndex == icLauncherKeyIdx || entryKeyIndex == icLauncherRoundKeyIdx || entryKeyIndex == icLauncherFgKeyIdx)) {
                                AppLogger.d(TAG, "  Entry $entryIdx: keyIndex=$entryKeyIndex is COMPLEX (bag/map), skipping")
                            }
                            continue
                        }


                        if (entryPos + entrySize + 8 > packageData.size) continue
                        val valueSize = buf.short.toInt() and 0xFFFF
                        buf.get()
                        val valueType = buf.get().toInt() and 0xFF
                        val valueData = buf.int

                        if (isInteresting && (entryKeyIndex == icLauncherKeyIdx || entryKeyIndex == icLauncherRoundKeyIdx || entryKeyIndex == icLauncherFgKeyIdx)) {
                            val keyName = if (entryKeyIndex >= 0 && entryKeyIndex < keyStrings.size) keyStrings[entryKeyIndex] else "?"
                            val pathStr = if (valueType == 0x03 && valueData >= 0 && valueData < globalStrings.size) globalStrings[valueData] else "N/A"
                            AppLogger.d(TAG, "  Entry $entryIdx: keyIndex=$entryKeyIndex('$keyName'), valueType=0x${valueType.toString(16)}, valueData=$valueData, path='$pathStr'")
                        }


                        if (valueType != 0x03) continue

                        val globalStrIdx = valueData
                        if (globalStrIdx < 0 || globalStrIdx >= globalStrings.size) continue






                        if (typeId == mipmapTypeId) {
                            if (entryKeyIndex == icLauncherKeyIdx || entryKeyIndex == icLauncherRoundKeyIdx) {
                                val oldPath = globalStrings[globalStrIdx]
                                val keyName = if (entryKeyIndex >= 0 && entryKeyIndex < keyStrings.size) keyStrings[entryKeyIndex] else "?"
                                AppLogger.d(TAG, "Found mipmap/$keyName → '$oldPath' (adaptive icon XML, KEEPING)")
                            }
                        }



                        if (typeId == drawableTypeId) {
                            if (entryKeyIndex == icLauncherFgKeyIdx) {
                                val oldPath = globalStrings[globalStrIdx]
                                AppLogger.d(TAG, "Found drawable/ic_launcher_foreground → '$oldPath' (foreground image, REPLACING)")
                                result.add(globalStrIdx to oldPath)
                            }
                        }
                    }
                }

                pos += chunkSize
            }
            AppLogger.d(TAG, "Scan complete: typeChunks=$typeChunkCount, mipmapChunks=$mipmapTypeChunkCount, drawableChunks=$drawableTypeChunkCount, results=${result.size}")

        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to find icon path indices", e)
        }


        return result.distinctBy { it.first }
    }




    private fun readStringPool(buf: ByteBuffer, fullData: ByteArray): List<String> {
        val startPos = buf.position()
        val poolType = buf.short
        val poolHeaderSize = buf.short.toInt() and 0xFFFF
        val poolSize = buf.int
        val strCount = buf.int
        val styleCount = buf.int
        val flags = buf.int
        val strStart = buf.int


        val isUtf8 = (flags and UTF8_FLAG) != 0


        buf.position(startPos + poolHeaderSize)

        val offsets = IntArray(strCount)
        for (i in 0 until strCount) {
            offsets[i] = buf.int
        }

        val stringsDataStart = startPos + strStart
        val result = mutableListOf<String>()
        for (i in 0 until strCount) {
            result.add(readStringAt(fullData, stringsDataStart + offsets[i], isUtf8))
        }


        buf.position(startPos + poolSize)
        return result
    }




    private fun readStringAt(data: ByteArray, offset: Int, isUtf8: Boolean): String {
        return if (isUtf8) {
            readUtf8String(data, offset)
        } else {
            readUtf16String(data, offset)
        }
    }





    private fun readUtf8String(data: ByteArray, offset: Int): String {
        var pos = offset


        var charLen = data[pos].toInt() and 0xFF
        pos++
        if ((charLen and 0x80) != 0) {
            charLen = ((charLen and 0x7F) shl 8) or (data[pos].toInt() and 0xFF)
            pos++
        }


        var byteLen = data[pos].toInt() and 0xFF
        pos++
        if ((byteLen and 0x80) != 0) {
            byteLen = ((byteLen and 0x7F) shl 8) or (data[pos].toInt() and 0xFF)
            pos++
        }


        return String(data, pos, byteLen, Charsets.UTF_8)
    }





    private fun readUtf16String(data: ByteArray, offset: Int): String {
        var pos = offset


        var charLen = (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8)
        pos += 2

        if ((charLen and 0x8000) != 0) {
            val high = charLen and 0x7FFF
            val low = (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8)
            charLen = (high shl 16) or low
            pos += 2
        }


        val bytes = ByteArray(charLen * 2)
        System.arraycopy(data, pos, bytes, 0, charLen * 2)
        return String(bytes, Charsets.UTF_16LE)
    }




    private fun buildStringPool(strings: List<String>, isUtf8: Boolean, styleCount: Int, flags: Int): ByteArray {
        val stringCount = strings.size


        val stringDataList = mutableListOf<ByteArray>()
        for (str in strings) {
            val encoded = if (isUtf8) {
                encodeUtf8String(str)
            } else {
                encodeUtf16String(str)
            }
            stringDataList.add(encoded)
        }


        val headerSize = 28
        val offsetsSize = (stringCount + styleCount) * 4
        val stringsDataSize = stringDataList.sumOf { it.size }


        val stringsStart = headerSize + offsetsSize


        val alignedStringsDataSize = (stringsDataSize + 3) and 0x7FFFFFFC.toInt()

        val totalSize = stringsStart + alignedStringsDataSize

        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)


        buffer.putShort(RES_STRING_POOL_TYPE)
        buffer.putShort(headerSize.toShort())
        buffer.putInt(totalSize)
        buffer.putInt(stringCount)
        buffer.putInt(styleCount)
        buffer.putInt(flags)
        buffer.putInt(stringsStart)
        buffer.putInt(0)


        var currentOffset = 0
        for (data in stringDataList) {
            buffer.putInt(currentOffset)
            currentOffset += data.size
        }


        for (i in 0 until styleCount) {
            buffer.putInt(0)
        }


        for (data in stringDataList) {
            buffer.put(data)
        }


        while (buffer.position() < totalSize) {
            buffer.put(0)
        }

        return buffer.array()
    }




    private fun encodeUtf8String(str: String): ByteArray {
        val utf8Bytes = str.toByteArray(Charsets.UTF_8)
        val charLen = str.length
        val byteLen = utf8Bytes.size


        val charLenSize = if (charLen > 0x7F) 2 else 1
        val byteLenSize = if (byteLen > 0x7F) 2 else 1

        val result = ByteArray(charLenSize + byteLenSize + byteLen + 1)
        var pos = 0


        if (charLen > 0x7F) {
            result[pos++] = (0x80 or ((charLen shr 8) and 0x7F)).toByte()
            result[pos++] = (charLen and 0xFF).toByte()
        } else {
            result[pos++] = charLen.toByte()
        }


        if (byteLen > 0x7F) {
            result[pos++] = (0x80 or ((byteLen shr 8) and 0x7F)).toByte()
            result[pos++] = (byteLen and 0xFF).toByte()
        } else {
            result[pos++] = byteLen.toByte()
        }


        System.arraycopy(utf8Bytes, 0, result, pos, byteLen)
        pos += byteLen


        result[pos] = 0

        return result
    }




    private fun encodeUtf16String(str: String): ByteArray {
        val utf16Bytes = str.toByteArray(Charsets.UTF_16LE)
        val charLen = str.length


        val lenSize = if (charLen > 0x7FFF) 4 else 2

        val result = ByteArray(lenSize + utf16Bytes.size + 2)
        var pos = 0


        if (charLen > 0x7FFF) {
            val high = 0x8000 or ((charLen shr 16) and 0x7FFF)
            val low = charLen and 0xFFFF
            result[pos++] = (high and 0xFF).toByte()
            result[pos++] = ((high shr 8) and 0xFF).toByte()
            result[pos++] = (low and 0xFF).toByte()
            result[pos++] = ((low shr 8) and 0xFF).toByte()
        } else {
            result[pos++] = (charLen and 0xFF).toByte()
            result[pos++] = ((charLen shr 8) and 0xFF).toByte()
        }


        System.arraycopy(utf16Bytes, 0, result, pos, utf16Bytes.size)
        pos += utf16Bytes.size


        result[pos++] = 0
        result[pos] = 0

        return result
    }
}
