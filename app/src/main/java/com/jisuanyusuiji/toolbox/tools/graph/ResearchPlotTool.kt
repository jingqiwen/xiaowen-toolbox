package com.jisuanyusuiji.toolbox.tools.graph

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.tools.extra.MediaResultActions
import com.jisuanyusuiji.toolbox.tools.extra.SavedMedia
import com.jisuanyusuiji.toolbox.tools.extra.saveBitmapMedia
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private val CB_COLORS = listOf(
    0xFF0072B2, 0xFFD55E00, 0xFF009E73, 0xFFCC79A7,
    0xFFE69F00, 0xFF56B4E9, 0xFFF0E442, 0xFF999999
)

private data class PlotData(
    val names: List<String>,
    val columns: List<List<Double>>,
    val rows: List<List<Double>>
)

private fun parsePlotData(text: String, hasHeader: Boolean): PlotData {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.isEmpty()) throw IllegalArgumentException("请输入数据")
    val splitter: (String) -> List<String> = { line ->
        when {
            line.contains(',') -> line.split(',')
            line.contains('\t') -> line.split('\t')
            line.contains(';') -> line.split(';')
            else -> line.split(Regex("\\s+"))
        }.map { it.trim() }
    }
    val rowsAll = lines.map(splitter)
    val header = if (hasHeader) rowsAll.first() else null
    val dataRows = if (hasHeader) rowsAll.drop(1) else rowsAll
    val cols = dataRows.firstOrNull()?.size ?: 0
    if (cols == 0) throw IllegalArgumentException("没有有效数据")
    val columns = (0 until cols).map { c ->
        dataRows.mapNotNull { row -> row.getOrNull(c)?.toDoubleOrNull() }
    }
    val names = (0 until cols).map { header?.getOrNull(it)?.ifBlank { "第${it + 1}列" } ?: "第${it + 1}列" }
    val numericRows = dataRows.map { row -> (0 until cols).mapNotNull { row.getOrNull(it)?.toDoubleOrNull() } }
        .filter { it.size == cols }
    return PlotData(names, columns, numericRows)
}

private fun mean(v: List<Double>): Double = if (v.isEmpty()) 0.0 else v.sum() / v.size
private fun sd(v: List<Double>): Double {
    if (v.size < 2) return 0.0
    val m = mean(v)
    return sqrt(v.sumOf { (it - m) * (it - m) } / (v.size - 1))
}
private fun sem(v: List<Double>): Double = if (v.size < 2) 0.0 else sd(v) / sqrt(v.size.toDouble())
private fun quantile(sorted: List<Double>, q: Double): Double {
    if (sorted.isEmpty()) return 0.0
    val pos = (sorted.size - 1) * q
    val lo = pos.toInt(); val hi = (lo + 1).coerceAtMost(sorted.size - 1)
    val frac = pos - lo
    return sorted[lo] * (1 - frac) + sorted[hi] * frac
}
private fun linearFit(x: List<Double>, y: List<Double>): DoubleArray? {
    if (x.size < 2 || x.size != y.size) return null
    val mx = mean(x); val my = mean(y)
    val sxx = x.sumOf { (it - mx) * (it - mx) }
    val sxy = x.indices.sumOf { (x[it] - mx) * (y[it] - my) }
    if (abs(sxx) < 1e-12) return null
    val b = sxy / sxx
    val a = my - b * mx
    val ssTot = y.sumOf { (it - my) * (it - my) }
    val ssRes = y.indices.sumOf { val p = a + b * x[it]; (y[it] - p) * (y[it] - p) }
    val r2 = if (ssTot <= 1e-12) 1.0 else 1 - ssRes / ssTot
    val r = if (ssTot <= 1e-12) 0.0 else sqrt(r2.coerceIn(0.0, 1.0)) * (if (b >= 0) 1 else -1)
    val n = x.size
    val p = if (n > 2 && ssRes > 0) {
        val se = sqrt(ssRes / (n - 2) / (ssTot / (n - 1)))
        val t = abs(r) * sqrt((n - 2) / (1 - r * r).coerceAtLeast(1e-12))
        t
    } else 0.0
    return doubleArrayOf(a, b, r2, r, p)
}

