package com.jisuanyusuiji.toolbox.tools.calc

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.tools.graph.GraphParser
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.InfoRow
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.ResultText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

private data class SciConstant(val name: String, val symbol: String, val value: String, val unit: String)

private val CONSTANTS = listOf(
    SciConstant("真空中光速", "c", "2.99792458×10⁸", "m/s"),
    SciConstant("普朗克常数", "h", "6.62607015×10⁻³⁴", "J·s"),
    SciConstant("约化普朗克常数", "ħ", "1.054571817×10⁻³⁴", "J·s"),
    SciConstant("元电荷", "e", "1.602176634×10⁻¹⁹", "C"),
    SciConstant("电子质量", "mₑ", "9.1093837015×10⁻³¹", "kg"),
    SciConstant("质子质量", "mₚ", "1.67262192369×10⁻²⁷", "kg"),
    SciConstant("中子质量", "mₙ", "1.67492749804×10⁻²⁷", "kg"),
    SciConstant("阿伏伽德罗常数", "Nₐ", "6.02214076×10²³", "/mol"),
    SciConstant("玻尔兹曼常数", "k", "1.380649×10⁻²³", "J/K"),
    SciConstant("理想气体常数", "R", "8.314462618", "J/(mol·K)"),
    SciConstant("引力常数", "G", "6.67430×10⁻¹¹", "N·m²/kg²"),
    SciConstant("标准重力加速度", "g", "9.80665", "m/s²"),
    SciConstant("真空介电常数", "ε₀", "8.8541878128×10⁻¹²", "F/m"),
    SciConstant("真空磁导率", "μ₀", "1.25663706212×10⁻⁶", "N/A²"),
    SciConstant("库仑常数", "kₑ", "8.9875517923×10⁹", "N·m²/C²"),
    SciConstant("法拉第常数", "F", "96485.33212", "C/mol"),
    SciConstant("斯特藩-玻尔兹曼常数", "σ", "5.670374419×10⁻⁸", "W/(m²·K⁴)"),
    SciConstant("维恩位移常数", "b", "2.897771955×10⁻³", "m·K"),
    SciConstant("里德伯常数", "R∞", "1.0973731568160×10⁷", "1/m"),
    SciConstant("玻尔半径", "a₀", "5.29177210903×10⁻¹¹", "m"),
    SciConstant("电子伏特", "eV", "1.602176634×10⁻¹⁹", "J"),
    SciConstant("原子质量单位", "u", "1.66053906660×10⁻²⁷", "kg"),
    SciConstant("标准大气压", "atm", "101325", "Pa"),
    SciConstant("冰点温度", "T₀", "273.15", "K"),
    SciConstant("绝对零度", "0K", "-273.15", "℃"),
    SciConstant("水的密度（4℃）", "ρ水", "999.972", "kg/m³"),
    SciConstant("空气中的声速（20℃）", "v声", "343.2", "m/s"),
    SciConstant("地球质量", "M⊕", "5.9722×10²⁴", "kg"),
    SciConstant("地球半径", "R⊕", "6.371×10⁶", "m"),
    SciConstant("天文单位", "AU", "1.495978707×10¹¹", "m"),
    SciConstant("光年", "ly", "9.4607304725808×10¹⁵", "m"),
    SciConstant("秒差距", "pc", "3.085677581491×10¹⁶", "m")
)

private data class UnitGroup(val name: String, val units: List<Pair<String, Double>>)

