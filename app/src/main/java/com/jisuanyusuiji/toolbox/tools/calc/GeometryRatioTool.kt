package com.jisuanyusuiji.toolbox.tools.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.InfoRow
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.sqrt

private fun f2(v: Double): String = try {
    BigDecimal(v).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
} catch (_: Exception) { v.toString() }

private fun d(text: String): Double? = text.trim().toDoubleOrNull()

// ============================================================
// 22. 几何计算器
// ============================================================
@Composable
fun GeometryTool() {
    var shape by remember { mutableStateOf("圆") }
    var r by remember { mutableStateOf("5") }
    var a by remember { mutableStateOf("3") }
    var b by remember { mutableStateOf("4") }
    var c by remember { mutableStateOf("5") }
    var h by remember { mutableStateOf("4") }
    var result by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }

    fun calc() {
        error = ""
        result = emptyList()
        when (shape) {
            "圆" -> {
                val rr = d(r) ?: run { error = "请输入有效半径"; return }
                if (rr < 0) { error = "半径不能为负"; return }
                result = listOf("周长：${f2(2 * PI * rr)}", "面积：${f2(PI * rr * rr)}")
            }
            "三角形" -> {
                val x = d(a); val y = d(b); val z = d(c)
                if (x == null || y == null || z == null) { error = "请输入有效边长"; return }
                if (x <= 0 || y <= 0 || z <= 0 || x + y <= z || x + z <= y || y + z <= x) {
                    error = "三边无法构成三角形"
                    return
                }
                val s = (x + y + z) / 2
                val area = sqrt(s * (s - x) * (s - y) * (s - z))
                result = listOf("周长：${f2(x + y + z)}", "面积（海伦公式）：${f2(area)}")
            }
            "矩形" -> {
                val w = d(a); val hh = d(b)
                if (w == null || hh == null || w <= 0 || hh <= 0) { error = "请输入有效的长和宽"; return }
                result = listOf("周长：${f2(2 * (w + hh))}", "面积：${f2(w * hh)}")
            }
            "梯形" -> {
                val top = d(a); val bottom = d(b); val hh = d(h)
                val left = d(c)?.let { if (it > 0) it else null } ?: 0.0
                val right = d(r)?.let { if (it > 0) it else null } ?: 0.0
                if (top == null || bottom == null || hh == null || top < 0 || bottom < 0 || hh <= 0) {
                    error = "请填写有效的上底、下底和高"
                    return
                }
                result = listOf(
                    "面积：${f2((top + bottom) * hh / 2)}",
                    "周长：${f2(top + bottom + left + right)}（需填写两腰长）"
                )
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "图形") {
            ChoiceChips(listOf("圆", "三角形", "矩形", "梯形"), shape, { shape = it }, { it })
        }
        SectionCard(title = "参数") {
            when (shape) {
                "圆" -> LabeledField(r, { r = it }, "半径", keyboardType = KeyboardType.Decimal)
                "三角形" -> {
                    LabeledField(a, { a = it }, "边 a", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(b, { b = it }, "边 b", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(c, { c = it }, "边 c", keyboardType = KeyboardType.Decimal)
                }
                "矩形" -> {
                    LabeledField(a, { a = it }, "长", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(b, { b = it }, "宽", keyboardType = KeyboardType.Decimal)
                }
                "梯形" -> {
                    LabeledField(a, { a = it }, "上底", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(b, { b = it }, "下底", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(h, { h = it }, "高", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(c, { c = it }, "左腰长（计算周长用，可留空）", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(r, { r = it }, "右腰长（计算周长用，可留空）", keyboardType = KeyboardType.Decimal)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { calc() }, modifier = Modifier.fillMaxWidth()) { Text("计算") }
        }
        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "结果") {
                result.forEach { InfoRow("", it) }
            }
        }
    }
}

// ============================================================
// 23. 比例计算器
// ============================================================
@Composable
fun RatioTool() {
    var mode by remember { mutableStateOf("解比例 A:B=C:D") }
    var a by remember { mutableStateOf("2") }
    var b by remember { mutableStateOf("6") }
    var c by remember { mutableStateOf("5") }
    var d by remember { mutableStateOf("") }
    var ratioText by remember { mutableStateOf("2:3:5") }
    var totalText by remember { mutableStateOf("1000") }
    var result by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }

    fun parseRatio(text: String): List<Double>? {
        val parts = text.split(':', '：', ',', '，').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        val nums = parts.map { it.toDoubleOrNull() ?: return null }
        if (nums.any { it <= 0 }) return null
        return nums
    }

    fun gcdLong(x: Long, y: Long): Long = if (y == 0L) x else gcdLong(y, x % y)

    fun solve() {
        error = ""
        result = emptyList()
        when (mode) {
            "解比例 A:B=C:D" -> {
                val values = listOf(a, b, c, d).map { it.trim() }
                if (values.count { it.isEmpty() } != 1) { error = "请只留空一个未知项"; return }
                val nums = values.map { it.toDoubleOrNull() }
                val idx = nums.indexOfFirst { it == null }
                if (idx < 0) { error = "请至少留空一个未知项"; return }
                val known = nums.filterIndexed { i, _ -> i != idx }.map { it!! }
                val denominator = when (idx) {
                    0 -> known[2]
                    1 -> known[1]
                    2 -> known[1]
                    else -> known[0]
                }
                if (denominator == 0.0) { error = "除数为 0，无法求解"; return }
                val value = when (idx) {
                    0 -> known[0] * known[1] / known[2]
                    1 -> known[0] * known[2] / known[1]
                    2 -> known[0] * known[2] / known[1]
                    else -> known[1] * known[2] / known[0]
                }
                result = listOf("${listOf("A", "B", "C", "D")[idx]} = ${f2(value)}")
            }
            "比例化简" -> {
                val nums = parseRatio(ratioText) ?: run { error = "请输入如 6:9 或 2:3:4 的比例"; return }
                val allInt = nums.all { it == it.toLong().toDouble() }
                val label = ratioText.trim()
                if (allInt) {
                    val longs = nums.map { it.toLong() }
                    var g = longs[0]
                    longs.drop(1).forEach { g = gcdLong(g, it) }
                    val simplest = longs.map { it / g }
                    result = listOf(
                        "原比例：$label",
                        "最简整数比：${simplest.joinToString(":")}",
                        "比值（前项÷后项）：${f2(nums[0] / nums[1])}"
                    )
                } else {
                    val min = nums.min()
                    result = listOf("原比例：$label", "统一化简（除以最小值）：${nums.joinToString(":") { f2(it / min) }}")
                }
            }
            "按比例分配" -> {
                val nums = parseRatio(ratioText) ?: run { error = "请输入如 2:3:5 的比例"; return }
                val total = totalText.trim().toDoubleOrNull() ?: run { error = "请输入有效总金额/总量"; return }
                val sum = nums.sum()
                result = nums.mapIndexed { index, value ->
                    "第 ${index + 1} 份（占比 ${f2(value / sum * 100)}%）：${f2(total * value / sum)}"
                }
            }
            else -> {
                val nums = parseRatio(ratioText) ?: run { error = "请输入如 2:3:5 的比例"; return }
                val sum = nums.sum()
                result = nums.mapIndexed { index, value ->
                    "第 ${index + 1} 项：${f2(value / sum * 100)}%"
                }
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "功能") {
            ChoiceChips(
                options = listOf("解比例 A:B=C:D", "比例化简", "按比例分配", "比例转百分比"),
                selected = mode,
                onSelect = { mode = it; result = emptyList(); error = "" },
                label = { it }
            )
        }

        SectionCard(title = mode) {
            when (mode) {
                "解比例 A:B=C:D" -> {
                    LabeledField(a, { a = it }, "A", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(b, { b = it }, "B", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(c, { c = it }, "C", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(d, { d = it }, "D（未知项请留空）", keyboardType = KeyboardType.Decimal)
                }
                "按比例分配" -> {
                    LabeledField(totalText, { totalText = it }, "总金额 / 总量（如 1000）", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(ratioText, { ratioText = it }, "比例（如 2:3:5）")
                }
                else -> LabeledField(ratioText, { ratioText = it }, "比例（如 6:9 或 2:3:4）")
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { solve() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (mode == "解比例 A:B=C:D") "求解未知项" else "计算")
            }
        }

        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "结果") {
                result.forEach { line ->
                    Text(line, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        SectionCard(title = "知识点") {
            Text(
                "• 比例的基本性质：内项之积 = 外项之积（A×D = B×C）。\n" +
                    "• 比例化简：各项同时除以最大公约数。\n" +
                    "• 按比例分配：每份 = 总量 ÷ 比例之和 × 该项比例。\n" +
                    "• 比例与百分比：某项占比 = 该项 ÷ 各项之和 × 100%。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
