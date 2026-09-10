package com.jisuanyusuiji.toolbox.tools.extra

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import java.io.ByteArrayOutputStream
import kotlin.math.max

private fun sizeText(bytes: Int): String = when {
    bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / 1024.0 / 1024.0)
    else -> String.format("%.1f KB", bytes / 1024.0)
}

@Composable
private fun PickImageButton(uri: Uri?, label: String = "选择图片", onPicked: (Uri?) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { onPicked(it) }
    OutlinedButton(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
        Text(if (uri == null) "📂 $label" else "✅ 已选择：${uri.lastPathSegment ?: "图片"}")
    }
}

// ============================================================
// 图片格式转换（jpg / png / bmp / tif / webp）
// ============================================================
@Composable
fun ImageFormatConvertTool() {
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var format by remember { mutableStateOf("jpg") }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    var lastOutputSize by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val bitmap = remember(uri) { uri?.let { decodeImage(context, it) } }

    fun convert() {
        val bmp = bitmap ?: run { error = "请先选择图片"; return }
        error = ""
        saved = null
        val bytes: ByteArray
        val mime: String
        val ext: String
        when (format) {
            "jpg" -> {
                val out = ByteArrayOutputStream(); bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                bytes = out.toByteArray(); mime = "image/jpeg"; ext = "jpg"
            }
            "png" -> {
                val out = ByteArrayOutputStream(); bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                bytes = out.toByteArray(); mime = "image/png"; ext = "png"
            }
            "webp" -> {
                val out = ByteArrayOutputStream(); bmp.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, out)
                bytes = out.toByteArray(); mime = "image/webp"; ext = "webp"
            }
            "bmp" -> { bytes = encodeBmp(bmp); mime = "image/bmp"; ext = "bmp" }
            else -> { bytes = encodeTiff(bmp); mime = "image/tiff"; ext = "tif" }
        }
        saved = saveMedia(context, bytes, mime, "IMG_${System.currentTimeMillis()}.$ext")
        lastOutputSize = "输出大小：${sizeText(bytes.size)}"
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "图片格式转换") {
            PickImageButton(uri) { uri = it }
            if (bitmap != null) {
                Spacer(Modifier.height(10.dp))
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "原图",
                    modifier = Modifier.fillMaxWidth().height(220.dp).background(Color.Black)
                )
                Text("尺寸：${bitmap.width} × ${bitmap.height}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            Text("输出格式", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            ChoiceChips(listOf("jpg", "png", "bmp", "tif", "webp"), format, { format = it }, { it.uppercase() })
            Spacer(Modifier.height(12.dp))
            Button(onClick = { convert() }, enabled = bitmap != null, modifier = Modifier.fillMaxWidth()) { Text("开始转换") }
        }
        ErrorText(error)
        val converted = saved
        if (converted != null) {
            SectionCard(title = "转换结果（已直接预览）") {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "转换结果",
                        modifier = Modifier.fillMaxWidth().height(220.dp).background(Color.Black)
                    )
                }
                if (lastOutputSize.isNotBlank()) Text(lastOutputSize, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                MediaResultActions(converted)
            }
        }
        Text("提示：TIF 输出为未压缩格式；TIF 输入需先用其他应用转为常见格式。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

// ============================================================
// 修改图片尺寸
// ============================================================
@Composable
fun ImageResizeTool() {
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var widthText by remember { mutableStateOf("1080") }
    var heightText by remember { mutableStateOf("1920") }
    var format by remember { mutableStateOf("jpg") }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf("") }
    val bitmap = remember(uri) { uri?.let { decodeImage(context, it) } }

    fun run() {
        val bmp = bitmap ?: run { error = "请先选择图片"; return }
        val w = widthText.toIntOrNull() ?: run { error = "宽度必须是整数"; return }
        val h = heightText.toIntOrNull() ?: run { error = "高度必须是整数"; return }
        if (w <= 0 || h <= 0 || w > 12000 || h > 12000) { error = "尺寸需在 1~12000 之间"; return }
        error = ""
        val scaled = Bitmap.createScaledBitmap(bmp, w, h, true)
        resultBitmap = scaled
        val mime = if (format == "png") "image/png" else "image/jpeg"
        val compress = if (format == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        saved = saveBitmapMedia(context, scaled, mime, compress, 92, "RESIZE_${System.currentTimeMillis()}.$format")
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "修改图片尺寸") {
            PickImageButton(uri) { uri = it }
            bitmap?.let {
                Spacer(Modifier.height(8.dp))
                Text("原尺寸：${it.width} × ${it.height}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            LabeledField(widthText, { widthText = it }, "目标宽度（px）")
            Spacer(Modifier.height(8.dp))
            LabeledField(heightText, { heightText = it }, "目标高度（px）")
            Spacer(Modifier.height(8.dp))
            ChoiceChips(listOf("jpg", "png"), format, { format = it }, { it.uppercase() })
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, enabled = bitmap != null, modifier = Modifier.fillMaxWidth()) { Text("调整尺寸并保存") }
        }
        ErrorText(error)
        val result = saved
        if (result != null) {
            SectionCard(title = "调整结果") {
                resultBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "调整后",
                        modifier = Modifier.fillMaxWidth().height(220.dp).background(Color.Black)
                    )
                    Text("新尺寸：${it.width} × ${it.height}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(10.dp))
                MediaResultActions(result)
            }
        }
    }
}

// ============================================================
// 证件照尺寸调整
// ============================================================
@Composable
fun IdPhotoTool() {
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    val presets = listOf(
        "一寸 295×413" to (295 to 413),
        "小一寸 260×378" to (260 to 378),
        "大一寸 390×567" to (390 to 567),
        "二寸 413×579" to (413 to 579),
        "小二寸 358×441" to (358 to 441),
        "护照 390×567" to (390 to 567)
    )
    var preset by remember { mutableStateOf(presets.first().first) }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf("") }
    val bitmap = remember(uri) { uri?.let { decodeImage(context, it) } }

    fun centerCrop(src: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val targetRatio = targetW.toFloat() / targetH
        var cropW = src.width.toFloat()
        var cropH = src.height.toFloat()
        if (cropW / cropH > targetRatio) cropW = cropH * targetRatio else cropH = cropW / targetRatio
        val left = ((src.width - cropW) / 2).toInt().coerceAtLeast(0)
        val top = ((src.height - cropH) / 2).toInt().coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(src, left, top, cropW.toInt(), cropH.toInt())
        return Bitmap.createScaledBitmap(cropped, targetW, targetH, true)
    }

    fun run() {
        val bmp = bitmap ?: run { error = "请先选择照片"; return }
        error = ""
        val (w, h) = presets.first { it.first == preset }.second
        val result = centerCrop(bmp, w, h)
        resultBitmap = result
        saved = saveBitmapMedia(context, result, "image/jpeg", Bitmap.CompressFormat.JPEG, 95, "IDPHOTO_${System.currentTimeMillis()}.jpg")
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "证件照尺寸调整") {
            PickImageButton(uri) { uri = it }
            Spacer(Modifier.height(10.dp))
            Text("选择规格", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            ChoiceChips(presets.map { it.first }, preset, { preset = it }, { it })
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, enabled = bitmap != null, modifier = Modifier.fillMaxWidth()) { Text("生成证件照") }
        }
        ErrorText(error)
        val idResult = saved
        if (idResult != null) {
            SectionCard(title = "证件照结果") {
                resultBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "证件照",
                        modifier = Modifier.fillMaxWidth().height(260.dp).background(Color.Black)
                    )
                    Text("输出尺寸：${it.width} × ${it.height} px", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(10.dp))
                MediaResultActions(idResult)
            }
        }
        Text("说明：自动居中裁剪为对应比例并缩放；换底色需要抠图，请用专业工具。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

// ============================================================
// 图片压缩
// ============================================================
@Composable
fun ImageCompressTool() {
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var quality by remember { mutableStateOf(70f) }
    var maxSize by remember { mutableStateOf("1920") }
    var format by remember { mutableStateOf("jpg") }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var outputInfo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val bitmap = remember(uri) { uri?.let { decodeImage(context, it) } }

    fun run() {
        val bmp = bitmap ?: run { error = "请先选择图片"; return }
        error = ""
        var out = bmp
        val max = maxSize.toIntOrNull()
        if (max != null && max > 0 && max(bmp.width, bmp.height) > max) {
            val scale = max.toFloat() / max(bmp.width, bmp.height)
            out = Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
        }
        val bytes = ByteArrayOutputStream()
        if (format == "jpg") out.compress(Bitmap.CompressFormat.JPEG, quality.toInt(), bytes)
        else out.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality.toInt(), bytes)
        val mime = if (format == "jpg") "image/jpeg" else "image/webp"
        resultBitmap = out
        outputInfo = "输出尺寸：${out.width} × ${out.height} · 大小：${sizeText(bytes.size())}"
        saved = saveMedia(context, bytes.toByteArray(), mime, "COMPRESS_${System.currentTimeMillis()}.$format")
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "图片压缩") {
            PickImageButton(uri) { uri = it }
            bitmap?.let {
                Spacer(Modifier.height(8.dp))
                Text("原尺寸：${it.width} × ${it.height}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            Text("压缩质量：${quality.toInt()}%")
            Slider(value = quality, onValueChange = { quality = it }, valueRange = 5f..100f)
            LabeledField(maxSize, { maxSize = it }, "最大边长（px，0 或空 = 不缩放）")
            Spacer(Modifier.height(8.dp))
            ChoiceChips(listOf("jpg", "webp"), format, { format = it }, { it.uppercase() })
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, enabled = bitmap != null, modifier = Modifier.fillMaxWidth()) { Text("压缩并保存") }
        }
        ErrorText(error)
        val compressResult = saved
        if (compressResult != null) {
            SectionCard(title = "压缩结果") {
                resultBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "压缩后",
                        modifier = Modifier.fillMaxWidth().height(220.dp).background(Color.Black)
                    )
                    Text(outputInfo, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(10.dp))
                MediaResultActions(compressResult)
            }
        }
    }
}