private val UNIT_GROUPS = listOf(
    UnitGroup("长度", listOf("米 m" to 1.0, "千米 km" to 1000.0, "厘米 cm" to 0.01, "毫米 mm" to 0.001, "英寸 in" to 0.0254, "英尺 ft" to 0.3048, "英里 mi" to 1609.344, "海里 nmi" to 1852.0)),
    UnitGroup("质量", listOf("千克 kg" to 1.0, "克 g" to 0.001, "毫克 mg" to 1e-6, "吨 t" to 1000.0, "斤" to 0.5, "磅 lb" to 0.45359237, "盎司 oz" to 0.028349523)),
    UnitGroup("面积", listOf("平方米 m²" to 1.0, "平方千米 km²" to 1e6, "平方厘米 cm²" to 1e-4, "公顷 ha" to 1e4, "亩" to 2000.0 / 3.0, "英亩 acre" to 4046.8564224)),
    UnitGroup("体积", listOf("立方米 m³" to 1.0, "升 L" to 0.001, "毫升 mL" to 1e-6, "立方厘米 cm³" to 1e-6, "加仑 gal(US)" to 0.003785411784)),
    UnitGroup("速度", listOf("米/秒 m/s" to 1.0, "千米/时 km/h" to 1.0 / 3.6, "英里/时 mph" to 0.44704, "节 kn" to 0.514444)),
    UnitGroup("压力", listOf("帕 Pa" to 1.0, "千帕 kPa" to 1000.0, "兆帕 MPa" to 1e6, "巴 bar" to 1e5, "标准大气压 atm" to 101325.0, "毫米汞柱 mmHg" to 133.322)),
    UnitGroup("能量", listOf("焦耳 J" to 1.0, "千焦 kJ" to 1000.0, "卡 cal" to 4.184, "千卡 kcal" to 4184.0, "瓦时 Wh" to 3600.0, "千瓦时 kWh" to 3.6e6)),
    UnitGroup("功率", listOf("瓦 W" to 1.0, "千瓦 kW" to 1000.0, "马力 hp" to 745.7, "千卡/时 kcal/h" to 1.163))
)

