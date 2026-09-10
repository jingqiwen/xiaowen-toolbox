package com.jisuanyusuiji.toolbox.tools.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.ResultText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import com.jisuanyusuiji.toolbox.ui.components.Stepper
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

private fun f(v: Double): String = try {
    BigDecimal(v).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
} catch (_: Exception) { v.toString() }

private fun parseMatrix(values: List<String>, rows: Int, cols: Int): Array<DoubleArray>? {
    val m = Array(rows) { DoubleArray(cols) }
    for (i in 0 until rows * cols) {
        val v = values[i].trim().toDoubleOrNull() ?: return null
        m[i / cols][i % cols] = v
    }
    return m
}

private fun formatMatrix(m: Array<DoubleArray>): String =
    m.joinToString("\n") { row -> "[ " + row.joinToString("  ") { f(it) } + " ]" }

private fun identity(n: Int) = Array(n) { i -> DoubleArray(n) { j -> if (i == j) 1.0 else 0.0 } }

/** 行最简形（RREF），返回矩阵与主元列。 */
private fun rref(input: Array<DoubleArray>): Pair<Array<DoubleArray>, List<Int>> {
    val m = Array(input.size) { input[it].copyOf() }
    val rows = m.size
    val cols = if (rows > 0) m[0].size else 0
    val pivots = mutableListOf<Int>()
    var row = 0
    var col = 0
    while (row < rows && col < cols) {
        var pivot = row
        for (i in row until rows) if (abs(m[i][col]) > abs(m[pivot][col])) pivot = i
        if (abs(m[pivot][col]) < 1e-12) { col++; continue }
        val tmp = m[row]; m[row] = m[pivot]; m[pivot] = tmp
        val div = m[row][col]
        for (j in 0 until cols) m[row][j] /= div
        for (i in 0 until rows) {
            if (i != row && abs(m[i][col]) > 1e-12) {
                val factor = m[i][col]
                for (j in 0 until cols) m[i][j] -= factor * m[row][j]
            }
        }
        pivots.add(col)
        row++; col++
    }
    return m to pivots
}

private fun determinant(m: Array<DoubleArray>): Double {
    val n = m.size
    val a = Array(n) { m[it].copyOf() }
    var det = 1.0
    for (i in 0 until n) {
        var pivot = i
        for (j in i + 1 until n) if (abs(a[j][i]) > abs(a[pivot][i])) pivot = j
        if (abs(a[pivot][i]) < 1e-12) return 0.0
        if (pivot != i) { val t = a[i]; a[i] = a[pivot]; a[pivot] = t; det = -det }
        det *= a[i][i]
        for (j in i + 1 until n) {
            val factor = a[j][i] / a[i][i]
            for (k in i until n) a[j][k] -= factor * a[i][k]
        }
    }
    return det
}

private fun inverse(m: Array<DoubleArray>): Array<DoubleArray>? {
    val n = m.size
    val aug = Array(n) { i -> DoubleArray(2 * n) { j -> if (j < n) m[i][j] else if (j - n == i) 1.0 else 0.0 } }
    for (i in 0 until n) {
        var pivot = i
        for (j in i + 1 until n) if (abs(aug[j][i]) > abs(aug[pivot][i])) pivot = j
        if (abs(aug[pivot][i]) < 1e-12) return null
        if (pivot != i) { val t = aug[i]; aug[i] = aug[pivot]; aug[pivot] = t }
        val div = aug[i][i]
        for (j in 0 until 2 * n) aug[i][j] /= div
        for (j in 0 until n) {
            if (j != i && abs(aug[j][i]) > 1e-12) {
                val factor = aug[j][i]
                for (k in 0 until 2 * n) aug[j][k] -= factor * aug[i][k]
            }
        }
    }
    return Array(n) { i -> DoubleArray(n) { j -> aug[i][j + n] } }
}

private fun rank(m: Array<DoubleArray>): Int = rref(m).second.size

