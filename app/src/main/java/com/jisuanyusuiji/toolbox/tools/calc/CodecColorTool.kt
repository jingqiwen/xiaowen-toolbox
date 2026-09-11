package com.jisuanyusuiji.toolbox.tools.calc

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64
import kotlin.math.abs
import kotlin.math.roundToInt

// ============================================================
// 30. Base64 编码 / 解码
// ============================================================
@Composable
fun Base64Tool() {
    var input by remember { mutableStateOf("hello，我是温景淇") }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var lastAction by remember { mutableStateOf("") }

    fun encode() {
        error = ""
        try {
            output = Base64.getEncoder().encodeToString(input.toByteArray(Charsets.UTF_8))
            lastAction = "编码"
        } catch (e: Exception) {
            error = "编码失败：${e.message}"
        }
    }

    fun decode() {
        error = ""
        try {
            output = String(Base64.getDecoder().decode(input.trim()), Charsets.UTF_8)
            lastAction = "解码"
        } catch (_: Exception) {
            error = "解码失败：输入不是有效的 Base64 文本"
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "本地 Base64 编码 / 解码") {
            LabeledField(input, { input = it }, "输入文本或 Base64", singleLine = false, minLines = 4)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { encode() }, modifier = Modifier.weight(1f)) { Text("编码") }
                OutlinedButton(onClick = { decode() }, modifier = Modifier.weight(1f)) { Text("解码") }
            }
        }
        ErrorText(error)
        if (output.isNotBlank()) {
            SectionCard(title = "结果（${lastAction}）") {
                Text(output, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                CopyButton(output)
            }
        }
    }
}

// ============================================================
// 31. URL 编码 / 解码
// ============================================================
@Composable
fun UrlCodecTool() {
    var input by remember { mutableStateOf("https://example.com/搜索?q=随机&page=1") }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var lastAction by remember { mutableStateOf("") }

    fun encode() {
        error = ""
        try {
            output = URLEncoder.encode(input, "UTF-8")
            lastAction = "编码"
        } catch (e: Exception) {
            error = "编码失败：${e.message}"
        }
    }

    fun decode() {
        error = ""
        try {
            output = URLDecoder.decode(input, "UTF-8")
            lastAction = "解码"
        } catch (_: Exception) {
            error = "解码失败：输入格式不正确"
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "本地 URL 编码 / 解码") {
            LabeledField(input, { input = it }, "输入文本或 URL", singleLine = false, minLines = 3)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { encode() }, modifier = Modifier.weight(1f)) { Text("编码") }
                OutlinedButton(onClick = { decode() }, modifier = Modifier.weight(1f)) { Text("解码") }
            }
        }
        ErrorText(error)
        if (output.isNotBlank()) {
            SectionCard(title = "结果（${lastAction}）") {
                Text(output, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                CopyButton(output)
            }
        }
    }
}

// ============================================================
// 47. 颜色拾取器（RGB / HEX 转换 + 图片取色）
// ============================================================
private data class NamedColor(val name: String, val r: Int, val g: Int, val b: Int)

