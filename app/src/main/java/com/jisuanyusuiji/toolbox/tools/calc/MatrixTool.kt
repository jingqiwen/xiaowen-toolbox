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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.ResultText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import java.math.BigDecimal
import java.math.RoundingMode

private fun f(v: Double): String = try {
    BigDecimal(v).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
} catch (_: Exception) { v.toString() }

private fun parseMatrix(values: List<String>, size: Int): Array<DoubleArray>? {
    val m = Array(size) { DoubleArray(size) }
    for (i in 0 until size * size) {
        val v = values[i].trim().toDoubleOrNull() ?: return null
        m[i / size][i % size] = v
    }
    return m
}

private fun formatMatrix(m: Array<DoubleArray>): String =
    m.joinToString("\n") { row -> row.joinToString("  ") { f(it) } }

private fun det2(m: Array<DoubleArray>) = m[0][0] * m[1][1] - m[0][1] * m[1][0]

private fun det3(m: Array<DoubleArray>): Double {
    fun d(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double, g: Double, h: Double, i: Double) =
        a * e * i + b * f * g + c * d * h - c * e * g - b * d * i - a * f * h
    return d(m[0][0], m[0][1], m[0][2], m[1][0], m[1][1], m[1][2], m[2][0], m[2][1], m[2][2])
}

private fun inverse(m: Array<DoubleArray>): Array<DoubleArray>? {
    val n = m.size
    val det = if (n == 2) det2(m) else det3(m)
    if (kotlin.math.abs(det) < 1e-12) return null
    return if (n == 2) {
        arrayOf(
            doubleArrayOf(m[1][1] / det, -m[0][1] / det),
            doubleArrayOf(-m[1][0] / det, m[0][0] / det)
        )
    } else {
        val inv = Array(3) { DoubleArray(3) }
        for (i in 0 until 3) for (j in 0 until 3) {
            val a = m[(i + 1) % 3][(j + 1) % 3]
            val b = m[(i + 1) % 3][(j + 2) % 3]
            val c = m[(i + 2) % 3][(j + 1) % 3]
            val d = m[(i + 2) % 3][(j + 2) % 3]
            val cofactor = a * d - b * c
            inv[j][i] = cofactor / det // 转置伴随
        }
        inv
    }
}

private fun operate(op: String, a: Array<DoubleArray>, b: Array<DoubleArray>): String {
    val n = a.size
    return when (op) {
        "行列式 |A|" -> "det(A) = ${f(if (n == 2) det2(a) else det3(a))}"
        "逆矩阵 A⁻¹" -> {
            val inv = inverse(a)
            if (inv == null) "矩阵不可逆（行列式为 0）" else formatMatrix(inv)
        }
        "A + B", "A − B", "A × B" -> {
            val c = Array(n) { DoubleArray(n) }
            for (i in 0 until n) for (j in 0 until n) {
                c[i][j] = when (op) {
                    "A + B" -> a[i][j] + b[i][j]
                    "A − B" -> a[i][j] - b[i][j]
                    else -> (0 until n).sumOf { k -> a[i][k] * b[k][j] }
                }
            }
            formatMatrix(c)
        }
        else -> ""
    }
}

@Composable
fun MatrixTool() {
    var size by remember { mutableStateOf(2) }
    var a by remember(size) { mutableStateOf(List(size * size) { if (it % (size + 1) == 0) "1" else "0" }) }
    var b by remember(size) { mutableStateOf(List(size * size) { if (it % (size + 1) == 0) "1" else "0" }) }
    var op by remember { mutableStateOf("行列式 |A|") }
    var result by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    val ops = if (size == 2) listOf("行列式 |A|", "逆矩阵 A⁻¹", "A + B", "A − B", "A × B")
    else listOf("行列式 |A|", "逆矩阵 A⁻¹", "A + B", "A − B", "A × B")

    fun run() {
        error = ""
        result = ""
        val ma = parseMatrix(a, size) ?: run { error = "矩阵 A 存在无效数字"; return }
        val needB = op in listOf("A + B", "A − B", "A × B")
        val mb = if (needB) parseMatrix(b, size) ?: run { error = "矩阵 B 存在无效数字"; return } else ma
        result = operate(op, ma, mb)
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "矩阵阶数") {
            ChoiceChips(listOf(2, 3), size, { size = it }, { "${it}×${it} 阶" })
        }
        SectionCard(title = "运算") {
            ChoiceChips(ops, op, { op = it }, { it })
        }
        SectionCard(title = "矩阵 A") {
            MatrixInput(size, a) { a = it }
        }
        if (op in listOf("A + B", "A − B", "A × B")) {
            SectionCard(title = "矩阵 B") {
                MatrixInput(size, b) { b = it }
            }
        }
        androidx.compose.material3.Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) { Text("计算") }
        ErrorText(error)
        if (result.isNotBlank()) {
            SectionCard(title = "结果") {
                ResultText(result)
                Spacer(Modifier.height(10.dp))
                CopyButton(result)
            }
        }
    }
}

@Composable
private fun MatrixInput(size: Int, values: List<String>, onChange: (List<String>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until size) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (j in 0 until size) {
                    val index = i * size + j
                    OutlinedTextField(
                        value = values[index],
                        onValueChange = { text ->
                            onChange(values.toMutableList().also { it[index] = text })
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
        }
    }
}
