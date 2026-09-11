package com.jisuanyusuiji.toolbox.tools.calc

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
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

private data class Fraction(val num: Long, val den: Long) {
    fun reduced(): Fraction {
        if (num == 0L) return Fraction(0L, 1L)
        val g = gcd(abs(num), abs(den))
        val sign = if (den < 0) -1L else 1L
        return Fraction(sign * num / g, abs(den) / g)
    }
}

private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

/** 由“整数部分 + 分子 + 分母”组成分数；整数与分子至少填一个。 */
private fun buildFraction(whole: String, num: String, den: String): Fraction? {
    val d = den.trim().ifBlank { "1" }.toLongOrNull() ?: return null
    if (d == 0L) return null
    val w = whole.trim().ifBlank { "0" }.toLongOrNull() ?: return null
    val n = num.trim().toLongOrNull() ?: return null
    val sign = if (w < 0) -1L else 1L
    val value = sign * (abs(w) * d + n)
    return Fraction(value, d)
}

private fun mixedString(f: Fraction): String {
    val r = f.reduced()
    val whole = r.num / r.den
    val remain = abs(r.num % r.den)
    return when {
        whole != 0L && remain != 0L -> "$whole ${remain}/${r.den}"
        whole != 0L -> "$whole"
        remain != 0L -> "${r.num}/${r.den}"
        else -> "0"
    }
}

/** 该分数是否能化成有限小数 */
private fun terminating(f: Fraction): Boolean {
    var d = f.reduced().den
    while (d % 2L == 0L) d /= 2L
    while (d % 5L == 0L) d /= 5L
    return d == 1L
}

private fun decimalString(f: Fraction): String {
    val r = f.reduced()
    return BigDecimal(r.num)
        .divide(BigDecimal(r.den), 12, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}

@Composable
fun FractionTool() {
    var w1 by remember { mutableStateOf("") }
    var n1 by remember { mutableStateOf("3") }
    var d1 by remember { mutableStateOf("4") }
    var w2 by remember { mutableStateOf("") }
    var n2 by remember { mutableStateOf("1") }
    var d2 by remember { mutableStateOf("2") }
    var op by remember { mutableStateOf("＋") }
    var result by remember { mutableStateOf("") }
    var decimal by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun calc() {
        error = ""
        result = ""
        decimal = ""
        val a = buildFraction(w1, n1, d1) ?: run { error = "分数 1 填写有误：整数、分子需为整数，分母不能为 0"; return }
        val b = buildFraction(w2, n2, d2) ?: run { error = "分数 2 填写有误：整数、分子需为整数，分母不能为 0"; return }
        val value = when (op) {
            "＋" -> Fraction(a.num * b.den + b.num * a.den, a.den * b.den)
            "−" -> Fraction(a.num * b.den - b.num * a.den, a.den * b.den)
            "×" -> Fraction(a.num * b.num, a.den * b.den)
            else -> {
                if (b.num == 0L) { error = "除数不能为 0"; return }
                Fraction(a.num * b.den, a.den * b.num)
            }
        }
        val r = value.reduced()
        result = "假分数：${r.num}/${r.den}\n" +
            "带分数：${mixedString(r)}"
        decimal = if (terminating(r)) "${decimalString(r)}" else "≈ ${decimalString(r)}"
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "分数 1（可不填整数；只填分子时表示整数）") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(w1, { w1 = it }, "整数部分", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                LabeledField(n1, { n1 = it }, "分子", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                LabeledField(d1, { d1 = it }, "分母", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
        }
        SectionCard(title = "运算") {
            ChoiceChips(listOf("＋", "−", "×", "÷"), op, { op = it }, { it })
        }
        SectionCard(title = "分数 2") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(w2, { w2 = it }, "整数部分", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                LabeledField(n2, { n2 = it }, "分子", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                LabeledField(d2, { d2 = it }, "分母", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
        }
        Button(onClick = { calc() }, modifier = Modifier.fillMaxWidth()) { Text("计算") }
        ErrorText(error)
        if (result.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(result, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "小数形式：$decimal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
                CopyButton("$result\n小数形式：$decimal")
            }
        }
    }
}