private val NAMED_COLORS = listOf(
    NamedColor("黑色", 0, 0, 0),
    NamedColor("深灰", 64, 64, 64),
    NamedColor("灰色", 128, 128, 128),
    NamedColor("浅灰", 200, 200, 200),
    NamedColor("白色", 255, 255, 255),
    NamedColor("象牙白", 255, 255, 240),
    NamedColor("米色", 245, 245, 220),
    NamedColor("奶油黄", 255, 253, 208),
    NamedColor("淡黄", 255, 255, 153),
    NamedColor("黄色", 255, 255, 0),
    NamedColor("金黄色", 255, 215, 0),
    NamedColor("柠檬黄", 255, 247, 0),
    NamedColor("土黄", 218, 165, 32),
    NamedColor("橙色", 255, 165, 0),
    NamedColor("深橙", 255, 140, 0),
    NamedColor("橙红", 255, 69, 0),
    NamedColor("珊瑚色", 255, 127, 80),
    NamedColor("红色", 255, 0, 0),
    NamedColor("猩红", 220, 20, 60),
    NamedColor("深红", 139, 0, 0),
    NamedColor("酒红", 128, 0, 32),
    NamedColor("粉红", 255, 192, 203),
    NamedColor("亮粉", 255, 105, 180),
    NamedColor("玫瑰红", 255, 0, 127),
    NamedColor("品红", 255, 0, 255),
    NamedColor("紫色", 128, 0, 128),
    NamedColor("深紫", 75, 0, 130),
    NamedColor("淡紫", 216, 191, 216),
    NamedColor("薰衣草紫", 230, 230, 250),
    NamedColor("靛蓝", 75, 0, 130),
    NamedColor("蓝紫色", 138, 43, 226),
    NamedColor("蓝色", 0, 0, 255),
    NamedColor("中蓝", 0, 0, 205),
    NamedColor("深蓝", 0, 0, 139),
    NamedColor("藏青", 25, 25, 112),
    NamedColor("天蓝", 135, 206, 235),
    NamedColor("海蓝", 0, 105, 148),
    NamedColor("钢蓝", 70, 130, 180),
    NamedColor("青色", 0, 255, 255),
    NamedColor("蓝绿", 0, 128, 128),
    NamedColor("深青", 0, 139, 139),
    NamedColor("薄荷绿", 189, 252, 201),
    NamedColor("亮绿", 0, 255, 0),
    NamedColor("春绿", 0, 255, 127),
    NamedColor("草绿", 124, 252, 0),
    NamedColor("黄绿", 154, 205, 50),
    NamedColor("绿色", 0, 128, 0),
    NamedColor("森林绿", 34, 139, 34),
    NamedColor("墨绿", 0, 100, 0),
    NamedColor("橄榄绿", 128, 128, 0),
    NamedColor("棕色", 165, 42, 42),
    NamedColor("巧克力色", 210, 105, 30),
    NamedColor("浅棕", 181, 101, 29),
    NamedColor("深棕", 101, 67, 33),
    NamedColor("小麦色", 245, 222, 179),
    NamedColor("卡其色", 195, 176, 145),
    NamedColor("桃色", 255, 218, 185),
    NamedColor("杏色", 251, 206, 177),
    NamedColor("石板灰", 112, 128, 144),
    NamedColor("灰蓝", 119, 136, 153),
    NamedColor("雪白", 255, 250, 250)
)

private fun nearestColorName(r: Int, g: Int, b: Int): String {
    var best = NAMED_COLORS[0]
    var bestDistance = Double.MAX_VALUE
    NAMED_COLORS.forEach { c ->
        val dr = (c.r - r).toDouble()
        val dg = (c.g - g).toDouble()
        val db = (c.b - b).toDouble()
        // 人眼对绿色更敏感，加权距离比直接欧氏距离更接近直觉
        val d = 2 * dr * dr + 4 * dg * dg + 3 * db * db
        if (d < bestDistance) {
            bestDistance = d
            best = c
        }
    }
    return best.name
}

