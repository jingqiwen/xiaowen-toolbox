package com.jisuanyusuiji.toolbox.tools.probability

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.InfoRow
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

private fun erf(x: Double): Double {
    val sign = if (x < 0) -1.0 else 1.0
    val ax = abs(x)
    val t = 1.0 / (1.0 + 0.3275911 * ax)
    val y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * exp(-ax * ax)
    return sign * y
}

private fun normalCdf(z: Double): Double = 0.5 * (1.0 + erf(z / sqrt(2.0)))

private fun normalPdf(x: Double, mu: Double, sigma: Double): Double =
    exp(-((x - mu) * (x - mu)) / (2 * sigma * sigma)) / (sigma * sqrt(2 * Math.PI))

private fun normalInv(p: Double): Double {
    if (p <= 0.0) return Double.NEGATIVE_INFINITY
    if (p >= 1.0) return Double.POSITIVE_INFINITY
    val a = doubleArrayOf(-3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02, 1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00)
    val b = doubleArrayOf(-5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02, 6.680131188771972e+01, -1.328068155288572e+01)
    val c = doubleArrayOf(-7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00, -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00)
    val d = doubleArrayOf(7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00)
    val plow = 0.02425
    val phigh = 1 - plow
    return when {
        p < plow -> {
            val q = sqrt(-2 * ln(p))
            (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1)
        }
        p <= phigh -> {
            val q = p - 0.5
            val r = q * q
            (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q /
                (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1)
        }
        else -> {
            val q = sqrt(-2 * ln(1 - p))
            -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1)
        }
    }
}

private fun combination(n: Int, k: Int): Double {
    if (k < 0 || k > n) return 0.0
    var result = 1.0
    for (i in 1..k) result = result * (n - k + i) / i
    return result
}

