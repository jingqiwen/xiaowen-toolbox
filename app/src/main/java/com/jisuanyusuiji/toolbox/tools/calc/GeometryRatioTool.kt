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
    var a by remember { mutableStateOf("2") }
    var b by remember { mutableStateOf("6") }
    var c by remember { mutableStateOf("5") }
    var d by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun solve() {
        error = ""
        result = ""
        val values = listOf(a, b, c, d).map { it.trim() }
        val blanks = values.count { it.isEmpty() }
        if (blanks != 1) {
            error = "请只留空一个未知项（A:B = C:D）"
            return
        }
        val nums = values.map { it.toDoubleOrNull() }
        val idx = nums.indexOfFirst { it == null }
        val known = nums.filterIndexed { i, _ -> i != idx }.map { it!! }
        val denominator = when (idx) {
            0 -> known[1] // A = B*C/D
            1 -> known[0] // B = A*D/C
            2 -> known[2] // C = A*D/B
            else -> known[1] // D = B*C/A
        }
        if (denominator == 0.0) { error = "比例中除数为 0，无法求解"; return }
        val value = when (idx) {
            0 -> known[0] * known[1] / known[2] // B*C/D (known=[B,C,D])
            1 -> known[0] * known[2] / known[1] // A*D/C (known=[A,C,D])
            2 -> known[0] * known[2] / known[1] // A*D/B (known=[A,B,D])
            else -> known[1] * known[2] / known[0] // B*C/A (known=[A,B,C])
        }
        result = "${listOf("A", "B", "C", "D")[idx]} = ${f2(value)}"
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "A : B = C : D（留空一项求解）") {
            LabeledField(a, { a = it }, "A", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(b, { b = it }, "B", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(c, { c = it }, "C", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(d, { d = it }, "D（未知请留空）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { solve() }, modifier = Modifier.fillMaxWidth()) { Text("求解未知项") }
        }
        ErrorText(error)
        if (result.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(result, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}
