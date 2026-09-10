package com.jisuanyusuiji.toolbox.tools.extra

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

/** 从 Uri 解码图片，自动处理 EXIF 旋转与超大图缩放。 */
fun decodeImage(context: Context, uri: Uri, maxSize: Int = 4096): Bitmap? = try {
    var bmp: Bitmap? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        bmp = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.isMutableRequired = false
        }
    } else {
        val stream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
        stream.use { bmp = BitmapFactory.decodeStream(it) }
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            context.contentResolver.openInputStream(uri)?.use { ins ->
                orientation = ExifInterface(ins).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (_: Exception) {
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        if (!matrix.isIdentity && bmp != null) {
            bmp = Bitmap.createBitmap(bmp!!, 0, 0, bmp!!.width, bmp!!.height, matrix, true)
        }
    }
    bmp?.let {
        if (max(it.width, it.height) > maxSize) {
            val scale = maxSize.toFloat() / max(it.width, it.height)
            Bitmap.createScaledBitmap(it, (it.width * scale).toInt(), (it.height * scale).toInt(), true)
        } else it
    }
} catch (_: Exception) { null }

/** 保存 Bitmap 到相册（API 29+ 走 MediaStore，低版本存应用图片目录）。 */
fun saveBitmapToGallery(
    context: Context,
    bitmap: Bitmap,
    mimeType: String,
    format: Bitmap.CompressFormat,
    quality: Int = 90,
    displayName: String
): String {
    val bytes = ByteArrayOutputStream()
    bitmap.compress(format, quality, bytes)
    return saveBytesToGallery(context, bytes.toByteArray(), mimeType, displayName)
}

/** 保存字节（如 PDF）到相册/Download 或应用目录。 */
fun saveBytesToGallery(context: Context, bytes: ByteArray, mimeType: String, displayName: String): String {
    val subDir = when {
        mimeType.contains("pdf") -> Environment.DIRECTORY_DOCUMENTS
        mimeType.startsWith("audio") -> Environment.DIRECTORY_MUSIC
        mimeType.startsWith("video") -> Environment.DIRECTORY_MOVIES
        else -> Environment.DIRECTORY_PICTURES
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$subDir/Toolbox")
        }
        val collection = when {
            mimeType.contains("pdf") -> MediaStore.Files.getContentUri("external")
            mimeType.startsWith("audio") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("video") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val uri = context.contentResolver.insert(collection, values)
            ?: return "保存失败：无法创建媒体文件"
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: return "保存失败：无法写入"
        return "已保存到 $subDir/Toolbox"
    } else {
        val dir = context.getExternalFilesDir(subDir)
        val file = File(dir, displayName)
        file.outputStream().use { it.write(bytes) }
        return "已保存：${file.absolutePath}"
    }
}

/** 分享文件（FileProvider）。 */
fun shareFile(context: Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "分享文件"))
    } catch (_: Exception) {
    }
}

/** 手动编码 24 位 BMP（Android 不提供 BMP 输出）。 */
fun encodeBmp(bitmap: Bitmap): ByteArray {
    val w = bitmap.width
    val h = bitmap.height
    val rowSize = (w * 3 + 3) / 4 * 4
    val dataSize = rowSize * h
    val fileSize = 54 + dataSize
    val out = ByteBuffer.allocate(fileSize).order(ByteOrder.LITTLE_ENDIAN)
    out.put('B'.code.toByte()).put('M'.code.toByte())
    out.putInt(fileSize)
    out.putShort(0).putShort(0)
    out.putInt(54)
    out.putInt(40)
    out.putInt(w).putInt(h)
    out.putShort(1)
    out.putShort(24)
    out.putInt(0)
    out.putInt(dataSize)
    out.putInt(2835).putInt(2835)
    out.putInt(0).putInt(0)
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    for (y in h - 1 downTo 0) {
        for (x in 0 until w) {
            val p = pixels[y * w + x]
            out.put((p and 0xFF).toByte())
            out.put(((p shr 8) and 0xFF).toByte())
            out.put(((p shr 16) and 0xFF).toByte())
        }
        for (pad in 0 until rowSize - w * 3) out.put(0)
    }
    return out.array()
}

/** 手动编码未压缩 RGB TIFF（Little-Endian）。 */
fun encodeTiff(bitmap: Bitmap): ByteArray {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    val imageData = ByteArray(w * h * 3)
    var p = 0
    for (i in 0 until w * h) {
        val c = pixels[i]
        imageData[p++] = (c and 0xFF).toByte()
        imageData[p++] = ((c shr 8) and 0xFF).toByte()
        imageData[p++] = ((c shr 16) and 0xFF).toByte()
    }

    val headerSize = 8
    val entryCount = 11
    val ifdSize = 2 + entryCount * 12 + 4
    val extraDataSize = 6 + 8 + 8          // BitsPerSample + XResolution + YResolution
    val dataOffset = headerSize + ifdSize
    val imageOffset = dataOffset + extraDataSize
    val total = imageOffset + imageData.size
    val buf = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
    buf.put('I'.code.toByte()).put('I'.code.toByte())
    buf.putShort(42)
    buf.putInt(8)
    buf.putShort(entryCount.toShort())

    fun entry(tag: Int, type: Int, count: Int, value: Int) {
        buf.putShort(tag.toShort())
        buf.putShort(type.toShort())
        buf.putInt(count)
        buf.putInt(value)
    }

    entry(256, 4, 1, w)                 // ImageWidth
    entry(257, 4, 1, h)                 // ImageLength
    entry(258, 3, 3, dataOffset)        // BitsPerSample 8,8,8
    entry(259, 3, 1, 1)                 // Compression = none
    entry(262, 3, 1, 2)                 // Photometric = RGB
    entry(273, 4, 1, imageOffset)       // StripOffsets
    entry(277, 3, 1, 3)                 // SamplesPerPixel
    entry(278, 4, 1, h)                 // RowsPerStrip
    entry(279, 4, 1, imageData.size)    // StripByteCounts
    entry(282, 5, 1, dataOffset + 6)    // XResolution 72/1
    entry(283, 5, 1, dataOffset + 14)   // YResolution 72/1
    buf.putInt(0)                       // next IFD

    buf.putShort(8).putShort(8).putShort(8)
    buf.putInt(72).putInt(1)
    buf.putInt(72).putInt(1)
    buf.put(imageData)
    return buf.array()
}