private fun solveLinear(a: Array<DoubleArray>, b: DoubleArray): String {
    val n = a.size
    if (b.size != n) return "向量 b 的长度必须等于矩阵 A 的行数"
    val aug = Array(n) { i -> DoubleArray(n + 1) { j -> if (j < n) a[i][j] else b[i] } }
    val (r, pivots) = rref(aug)
    if (pivots.contains(n)) return "方程组无解（出现 0 = 非零）"
    if (pivots.size < n) return "方程组有无穷多解（自由变量 ${n - pivots.size} 个）"
    val x = DoubleArray(n)
    for (i in 0 until n) x[i] = r[i][n]
    return "唯一解：\n" + x.mapIndexed { i, v -> "x${i + 1} = ${f(v)}" }.joinToString("\n")
}

@Composable
fun MatrixTool() {
    var aRows by remember { mutableStateOf(2) }
    var aCols by remember { mutableStateOf(2) }
    var bRows by remember { mutableStateOf(2) }
    var bCols by remember { mutableStateOf(2) }
    var aValues by remember(aRows, aCols) {
        mutableStateOf(List(aRows * aCols) { if (it % (aCols + 1) == 0) "1" else "0" })
    }
    var bValues by remember(bRows, bCols) {
        mutableStateOf(List(bRows * bCols) { if (it % (bCols + 1) == 0) "1" else "0" })
    }
    var bVector by remember(aRows) { mutableStateOf(List(aRows) { "0" }) }
    var scalar by remember { mutableStateOf("2") }
    var op by remember { mutableStateOf("A + B") }
    var result by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    val ops = listOf(
        "A + B", "A − B", "A × B", "kA 数乘", "Aᵀ 转置",
        "det(A) 行列式", "A⁻¹ 逆矩阵", "rank(A) 秩", "tr(A) 迹", "RREF 行最简", "解 Ax=b"
    )

    fun needB(): Boolean = op in listOf("A + B", "A − B", "A × B")
    fun isSquare(): Boolean = aRows == aCols

    fun compute() {
        error = ""
        result = ""
        val a = parseMatrix(aValues, aRows, aCols) ?: run { error = "矩阵 A 存在无效数字"; return }
        val b = if (needB()) parseMatrix(bValues, bRows, bCols) ?: run { error = "矩阵 B 存在无效数字"; return } else a
        when (op) {
            "A + B", "A − B" -> {
                if (aRows != bRows || aCols != bCols) { error = "加减法要求 A、B 维度完全相同"; return }
                val c = Array(aRows) { i -> DoubleArray(aCols) { j ->
                    if (op == "A + B") a[i][j] + b[i][j] else a[i][j] - b[i][j]
                } }
                result = formatMatrix(c)
            }
            "A × B" -> {
                if (aCols != bRows) { error = "矩阵乘法要求 A 的列数 = B 的行数（当前 ${aCols} ≠ ${bRows}）"; return }
                val c = Array(aRows) { i -> DoubleArray(bCols) { j ->
                    (0 until aCols).sumOf { k -> a[i][k] * b[k][j] }
                } }
                result = formatMatrix(c)
            }
            "kA 数乘" -> {
                val k = scalar.trim().toDoubleOrNull() ?: run { error = "请输入有效的数乘系数 k"; return }
                val c = Array(aRows) { i -> DoubleArray(aCols) { j -> k * a[i][j] } }
                result = formatMatrix(c)
            }
            "Aᵀ 转置" -> {
                val t = Array(aCols) { i -> DoubleArray(aRows) { j -> a[j][i] } }
                result = formatMatrix(t)
            }
            "det(A) 行列式" -> {
                if (!isSquare()) { error = "只有方阵才有行列式"; return }
                result = "det(A) = ${f(determinant(a))}"
            }
            "A⁻¹ 逆矩阵" -> {
                if (!isSquare()) { error = "只有方阵才可能求逆"; return }
                val inv = inverse(a)
                result = if (inv == null) "矩阵不可逆（det = 0）" else formatMatrix(inv)
            }
            "rank(A) 秩" -> result = "rank(A) = ${rank(a)}"
            "tr(A) 迹" -> {
                if (!isSquare()) { error = "只有方阵才有迹"; return }
                result = "tr(A) = ${f((0 until aRows).sumOf { a[it][it] })}"
            }
            "RREF 行最简" -> {
                val (r, pivots) = rref(a)
                result = formatMatrix(r) + "\n主元列：" + pivots.joinToString(", ") { "第${it + 1}列" }
            }
            "解 Ax=b" -> {
                if (!isSquare()) { error = "解方程组要求 A 为方阵"; return }
                val bVec = DoubleArray(aRows) { bVector[it].trim().toDoubleOrNull() ?: Double.NaN }
                if (bVec.any { it.isNaN() }) { error = "向量 b 存在无效数字"; return }
                result = solveLinear(a, bVec)
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "矩阵维度（1~6 阶）") {
            Stepper("A 行数", aRows, { aRows = it }, 1..6)
            Stepper("A 列数", aCols, { aCols = it }, 1..6)
            if (needB()) {
                Spacer(Modifier.height(6.dp))
                Stepper("B 行数", bRows, { bRows = it }, 1..6)
                Stepper("B 列数", bCols, { bCols = it }, 1..6)
            }
        }

        SectionCard(title = "选择运算") {
            ChoiceChips(ops, op, { op = it }, { it })
        }

        SectionCard(title = "矩阵 A（${aRows}×${aCols}）") {
            MatrixInput(aRows, aCols, aValues) { aValues = it }
        }

        if (needB()) {
            SectionCard(title = "矩阵 B（${bRows}×${bCols}）") {
                MatrixInput(bRows, bCols, bValues) { bValues = it }
            }
        }

        if (op == "kA 数乘") {
            SectionCard(title = "数乘系数 k") {
                OutlinedTextField(
                    value = scalar,
                    onValueChange = { scalar = it },
                    label = { Text("k =") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (op == "解 Ax=b") {
            SectionCard(title = "向量 b（长度 ${aRows}）") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until aRows) {
                        OutlinedTextField(
                            value = bVector.getOrElse(i) { "0" },
                            onValueChange = { text ->
                                bVector = bVector.toMutableList().also { if (i < it.size) it[i] = text }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
            }
        }

        Button(onClick = { compute() }, modifier = Modifier.fillMaxWidth()) { Text("计算") }
        ErrorText(error)
        if (result.isNotBlank()) {
            SectionCard(title = "结果") {
                ResultText(result)
                Spacer(Modifier.height(10.dp))
                CopyButton(result)
            }
        }

        SectionCard(title = "📘 线代知识点") {
            Text(
                "• 矩阵加法/减法：要求同型矩阵，对应元素相加减。\n" +
                    "• 矩阵乘法：A(m×n) × B(n×p) = C(m×p)，Cᵢⱼ = Σ AᵢₖBₖⱼ，不满足交换律。\n" +
                    "• 转置：行列互换，(AB)ᵀ = BᵀAᵀ。\n" +
                    "• 行列式：只有方阵有；det=0 说明矩阵不可逆、行向量线性相关。\n" +
                    "• 逆矩阵：A A⁻¹ = I；只有 det≠0 时才存在。\n" +
                    "• 秩：矩阵中线性无关的行（列）的最大个数，等于行最简形主元个数。\n" +
                    "• 迹：主对角线元素之和，tr(A) = Σaᵢᵢ。\n" +
                    "• 解 Ax=b：无解 / 唯一解 / 无穷多解三种情况，由秩与增广矩阵秩决定。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MatrixInput(rows: Int, cols: Int, values: List<String>, onChange: (List<String>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (j in 0 until cols) {
                    val index = i * cols + j
                    OutlinedTextField(
                        value = values.getOrElse(index) { "0" },
                        onValueChange = { text ->
                            val list = values.toMutableList()
                            while (list.size <= index) list.add("0")
                            list[index] = text
                            onChange(list)
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
