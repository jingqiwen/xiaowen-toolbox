package com.jisuanyusuiji.toolbox.tools.graph

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.tools.extra.MediaResultActions
import com.jisuanyusuiji.toolbox.tools.extra.SavedMedia
import com.jisuanyusuiji.toolbox.tools.extra.saveBitmapMedia
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private data class ViewAngle(val name: String, val yaw: Float, val pitch: Float)

private val VIEW_ANGLES = listOf(
    ViewAngle("等轴测", -0.6f, 0.9f),
    ViewAngle("俯视", 0f, 1.5f),
    ViewAngle("正视", -0.05f, 0.18f),
    ViewAngle("侧视", -1.5708f, 0.18f),
    ViewAngle("斜视", -0.785f, 0.55f)
)

@Composable
fun Surface3DTool() {
    val context = LocalContext.current
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    var expression by remember { mutableStateOf("sin(x)*cos(y)") }
    var xMin by remember { mutableStateOf("-5") }
    var xMax by remember { mutableStateOf("5") }
    var yMin by remember { mutableStateOf("-5") }
    var yMax by remember { mutableStateOf("5") }
    var resolution by remember { mutableStateOf(22) }
    var yaw by remember { mutableStateOf(-0.6f) }
    var pitch by remember { mutableStateOf(0.9f) }
    var zoom by remember { mutableStateOf(1f) }
    var error by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }

    val x1 = xMin.toDoubleOrNull() ?: -5.0
    val x2 = xMax.toDoubleOrNull() ?: 5.0
    val y1 = yMin.toDoubleOrNull() ?: -5.0
    val y2 = yMax.toDoubleOrNull() ?: 5.0
    val rangeOk = x1 < x2 && y1 < y2

    fun evalPoint(x: Double, y: Double): Double? = try {
        GraphParser(expression) { name ->
            when (name) {
                "x" -> x
                "y" -> y
                else -> null
            }
        }.parse().takeIf { it.isFinite() }
    } catch (_: Exception) {
        null
    }

    fun export() {
        scope.launch {
            try {
                val bmp = graphicsLayer.toImageBitmap().asAndroidBitmap()
                saved = saveBitmapMedia(context, bmp, "image/png", Bitmap.CompressFormat.PNG, 100, "SURFACE_${System.currentTimeMillis()}.png")
            } catch (e: Exception) {
                error = "导出失败：${e.message}"
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "三维曲面 z = f(x, y)") {
            LabeledField(expression, { expression = it }, "z =")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(xMin, { xMin = it }, "x 最小", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                LabeledField(xMax, { xMax = it }, "x 最大", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(yMin, { yMin = it }, "y 最小", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                LabeledField(yMax, { yMax = it }, "y 最大", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("网格精度：$resolution", modifier = Modifier.weight(1f))
                Button(onClick = { resolution = (resolution + 6).let { if (it > 40) 16 else it } }) { Text("切换 16~40") }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "写法示例：\n" +
                    "· sinx 或 sin(x)、cosy 或 cos(y) 都可以；sin2x 表示 sin(2x)\n" +
                    "· 隐式乘法：2x、3xy、x(y+1)、x^2 均可\n" +
                    "· 常量 pi、π、e；其他函数 sqrt、ln、exp、abs、pow、min、max\n" +
                    "· 变量只能是 x 和 y",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard(title = "曲面图（拖动旋转 · 双指缩放）") {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("视角：", modifier = Modifier.padding(end = 6.dp))
                ChoiceChips(
                    options = VIEW_ANGLES,
                    selected = VIEW_ANGLES.firstOrNull { abs(it.yaw - yaw) < 0.02f && abs(it.pitch - pitch) < 0.02f } ?: VIEW_ANGLES[0],
                    onSelect = { yaw = it.yaw; pitch = it.pitch },
                    label = { it.name },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            if (!rangeOk) {
                Text("范围无效：请保证 x 最小 < x 最大、y 最小 < y 最大", color = MaterialTheme.colorScheme.error)
            } else {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(Color(0xFF101426))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoomChange, _ ->
                                yaw += pan.x * 0.012f
                                pitch = (pitch + pan.y * 0.012f).coerceIn(0.05f, 1.55f)
                                zoom = (zoom * zoomChange).coerceIn(0.4f, 4f)
                            }
                        }
                        .drawWithContent {
                            graphicsLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(graphicsLayer)
                        }
                ) {
                    val n = resolution
                    val zs = Array(n + 1) { i -> DoubleArray(n + 1) { j ->
                        val x = x1 + (x2 - x1) * i / n
                        val y = y1 + (y2 - y1) * j / n
                        evalPoint(x, y) ?: Double.NaN
                    } }
                    val finite: List<Double> = zs.flatMap { row -> row.toList() }.filter { it.isFinite() }
                    if (finite.isEmpty()) return@Canvas
                    val zMin = finite.minOrNull() ?: 0.0
                    val zMax = finite.maxOrNull() ?: 1.0
                    val zSpan = (zMax - zMin).takeIf { it > 1e-9 } ?: 1.0

                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val scale = (minOf(size.width, size.height) / (2.4 * maxOf(x2 - x1, y2 - y1, zSpan))).toFloat() * zoom
                    val cy0 = cos(yaw.toDouble()); val sy0 = sin(yaw.toDouble())
                    val cp = cos(pitch.toDouble()); val sp = sin(pitch.toDouble())

                    data class Quad(val points: List<Offset>, val depth: Float, val color: Color)

                    fun project(px: Double, py: Double, pz: Double): Triple<Float, Float, Float> {
                        val rx = px * cy0 - py * sy0
                        val ry = px * sy0 + py * cy0
                        val rz = pz
                        val ry2 = ry * cp - rz * sp
                        val rz2 = ry * sp + rz * cp
                        return Triple((cx + rx * scale).toFloat(), (cy - rz2 * scale * 0.8f).toFloat(), ry2.toFloat())
                    }

                    // ---- 坐标轴框架 + 刻度数值 ----
                    val axisColor = Color(0xFF78909C)
                    val origin = project(x1, y1, zMin)
                    val xEnd = project(x2, y1, zMin)
                    val yEnd = project(x1, y2, zMin)
                    val zEnd = project(x1, y1, zMax)
                    drawLine(axisColor, Offset(origin.first, origin.second), Offset(xEnd.first, xEnd.second), strokeWidth = 2.5f)
                    drawLine(axisColor, Offset(origin.first, origin.second), Offset(yEnd.first, yEnd.second), strokeWidth = 2.5f)
                    drawLine(axisColor, Offset(origin.first, origin.second), Offset(zEnd.first, zEnd.second), strokeWidth = 2.5f)

                    val tickPaint = Paint().apply {
                        color = 0xFFECEFF1.toInt()
                        textSize = 20f
                        isAntiAlias = true
                    }
                    val axisPaint = Paint().apply {
                        color = 0xFF80DEEA.toInt()
                        textSize = 24f
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    fun drawTicks(count: Int, from: Double, to: Double, at: (Double) -> Triple<Float, Float, Float>, horizontal: Boolean) {
                        val step = niceStep((to - from) / count)
                        var t = ceil(from / step) * step
                        while (t <= to + 1e-9) {
                            val p = at(t)
                            drawCircle(Color(0xFFB0BEC5), radius = 3f, center = Offset(p.first, p.second))
                            val text = formatTick(t)
                            if (horizontal) {
                                drawIntoCanvas { it.nativeCanvas.drawText(text, p.first - text.length * 5f, p.second + 24f, tickPaint) }
                            } else {
                                drawIntoCanvas { it.nativeCanvas.drawText(text, p.first - text.length * 10f - 6f, p.second + 6f, tickPaint) }
                            }
                            t += step
                        }
                    }
                    drawTicks(6, x1, x2, { v -> project(v, y1, zMin) }, true)
                    drawTicks(6, y1, y2, { v -> project(x1, v, zMin) }, false)
                    drawTicks(5, zMin, zMax, { v -> project(x1, y1, v) }, false)
                    drawIntoCanvas {
                        it.nativeCanvas.drawText("x", xEnd.first + 8f, xEnd.second + 22f, axisPaint)
                        it.nativeCanvas.drawText("y", yEnd.first - 22f, yEnd.second + 10f, axisPaint)
                        it.nativeCanvas.drawText("z", zEnd.first - 24f, zEnd.second - 8f, axisPaint)
                    }

                    // ---- 曲面 ----
                    val quads = mutableListOf<Quad>()
                    for (i in 0 until n) for (j in 0 until n) {
                        val pts = mutableListOf<Offset>()
                        var depth = 0f
                        var ok = true
                        for ((di, dj) in listOf(0 to 0, 1 to 0, 1 to 1, 0 to 1)) {
                            val z = zs[i + di][j + dj]
                            if (!z.isFinite()) { ok = false; break }
                            val x = x1 + (x2 - x1) * (i + di) / n
                            val y = y1 + (y2 - y1) * (j + dj) / n
                            val (sx, sy, d) = project(x, y, z)
                            pts.add(Offset(sx, sy))
                            depth += d
                        }
                        if (ok && pts.size == 4) {
                            val avgZ = (zs[i][j] + zs[i + 1][j] + zs[i + 1][j + 1] + zs[i][j + 1]) / 4.0
                            val t = ((avgZ - zMin) / zSpan).toFloat().coerceIn(0f, 1f)
                            val color = Color(
                                red = (0.25f + 0.65f * t),
                                green = (0.35f + 0.4f * (1 - abs(t - 0.5f) * 2)),
                                blue = (0.95f - 0.75f * t),
                                alpha = 1f
                            )
                            quads.add(Quad(pts, depth, color))
                        }
                    }
                    quads.sortedByDescending { it.depth }.forEach { quad ->
                        val path = Path().apply {
                            moveTo(quad.points[0].x, quad.points[0].y)
                            for (k in 1 until quad.points.size) lineTo(quad.points[k].x, quad.points[k].y)
                            close()
                        }
                        drawPath(path, quad.color)
                        drawPath(path, Color(0x22000000), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
                    }

                    // ---- 数值信息 ----
                    drawIntoCanvas { canvas ->
                        val info = Paint().apply {
                            color = 0xFFECEFF1.toInt()
                            textSize = 22f
                            isAntiAlias = true
                        }
                        canvas.nativeCanvas.drawText("z: ${formatTick(zMin)} ~ ${formatTick(zMax)}", 16f, 30f, info)
                        canvas.nativeCanvas.drawText("拖动旋转 · 双指缩放", 16f, 58f, info)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { zoom = (zoom * 1.25f).coerceAtMost(4f) }, modifier = Modifier.weight(1f)) { Text("放大 +") }
                Button(onClick = { zoom = (zoom / 1.25f).coerceAtLeast(0.4f) }, modifier = Modifier.weight(1f)) { Text("缩小 −") }
                Button(onClick = { yaw = -0.6f; pitch = 0.9f; zoom = 1f }, modifier = Modifier.weight(1f)) { Text("重置视角") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { export() }, modifier = Modifier.weight(1f)) { Text("导出图片") }
                Text(
                    "导出为 PNG，可直接插入文档",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        ErrorText(error)
        saved?.let { MediaResultActions(it) }
    }
}

private fun formatTick(v: Double): String {
    if (abs(v) < 1e-12) return "0"
    return if (abs(v) >= 1e5 || abs(v) < 1e-4) "%.1e".format(v)
    else java.math.BigDecimal(v).setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

private fun niceStep(raw: Double): Double {
    if (raw <= 0) return 1.0
    val exp = kotlin.math.floor(kotlin.math.log10(raw))
    val base = 10.0.pow(exp)
    val n = raw / base
    return when {
        n <= 1 -> 1.0 * base
        n <= 2 -> 2.0 * base
        n <= 5 -> 5.0 * base
        else -> 10.0 * base
    }
}