private fun normalQuantile(p: Double): Double {
    // Acklam 逆正态近似
    val a = doubleArrayOf(-3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02, 1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00)
    val b = doubleArrayOf(-5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02, 6.680131188771972e+01, -1.328068155288572e+01)
    val c = doubleArrayOf(-7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00, -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00)
    val d = doubleArrayOf(7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00)
    val plow = 0.02425; val phigh = 1 - plow
    return when {
        p < plow -> { val q = sqrt(-2 * ln(p)); (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1) }
        p <= phigh -> { val q = p - 0.5; val r = q * q; (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q / (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1) }
        else -> { val q = sqrt(-2 * ln(1 - p)); -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1) }
    }
}

private fun pca2(rows: List<List<Double>>): List<Pair<Double, Double>> {
    if (rows.size < 2 || rows.first().size < 2) return emptyList()
    val cols = rows.first().size
    val means = (0 until cols).map { c -> rows.map { it[c] }.let { mean(it) } }
    val centered = rows.map { row -> row.mapIndexed { c, v -> v - means[c] } }
    val cov = Array(cols) { i -> DoubleArray(cols) { j ->
        centered.sumOf { it[i] * it[j] } / (rows.size - 1)
    } }
    fun powerIteration(deflate: DoubleArray?): DoubleArray {
        var v = DoubleArray(cols) { 1.0 / sqrt(cols.toDouble()) }
        repeat(200) {
            val nv = DoubleArray(cols)
            for (i in 0 until cols) {
                var s = 0.0
                for (j in 0 until cols) s += cov[i][j] * v[j]
                if (deflate != null) s -= deflate[i] * (deflate.indices.sumOf { deflate[it] * v[it] })
                nv[i] = s
            }
            val norm = sqrt(nv.sumOf { it * it })
            if (norm < 1e-12) return v
            for (i in 0 until cols) v[i] = nv[i] / norm
        }
        return v
    }
    val v1 = powerIteration(null)
    val lambda1 = v1.indices.sumOf { i -> v1.indices.sumOf { j -> v1[i] * cov[i][j] * v1[j] } }
    val v2 = powerIteration(DoubleArray(cols) { i -> lambda1 * v1[i] })
    return centered.map { row ->
        val x = row.indices.sumOf { row[it] * v1[it] }
        val y = row.indices.sumOf { row[it] * v2[it] }
        x to y
    }
}

