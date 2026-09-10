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
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
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

/** 支持 "3/4"、"7/2"、"1 2/3"（带分数）、"-5/8" 等格式。 */
private fun parseFraction(text: String): Fraction? {
    var s = text.trim().replace("，", " ")
    if (s.isEmpty()) return null
    var whole = 0L
    val parts = s.split(Regex("\\s+"))
    if (parts.size == 2) {
        whole = parts[0].toLongOrNull() ?: return null
        s = parts[1]
    } else if (parts.size > 2) return null
    val negative = s.startsWith("-")
    if (negative) s = s.drop(1)
    val fracParts = s.split("/")
    val num: Long
    val den: Long
    if (fracParts.size == 1) {
        num = fracParts[0].toLongOrNull() ?: return null
        den = 1L
    } else if (fracParts.size == 2) {
        num = fracParts[0].toLongOrNull() ?: return null
        den = fracParts[1].toLongOrNull() ?: return null
        if (den == 0L) return null
    } else return null
    var n = if (whole >= 0) whole * den + num else -((-whole) * den + num)
    if (negative) n = -n
    return Fraction(n, den)
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

@Composable
fun FractionTool() {
    var f1 by remember { mutableStateOf("3/4") }
    var f2 by remember { mutableStateOf("1/2") }
    var op by remember { mutableStateOf("＋") }
    var result by remember { mutableStateOf("") }
    var decimal by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun calc() {
        error = ""
        result = ""
        decimal = ""
        val a = parseFraction(f1) ?: run { error = "分数 1 格式错误（示例：3/4 或 1 2/3）"; return }
        val b = parseFraction(f2) ?: run { error = "分数 2 格式错误（示例：3/4 或 1 2/3）"; return }
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
        result = "假分数：${r.num}/${r.den}\n带分数：${mixedString(r)}"
        decimal = "≈ ${r.num.toDouble() / r.den}"
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "分数四则运算（自动约分）") {
            LabeledField(f1, { f1 = it }, "分数 1（如 3/4 或 1 2/3）")
            Spacer(Modifier.height(8.dp))
            ChoiceChips(listOf("＋", "−", "×", "÷"), op, { op = it }, { it })
            Spacer(Modifier.height(8.dp))
            LabeledField(f2, { f2 = it }, "分数 2（如 5/6）")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { calc() }, modifier = Modifier.fillMaxWidth()) { Text("计算") }
        }
        ErrorText(error)
        if (result.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(result, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(decimal, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