// ============================================================
// 图片裁剪（拖动 + 比例 + 缩放）
// ============================================================
@Composable
fun ImageCropTool() {
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var aspect by remember { mutableStateOf("自由") }
    var zoom by remember { mutableStateOf(0.7f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var cropped by remember { mutableStateOf<Bitmap?>(null) }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    var error by remember { mutableStateOf("") }
    val bitmap = remember(uri) { uri?.let { decodeImage(context, it) } }

    val ratio = when (aspect) {
        "1:1" -> 1f
        "3:4" -> 3f / 4f
        "4:3" -> 4f / 3f
        "16:9" -> 16f / 9f
        else -> null
    }

    fun crop() {
        val bmp = bitmap ?: run { error = "请先选择图片"; return }
        val cw = containerSize.width.toFloat()
        val ch = containerSize.height.toFloat()
        if (cw <= 0 || ch <= 0) { error = "预览尚未准备好"; return }
        val scale = minOf(cw / bmp.width, ch / bmp.height)
        val dispW = bmp.width * scale
        val dispH = bmp.height * scale
        val left = (cw - dispW) / 2f
        val top = (ch - dispH) / 2f
        var cropH = minOf(dispW, dispH) * zoom
        var cropW = cropH * (ratio ?: (dispW / dispH))
        if (cropW > dispW) { cropW = dispW; if (ratio != null) cropH = cropW / ratio }
        if (cropH > dispH) { cropH = dispH; cropW = cropH * (ratio ?: 1f) }
        val moveX = (dispW - cropW) / 2f
        val moveY = (dispH - cropH) / 2f
        val cropLeft = left + dispW / 2f - cropW / 2f + offsetX * moveX * 2f
        val cropTop = top + dispH / 2f - cropH / 2f + offsetY * moveY * 2f
        val srcX = ((cropLeft - left) / dispW * bmp.width).toInt().coerceIn(0, bmp.width - 1)
        val srcY = ((cropTop - top) / dispH * bmp.height).toInt().coerceIn(0, bmp.height - 1)
        val srcW = (cropW / dispW * bmp.width).toInt().coerceAtLeast(1)
        val srcH = (cropH / dispH * bmp.height).toInt().coerceAtLeast(1)
        cropped = Bitmap.createBitmap(bmp, srcX, srcY, minOf(srcW, bmp.width - srcX), minOf(srcH, bmp.height - srcY))
        saved = null
    }

    fun saveCrop() {
        val c = cropped ?: run { error = "请先裁剪"; return }
        saved = saveBitmapMedia(context, c, "image/jpeg", Bitmap.CompressFormat.JPEG, 95, "CROP_${System.currentTimeMillis()}.jpg")
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "图片裁剪") {
            PickImageButton(uri) { uri = it }
            Spacer(Modifier.height(10.dp))
            ChoiceChips(listOf("自由", "1:1", "3:4", "4:3", "16:9"), aspect, { aspect = it }, { it })
            Spacer(Modifier.height(8.dp))
            Text("裁剪缩放：${(zoom * 100).toInt()}%")
            Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 0.2f..1f)
            if (bitmap != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .onSizeChanged { containerSize = it }
                        .background(Color.Black)
                        .pointerInput(bitmap, containerSize) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                offsetX = (offsetX + drag.x / containerSize.width).coerceIn(-0.5f, 0.5f)
                                offsetY = (offsetY + drag.y / containerSize.height).coerceIn(-0.5f, 0.5f)
                            }
                        }
                ) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        val cw = size.width
                        val ch = size.height
                        val scale = minOf(cw / bitmap.width, ch / bitmap.height)
                        val dispW = bitmap.width * scale
                        val dispH = bitmap.height * scale
                        val left = (cw - dispW) / 2f
                        val top = (ch - dispH) / 2f
                        drawImage(
                            image = bitmap.asImageBitmap(),
                            dstOffset = IntOffset(left.toInt(), top.toInt()),
                            dstSize = IntSize(dispW.toInt(), dispH.toInt())
                        )
                        var cropH = minOf(dispW, dispH) * zoom
                        var cropW = cropH * (ratio ?: (dispW / dispH))
                        if (cropW > dispW) { cropW = dispW; if (ratio != null) cropH = cropW / ratio }
                        if (cropH > dispH) { cropH = dispH; cropW = cropH * (ratio ?: 1f) }
                        val moveX = (dispW - cropW) / 2f
                        val moveY = (dispH - cropH) / 2f
                        val cropLeft = left + dispW / 2f - cropW / 2f + offsetX * moveX * 2f
                        val cropTop = top + dispH / 2f - cropH / 2f + offsetY * moveY * 2f
                        // 四周遮罩
                        drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(0f, 0f), size = Size(cw, cropTop))
                        drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(0f, cropTop + cropH), size = Size(cw, ch - cropTop - cropH))
                        drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(0f, cropTop), size = Size(cropLeft, cropH))
                        drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(cropLeft + cropW, cropTop), size = Size(cw - cropLeft - cropW, cropH))
                        drawRect(Color.White, topLeft = Offset(cropLeft, cropTop), size = Size(cropW, cropH), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { crop() }, modifier = Modifier.weight(1f)) { Text("裁剪") }
                    Button(onClick = { saveCrop() }, modifier = Modifier.weight(1f)) { Text("保存") }
                }
                cropped?.let {
                    Image(it.asImageBitmap(), "裁剪结果", Modifier.fillMaxWidth().height(180.dp))
                }
            }
        }
        ErrorText(error)
        val cropSaved = saved
        if (cropSaved != null) {
            SectionCard(title = "裁剪结果") {
                cropped?.let {
                    Image(it.asImageBitmap(), "裁剪结果", Modifier.fillMaxWidth().height(180.dp))
                    Text("尺寸：${it.width} × ${it.height}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(10.dp))
                MediaResultActions(cropSaved)
            }
        }
    }
}

