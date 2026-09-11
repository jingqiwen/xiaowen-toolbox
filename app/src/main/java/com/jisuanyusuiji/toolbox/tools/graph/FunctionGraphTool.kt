package com.jisuanyusuiji.toolbox.tools.graph

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.jisuanyusuiji.toolbox.tools.extra.MediaResultActions
import com.jisuanyusuiji.toolbox.tools.extra.SavedMedia
import com.jisuanyusuiji.toolbox.tools.extra.saveBitmapMedia
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private val CURVE_COLORS = listOf(0xFFE53935, 0xFF1E88E5, 0xFF43A047)

private data class VarHolder(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var t: Double = 0.0,
    var theta: Double = 0.0,
    var a: Double = 1.0,
    var b: Double = 1.0,
    var c: Double = 1.0,
    var d: Double = 1.0
)

private fun eval(expr: String, holder: VarHolder): Double? = try {
    GraphParser(expr) { name ->
        when (name) {
            "x" -> holder.x
            "y" -> holder.y
            "t" -> holder.t
            "theta", "θ" -> holder.theta
            "r" -> holder.x
            "a" -> holder.a
            "b" -> holder.b
            "c" -> holder.c
            "d" -> holder.d
            else -> null
        }
    }.parse().takeIf { it.isFinite() }
} catch (_: Exception) {
    null
}