@Composable
fun ProbabilityTool() {
    var tab by remember { mutableStateOf("公式库") }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("全部") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("🎲 概率论公式与分布计算", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        ChoiceChips(listOf("公式库", "分布计算"), tab, { tab = it }, { it })
        Spacer(Modifier.height(8.dp))
        if (tab == "公式库") {
            val categories = remember { listOf("全部") + ProbabilityData.all.map { it.category }.distinct() }
            val items = remember(query, category) {
                ProbabilityData.all.filter {
                    (category == "全部" || it.category == category) &&
                        (query.isBlank() || it.name.contains(query, true) || it.expression.contains(query, true) || it.note.contains(query, true))
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("搜索概率公式") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            ChoiceChips(categories, category, { category = it }, { it })
            Spacer(Modifier.height(6.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.category + it.name }) { f ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text("${f.name} · ${f.category}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(f.expression, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(f.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            CopyButton("${f.name}\n${f.expression}\n${f.note}")
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        } else {
            DistributionCalculator()
        }
    }
}

@Composable
private fun DistributionCalculator() {
    var type by remember { mutableStateOf("正态分布") }
    var p1 by remember { mutableStateOf("0") }
    var p2 by remember { mutableStateOf("1") }
    var p3 by remember { mutableStateOf("1.96") }
    var result by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var error by remember { mutableStateOf("") }

    fun calc() {
        error = ""
        result = emptyList()
        try {
            when (type) {
                "正态分布" -> {
                    val mu = p1.toDoubleOrNull() ?: throw IllegalArgumentException("μ 无效")
                    val sigma = p2.toDoubleOrNull() ?: throw IllegalArgumentException("σ 无效")
                    if (sigma <= 0) throw IllegalArgumentException("σ 必须大于 0")
                    val x = p3.toDoubleOrNull() ?: throw IllegalArgumentException("x 无效")
                    val z = (x - mu) / sigma
                    result = listOf(
                        "标准化 Z" to "%.6f".format(z),
                        "概率密度 f(x)" to "%.8f".format(normalPdf(x, mu, sigma)),
                        "P(X ≤ x) = Φ(z)" to "%.8f".format(normalCdf(z)),
                        "P(X > x)" to "%.8f".format(1 - normalCdf(z)),
                        "P(μ-σ < X < μ+σ)" to "0.682689",
                        "P(μ-2σ < X < μ+2σ)" to "0.954500",
                        "P(μ-3σ < X < μ+3σ)" to "0.997300"
                    )
                }
                "正态逆分布" -> {
                    val mu = p1.toDoubleOrNull() ?: throw IllegalArgumentException("μ 无效")
                    val sigma = p2.toDoubleOrNull() ?: throw IllegalArgumentException("σ 无效")
                    val p = p3.toDoubleOrNull() ?: throw IllegalArgumentException("概率 p 无效")
                    if (p <= 0 || p >= 1) throw IllegalArgumentException("p 必须在 (0,1) 之间")
                    val z = normalInv(p)
                    result = listOf("分位数 z" to "%.6f".format(z), "对应 X" to "%.6f".format(mu + sigma * z))
                }
                "二项分布" -> {
                    val n = p1.toIntOrNull() ?: throw IllegalArgumentException("n 无效")
                    val p = p2.toDoubleOrNull() ?: throw IllegalArgumentException("p 无效")
                    val k = p3.toIntOrNull() ?: throw IllegalArgumentException("k 无效")
                    if (n < 0 || p !in 0.0..1.0 || k !in 0..n) throw IllegalArgumentException("参数范围错误")
                    val pk = combination(n, k) * Math.pow(p, k.toDouble()) * Math.pow(1 - p, (n - k).toDouble())
                    var cumulative = 0.0
                    for (i in 0..k) cumulative += combination(n, i) * Math.pow(p, i.toDouble()) * Math.pow(1 - p, (n - i).toDouble())
                    result = listOf(
                        "P(X = k)" to "%.8f".format(pk),
                        "P(X ≤ k)" to "%.8f".format(cumulative),
                        "E(X) = np" to "%.6f".format(n * p),
                        "D(X) = np(1-p)" to "%.6f".format(n * p * (1 - p))
                    )
                }
                "泊松分布" -> {
                    val lambda = p1.toDoubleOrNull() ?: throw IllegalArgumentException("λ 无效")
                    val k = p2.toIntOrNull() ?: throw IllegalArgumentException("k 无效")
                    if (lambda <= 0 || k < 0) throw IllegalArgumentException("参数范围错误")
                    var term = Math.pow(lambda, k.toDouble()) * exp(-lambda)
                    var fact = 1.0
                    for (i in 1..k) fact *= i
                    val pk = term / fact
                    var cumulative = 0.0
                    for (i in 0..k) {
                        var f = 1.0
                        for (j in 1..i) f *= j
                        cumulative += Math.pow(lambda, i.toDouble()) * exp(-lambda) / f
                    }
                    result = listOf(
                        "P(X = k)" to "%.8f".format(pk),
                        "P(X ≤ k)" to "%.8f".format(cumulative),
                        "E(X) = D(X) = λ" to "%.6f".format(lambda)
                    )
                }
                else -> {
                    val lambda = p1.toDoubleOrNull() ?: throw IllegalArgumentException("λ 无效")
                    val x = p2.toDoubleOrNull() ?: throw IllegalArgumentException("x 无效")
                    if (lambda <= 0 || x < 0) throw IllegalArgumentException("参数范围错误")
                    result = listOf(
                        "概率密度 f(x)" to "%.8f".format(lambda * exp(-lambda * x)),
                        "P(X ≤ x)" to "%.8f".format(1 - exp(-lambda * x)),
                        "E(X) = 1/λ" to "%.6f".format(1 / lambda)
                    )
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "输入错误"
        }
    }

    Column(Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        SectionCard(title = "选择分布") {
            ChoiceChips(
                listOf("正态分布", "正态逆分布", "二项分布", "泊松分布", "指数分布"),
                type,
                { type = it; result = emptyList(); error = "" },
                { it }
            )
        }
        SectionCard(title = "参数") {
            when (type) {
                "正态分布" -> {
                    LabeledField(p1, { p1 = it }, "均值 μ", keyboardType = KeyboardType.Decimal)
                    LabeledField(p2, { p2 = it }, "标准差 σ", keyboardType = KeyboardType.Decimal)
                    LabeledField(p3, { p3 = it }, "x（求 P(X≤x) 和密度）", keyboardType = KeyboardType.Decimal)
                }
                "正态逆分布" -> {
                    LabeledField(p1, { p1 = it }, "均值 μ", keyboardType = KeyboardType.Decimal)
                    LabeledField(p2, { p2 = it }, "标准差 σ", keyboardType = KeyboardType.Decimal)
                    LabeledField(p3, { p3 = it }, "累积概率 p（0~1）", keyboardType = KeyboardType.Decimal)
                }
                "二项分布" -> {
                    LabeledField(p1, { p1 = it }, "试验次数 n", keyboardType = KeyboardType.Number)
                    LabeledField(p2, { p2 = it }, "成功概率 p", keyboardType = KeyboardType.Decimal)
                    LabeledField(p3, { p3 = it }, "成功次数 k", keyboardType = KeyboardType.Number)
                }
                "泊松分布" -> {
                    LabeledField(p1, { p1 = it }, "λ（平均发生次数）", keyboardType = KeyboardType.Decimal)
                    LabeledField(p2, { p2 = it }, "次数 k", keyboardType = KeyboardType.Number)
                }
                else -> {
                    LabeledField(p1, { p1 = it }, "λ", keyboardType = KeyboardType.Decimal)
                    LabeledField(p2, { p2 = it }, "x", keyboardType = KeyboardType.Decimal)
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = { calc() }, modifier = Modifier.fillMaxWidth()) { Text("计算") }
        }
        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "计算结果") {
                result.forEach { (k, v) -> InfoRow("$k：", v) }
            }
        }
        SectionCard(title = "知识点") {
            Text(
                "• 正态分布是中心极限定理的核心，大量独立随机变量之和近似正态。\n" +
                    "• 二项分布适合“n 次独立重复试验的成功次数”，n 大 p 小时可用泊松分布近似（λ=np）。\n" +
                    "• 泊松分布描述单位时间/面积内稀有事件发生次数，期望与方差都等于 λ。\n" +
                    "• 指数分布描述等待时间，具有无记忆性。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
