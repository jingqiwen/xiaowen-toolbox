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
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlin.math.sqrt

private fun fmt2(v: Double): String = ComplexCalc.formatComplex(v, 0.0)

@Composable
fun EquationTool() {
    var mode by remember { mutableStateOf("一元一次方程 ax + b = 0") }
    var a by remember { mutableStateOf("2") }
    var b by remember { mutableStateOf("-8") }
    var c by remember { mutableStateOf("1") }
    var result by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }

    fun solve() {
        error = ""
        result = emptyList()
        val av = a.toDoubleOrNull() ?: run { error = "请输入有效的 a"; return }
        val bv = b.toDoubleOrNull() ?: run { error = "请输入有效的 b"; return }
        if (mode.startsWith("一元一次")) {
            if (av == 0.0) {
                error = if (bv == 0.0) "a = 0 且 b = 0，方程有无穷多解" else "a = 0 且 b ≠ 0，方程无解"
                return
            }
            val x = -bv / av
            result = listOf("x = ${fmt2(x)}")
        } else {
            val cv = c.toDoubleOrNull() ?: run { error = "请输入有效的 c"; return }
            if (av == 0.0) { error = "二次项系数 a 不能为 0"; return }
            val delta = bv * bv - 4 * av * cv
            if (delta >= 0) {
                val x1 = (-bv + sqrt(delta)) / (2 * av)
                val x2 = (-bv - sqrt(delta)) / (2 * av)
                result = listOf("判别式 Δ = ${fmt2(delta)}", "x₁ = ${fmt2(x1)}", "x₂ = ${fmt2(x2)}")
            } else {
                val re = -bv / (2 * av)
                val im = sqrt(-delta) / (2 * av)
                result = listOf(
                    "判别式 Δ = ${fmt2(delta)} < 0，有两个共轭复数根",
                    "x₁ = ${ComplexCalc.formatComplex(re, im)}",
                    "x₂ = ${ComplexCalc.formatComplex(re, -im)}"
                )
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "方程类型") {
            ChoiceChips(
                options = listOf("一元一次方程 ax + b = 0", "一元二次方程 ax² + bx + c = 0"),
                selected = mode,
                onSelect = { mode = it },
                label = { it }
            )
        }
        SectionCard(title = "系数") {
            LabeledField(a, { a = it }, "a", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(b, { b = it }, "b", keyboardType = KeyboardType.Decimal)
            if (mode.startsWith("一元二次")) {
                Spacer(Modifier.height(8.dp))
                LabeledField(c, { c = it }, "c", keyboardType = KeyboardType.Decimal)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { solve() }, modifier = Modifier.fillMaxWidth()) { Text("求解") }
        }
        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "解") {
                result.forEach { line ->
                    Text(line, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