@Composable
fun FunctionGraphTool() {
    val context = LocalContext.current
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf("显函数 y=f(x)") }
    var e1 by remember { mutableStateOf("x^2") }
    var e2 by remember { mutableStateOf("sin(x)") }
    var e3 by remember { mutableStateOf("") }
    var xMin by remember { mutableStateOf("-10") }
    var xMax by remember { mutableStateOf("10") }
    var yMin by remember { mutableStateOf("-10") }
    var yMax by remember { mutableStateOf("10") }
    var a by remember { mutableStateOf(1f) }
    var b by remember { mutableStateOf(1f) }
    var c by remember { mutableStateOf(1f) }
    var d by remember { mutableStateOf(1f) }
    var marks by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }

    val range = remember(xMin, xMax, yMin, yMax) {
        val x1 = xMin.toDoubleOrNull() ?: -10.0
        val x2 = xMax.toDoubleOrNull() ?: 10.0
        val y1 = yMin.toDoubleOrNull() ?: -10.0
        val y2 = yMax.toDoubleOrNull() ?: 10.0
        if (x1 < x2 && y1 < y2) doubleArrayOf(x1, x2, y1, y2) else doubleArrayOf(-10.0, 10.0, -10.0, 10.0)
    }

    fun holderAt(x: Double, y: Double, t: Double, theta: Double) = VarHolder().apply {
        this.x = x; this.y = y; this.t = t; this.theta = theta
        this.a = a.toDouble(); this.b = b.toDouble(); this.c = c.toDouble(); this.d = d.toDouble()
    }

    fun computeMarks() {
        val h = holderAt(0.0, 0.0, 0.0, 0.0)
        fun f(expr: String, x: Double): Double? { h.x = x; return eval(expr, h) }
        val result = mutableListOf<String>()
        val step = (range[1] - range[0]) / 400.0
        // 零点
        var prev = f(e1, range[0])
        for (i in 1..400) {
            val x = range[0] + i * step
            val cur = f(e1, x)
            if (prev != null && cur != null && prev * cur < 0) {
                val root = x - step * cur / (cur - prev)
                result.add("零点：x ≈ ${"%.3f".format(root)}")
            }
            prev = cur
        }
        // 极值（斜率变号）
        var prevSlope: Double? = null
        var i = 2
        while (i <= 400) {
            val x1 = range[0] + (i - 2) * step
            val x2 = range[0] + (i - 1) * step
            val x3 = range[0] + i * step
            val y1 = f(e1, x1); val y2 = f(e1, x2); val y3 = f(e1, x3)
            if (y1 != null && y2 != null && y3 != null) {
                val slope = (y3 - y1) / (2 * step)
                if (prevSlope != null && prevSlope!! * slope < 0) {
                    result.add("极值点：x ≈ ${"%.3f".format(x2)}，y ≈ ${"%.3f".format(y2)}")
                }
                prevSlope = slope
            }
            i += 2
        }
        // 交点（前两条曲线）
        if (e2.isNotBlank()) {
            var p1 = f(e1, range[0]); var p2 = f(e2, range[0])
            for (i2 in 1..400) {
                val x = range[0] + i2 * step
                val c1 = f(e1, x); val c2 = f(e2, x)
                if (p1 != null && p2 != null && c1 != null && c2 != null) {
                    if ((p1 - p2) * (c1 - c2) < 0) {
                        result.add("交点：x ≈ ${"%.3f".format(x)}")
                    }
                }
                p1 = c1; p2 = c2
            }
        }
        marks = result.take(20)
    }

    fun export() {
        error = ""
        scope.launch {
            try {
                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                saved = saveBitmapMedia(context, bitmap, "image/png", Bitmap.CompressFormat.PNG, 100, "GRAPH_${System.currentTimeMillis()}.png")
            } catch (ex: Exception) {
                error = "导出失败：${ex.message}"
            }
        }
    }

    fun drawGraph(width: Float, height: Float, draw: androidx.compose.ui.graphics.drawscope.DrawScope, exported: Boolean = false) {
        with(draw) {
            val x1 = range[0]; val x2 = range[1]; val y1 = range[2]; val y2 = range[3]
            fun sx(x: Double) = ((x - x1) / (x2 - x1) * width).toFloat()
            fun sy(y: Double) = (height - (y - y1) / (y2 - y1) * height).toFloat()
            val gridColor = Color(0x22000000)
            // 网格
            val xStep = niceStep((x2 - x1) / 10.0)
            var gx = kotlin.math.ceil(x1 / xStep) * xStep
            while (gx <= x2) {
                drawLine(gridColor, androidx.compose.ui.geometry.Offset(sx(gx), 0f), androidx.compose.ui.geometry.Offset(sx(gx), height), strokeWidth = 1f)
                gx += xStep
            }
            val yStep = niceStep((y2 - y1) / 10.0)
            var gy = kotlin.math.ceil(y1 / yStep) * yStep
            while (gy <= y2) {
                drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, sy(gy)), androidx.compose.ui.geometry.Offset(width, sy(gy)), strokeWidth = 1f)
                gy += yStep
            }
            // 坐标轴
            if (y1 <= 0 && y2 >= 0) drawLine(Color(0xFF546E7A), androidx.compose.ui.geometry.Offset(0f, sy(0.0)), androidx.compose.ui.geometry.Offset(width, sy(0.0)), strokeWidth = 3f)
            if (x1 <= 0 && x2 >= 0) drawLine(Color(0xFF546E7A), androidx.compose.ui.geometry.Offset(sx(0.0), 0f), androidx.compose.ui.geometry.Offset(sx(0.0), height), strokeWidth = 3f)

            // 刻度数值标注
            val labelPaint = android.graphics.Paint().apply {
                color = 0xFF37474F.toInt()
                textSize = if (exported) 26f else 22f
                isAntiAlias = true
            }
            val axisPaint = android.graphics.Paint().apply {
                color = 0xFF546E7A.toInt()
                textSize = if (exported) 28f else 24f
                isAntiAlias = true
                isFakeBoldText = true
            }
            drawIntoCanvas { canvas ->
                val nc = canvas.nativeCanvas
                val zeroY = if (y1 <= 0 && y2 >= 0) sy(0.0) else height - 6f
                val zeroX = if (x1 <= 0 && x2 >= 0) sx(0.0) else 34f
                var tx = kotlin.math.ceil(x1 / xStep) * xStep
                while (tx <= x2 + 1e-9) {
                    val px = sx(tx)
                    if (abs(tx) > 1e-9) {
                        drawLine(Color(0xFF90A4AE), androidx.compose.ui.geometry.Offset(px, zeroY - 6f), androidx.compose.ui.geometry.Offset(px, zeroY + 6f), strokeWidth = 2f)
                        nc.drawText(formatTick(tx), px + 4f, zeroY + 26f, labelPaint)
                    }
                    tx += xStep
                }
                var ty = kotlin.math.ceil(y1 / yStep) * yStep
                while (ty <= y2 + 1e-9) {
                    val py = sy(ty)
                    if (abs(ty) > 1e-9) {
                        drawLine(Color(0xFF90A4AE), androidx.compose.ui.geometry.Offset(zeroX - 6f, py), androidx.compose.ui.geometry.Offset(zeroX + 6f, py), strokeWidth = 2f)
                        nc.drawText(formatTick(ty), zeroX + 8f, py - 6f, labelPaint)
                    }
                    ty += yStep
                }
                nc.drawText("O", zeroX + 6f, zeroY + 26f, axisPaint)
                nc.drawText("x", width - 18f, zeroY - 8f, axisPaint)
                nc.drawText("y", zeroX + 8f, 24f, axisPaint)
            }

            val h = holderAt(0.0, 0.0, 0.0, 0.0)
            when (mode) {
                "显函数 y=f(x)" -> {
                    listOf(e1, e2, e3).forEachIndexed { index, expr ->
                        if (expr.isBlank()) return@forEachIndexed
                        val points = mutableListOf<androidx.compose.ui.geometry.Offset>()
                        val n = 500
                        for (i in 0..n) {
                            val x = x1 + (x2 - x1) * i / n
                            h.x = x
                            val y = eval(expr, h) ?: continue
                            if (y < y1 - (y2 - y1) || y > y2 + (y2 - y1)) { points.clear(); continue }
                            points.add(androidx.compose.ui.geometry.Offset(sx(x), sy(y)))
                        }
                        for (i in 1 until points.size) {
                            val a1 = points[i - 1]; val b1 = points[i]
                            if (abs(a1.y - b1.y) > height) continue
                            drawLine(Color(CURVE_COLORS[index % CURVE_COLORS.size].toInt()), a1, b1, strokeWidth = if (exported) 4f else 3f)
                        }
                    }
                }
                "极坐标 r=f(θ)" -> {
                    val expr = e1
                    val points = mutableListOf<androidx.compose.ui.geometry.Offset>()
                    val n = 720
                    for (i in 0..n) {
                        val theta = 2 * Math.PI * i / n
                        h.theta = theta; h.x = 0.0; h.y = 0.0
                        val r = eval(expr, h) ?: continue
                        val gx2 = r * cos(theta); val gy2 = r * sin(theta)
                        points.add(androidx.compose.ui.geometry.Offset(sx(gx2), sy(gy2)))
                    }
                    for (i in 1 until points.size) {
                        drawLine(Color(CURVE_COLORS[0].toInt()), points[i - 1], points[i], strokeWidth = 3f)
                    }
                }
                "参数方程" -> {
                    val points = mutableListOf<androidx.compose.ui.geometry.Offset>()
                    val n = 600
                    val tMax = 2 * Math.PI
                    for (i in 0..n) {
                        val t = tMax * i / n
                        h.t = t
                        val gx2 = eval(e1, h) ?: continue
                        val gy2 = eval(e2, h) ?: continue
                        points.add(androidx.compose.ui.geometry.Offset(sx(gx2), sy(gy2)))
                    }
                    for (i in 1 until points.size) {
                        drawLine(Color(CURVE_COLORS[0].toInt()), points[i - 1], points[i], strokeWidth = 3f)
                    }
                }
                "隐函数 F(x,y)=0" -> {
                    val n = 90
                    for (i in 0 until n) for (j in 0 until n) {
                        val gx2 = x1 + (x2 - x1) * (i + 0.5) / n
                        val gy2 = y1 + (y2 - y1) * (j + 0.5) / n
                        h.x = gx2; h.y = gy2
                        val v = eval(e1, h) ?: continue
                        val vx = eval(e1, holderAt(gx2 + (x2 - x1) / n, gy2, 0.0, 0.0)) ?: continue
                        val vy = eval(e1, holderAt(gx2, gy2 + (y2 - y1) / n, 0.0, 0.0)) ?: continue
                        if (v * vx <= 0 || v * vy <= 0) {
                            drawCircle(Color(CURVE_COLORS[0].toInt()), radius = 2.2f, center = androidx.compose.ui.geometry.Offset(sx(gx2), sy(gy2)))
                        }
                    }
                }
                else -> {
                    val n = 70
                    val cellW = width / n
                    val cellH = height / n
                    for (i in 0 until n) for (j in 0 until n) {
                        val gx2 = x1 + (x2 - x1) * (i + 0.5) / n
                        val gy2 = y1 + (y2 - y1) * (j + 0.5) / n
                        h.x = gx2; h.y = gy2
                        val v = eval(e1, h) ?: continue
                        if (v <= 0) {
                            drawRect(
                                Color(0x552E7D32),
                                topLeft = androidx.compose.ui.geometry.Offset(i * cellW, height - (j + 1) * cellH),
                                size = androidx.compose.ui.geometry.Size(cellW + 1, cellH + 1)
                            )
                        }
                    }
                }
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "绘图类型") {
            ChoiceChips(
                options = listOf("显函数 y=f(x)", "极坐标 r=f(θ)", "参数方程", "隐函数 F(x,y)=0", "不等式区域"),
                selected = mode,
                onSelect = { mode = it },
                label = { it }
            )
        }
        SectionCard(title = "函数表达式") {
            LabeledField(e1, { e1 = it }, when (mode) {
                "显函数 y=f(x)" -> "y ="
                "极坐标 r=f(θ)" -> "r ="
                "参数方程" -> "x(t) ="
                "隐函数 F(x,y)=0" -> "F(x,y) ="
                else -> "F(x,y) ≤ 0 中的 F ="
            })
            if (mode == "显函数 y=f(x)" || mode == "参数方程") {
                Spacer(Modifier.height(6.dp))
                LabeledField(e2, { e2 = it }, if (mode == "参数方程") "y(t) =" else "第二条曲线 y =")
            }
            if (mode == "显函数 y=f(x)") {
                Spacer(Modifier.height(6.dp))
                LabeledField(e3, { e3 = it }, "第三条曲线 y =")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "写法示例：\n" +
                    "· sinx 或 sin(x) 都可以；tanx、lnx、sqrtx 同理\n" +
                    "· 2x 表示 2×x；2(x+1)、3sinx、x(x-1) 都支持\n" +
                    "· 幂：x^2、x^3；根号：sqrt(x)；绝对值：abs(x)\n" +
                    "· 常量：pi、π、e；指数：e^x 或 exp(x)\n" +
                    "· 变量：x、y、t、θ；参数：a、b、c、d（可用下方滑块或直接输入）\n" +
                    "· 其他函数：log（常用对数）、ln（自然对数）、min、max、pow、mod、floor、round",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SectionCard(title = "坐标范围") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(xMin, { xMin = it }, "x 最小", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                LabeledField(xMax, { xMax = it }, "x 最大", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(yMin, { yMin = it }, "y 最小", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                LabeledField(yMax, { yMax = it }, "y 最大", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            }
        }
        SectionCard(title = "参数滑块（a b c d，可在表达式中使用）") {
            ParamSlider("a", a) { a = it }
            ParamSlider("b", b) { b = it }
            ParamSlider("c", c) { c = it }
            ParamSlider("d", d) { d = it }
        }
        Button(
            onClick = { computeMarks() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("计算零点 / 极值 / 交点") }

        SectionCard(title = "图像（本地绘制）") {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(Color.White)
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    }
            ) {
                drawGraph(size.width, size.height, this)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "坐标轴标注：x 每格 ${formatTick(niceStep((range[1] - range[0]) / 10.0))}，" +
                    "y 每格 ${formatTick(niceStep((range[3] - range[2]) / 10.0))}，原点为 O，标注随范围自动调整。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { export() }, modifier = Modifier.weight(1f)) { Text("导出图片") }
                Button(onClick = { marks = emptyList() }, modifier = Modifier.weight(1f)) { Text("清除标记") }
            }
        }
        ErrorText(error)
        saved?.let { MediaResultActions(it) }
        if (marks.isNotEmpty()) {
            SectionCard(title = "标记结果（近似值）") {
                marks.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun ParamSlider(name: String, value: Float, onChange: (Float) -> Unit) {
    var text by remember(name, value) {
        mutableStateOf(if (value == value.toInt().toFloat()) value.toInt().toString() else "%.3f".format(value).trimEnd('0').trimEnd('.'))
    }
    Column {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("$name =", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 6.dp))
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { t ->
                    text = t
                    t.toFloatOrNull()?.let(onChange)
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
        Slider(
            value = value.coerceIn(-5f, 5f),
            onValueChange = {
                onChange(it)
                text = if (it == it.toInt().toFloat()) it.toInt().toString() else "%.3f".format(it).trimEnd('0').trimEnd('.')
            },
            valueRange = -5f..5f
        )
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