private fun hslOf(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val l = (max + min) / 2f
    val d = max - min
    val s = if (d == 0f) 0f else d / (1f - abs(2f * l - 1f))
    val h = when {
        d == 0f -> 0f
        max == rf -> 60f * (((gf - bf) / d) % 6f)
        max == gf -> 60f * ((bf - rf) / d + 2f)
        else -> 60f * ((rf - gf) / d + 4f)
    }
    val hue = ((h % 360f) + 360f) % 360f
    return Triple(hue, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

private fun cmykOf(r: Int, g: Int, b: Int): FloatArray {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val k = 1f - maxOf(rf, gf, bf)
    if (k >= 1f) return floatArrayOf(0f, 0f, 0f, 1f)
    val c = (1f - rf - k) / (1f - k)
    val m = (1f - gf - k) / (1f - k)
    val y = (1f - bf - k) / (1f - k)
    return floatArrayOf(c, m, y, k)
}

/** WCAG 相对亮度（0=黑，1=白）。 */
private fun relativeLuminance(r: Int, g: Int, b: Int): Float {
    fun lin(v: Int): Float {
        val c = v / 255f
        return if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    }
    return 0.2126f * lin(r) + 0.7152f * lin(g) + 0.0722f * lin(b)
}

private fun contrastRatio(l1: Float, l2: Float): Float {
    val hi = maxOf(l1, l2)
    val lo = minOf(l1, l2)
    return (hi + 0.05f) / (lo + 0.05f)
}

private fun colorFamily(hue: Float, saturation: Float, value: Float): String {
    if (saturation < 0.08f) {
        return when {
            value > 0.93f -> "白色 / 近白"
            value < 0.12f -> "黑色 / 近黑"
            else -> "灰色"
        }
    }
    return when (hue) {
        in 0f..15f, in 345f..360f -> "红色系"
        in 15f..40f -> "橙色系"
        in 40f..65f -> "黄色系"
        in 65f..95f -> "黄绿色系"
        in 95f..150f -> "绿色系"
        in 150f..185f -> "青绿色系"
        in 185f..250f -> "蓝色系"
        in 250f..290f -> "紫色系"
        in 290f..330f -> "品红色系"
        else -> "粉红色系"
    }
}

private fun temperatureOf(hue: Float, saturation: Float): String = when {
    saturation < 0.10f -> "中性色"
    hue <= 70f || hue >= 290f -> "暖色调"
    hue in 140f..260f -> "冷色调"
    else -> "中性偏冷/偏暖"
}

private fun toneOf(saturation: Float, value: Float): String {
    val brightness = when {
        value < 0.28f -> "很暗"
        value < 0.55f -> "偏暗"
        value < 0.8f -> "中等明度"
        else -> "明亮"
    }
    if (saturation < 0.12f) return "$brightness、接近无彩色"
    val sat = when {
        saturation < 0.35f -> "低饱和"
        saturation < 0.7f -> "中等饱和"
        else -> "高饱和"
    }
    return "$brightness、$sat"
}

private data class ColorReport(
    val name: String,
    val family: String,
    val temperature: String,
    val tone: String,
    val hex: String,
    val rgb: String,
    val hsv: String,
    val hsl: String,
    val cmyk: String,
    val luminance: Float,
    val contrastWhite: Float,
    val contrastBlack: Float,
    val suggestText: String,
    val description: String
)

private fun analyzeColor(r: Int, g: Int, b: Int): ColorReport {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(AndroidColor.rgb(r, g, b), hsv)
    val (h, s, v) = Triple(hsv[0], hsv[1], hsv[2])
    val (hslH, hslS, hslL) = hslOf(r, g, b)
    val cmyk = cmykOf(r, g, b)
    val lum = relativeLuminance(r, g, b)
    val cw = contrastRatio(lum, 1f)
    val cb = contrastRatio(lum, 0f)
    val hex = "#%02X%02X%02X".format(r, g, b)
    val name = nearestColorName(r, g, b)
    val family = colorFamily(h, s, v)
    val temperature = temperatureOf(h, s)
    val tone = toneOf(s, v)
    val suggest = if (cb >= cw) "#000000（黑色文字）" else "#FFFFFF（白色文字）"
    val description = buildString {
        append("这是一种")
        append(tone)
        append("的")
        append(family)
        append("颜色（$temperature），最接近“$name”。")
        if (s < 0.10f) {
            append("几乎没有色彩倾向。")
        } else {
            append("色相约 ${h.roundToInt()}°，饱和度约 ${(s * 100).roundToInt()}%，明度约 ${(v * 100).roundToInt()}%。")
        }
        append(if (lum > 0.5f) "整体偏亮，" else "整体偏深，")
        append("适合搭配${if (cb >= cw) "黑色文字" else "白色文字"}。")
    }
    return ColorReport(
        name = name,
        family = family,
        temperature = temperature,
        tone = tone,
        hex = hex,
        rgb = "rgb($r, $g, $b)",
        hsv = "H ${h.roundToInt()}°  S ${(s * 100).roundToInt()}%  V ${(v * 100).roundToInt()}%",
        hsl = "H ${hslH.roundToInt()}°  S ${(hslS * 100).roundToInt()}%  L ${(hslL * 100).roundToInt()}%",
        cmyk = "C ${(cmyk[0] * 100).roundToInt()}%  M ${(cmyk[1] * 100).roundToInt()}%  Y ${(cmyk[2] * 100).roundToInt()}%  K ${(cmyk[3] * 100).roundToInt()}%",
        luminance = lum,
        contrastWhite = cw,
        contrastBlack = cb,
        suggestText = suggest,
        description = description
    )
}

/** 按屏幕缩放读取图片：限制最长边 2048，避免大图占用过多内存。 */
private fun decodeSampledBitmap(context: android.content.Context, uri: android.net.Uri): Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

private fun fitDisplayRect(w: Float, h: Float, bw: Int, bh: Int): android.graphics.RectF {
    val scale = minOf(w / bw, h / bh)
    val dw = bw * scale
    val dh = bh * scale
    val left = (w - dw) / 2f
    val top = (h - dh) / 2f
    return android.graphics.RectF(left, top, left + dw, top + dh)
}

@Composable
fun ColorPickerTool() {
    val context = LocalContext.current
    var red by remember { mutableStateOf(63f) }
    var green by remember { mutableStateOf(81f) }
    var blue by remember { mutableStateOf(181f) }
    var hexInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageError by remember { mutableStateOf("") }
    var tap by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var pickedPixel by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun toHex(v: Float): String = v.toInt().coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()
    val hex = "#${toHex(red)}${toHex(green)}${toHex(blue)}"
    val color = Color(AndroidColor.rgb(red.toInt(), green.toInt(), blue.toInt()))
    val rInt = red.toInt().coerceIn(0, 255)
    val gInt = green.toInt().coerceIn(0, 255)
    val bInt = blue.toInt().coerceIn(0, 255)
    val report = remember(rInt, gInt, bInt) { analyzeColor(rInt, gInt, bInt) }

    fun parseHex() {
        error = ""
        val text = hexInput.trim().removePrefix("#")
        if (text.length != 6) {
            error = "请输入 6 位十六进制颜色，如 3F51B5"
            return
        }
        val r = text.substring(0, 2).toIntOrNull(16)
        val g = text.substring(2, 4).toIntOrNull(16)
        val b = text.substring(4, 6).toIntOrNull(16)
        if (r == null || g == null || b == null) {
            error = "HEX 格式错误"
            return
        }
        red = r.toFloat(); green = g.toFloat(); blue = b.toFloat()
        tap = null
        pickedPixel = null
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        imageError = ""
        try {
            val decoded = decodeSampledBitmap(context, uri)
            if (decoded == null) {
                imageError = "图片读取失败，请换一张图片试试"
            } else {
                bitmap = decoded
                tap = null
                pickedPixel = null
            }
        } catch (e: Exception) {
            imageError = "图片读取失败：${e.message}"
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "颜色预览") {
            Box(
                Modifier.fillMaxWidth().height(110.dp).background(color, RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(10.dp))
            Text("$hex · ${report.name}", style = MaterialTheme.typography.titleLarge)
            Text(report.rgb, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            Text(report.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            CopyButton("$hex  ${report.rgb}  ${report.name}")
        }

        SectionCard(title = "🖼 从图片中的某个点取色") {
            Text(
                "选择一张图片，点一下图片上的任意位置，即可读取该点的像素颜色，并自动给出颜色名称与全部参数。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { pickImage.launch("image/*") }, modifier = Modifier.weight(1f)) {
                    Text(if (bitmap == null) "选择图片" else "重新选择图片")
                }
                if (bitmap != null) {
                    OutlinedButton(onClick = {
                        bitmap = null
                        tap = null
                        pickedPixel = null
                        imageError = ""
                    }) { Text("移除图片") }
                }
            }
            ErrorText(imageError)
            bitmap?.let { bmp ->
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x11000000))
                        .pointerInput(bmp) {
                            detectTapGestures { offset ->
                                val rect = fitDisplayRect(size.width.toFloat(), size.height.toFloat(), bmp.width, bmp.height)
                                if (offset.x >= rect.left && offset.x <= rect.right &&
                                    offset.y >= rect.top && offset.y <= rect.bottom
                                ) {
                                    val px = (((offset.x - rect.left) / rect.width()) * bmp.width).toInt().coerceIn(0, bmp.width - 1)
                                    val py = (((offset.y - rect.top) / rect.height()) * bmp.height).toInt().coerceIn(0, bmp.height - 1)
                                    val argb = bmp.getPixel(px, py)
                                    tap = offset
                                    pickedPixel = px to py
                                    red = AndroidColor.red(argb).toFloat()
                                    green = AndroidColor.green(argb).toFloat()
                                    blue = AndroidColor.blue(argb).toFloat()
                                }
                            }
                        }
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val rect = fitDisplayRect(size.width, size.height, bmp.width, bmp.height)
                        drawImage(
                            image = bmp.asImageBitmap(),
                            srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                            srcSize = androidx.compose.ui.unit.IntSize(bmp.width, bmp.height),
                            dstOffset = androidx.compose.ui.unit.IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(
                                (rect.right - rect.left).roundToInt().coerceAtLeast(1),
                                (rect.bottom - rect.top).roundToInt().coerceAtLeast(1)
                            )
                        )
                        tap?.let { point ->
                            val cross = Color.White
                            val crossDark = Color.Black
                            drawCircle(cross, radius = 9f, center = point, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
                            drawCircle(crossDark, radius = 9f, center = point, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f))
                            drawLine(crossDark, point.copy(x = point.x - 14f), point.copy(x = point.x + 14f), strokeWidth = 2.4f)
                            drawLine(crossDark, point.copy(y = point.y - 14f), point.copy(y = point.y + 14f), strokeWidth = 2.4f)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "图片尺寸：${bmp.width} × ${bmp.height} 像素" +
                        (pickedPixel?.let { "；取色点：x=${it.first}, y=${it.second}" } ?: "；点击图片开始取色"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (pickedPixel != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .background(color, RoundedCornerShape(10.dp))
                        )
                        Column {
                            Text("取到的颜色：${report.name}", style = MaterialTheme.typography.titleMedium)
                            Text("$hex  ·  ${report.rgb}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "该像素的完整参数见下方“颜色参数”。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        SectionCard(title = "📐 颜色参数（当前颜色）") {
            Text(report.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            val rows = listOf(
                "颜色名称" to "${report.name}（近似）",
                "HEX" to report.hex,
                "RGB" to report.rgb,
                "HSV" to report.hsv,
                "HSL" to report.hsl,
                "CMYK" to report.cmyk,
                "相对亮度" to "Y=${"%.4f".format(report.luminance)}（0=黑，1=白）",
                "对比度（白底）" to "%.2f:1".format(report.contrastWhite),
                "对比度（黑底）" to "%.2f:1".format(report.contrastBlack),
                "建议文字颜色" to report.suggestText,
                "色系 / 冷暖" to "${report.family} · ${report.temperature}",
                "明度 / 饱和度" to report.tone
            )
            rows.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(
                        k,
                        modifier = Modifier.weight(0.36f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        v,
                        modifier = Modifier.weight(0.64f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            CopyButton(
                buildString {
                    appendLine("颜色：${report.name}")
                    appendLine("HEX：${report.hex}")
                    appendLine("RGB：${report.rgb}")
                    appendLine("HSV：${report.hsv}")
                    appendLine("HSL：${report.hsl}")
                    appendLine("CMYK：${report.cmyk}")
                    appendLine("相对亮度：${"%.4f".format(report.luminance)}")
                    appendLine("对比度：白底 ${"%.2f".format(report.contrastWhite)}:1，黑底 ${"%.2f".format(report.contrastBlack)}:1")
                    appendLine("建议文字色：${report.suggestText}")
                    appendLine("色系：${report.family}；冷暖：${report.temperature}")
                    append("描述：${report.description}")
                }
            )
        }

        SectionCard(title = "RGB 滑块") {
            ColorSlider("R 红", red, { red = it; tap = null; pickedPixel = null })
            ColorSlider("G 绿", green, { green = it; tap = null; pickedPixel = null })
            ColorSlider("B 蓝", blue, { blue = it; tap = null; pickedPixel = null })
        }
        SectionCard(title = "输入 HEX 反向设置 RGB") {
            LabeledField(hexInput, { hexInput = it }, "HEX（如 3F51B5）")
            Spacer(Modifier.height(10.dp))
            Button(onClick = { parseHex() }, modifier = Modifier.fillMaxWidth()) { Text("应用 HEX") }
            ErrorText(error)
        }
    }
}

@Composable
private fun ColorSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Text("$label：${value.toInt()}")
        Slider(value = value, onValueChange = onChange, valueRange = 0f..255f)
    }
}
