package com.jisuanyusuiji.toolbox.tools.extra

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import java.io.File

private fun generateQrBitmap(text: String, size: Int): Bitmap? {
    if (text.isBlank()) return null
    return try {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    } catch (_: Exception) {
        null
    }
}

private fun saveToGallery(context: Context, bitmap: Bitmap): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "QR_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Toolbox")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return "保存失败：无法创建媒体文件"
        context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: return "保存失败：无法写入"
        "已保存到相册 Pictures/Toolbox"
    } else {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return "保存失败"
        val file = File(dir, "QR_${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        "已保存：${file.absolutePath}"
    }
}

private fun shareQr(context: Context, bitmap: Bitmap) {
    try {
        val dir = File(context.cacheDir, "share")
        dir.mkdirs()
        val file = File(dir, "qr_share.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享二维码"))
    } catch (_: Exception) {
    }
}

@Composable
fun QrCodeTool() {
    val context = LocalContext.current
    var text by remember { mutableStateOf("https://www.example.com") }
    var size by remember { mutableStateOf(600) }
    var batchMode by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val bitmap = remember(text, size) { generateQrBitmap(text, size) }
    val batchItems = remember(text, size) {
        if (batchMode) {
            text.lines().map { it.trim() }.filter { it.isNotEmpty() }.take(20)
                .mapNotNull { label -> generateQrBitmap(label, size)?.let { label to it } }
        } else emptyList()
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "二维码内容（本地生成，不上传）") {
            LabeledField(
                value = text,
                onChange = { text = it },
                label = if (batchMode) "批量模式：每行生成一个二维码（最多 20 个）" else "文本 / 网址",
                singleLine = false,
                minLines = 3
            )
            Spacer(Modifier.height(10.dp))
            ChoiceChips(
                options = listOf(300, 600, 900),
                selected = size,
                onSelect = { size = it },
                label = { "${it}×${it}" }
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("批量模式（每行一个二维码）", modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = batchMode, onCheckedChange = { batchMode = it })
            }
            if (text.length > 1000) ErrorText("内容过长，二维码可能无法生成")
        }

        if (batchMode && batchItems.isNotEmpty()) {
            SectionCard(title = "批量生成结果（${batchItems.size} 个）") {
                batchItems.chunked(2).forEach { rowItems ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowItems.forEach { (label, bmp) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "二维码：$label",
                                    modifier = Modifier.size(140.dp)
                                )
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    var ok = 0
                    batchItems.forEach { (_, bmp) ->
                        if (saveToGallery(context, bmp).startsWith("已保存")) ok++
                    }
                    message = "已保存 $ok / ${batchItems.size} 个二维码到相册"
                }, modifier = Modifier.fillMaxWidth()) { Text("💾 保存全部到相册") }
            }
        } else if (!batchMode) {
            SectionCard(title = "生成结果") {
                if (bitmap != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "二维码",
                            modifier = Modifier.size(280.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = {
                                message = saveToGallery(context, bitmap)
                            }, modifier = Modifier.weight(1f)) { Text("💾 保存") }
                            OutlinedButton(onClick = { shareQr(context, bitmap) }, modifier = Modifier.weight(1f)) {
                                Text("📤 分享")
                            }
                        }
                    }
                } else {
                    ErrorText(error.ifBlank { "请输入内容后自动生成" })
                }
            }
        }

        if (message.isNotBlank()) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        SectionCard(title = "提示") {
            Text("二维码完全在本机生成；保存到相册无需联网权限。", style = MaterialTheme.typography.bodySmall)
        }
    }
}
