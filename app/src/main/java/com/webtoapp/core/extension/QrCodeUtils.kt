package com.webtoapp.core.extension

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * 二维码工具类
 */
object QrCodeUtils {
    
    // L 级别纠错容量约 4296 字节，M 级别约 2953 字节
    // 使用 L 级别可以容纳更大的模块数据
    private const val QR_MAX_BYTES = 4200
    
    /**
     * 生成二维码 Bitmap
     * @param content 要编码的内容
     * @param size 二维码尺寸（像素）
     * @param margin 边距
     * @return 二维码 Bitmap，失败返回 null
     */
    fun generateQrCode(
        content: String,
        size: Int = 512,
        margin: Int = 1
    ): Bitmap? {
        return try {
            // 使用 L 级别纠错，容量更大（约 4296 字节）
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
                EncodeHintType.MARGIN to margin
            )
            
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            
            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    pixels[y * size + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, size, 0, 0, size, size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 生成带颜色的二维码
     * @param content 要编码的内容
     * @param size 二维码尺寸
     * @param foregroundColor 前景色（二维码点的颜色）
     * @param backgroundColor 背景色
     */
    fun generateColoredQrCode(
        content: String,
        size: Int = 512,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE,
        margin: Int = 1
    ): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
                EncodeHintType.MARGIN to margin
            )
            
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            
            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    pixels[y * size + x] = if (bitMatrix[x, y]) foregroundColor else backgroundColor
                }
            }
            
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, size, 0, 0, size, size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 检查内容是否可以生成二维码（长度限制）
     * 二维码有容量限制，取决于纠错级别
     * L 级别约可容纳 4296 字节
     */
    fun canGenerateQrCode(content: String): Boolean {
        return content.toByteArray(Charsets.UTF_8).size <= QR_MAX_BYTES
    }
    
    /**
     * 获取内容大小信息
     */
    fun getContentSize(content: String): Int {
        return content.toByteArray(Charsets.UTF_8).size
    }
    
    /**
     * 从 Bitmap 解析二维码
     * @param bitmap 包含二维码的图片
     * @return 解析出的内容，失败返回 null
     */
    fun decodeQrCode(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            
            val hints = mapOf(
                DecodeHintType.CHARACTER_SET to "UTF-8",
                DecodeHintType.TRY_HARDER to true
            )
            
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap, hints)
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