// ============================================================
// 图片拼接（竖向 / 横向）
// ============================================================
@Composable
fun ImageStitchTool() {
    val context = LocalContext.current
    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var vertical by remember { mutableStateOf(true) }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris = it.take(9) }

    fun run() {
        if (uris.size < 2) { error = "请至少选择 2 张图片"; return }
        error = ""
        val bitmaps = uris.mapNotNull { decodeImage(context, it, 2048) }
        if (bitmaps.size < 2) { error = "部分图片无法读取"; return }
        val width = if (vertical) bitmaps.maxOf { it.width } else bitmaps.sumOf { it.width }
        val height = if (vertical) bitmaps.sumOf { it.height } else bitmaps.maxOf { it.height }
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        var pos = 0
        bitmaps.forEach { bmp ->
            if (vertical) { canvas.drawBitmap(bmp, 0f, pos.toFloat(), null); pos += bmp.height }
            else { canvas.drawBitmap(bmp, pos.toFloat(), 0f, null); pos += bmp.width }
        }
        resultBitmap = out
        saved = saveBitmapMedia(context, out, "image/jpeg", Bitmap.CompressFormat.JPEG, 92, "STITCH_${System.currentTimeMillis()}.jpg")
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "图片拼接") {
            Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (uris.isEmpty()) "📂 选择多张图片（最多 9 张）" else "✅ 已选择 ${uris.size} 张")
            }
            Spacer(Modifier.height(10.dp))
            ChoiceChips(listOf(true, false), vertical, { vertical = it }, { if (it) "竖向拼接" else "横向拼接" })
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, enabled = uris.size >= 2, modifier = Modifier.fillMaxWidth()) { Text("开始拼接") }
        }
        ErrorText(error)
        val stitchSaved = saved
        if (stitchSaved != null) {
            SectionCard(title = "拼接结果") {
                resultBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "拼接后",
                        modifier = Modifier.fillMaxWidth().height(260.dp).background(Color.Black)
                    )
                    Text("拼接后：${it.width} × ${it.height}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(10.dp))
                MediaResultActions(stitchSaved)
            }
        }
    }
}