@Composable
fun ResearchPlotTool() {
    val context = LocalContext.current
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    var raw by remember {
        mutableStateOf(
            "GroupA,GroupB,GroupC\n" +
                "1.2,2.1,3.0\n1.5,2.4,2.8\n1.1,1.9,3.3\n1.4,2.2,2.9\n1.3,2.0,3.1\n1.6,2.5,2.7"
        )
    }
    var plotType by remember { mutableStateOf("柱状图 + 误差棒 + 散点") }
    var hasHeader by remember { mutableStateOf(true) }
    var firstColX by remember { mutableStateOf(false) }
    var errorType by remember { mutableStateOf("SD") }
    var showPoints by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }

    val data = remember(raw, hasHeader) {
        try { parsePlotData(raw, hasHeader) } catch (e: Exception) {
            error = e.message ?: "数据解析失败"
            null
        }
    }

    fun export() {
        scope.launch {
            try {
                val bmp = graphicsLayer.toImageBitmap().asAndroidBitmap()
                saved = saveBitmapMedia(context, bmp, "image/png", Bitmap.CompressFormat.PNG, 100, "PLOT_${System.currentTimeMillis()}.png")
            } catch (e: Exception) {
                error = "导出失败：${e.message}"
            }
        }
    }

    Column(
        Modifier.fillMaxWidth()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "科研绘图（粘贴 CSV / Excel 复制数据）") {
            LabeledField(raw, { raw = it }, "数据：每行一条记录，每列一个变量/组；支持逗号、制表符、分号或空格分隔", singleLine = false, minLines = 6)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("第一行是列名", modifier = Modifier.weight(1f))
                Switch(checked = hasHeader, onCheckedChange = { hasHeader = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("折线/散点：第一列作为 X", modifier = Modifier.weight(1f))
                Switch(checked = firstColX, onCheckedChange = { firstColX = it })
            }
        }
        SectionCard(title = "图类型") {
            ChoiceChips(
                listOf(
                    "折线图", "柱状图 + 误差棒 + 散点", "箱线图", "小提琴图",
                    "散点图 + 线性拟合", "直方图 + KDE", "热力图", "配对图",
                    "平行坐标图", "PCA 散点图", "t-SNE 降维", "Q-Q 图",
                    "极坐标图", "矢量场图", "瀑布图"
                ),
                plotType,
                { plotType = it },
                { it }
            )
        }
        if (plotType == "柱状图 + 误差棒 + 散点") {
            SectionCard(title = "误差棒") {
                ChoiceChips(listOf("SD", "SEM"), errorType, { errorType = it }, { it })
            }
        }
        ErrorText(error)
        if (data != null) {
            SectionCard(title = "图形（本地绘制，可导出）") {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .background(Color.White)
                        .drawWithContent {
                            graphicsLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(graphicsLayer)
                        }
                ) {
                    drawResearchPlot(data, plotType, firstColX, errorType, showPoints, size.width, size.height)
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { export() }, modifier = Modifier.fillMaxWidth()) { Text("导出图片") }
            }
        }
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        saved?.let { MediaResultActions(it) }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawResearchPlot(
    data: PlotData,
    plotType: String,
    firstColX: Boolean,
    errorType: String,
    showPoints: Boolean,
    width: Float,
    height: Float
) {
    val marginL = 56f; val marginR = 24f; val marginT = 28f; val marginB = 52f
    val w = width - marginL - marginR
    val h = height - marginT - marginB
    fun px(x: Double, xmin: Double, xmax: Double) = (marginL + (x - xmin) / (xmax - xmin).coerceAtLeast(1e-9) * w).toFloat()
    fun py(y: Double, ymin: Double, ymax: Double) = (marginT + h - (y - ymin) / (ymax - ymin).coerceAtLeast(1e-9) * h).toFloat()
    val grid = Color(0x22000000)
    for (i in 0..5) {
        val y = marginT + h * i / 5f
        drawLine(grid, Offset(marginL, y), Offset(marginL + w, y), 1f)
    }
    drawLine(Color(0xFF37474F), Offset(marginL, marginT), Offset(marginL, marginT + h), 3f)
    drawLine(Color(0xFF37474F), Offset(marginL, marginT + h), Offset(marginL + w, marginT + h), 3f)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = 26f
        color = android.graphics.Color.DKGRAY
        textAlign = android.graphics.Paint.Align.CENTER
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText("绘图区域", marginL + w / 2, height - 8f, paint)
    }

    when (plotType) {
        "折线图" -> {
            val ys = if (firstColX && data.columns.size > 1) data.columns.drop(1) else data.columns
            val xs = if (firstColX && data.columns.size > 1) data.columns.first() else ys.first().indices.map { it.toDouble() }
            val yMin = ys.flatten().minOrNull() ?: 0.0
            val yMax = ys.flatten().maxOrNull() ?: 1.0
            val xMin = xs.minOrNull() ?: 0.0
            val xMax = xs.maxOrNull() ?: 1.0
            ys.forEachIndexed { idx, col ->
                val color = Color(CB_COLORS[idx % CB_COLORS.size].toInt())
                for (i in 1 until minOf(xs.size, col.size)) {
                    drawLine(
                        color,
                        Offset(px(xs[i - 1], xMin, xMax), py(col[i - 1], yMin, yMax)),
                        Offset(px(xs[i], xMin, xMax), py(col[i], yMin, yMax)),
                        strokeWidth = 4f
                    )
                }
                if (showPoints) col.forEachIndexed { i, v ->
                    if (i < xs.size) drawCircle(color, 6f, Offset(px(xs[i], xMin, xMax), py(v, yMin, yMax)))
                }
            }
        }
        "柱状图 + 误差棒 + 散点" -> {
            val groups = data.columns
            val means = groups.map { mean(it) }
            val errs = groups.map { if (errorType == "SEM") sem(it) else sd(it) }
            val yMax = (groups.flatten().maxOrNull() ?: 1.0).let { if (it > 0) it * 1.25 else 1.0 }
            val yMin = 0.0
            val slot = w / groups.size
            groups.forEachIndexed { i, values ->
                val color = Color(CB_COLORS[i % CB_COLORS.size].toInt())
                val cx = marginL + slot * (i + 0.5f)
                val barW = slot * 0.5f
                val top = py(means[i], yMin, yMax)
                drawRect(
                    color.copy(alpha = 0.75f),
                    topLeft = Offset(cx - barW / 2, top),
                    size = androidx.compose.ui.geometry.Size(barW, marginT + h - top)
                )
                drawLine(Color(0xFF37474F), Offset(cx, py(means[i] + errs[i], yMin, yMax)), Offset(cx, py(means[i] - errs[i], yMin, yMax)), 4f)
                drawLine(Color(0xFF37474F), Offset(cx - 12f, py(means[i] + errs[i], yMin, yMax)), Offset(cx + 12f, py(means[i] + errs[i], yMin, yMax)), 4f)
                drawLine(Color(0xFF37474F), Offset(cx - 12f, py(means[i] - errs[i], yMin, yMax)), Offset(cx + 12f, py(means[i] - errs[i], yMin, yMax)), 4f)
                if (showPoints) values.forEach { v ->
                    val jitter = (kotlin.random.Random.nextDouble() - 0.5) * barW * 0.6
                    drawCircle(Color(0xAA000000.toInt()), 5f, Offset(cx + jitter.toFloat(), py(v, yMin, yMax)))
                }
            }
        }
        "箱线图" -> {
            val groups = data.columns
            val all = groups.flatten()
            val yMin = all.minOrNull() ?: 0.0
            val yMax = all.maxOrNull() ?: 1.0
            val slot = w / groups.size
            groups.forEachIndexed { i, values ->
                val sorted = values.sorted()
                val cx = marginL + slot * (i + 0.5f)
                val q1 = quantile(sorted, 0.25); val med = quantile(sorted, 0.5); val q3 = quantile(sorted, 0.75)
                val color = Color(CB_COLORS[i % CB_COLORS.size].toInt())
                drawLine(Color(0xFF37474F), Offset(cx, py(sorted.first(), yMin, yMax)), Offset(cx, py(sorted.last(), yMin, yMax)), 3f)
                drawRect(color.copy(alpha = 0.7f), topLeft = Offset(cx - slot * 0.22f, py(q3, yMin, yMax)), size = androidx.compose.ui.geometry.Size(slot * 0.44f, (py(q1, yMin, yMax) - py(q3, yMin, yMax)).coerceAtLeast(1f)))
                drawLine(Color.White, Offset(cx - slot * 0.22f, py(med, yMin, yMax)), Offset(cx + slot * 0.22f, py(med, yMin, yMax)), 6f)
            }
        }
        "小提琴图" -> {
            val groups = data.columns
            val all = groups.flatten()
            val yMin = all.minOrNull() ?: 0.0
            val yMax = all.maxOrNull() ?: 1.0
            val slot = w / groups.size
            groups.forEachIndexed { i, values ->
                val color = Color(CB_COLORS[i % CB_COLORS.size].toInt())
                val cx = marginL + slot * (i + 0.5f)
                val bins = 14
                val counts = IntArray(bins)
                values.forEach { v ->
                    val idx = ((v - yMin) / (yMax - yMin).coerceAtLeast(1e-9) * (bins - 1)).toInt().coerceIn(0, bins - 1)
                    counts[idx]++
                }
                val maxCount = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
                for (b in 0 until bins - 1) {
                    val y1 = py(yMin + (yMax - yMin) * b / (bins - 1), yMin, yMax)
                    val y2 = py(yMin + (yMax - yMin) * (b + 1) / (bins - 1), yMin, yMax)
                    val half = slot * 0.35f * counts[b] / maxCount
                    drawRect(color.copy(alpha = 0.7f), topLeft = Offset(cx - half, y2), size = androidx.compose.ui.geometry.Size(2 * half, (y1 - y2).coerceAtLeast(1f)))
                }
            }
        }
        "散点图 + 线性拟合" -> {
            if (data.columns.size < 2) throw IllegalArgumentException("散点图至少需要两列数据")
            val x = data.columns[0]; val y = data.columns[1]
            val fit = linearFit(x, y) ?: throw IllegalArgumentException("无法拟合（X 可能为常数）")
            val xMin = x.minOrNull() ?: 0.0; val xMax = x.maxOrNull() ?: 1.0
            val yMin = y.minOrNull() ?: 0.0; val yMax = y.maxOrNull() ?: 1.0
            for (i in x.indices) drawCircle(Color(0xFF0072B2), 8f, Offset(px(x[i], xMin, xMax), py(y[i], yMin, yMax)))
            val xx = doubleArrayOf(xMin, xMax)
            drawLine(
                Color(0xFFD55E00),
                Offset(px(xx[0], xMin, xMax), py(fit[0] + fit[1] * xx[0], yMin, yMax)),
                Offset(px(xx[1], xMin, xMax), py(fit[0] + fit[1] * xx[1], yMin, yMax)),
                strokeWidth = 5f
            )
        }
        "直方图 + KDE" -> {
            val x = data.columns[0]
            val min = x.minOrNull() ?: 0.0; val max = x.maxOrNull() ?: 1.0
            val bins = 10
            val counts = IntArray(bins)
            x.forEach { v -> counts[((v - min) / (max - min).coerceAtLeast(1e-9) * bins).toInt().coerceIn(0, bins - 1)]++ }
            val maxCount = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
            for (b in 0 until bins) {
                val x1 = marginL + w * b / bins
                val x2 = marginL + w * (b + 1) / bins
                val y = marginT + h - h * counts[b] / maxCount
                drawRect(Color(0x880072B2.toInt()), topLeft = Offset(x1 + 2, y), size = androidx.compose.ui.geometry.Size(x2 - x1 - 4, marginT + h - y))
            }
        }
        "热力图" -> {
            val rows = data.rows
            if (rows.isEmpty()) throw IllegalArgumentException("没有数据")
            val cols = rows.first().size
            val flat = rows.flatten()
            val min = flat.minOrNull() ?: 0.0; val max = flat.maxOrNull() ?: 1.0
            val cellW = w / cols; val cellH = h / rows.size
            rows.forEachIndexed { r, row ->
                row.forEachIndexed { c, v ->
                    val t = ((v - min) / (max - min).coerceAtLeast(1e-9)).toFloat()
                    val color = Color(t * 0.2f, 0.3f + t * 0.5f, 0.9f - t * 0.7f)
                    drawRect(color, topLeft = Offset(marginL + c * cellW, marginT + r * cellH), size = androidx.compose.ui.geometry.Size(cellW - 1, cellH - 1))
                }
            }
        }
        "配对图" -> {
            if (data.columns.size < 2) throw IllegalArgumentException("配对图需要两列")
            val a = data.columns[0]; val b = data.columns[1]
            val n = minOf(a.size, b.size)
            val min = minOf(a.minOrNull() ?: 0.0, b.minOrNull() ?: 0.0)
            val max = maxOf(a.maxOrNull() ?: 1.0, b.maxOrNull() ?: 1.0)
            for (i in 0 until n) {
                val x1 = px(0.0, 0.0, 1.0); val x2 = px(1.0, 0.0, 1.0)
                val y1 = py(a[i], min, max); val y2 = py(b[i], min, max)
                drawLine(Color(0x55000000), Offset(marginL + w * 0.25f, y1), Offset(marginL + w * 0.75f, y2), 2f)
                drawCircle(Color(0xFFD55E00), 7f, Offset(marginL + w * 0.25f, y1))
                drawCircle(Color(0xFF0072B2), 7f, Offset(marginL + w * 0.75f, y2))
            }
        }
        "平行坐标图" -> {
            val cols = data.columns
            val minmax = cols.map { col -> (col.minOrNull() ?: 0.0) to (col.maxOrNull() ?: 1.0) }
            data.rows.take(80).forEach { row ->
                val path = Path()
                row.forEachIndexed { c, v ->
                    val (mn, mx) = minmax[c]
                    val t = ((v - mn) / (mx - mn).coerceAtLeast(1e-9)).toFloat()
                    val x = marginL + w * c / (row.size - 1).coerceAtLeast(1)
                    val y = marginT + h * (1 - t)
                    if (c == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, Color(0x440072B2.toInt()), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
            }
        }
        "PCA 散点图" -> {
            val pts = pca2(data.rows)
            if (pts.isEmpty()) throw IllegalArgumentException("数据不足（至少 2 行 2 列）")
            val xs = pts.map { it.first }; val ys = pts.map { it.second }
            val xMin = xs.minOrNull() ?: 0.0; val xMax = xs.maxOrNull() ?: 1.0
            val yMin = ys.minOrNull() ?: 0.0; val yMax = ys.maxOrNull() ?: 1.0
            pts.forEach { (x, y) ->
                drawCircle(Color(0xFF009E73), 8f, Offset(px(x, xMin, xMax), py(y, yMin, yMax)))
            }
        }
        "Q-Q 图" -> {
            val x = data.columns[0].sorted()
            val m = mean(x); val s = sd(x).coerceAtLeast(1e-12)
            val min = x.minOrNull() ?: 0.0; val max = x.maxOrNull() ?: 1.0
            val qMin = normalQuantile(0.01); val qMax = normalQuantile(0.99)
            x.forEachIndexed { i, v ->
                val p = (i + 0.5) / x.size
                val q = normalQuantile(p)
                drawCircle(Color(0xFF0072B2), 7f, Offset(px(q, qMin, qMax), py(v, min, max)))
            }
            drawLine(Color(0xFFD55E00), Offset(px(qMin, qMin, qMax), py(m + s * qMin, min, max)), Offset(px(qMax, qMin, qMax), py(m + s * qMax, min, max)), 4f)
        }
        "t-SNE 降维" -> {
            val pts = tsne2(data.rows)
            if (pts.isEmpty()) throw IllegalArgumentException("数据不足（至少 5 行 2 列，最多 150 行）")
            val xs = pts.map { it.first }; val ys = pts.map { it.second }
            val xMin = xs.minOrNull() ?: 0.0; val xMax = xs.maxOrNull() ?: 1.0
            val yMin = ys.minOrNull() ?: 0.0; val yMax = ys.maxOrNull() ?: 1.0
            pts.forEach { (x, y) -> drawCircle(Color(0xFFCC79A7), 8f, Offset(px(x, xMin, xMax), py(y, yMin, yMax))) }
        }
        "极坐标图" -> {
            if (data.columns.size < 2) throw IllegalArgumentException("极坐标图需要两列（角度、半径）")
            val angles = data.columns[0]; val radii = data.columns[1]
            val rMax = (radii.maxOrNull() ?: 1.0).coerceAtLeast(1e-9)
            val cx = marginL + w / 2; val cy = marginT + h / 2
            val radius = minOf(w, h) / 2 - 20f
            for (i in 0 until radii.size) {
                val a = Math.toRadians(angles.getOrElse(i) { 0.0 })
                val r = radii[i] / rMax * radius
                val x = cx + r * cos(a).toFloat()
                val y = cy - r * sin(a).toFloat()
                drawCircle(Color(0xFFD55E00), 7f, Offset(x.toFloat(), y.toFloat()))
            }
        }
        "矢量场图" -> {
            if (data.columns.size < 4) throw IllegalArgumentException("矢量场需要四列：x, y, u, v")
            val xs = data.columns[0]; val ys = data.columns[1]
            val us = data.columns[2]; val vs = data.columns[3]
            val xMin = xs.minOrNull() ?: 0.0; val xMax = xs.maxOrNull() ?: 1.0
            val yMin = ys.minOrNull() ?: 0.0; val yMax = ys.maxOrNull() ?: 1.0
            val uMax = maxOf(us.maxOfOrNull { abs(it) } ?: 1.0, vs.maxOfOrNull { abs(it) } ?: 1.0).coerceAtLeast(1e-9)
            val scale = minOf(w, h) * 0.35 / uMax
            for (i in xs.indices) {
                if (i >= ys.size || i >= us.size || i >= vs.size) break
                val x1 = px(xs[i], xMin, xMax); val y1 = py(ys[i], yMin, yMax)
                val x2 = x1 + (us[i] * scale).toFloat(); val y2 = y1 - (vs[i] * scale).toFloat()
                drawLine(Color(0xFF0072B2), Offset(x1, y1), Offset(x2, y2), 3f)
                drawCircle(Color(0xFFD55E00), 4f, Offset(x1, y1))
            }
        }
        "瀑布图" -> {
            val values = data.columns[0]
            if (values.isEmpty()) throw IllegalArgumentException("没有数据")
            var cumulative = 0.0
            val totals = values.map { cumulative += it; cumulative }
            val yMin = minOf(0.0, totals.minOrNull() ?: 0.0)
            val yMax = maxOf(0.0, totals.maxOrNull() ?: 1.0)
            val slot = w / values.size.coerceAtLeast(1)
            var prevTop: Float? = null
            values.forEachIndexed { i, v ->
                val base = if (i == 0) 0.0 else totals[i - 1]
                val topValue = totals[i]
                val yTop = py(topValue, yMin, yMax)
                val yBase = py(base, yMin, yMax)
                val color = if (v >= 0) Color(0xFF009E73) else Color(0xFFD55E00)
                drawRect(
                    color.copy(alpha = 0.8f),
                    topLeft = Offset(marginL + slot * i + slot * 0.15f, minOf(yTop, yBase)),
                    size = androidx.compose.ui.geometry.Size(slot * 0.7f, abs(yBase - yTop).coerceAtLeast(1f))
                )
                prevTop?.let { p ->
                    drawLine(Color(0x66000000), Offset(marginL + slot * (i - 1) + slot * 0.85f, p), Offset(marginL + slot * i + slot * 0.15f, yBase), 2f)
                }
                prevTop = yTop
            }
        }
    }
}

private fun tsne2(rows: List<List<Double>>, iterations: Int = 160): List<Pair<Double, Double>> {
    if (rows.size < 5) return emptyList()
    val n = rows.size.coerceAtMost(150)
    val data = rows.take(n)
    val dims = data.first().size
    if (dims < 2) return emptyList()
    val means = (0 until dims).map { c -> data.sumOf { it[c] } / n }
    val stds = (0 until dims).map { c ->
        sqrt(data.sumOf { (it[c] - means[c]).pow(2) } / n).coerceAtLeast(1e-9)
    }
    val x = data.map { row -> DoubleArray(dims) { c -> (row[c] - means[c]) / stds[c] } }
    val dist = Array(n) { i -> DoubleArray(n) { j ->
        if (i == j) 0.0 else sqrt(x[i].indices.sumOf { k -> (x[i][k] - x[j][k]).pow(2) })
    } }
    var sigma = 1.0
    run {
        var sum = 0.0; var count = 0
        for (i in 0 until n) for (j in i + 1 until n) { sum += dist[i][j]; count++ }
        if (count > 0) sigma = (sum / count / 2.0).coerceAtLeast(1e-6)
    }
    val p = Array(n) { DoubleArray(n) }
    var pSum = 0.0
    for (i in 0 until n) for (j in 0 until n) {
        if (i != j) {
            p[i][j] = exp(-dist[i][j] * dist[i][j] / (2 * sigma * sigma))
            pSum += p[i][j]
        }
    }
    if (pSum <= 0) return emptyList()
    for (i in 0 until n) for (j in 0 until n) p[i][j] = (p[i][j] / pSum).coerceAtLeast(1e-12)
    val y = Array(n) { DoubleArray(2) { kotlin.random.Random.nextDouble() * 1e-4 } }
    val velocity = Array(n) { DoubleArray(2) }
    val lr = 0.6
    repeat(iterations) {
        val q = Array(n) { DoubleArray(n) }
        var qSum = 0.0
        for (i in 0 until n) for (j in i + 1 until n) {
            val d = 1.0 / (1.0 + (y[i][0] - y[j][0]).pow(2) + (y[i][1] - y[j][1]).pow(2))
            q[i][j] = d; q[j][i] = d; qSum += 2 * d
        }
        if (qSum <= 0) return@repeat
        for (i in 0 until n) for (j in 0 until n) q[i][j] = (q[i][j] / qSum).coerceAtLeast(1e-12)
        val grad = Array(n) { DoubleArray(2) }
        for (i in 0 until n) for (j in 0 until n) {
            if (i == j) continue
            val d2 = 1.0 + (y[i][0] - y[j][0]).pow(2) + (y[i][1] - y[j][1]).pow(2)
            val factor = 4 * (p[i][j] - q[i][j]) / d2
            grad[i][0] += factor * (y[i][0] - y[j][0])
            grad[i][1] += factor * (y[i][1] - y[j][1])
        }
        for (i in 0 until n) {
            velocity[i][0] = 0.8 * velocity[i][0] - lr * grad[i][0]
            velocity[i][1] = 0.8 * velocity[i][1] - lr * grad[i][1]
            y[i][0] += velocity[i][0]
            y[i][1] += velocity[i][1]
        }
        var mx = 0.0; var my = 0.0
        for (i in 0 until n) { mx += y[i][0]; my += y[i][1] }
        mx /= n; my /= n
        for (i in 0 until n) { y[i][0] -= mx; y[i][1] -= my }
    }
    return y.map { it[0] to it[1] }
}