@Composable
fun CasioProTool() {
    var mode by remember { mutableStateOf("统计") }
    var text by remember { mutableStateOf("1,2,3,4,5,6,7,8,9,10") }
    var text2 by remember { mutableStateOf("1,2\n2,4\n3,6\n4,8\n5,10") }
    var expr by remember { mutableStateOf("x^2") }
    var p1 by remember { mutableStateOf("1") }
    var p2 by remember { mutableStateOf("10") }
    var p3 by remember { mutableStateOf("1") }
    var result by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var error by remember { mutableStateOf("") }

    fun nums(s: String): List<Double> =
        Regex("-?\\d+(\\.\\d+)?([eE][-+]?\\d+)?").findAll(s).mapNotNull { it.value.toDoubleOrNull() }.toList()

    fun calcStats() {
        val v = nums(text)
        if (v.size < 2) throw IllegalArgumentException("至少输入两个数字")
        val m = v.sum() / v.size
        val variance = v.sumOf { (it - m) * (it - m) } / (v.size - 1)
        val sorted = v.sorted()
        fun q(p: Double): Double {
            val pos = (sorted.size - 1) * p
            val lo = pos.toInt(); val hi = (lo + 1).coerceAtMost(sorted.size - 1)
            return sorted[lo] * (1 - (pos - lo)) + sorted[hi] * (pos - lo)
        }
        result = listOf(
            "样本数 n" to "${v.size}",
            "平均值 x̄" to "%.6f".format(m),
            "样本标准差 s" to "%.6f".format(sqrt(variance)),
            "样本方差 s²" to "%.6f".format(variance),
            "标准误 SEM" to "%.6f".format(sqrt(variance / v.size)),
            "最小值" to "%.6f".format(sorted.first()),
            "Q1" to "%.6f".format(q(0.25)),
            "中位数" to "%.6f".format(q(0.5)),
            "Q3" to "%.6f".format(q(0.75)),
            "最大值" to "%.6f".format(sorted.last()),
            "极差" to "%.6f".format(sorted.last() - sorted.first())
        )
    }

    fun linfit(x: List<Double>, y: List<Double>): DoubleArray {
        val n = x.size
        val mx = x.sum() / n; val my = y.sum() / n
        val sxx = x.sumOf { (it - mx) * (it - mx) }
        val sxy = x.indices.sumOf { (x[it] - mx) * (y[it] - my) }
        val b = sxy / sxx
        val a = my - b * mx
        val ssTot = y.sumOf { (it - my) * (it - my) }
        val ssRes = y.indices.sumOf { val p = a + b * x[it]; (y[it] - p) * (y[it] - p) }
        val r2 = if (ssTot < 1e-12) 1.0 else 1 - ssRes / ssTot
        return doubleArrayOf(a, b, r2)
    }

    fun calcRegression(regMode: String) {
        val pairs = text2.lines().mapNotNull { line ->
            val parts = Regex("-?\\d+(\\.\\d+)?").findAll(line).map { it.value.toDouble() }.toList()
            if (parts.size >= 2) parts[0] to parts[1] else null
        }
        if (pairs.size < 3) throw IllegalArgumentException("至少输入 3 组 x,y 数据")
        val x = pairs.map { it.first }; val y = pairs.map { it.second }
        when (regMode) {
            "线性 y=a+bx" -> {
                val f = linfit(x, y)
                result = listOf("a（截距）" to "%.6f".format(f[0]), "b（斜率）" to "%.6f".format(f[1]), "R²" to "%.6f".format(f[2]))
            }
            "二次 y=a+bx+cx²" -> {
                val n = x.size
                val sx = x.sum(); val sx2 = x.sumOf { it * it }; val sx3 = x.sumOf { it * it * it }; val sx4 = x.sumOf { it.pow(4) }
                val sy = y.sum(); val sxy = x.indices.sumOf { x[it] * y[it] }; val sx2y = x.indices.sumOf { x[it] * x[it] * y[it] }
                val m = arrayOf(
                    doubleArrayOf(n.toDouble(), sx, sx2, sy),
                    doubleArrayOf(sx, sx2, sx3, sxy),
                    doubleArrayOf(sx2, sx3, sx4, sx2y)
                )
                for (i in 0..2) {
                    var pivot = i
                    for (j in i + 1..2) if (abs(m[j][i]) > abs(m[pivot][i])) pivot = j
                    val t = m[i]; m[i] = m[pivot]; m[pivot] = t
                    for (j in i + 1..2) {
                        val f = m[j][i] / m[i][i]
                        for (k in i..3) m[j][k] -= f * m[i][k]
                    }
                }
                val c2 = m[2][3] / m[2][2]
                val b2 = (m[1][3] - m[1][2] * c2) / m[1][1]
                val a2 = (m[0][3] - m[0][1] * b2 - m[0][2] * c2) / m[0][0]
                val my = y.sum() / n
                val ssTot = y.sumOf { (it - my) * (it - my) }
                val ssRes = x.indices.sumOf { val p = a2 + b2 * x[it] + c2 * x[it] * x[it]; (y[it] - p) * (y[it] - p) }
                result = listOf("a" to "%.6f".format(a2), "b" to "%.6f".format(b2), "c" to "%.6f".format(c2), "R²" to "%.6f".format(if (ssTot < 1e-12) 1.0 else 1 - ssRes / ssTot))
            }
            "指数 y=a·e^(bx)" -> {
                val ly = y.map { ln(it) }
                val f = linfit(x, ly)
                result = listOf("a" to "%.6f".format(exp(f[0])), "b" to "%.6f".format(f[1]), "R²" to "%.6f".format(f[2]))
            }
            "对数 y=a+b·ln(x)" -> {
                val lx = x.map { ln(it) }
                val f = linfit(lx, y)
                result = listOf("a" to "%.6f".format(f[0]), "b" to "%.6f".format(f[1]), "R²" to "%.6f".format(f[2]))
            }
            else -> {
                val lx = x.map { ln(it) }; val ly = y.map { ln(it) }
                val f = linfit(lx, ly)
                result = listOf("a" to "%.6f".format(exp(f[0])), "b（指数）" to "%.6f".format(f[1]), "R²" to "%.6f".format(f[2]))
            }
        }
    }

    fun calcTable() {
        val start = p1.toDoubleOrNull() ?: throw IllegalArgumentException("起始 x 无效")
        val end = p2.toDoubleOrNull() ?: throw IllegalArgumentException("结束 x 无效")
        val step = p3.toDoubleOrNull() ?: throw IllegalArgumentException("步长无效")
        if (step == 0.0) throw IllegalArgumentException("步长不能为 0")
        val rows = mutableListOf<Pair<String, String>>()
        var x = start
        var count = 0
        while ((step > 0 && x <= end + 1e-12) || (step < 0 && x >= end - 1e-12)) {
            val y = try {
                GraphParser(expr) { name -> if (name == "x") x else null }.parse()
            } catch (_: Exception) { Double.NaN }
            rows.add("%.4f".format(x) to "%.6f".format(y))
            x += step
            if (++count > 500) break
        }
        result = rows.take(80)
    }

    fun calcVector(op: String) {
        val a = nums(p1)
        val b = nums(p2)
        if (a.size < 2 || b.size < 2) throw IllegalArgumentException("向量至少需要 2 个分量（用逗号分隔）")
        fun dot(u: List<Double>, v: List<Double>) = u.indices.sumOf { u[it] * v[it] }
        fun norm(u: List<Double>) = sqrt(u.sumOf { it * it })
        result = when (op) {
            "A + B" -> listOf("A+B" to (a.indices).joinToString(", ") { "%.4f".format(a[it] + b.getOrElse(it) { 0.0 }) })
            "A − B" -> listOf("A−B" to (a.indices).joinToString(", ") { "%.4f".format(a[it] - b.getOrElse(it) { 0.0 }) })
            "点积 A·B" -> listOf("A·B" to "%.6f".format(dot(a, b)))
            "模长 |A|" -> listOf("|A|" to "%.6f".format(norm(a)), "|B|" to "%.6f".format(norm(b)))
            "单位向量 A/|A|" -> listOf("A 的单位向量" to a.joinToString(", ") { "%.6f".format(it / norm(a)) })
            "夹角" -> {
                val cosT = dot(a, b) / (norm(a) * norm(b))
                listOf("cosθ" to "%.6f".format(cosT), "θ" to "%.4f°".format(Math.toDegrees(kotlin.math.acos(cosT.coerceIn(-1.0, 1.0)))))
            }
            else -> {
                if (a.size < 3 || b.size < 3) throw IllegalArgumentException("叉积需要 3 维向量")
                val cx = a[1] * b[2] - a[2] * b[1]
                val cy = a[2] * b[0] - a[0] * b[2]
                val cz = a[0] * b[1] - a[1] * b[0]
                listOf("A×B" to "%.4f, %.4f, %.4f".format(cx, cy, cz))
            }
        }
    }

    fun calcInequality(op: String) {
        val a = p1.toDoubleOrNull() ?: throw IllegalArgumentException("a 无效")
        val b = p2.toDoubleOrNull() ?: throw IllegalArgumentException("b 无效")
        val c = p3.toDoubleOrNull() ?: throw IllegalArgumentException("c 无效")
        if (a == 0.0) {
            val x = -c / b
            result = listOf("退化为一次" to "解集参考 x ${if (b > 0) ">" else "<"} ${"%.4f".format(x)}（请按符号判断）")
            return
        }
        val delta = b * b - 4 * a * c
        val rootText = if (delta >= 0) {
            val r1 = (-b - sqrt(delta)) / (2 * a)
            val r2 = (-b + sqrt(delta)) / (2 * a)
            "根：x₁=%.4f，x₂=%.4f".format(minOf(r1, r2), maxOf(r1, r2))
        } else "无实根（Δ<0）"
        result = listOf(
            "判别式 Δ" to "%.4f".format(delta),
            "根" to rootText,
            "提示" to when {
                delta < 0 && a > 0 && op.contains(">") -> "因为 a>0 且 Δ<0，对所有实数成立"
                delta < 0 && a < 0 && op.contains(">") -> "空集（无解）"
                delta < 0 -> "请结合抛物线开口方向判断"
                else -> "解集为两根之间或两根之外，取决于开口方向和不等号方向"
            }
        )
    }

    val modes = listOf("统计", "回归", "函数表格", "电子表格", "常量表", "单位换算", "向量", "二次不等式", "变量存储")

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "fx-999 专业模式") {
            ChoiceChips(modes, mode, { mode = it; result = emptyList(); error = "" }, { it })
        }

        when (mode) {
            "统计" -> SectionCard(title = "列表式统计（逗号/空格分隔）") {
                LabeledField(text, { text = it }, "数据", singleLine = false, minLines = 4)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    error = ""
                    try { calcStats() } catch (e: Exception) { error = e.message ?: "错误" }
                }, modifier = Modifier.fillMaxWidth()) { Text("计算统计量") }
            }
            "回归" -> SectionCard(title = "回归分析（每行一组 x,y）") {
                LabeledField(text2, { text2 = it }, "数据", singleLine = false, minLines = 4)
                Spacer(Modifier.height(8.dp))
                regTypes.forEach { type ->
                    Button(onClick = {
                        error = ""
                        try { calcRegression(type) } catch (e: Exception) { error = e.message ?: "错误" }
                    }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(type) }
                }
            }
            "函数表格" -> SectionCard(title = "函数表格 f(x)") {
                LabeledField(expr, { expr = it }, "f(x) 表达式")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField(p1, { p1 = it }, "起始 x", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                    LabeledField(p2, { p2 = it }, "结束 x", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                }
                LabeledField(p3, { p3 = it }, "步长", keyboardType = KeyboardType.Decimal)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    error = ""
                    try { calcTable() } catch (e: Exception) { error = e.message ?: "错误" }
                }, modifier = Modifier.fillMaxWidth()) { Text("生成表格") }
            }
            "常量表" -> SectionCard(title = "科学常数表（${CONSTANTS.size} 条）") {
                CONSTANTS.forEach { c ->
                    InfoRow("${c.name}（${c.symbol}）", "${c.value} ${c.unit}")
                }
            }
            "电子表格" -> SpreadsheetSection()
            "单位换算" -> UnitConversionSection()
            "向量" -> SectionCard(title = "二维/三维向量") {
                LabeledField(p1, { p1 = it }, "向量 A（如 1,2,3）")
                LabeledField(p2, { p2 = it }, "向量 B（如 4,5,6）")
                Spacer(Modifier.height(8.dp))
                listOf("A + B", "A − B", "点积 A·B", "模长 |A|", "单位向量 A/|A|", "叉积 A×B", "夹角").forEach { op ->
                    Button(onClick = {
                        error = ""
                        try { calcVector(op) } catch (e: Exception) { error = e.message ?: "错误" }
                    }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(op) }
                }
            }
            "二次不等式" -> SectionCard(title = "一元二次不等式 ax²+bx+c") {
                LabeledField(p1, { p1 = it }, "a", keyboardType = KeyboardType.Decimal)
                LabeledField(p2, { p2 = it }, "b", keyboardType = KeyboardType.Decimal)
                LabeledField(p3, { p3 = it }, "c", keyboardType = KeyboardType.Decimal)
                Spacer(Modifier.height(8.dp))
                listOf("ax²+bx+c > 0", "ax²+bx+c < 0", "ax²+bx+c ≥ 0", "ax²+bx+c ≤ 0").forEach { op ->
                    Button(onClick = {
                        error = ""
                        try { calcInequality(op) } catch (e: Exception) { error = e.message ?: "错误" }
                    }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(op) }
                }
            }
            else -> VariableStoreSection()
        }

        ErrorText(error)
        if (result.isNotEmpty() && mode != "单位换算") {
            SectionCard(title = "结果") {
                result.forEach { (k, v) -> InfoRow(if (k.isBlank()) "" else "$k：", v) }
            }
        }
    }
}

