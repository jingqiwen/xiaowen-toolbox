package com.jisuanyusuiji.toolbox.tools.graph

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.tools.extra.MediaResultActions
import com.jisuanyusuiji.toolbox.tools.extra.SavedMedia
import com.jisuanyusuiji.toolbox.tools.extra.saveBitmapMedia
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    var error by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }

    fun evalPoint(x: Double, y: Double): Double? = try {
        GraphParser(expression) { name ->
            when (name) {
                "x" -> x
                "y" -> y
                "pi" -> PI
                "e" -> Math.E
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
        Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp),
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
            Text("支持：sin cos tan ln log sqrt abs exp pow min max mod ^ pi e，变量 x、y。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionCard(title = "曲面图（拖动旋转）") {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(Color(0xFF101426))
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            yaw += drag.x * 0.012f
                            pitch = (pitch + drag.y * 0.012f).coerceIn(0.1f, 1.5f)
                        }
                    }
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    }
            ) {
                val x1 = xMin.toDoubleOrNull() ?: -5.0
                val x2 = xMax.toDoubleOrNull() ?: 5.0
                val y1 = yMin.toDoubleOrNull() ?: -5.0
                val y2 = yMax.toDoubleOrNull() ?: 5.0
                val n = resolution
                if (x1 >= x2 || y1 >= y2) {
                    error = "范围无效"
                    return@Canvas
                }
                val zs = Array(n + 1) { i -> DoubleArray(n + 1) { j ->
                    val x = x1 + (x2 - x1) * i / n
                    val y = y1 + (y2 - y1) * j / n
                    evalPoint(x, y) ?: Double.NaN
                } }
                val finite: List<Double> = zs.flatMap { row -> row.toList() }.filter { it.isFinite() }
                if (finite.isEmpty()) {
                    error = "函数在当前范围内无有效值"
                    return@Canvas
                }
                val zMin = finite.minOrNull() ?: 0.0
                val zMax = finite.maxOrNull() ?: 1.0
                val zSpan = (zMax - zMin).takeIf { it > 1e-9 } ?: 1.0

                val cx = size.width / 2f
                val cy = size.height / 2f
                val scale = minOf(size.width, size.height) / (2.4 * maxOf(x2 - x1, y2 - y1, zSpan)).toFloat()
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
                            green = (0.35f + 0.4f * (1 - kotlin.math.abs(t - 0.5f) * 2)),
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
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { export() }, modifier = Modifier.weight(1f)) { Text("导出图片") }
                Button(onClick = { yaw = -0.6f; pitch = 0.9f }, modifier = Modifier.weight(1f)) { Text("重置视角") }
            }
        }
        ErrorText(error)
        saved?.let { MediaResultActions(it) }
    }
}