private val regTypes = listOf("线性 y=a+bx", "二次 y=a+bx+cx²", "指数 y=a·e^(bx)", "对数 y=a+b·ln(x)", "幂函数 y=a·x^b")

@Composable
private fun SpreadsheetSection() {
    val cols = listOf("A", "B", "C", "D", "E")
    val rows = 45
    val cells = remember { mutableStateMapOf<String, String>() }
    var computed by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var error by remember { mutableStateOf("") }
    fun ref(c: Int, r: Int) = "${cols[c]}${r + 1}"

    fun evalCell(ref: String, cache: MutableMap<String, Double>, visiting: MutableSet<String>): Double? {
        cache[ref]?.let { return it }
        if (!visiting.add(ref)) throw IllegalArgumentException("循环引用：$ref")
        try {
            val raw = cells[ref]?.trim().orEmpty()
            if (raw.isEmpty()) return null
            val value = if (!raw.startsWith("=")) {
                raw.toDoubleOrNull() ?: throw IllegalArgumentException("$ref 不是数字：$raw")
            } else {
                var expr = raw.substring(1)
                val rangeRegex = Regex("(SUM|AVERAGE|MIN|MAX|COUNT)\\(\\$?([A-E])\\$?(\\d+):\\$?([A-E])\\$?(\\d+)\\)", RegexOption.IGNORE_CASE)
                expr = rangeRegex.replace(expr) { m ->
                    val fn = m.groupValues[1].uppercase()
                    val c1 = cols.indexOf(m.groupValues[2]); val r1 = m.groupValues[3].toInt() - 1
                    val c2 = cols.indexOf(m.groupValues[4]); val r2 = m.groupValues[5].toInt() - 1
                    val values = mutableListOf<Double>()
                    for (c in minOf(c1, c2)..maxOf(c1, c2)) {
                        for (r in minOf(r1, r2)..maxOf(r1, r2)) {
                            evalCell(ref(c, r), cache, visiting)?.let { values.add(it) }
                        }
                    }
                    val res = when (fn) {
                        "SUM" -> values.sum()
                        "AVERAGE" -> if (values.isEmpty()) 0.0 else values.sum() / values.size
                        "MIN" -> values.minOrNull() ?: 0.0
                        "MAX" -> values.maxOrNull() ?: 0.0
                        else -> values.size.toDouble()
                    }
                    res.toString()
                }
                expr = Regex("\\$?([A-E])\\$?(\\d+)").replace(expr) { m ->
                    val target = "${m.groupValues[1]}${m.groupValues[2]}"
                    (evalCell(target, cache, visiting) ?: 0.0).toString()
                }
                val r = CalcExpr.evaluate(expr)
                if (!r.ok || r.value == null) throw IllegalArgumentException("$ref 公式错误：${r.error}")
                r.value
            }
            cache[ref] = value
            return value
        } finally {
            visiting.remove(ref)
        }
    }

    Column {
        SectionCard(title = "电子表格（5 列 × 45 行，支持 =SUM(A1:A5)、=A1+B1*2）") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                Text("行", Modifier.width(40.dp))
                cols.forEach { c -> Text(c, Modifier.width(86.dp), style = MaterialTheme.typography.titleMedium) }
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items((0 until rows).toList()) { r ->
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        Text("${r + 1}", Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall)
                        cols.forEachIndexed { c, name ->
                            val key = ref(c, r)
                            OutlinedTextField(
                                value = cells[key].orEmpty(),
                                onValueChange = { cells[key] = it },
                                modifier = Modifier.width(86.dp).padding(horizontal = 2.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                error = ""
                try {
                    val cache = mutableMapOf<String, Double>()
                    val results = mutableListOf<Pair<String, String>>()
                    for (refKey in cells.keys) {
                        val v = evalCell(refKey, cache, mutableSetOf())
                        if (v != null) results.add(refKey to "%.6f".format(v).trimEnd('0').trimEnd('.', ','))
                    }
                    computed = results.sortedBy { it.first }
                } catch (e: Exception) {
                    error = e.message ?: "计算错误"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("计算全部公式") }
        }
        ErrorText(error)
        if (computed.isNotEmpty()) {
            SectionCard(title = "计算结果") {
                computed.take(100).forEach { (k, v) -> InfoRow("$k =", v) }
            }
        }
        Text("支持：四则运算、括号、^、SUM/AVERAGE/MIN/MAX/COUNT、区域 A1:A5、绝对引用 \$A\$1（效果相同）。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun UnitConversionSection() {
    var groupIndex by remember { mutableStateOf(0) }
    var fromIndex by remember { mutableStateOf(0) }
    var toIndex by remember { mutableStateOf(1) }
    var valueText by remember { mutableStateOf("1") }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val group = UNIT_GROUPS[groupIndex]

    Column {
        SectionCard(title = "单位换算（${UNIT_GROUPS.size} 类）") {
            ChoiceChips(UNIT_GROUPS.indices.toList(), groupIndex, { groupIndex = it; fromIndex = 0; toIndex = 1 }, { UNIT_GROUPS[it].name })
            Spacer(Modifier.height(8.dp))
            ChoiceChips(group.units.indices.toList(), fromIndex, { fromIndex = it }, { group.units[it].first })
            Spacer(Modifier.height(6.dp))
            ChoiceChips(group.units.indices.toList(), toIndex, { toIndex = it }, { group.units[it].first })
            Spacer(Modifier.height(8.dp))
            LabeledField(valueText, { valueText = it }, "数值", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                error = ""
                val v = valueText.toDoubleOrNull()
                if (v == null) { error = "数值无效"; return@Button }
                val base = v * group.units[fromIndex].second
                val out = base / group.units[toIndex].second
                output = "%.8f".format(out).trimEnd('0').trimEnd('.')
            }, modifier = Modifier.fillMaxWidth()) { Text("换算") }
        }
        if (output.isNotBlank()) {
            SectionCard(title = "结果") { ResultText("$valueText ${group.units[fromIndex].first} = $output ${group.units[toIndex].first}") }
        }
        ErrorText(error)
    }
}

@Composable
private fun VariableStoreSection() {
    var exprText by remember { mutableStateOf("A+B") }
    val vars = remember { mutableStateMapOf<String, Double>() }
    var slot by remember { mutableStateOf("A") }
    var valueText by remember { mutableStateOf("0") }
    var output by remember { mutableStateOf("") }
    Column {
        SectionCard(title = "变量存储 A~F（可参与表达式）") {
            ChoiceChips(listOf("A", "B", "C", "D", "E", "F"), slot, { slot = it }, { it })
            Spacer(Modifier.height(6.dp))
            LabeledField(valueText, { valueText = it }, "数值", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(6.dp))
            Button(onClick = { valueText.toDoubleOrNull()?.let { vars[slot] = it } }, modifier = Modifier.fillMaxWidth()) { Text("存入 $slot") }
            Spacer(Modifier.height(10.dp))
            LabeledField(exprText, { exprText = it }, "表达式（如 A*B+C）")
            Spacer(Modifier.height(6.dp))
            Button(onClick = {
                output = try {
                    val v = GraphParser(exprText) { name -> vars[name.uppercase()] ?: vars[name] }.parse()
                    "%.8f".format(v)
                } catch (e: Exception) { "错误：${e.message}" }
            }, modifier = Modifier.fillMaxWidth()) { Text("计算") }
            Spacer(Modifier.height(8.dp))
            vars.forEach { (k, v) -> InfoRow("$k =", "%.6f".format(v)) }
            if (output.isNotBlank()) Text("结果：$output", fontWeight = FontWeight.Bold)
            Text("提示：变量只保存在本页面，退出后清空。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}
